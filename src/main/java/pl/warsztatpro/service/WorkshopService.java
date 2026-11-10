package pl.warsztatpro.service;

import pl.warsztatpro.model.Workshop;
import pl.warsztatpro.repository.WorkshopRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class WorkshopService {
    private final WorkshopRepository workshops;
    public WorkshopService(WorkshopRepository workshops) { this.workshops=workshops; }

    public Workshop create(Workshop workshop)throws SQLException {
        validate(workshop);
        if(workshops.existsByName(workshop.getName(),null))throw new IllegalArgumentException("Stanowisko o tej nazwie już istnieje.");
        workshop.setActive(true); return workshops.insert(workshop);
    }
    public List<Workshop> findAll()throws SQLException{return workshops.findAll();}
    public List<Workshop> findActive()throws SQLException{return workshops.findActive();}
    public Optional<Workshop> findById(long id)throws SQLException{return workshops.findById(id);}
    public Workshop update(Workshop workshop)throws SQLException {
        if(workshop.getId()==null||workshops.findById(workshop.getId()).isEmpty())throw new IllegalArgumentException("Nie znaleziono stanowiska.");
        validate(workshop);
        if(workshops.existsByName(workshop.getName(),workshop.getId()))throw new IllegalArgumentException("Stanowisko o tej nazwie już istnieje.");
        if(!workshops.update(workshop))throw new IllegalArgumentException("Nie udało się zaktualizować stanowiska.");
        return workshops.findById(workshop.getId()).orElse(workshop);
    }
    public void deactivate(long id)throws SQLException {
        Workshop workshop=workshops.findById(id).orElseThrow(()->new IllegalArgumentException("Nie znaleziono stanowiska."));
        if(!workshop.isActive())throw new IllegalArgumentException("Stanowisko jest już nieaktywne.");
        if(!workshops.deactivate(id))throw new IllegalArgumentException("Nie udało się dezaktywować stanowiska.");
    }
    private static void validate(Workshop workshop) {
        if(workshop==null||workshop.getName()==null||workshop.getName().isBlank())throw new IllegalArgumentException("Nazwa stanowiska jest wymagana.");
        workshop.setName(workshop.getName().trim());
        if(workshop.getName().length()>100)throw new IllegalArgumentException("Nazwa stanowiska może mieć maksymalnie 100 znaków.");
        if(workshop.getDescription()!=null&&workshop.getDescription().isBlank())workshop.setDescription(null);
        if(workshop.getDescription()!=null&&workshop.getDescription().length()>500)throw new IllegalArgumentException("Opis stanowiska może mieć maksymalnie 500 znaków.");
    }
}
