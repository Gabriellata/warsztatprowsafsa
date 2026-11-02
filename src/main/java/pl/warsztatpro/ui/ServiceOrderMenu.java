package pl.warsztatpro.ui;

import pl.warsztatpro.model.*;
import pl.warsztatpro.service.ServiceOrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ServiceOrderMenu {
    private final ServiceOrderService service;
    private final Scanner scanner;
    public ServiceOrderMenu(ServiceOrderService service, Scanner scanner) { this.service=service;this.scanner=scanner; }

    public void run() {
        boolean running=true;
        while(running){
            System.out.println("--- Zarządzanie zleceniami serwisowymi ---");
            System.out.println("1. Utwórz nowe zlecenie\n2. Wyświetl wszystkie zlecenia\n3. Wyszukaj zlecenie\n4. Wyświetl szczegóły zlecenia\n5. Edytuj zlecenie\n6. Zmień status zlecenia\n7. Zarządzaj usługami w zleceniu\n8. Historia zleceń pojazdu\n9. Historia zleceń klienta\n10. Powrót");
            String choice=read("Wybierz opcję: ");
            try{
                switch(choice){
                    case "1" -> create(); case "2" -> print(service.findAll()); case "3" -> search();
                    case "4" -> details(); case "5" -> edit(); case "6" -> status(); case "7" -> manageItems();
                    case "8" -> print(service.historyForVehicle(id("ID pojazdu: ")));
                    case "9" -> print(service.historyForClient(id("ID klienta: ")));
                    case "10" -> running=false; default -> System.out.println("Wybierz opcję 1-10.");
                }
            }catch(IllegalArgumentException e){System.out.println("Błąd: "+e.getMessage());}
            catch(InputClosedException e){running=false;}
            catch(SQLException e){System.out.println("Błąd bazy danych: "+e.getMessage());}
            if(running)System.out.println();
        }
    }
    private void create()throws SQLException{
        ServiceOrder o=new ServiceOrder();o.setClientId(id("ID klienta: "));o.setVehicleId(id("ID pojazdu klienta: "));
        o.setReceivedDate(date("Data przyjęcia (RRRR-MM-DD, puste = dziś): ",LocalDate.now(),true));
        o.setPlannedCompletionDate(date("Planowany termin (RRRR-MM-DD, opcjonalnie): ",null,false));
        o.setFaultDescription(required("Opis zgłoszonej usterki: "));o.setWorkDescription(optional("Opis wykonanych prac (opcjonalnie): "));
        o.setNotes(optional("Uwagi (opcjonalnie): "));
        if("t".equalsIgnoreCase(read("Dodać usługi do zlecenia teraz? (t/n): "))){
            while(true){
                String raw=read("ID usługi z cennika (puste = koniec): ");if(raw.isBlank())break;
                long serviceId;try{serviceId=Long.parseLong(raw);if(serviceId<=0)throw new NumberFormatException();}catch(NumberFormatException e){System.out.println("Podaj dodatnie ID usługi.");continue;}
                VehicleSize[] sizes=VehicleSize.values();for(int i=0;i<sizes.length;i++)System.out.println((i+1)+". "+sizes[i]);
                int size=positiveInt("Wielkość pojazdu dla ceny: ");if(size>sizes.length){System.out.println("Niepoprawny wybór.");continue;}
                try{o.getItems().add(service.createItem(serviceId,sizes[size-1],positiveInt("Ilość: ")));}
                catch(IllegalArgumentException e){System.out.println("Błąd: "+e.getMessage());}
            }
        }
        ServiceOrder saved=service.create(o);
        System.out.println("Utworzono zlecenie: "+saved.getOrderNumber());
        if(saved.getItems().isEmpty())System.out.println("Usługi można dodać później przez opcję 7.");
    }
    private void search()throws SQLException{
        String number=required("Numer zlecenia: ");Optional<ServiceOrder> result=service.findByNumber(number);
        if(result.isPresent())System.out.println(result.get());else System.out.println("Nie znaleziono zlecenia.");
    }
    private void details()throws SQLException{
        ServiceOrder o=service.findByNumber(required("Numer zlecenia: ")).orElse(null);
        if(o==null){System.out.println("Nie znaleziono zlecenia.");return;}
        System.out.println(o);System.out.println("Usterka: "+o.getFaultDescription());System.out.println("Wykonane prace: "+display(o.getWorkDescription()));
        System.out.println("Termin: "+display(o.getPlannedCompletionDate())+" | zakończono: "+display(o.getActualCompletionDate()));System.out.println("Uwagi: "+display(o.getNotes()));
        if(o.getItems().isEmpty())System.out.println("Brak usług.");else o.getItems().forEach(System.out::println);
    }
    private void edit()throws SQLException{
        ServiceOrder o=byNumber();if(o==null)return;
        o.setReceivedDate(date("Data przyjęcia ["+o.getReceivedDate()+"]: ",o.getReceivedDate(),true));
        o.setPlannedCompletionDate(date("Planowany termin ["+display(o.getPlannedCompletionDate())+"]: ",o.getPlannedCompletionDate(),true));
        o.setFaultDescription(readKeep("Opis usterki",o.getFaultDescription()));o.setWorkDescription(readKeep("Opis wykonanych prac",o.getWorkDescription()));o.setNotes(readKeep("Uwagi",o.getNotes()));
        System.out.println("Aktualny klient "+o.getClientId()+", pojazd "+o.getVehicleId()+". Zmień? (t/n)");
        if("t".equalsIgnoreCase(read("Wybór: "))){o.setClientId(id("ID klienta: "));o.setVehicleId(id("ID pojazdu tego klienta: "));}
        service.update(o);System.out.println("Zaktualizowano zlecenie.");
    }
    private void status()throws SQLException{
        ServiceOrder o=byNumber();if(o==null)return;ServiceOrderStatus[] values=ServiceOrderStatus.values();
        for(int i=0;i<values.length;i++)System.out.println((i+1)+". "+values[i]);
        int n=positiveInt("Nowy status: ");if(n>values.length){System.out.println("Niepoprawny status.");return;}service.changeStatus(o.getId(),values[n-1]);System.out.println("Zmieniono status.");
    }
    private void manageItems()throws SQLException{
        ServiceOrder o=byNumber();if(o==null)return;boolean loop=true;
        while(loop){System.out.println("Zlecenie "+o.getOrderNumber()+" | koszt "+money(o.getTotalCost()));for(OrderItem i:service.items(o.getId()))System.out.println(i);
            System.out.println("1. Dodaj usługę z cennika\n2. Edytuj ilość/cenę pozycji\n3. Usuń pozycję\n4. Usuń puste zlecenie\n5. Wróć");
            switch(read("Wybór: ")){
                case "1" -> addItem(o);case "2" -> editItem(o);case "3" -> service.removeItem(o.getId(),id("ID pozycji: "));
                case "4" -> {service.deleteIfEmpty(o.getId());System.out.println("Usunięto puste zlecenie.");return;}
                case "5" -> loop=false;default -> System.out.println("Wybierz opcję 1-5.");
            }
            o=service.findById(o.getId()).orElse(o);
        }
    }
    private void addItem(ServiceOrder o)throws SQLException{
        long serviceId=id("ID aktywnej usługi z cennika: ");VehicleSize[] sizes=VehicleSize.values();
        for(int i=0;i<sizes.length;i++)System.out.println((i+1)+". "+sizes[i]);int n=positiveInt("Wielkość pojazdu dla cennika: ");
        if(n>sizes.length){System.out.println("Niepoprawny wybór.");return;}int quantity=positiveInt("Ilość: ");OrderItem item=service.addItem(o.getId(),serviceId,sizes[n-1],quantity);System.out.println("Dodano migawkę ceny: "+item);
    }
    private void editItem(ServiceOrder o)throws SQLException{
        List<OrderItem> list=service.items(o.getId());if(list.isEmpty()){System.out.println("Brak pozycji.");return;}
        long id=id("ID pozycji: ");OrderItem item=list.stream().filter(i->i.getId()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Nie znaleziono pozycji."));
        item.setQuantity(optionalPositiveInt("Nowa ilość ["+item.getQuantity()+"]: ",item.getQuantity()));item.setUnitPrice(price("Nowa cena jednostkowa ["+money(item.getUnitPrice())+"]: ",item.getUnitPrice()));service.editItem(o.getId(),item);System.out.println("Zaktualizowano pozycję.");
    }
    private ServiceOrder byNumber()throws SQLException{return service.findByNumber(required("Numer zlecenia: ")).orElseGet(()->{System.out.println("Nie znaleziono zlecenia.");return null;});}
    private void print(List<ServiceOrder> list){if(list.isEmpty()){System.out.println("Brak zleceń.");return;}System.out.println("Numer | Klient | Pojazd | Status | Przyjęto | Koszt");list.forEach(System.out::println);}
    private long id(String prompt){while(true){try{long value=Long.parseLong(read(prompt));if(value>0)return value;}catch(NumberFormatException ignored){}System.out.println("Podaj dodatnie ID.");}}
    private int positiveInt(String prompt){while(true){try{int value=Integer.parseInt(read(prompt));if(value>0)return value;}catch(NumberFormatException ignored){}System.out.println("Podaj liczbę większą od zera.");}}
    private int optionalPositiveInt(String prompt,int current){while(true){String raw=read(prompt);if(raw.isBlank())return current;try{int value=Integer.parseInt(raw);if(value>0)return value;}catch(NumberFormatException ignored){}System.out.println("Podaj liczbę większą od zera.");}}
    private LocalDate date(String prompt,LocalDate current,boolean blankKeeps){while(true){String v=read(prompt);if(v.isBlank()&&blankKeeps)return current;if(v.isBlank()&&!blankKeeps)return null;try{return LocalDate.parse(v);}catch(Exception e){System.out.println("Użyj formatu RRRR-MM-DD.");}}}
    private long price(String prompt,long current){while(true){String v=read(prompt);if(v.isBlank())return current;try{BigDecimal d=new BigDecimal(v.replace(',','.')).setScale(2,RoundingMode.UNNECESSARY);if(d.signum()>=0)return d.movePointRight(2).longValueExact();}catch(Exception ignored){}System.out.println("Podaj nieujemną kwotę z maksymalnie dwoma miejscami po przecinku.");}}
    private String readKeep(String label,String current){String value=read(label+" ["+display(current)+"]: ");return value.isBlank()?current:value;}
    private String required(String prompt){while(true){String value=read(prompt);if(!value.isBlank())return value;System.out.println("To pole jest wymagane.");}}
    private String optional(String prompt){String v=read(prompt);return v.isBlank()?null:v;}
    private String read(String prompt){System.out.print(prompt);if(!scanner.hasNextLine())throw new InputClosedException();return scanner.nextLine().trim();}
    private static final class InputClosedException extends RuntimeException { }
    private static String money(long grosze){return BigDecimal.valueOf(grosze,2).toPlainString()+" zł";}
    private static String display(Object value){return value==null?"-":value.toString();}
}
