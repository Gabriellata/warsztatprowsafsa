# WarsztatPro

Lokalna aplikacja desktopowa do obsługi jednoosobowego warsztatu. Uruchamia wyłącznie graficzne okno Swing; nie wymaga serwera ani przeglądarki. Aplikacja nie ma logowania, kont użytkowników ani systemu uprawnień.

## Wymagania i uruchomienie

- Java 21
- Maven 3.8 lub nowszy

Zbuduj aplikację z katalogu projektu:

```text
mvn -Dmaven.test.skip=true package
```

Uruchom wersję desktopową:

```text
java -jar target/WarsztatPro-1.0-SNAPSHOT.jar
```

Interfejs desktopowy korzysta z Swing dostępnego w JDK 21, więc nie wymaga dodatkowych bibliotek GUI.

## Moduły desktopowe

- **Klienci** — lista, wyszukiwanie, dodawanie, edycja, historia klienta i usuwanie zgodnie z ograniczeniami powiązań.
- **Pojazdy** — lista, wyszukiwanie, dodawanie z klientem lub bez przypisania, edycja, historia oraz bezpieczne usuwanie.
- **Usługi i cennik** — dodawanie, edycja, wyszukiwanie, aktywacja i dezaktywacja usług; ceny dla małych, średnich i dużych pojazdów.
- **Zlecenia** — tworzenie, edycja, status, pozycje usług i ich ceny, anulowanie oraz powiązania z klientem, pojazdem i terminem wizyty.
- **Terminarz** — miesięczny kalendarz z wyróżnionymi dniami i statusami wizyt, przełączaniem miesięcy i przyciskiem „Dzisiaj”; termin zlecenia tworzy jedno zdarzenie, a jego edycja aktualizuje oba widoki. Usunięcie wpisu usuwa termin ze zlecenia, ale nie samo zlecenie.
- **Raporty** — liczba zleceń, statusy, kategorie usług, zapisane wartości usług i zleceń oraz stan realizacji w wybranym zakresie dat.
- **Konfiguracja** — aktywowanie i dezaktywowanie dostępnych typów pojazdów; skróty do modułów usług i stanowisk.
- **Stanowiska robocze** — dodawanie, edycja i dezaktywacja bez usuwania powiązanych wizyt.
- **Kopie zapasowe** — tworzenie i sprawdzanie lokalnych kopii bazy.

Listy można odświeżać i przeszukiwać. Formularze wyboru klienta, pojazdu, usługi i zlecenia pokazują nazwy zamiast technicznych identyfikatorów. W formularzu firmy NIP jest wymagany i sprawdzany, a w kartotece klientów można go odczytać i wyszukać. Właściciel pojazdu jest opcjonalny; zlecenie i wizyta mogą być powiązane z pojazdem bez przypisanego klienta.

W formularzach klawisze `Alt` + `↑` / `↓` przechodzą do poprzedniego lub następnego pola tekstowego. Listy wyboru, tabele i kalendarz zachowują standardowe działanie strzałek; nawigacja między dniami w kalendarzu odbywa się strzałkami na przycisku daty. `Tab` przechodzi między kontrolkami, `Enter` aktywuje przycisk, a `Esc` zamyka lub anuluje okno dialogowe.

Formularze i operacje bazodanowe używają istniejących serwisów aplikacji; czasochłonne odczyty i zapisy nie blokują okna. Walidacja i ograniczenia historii pozostają w warstwie serwisowej.

## Dane i znane ograniczenia

Baza SQLite znajduje się domyślnie w `data/warsztatpro.db`, a kopie w `backups/`. Przy starcie aplikacja sprawdza schemat i stosuje brakujące migracje bez zastępowania bazy użytkownika. Można wskazać inną bazę właściwością JVM `-Dwarsztatpro.db.path=ścieżka`.

Pozycje zleceń zachowują historyczne nazwy i kwoty usług. Kategoria usługi w raportach pochodzi z bieżącego katalogu, bo jej migawka nie jest zapisywana w pozycji. Typy pojazdów oraz statusy zleceń i wizyt są stałymi zestawami; typ pojazdu można wyłączyć z nowych wyborów, ale nie można dodawać własnych typów. Kategorie napraw obejmują olejowe, eksploatacyjne, mechaniczne, lakiernicze i blacharskie, konserwacyjne, wulkanizacyjne oraz diagnostyczne.

Wartości raportów dotyczą zleceń według daty przyjęcia. Raport opóźnień obejmuje niezakończone i nieanulowane zlecenia po planowanym terminie. Eksport raportów, przywracanie kopii zapasowych i edycja danych kartotekowych poza polami udostępnionymi w formularzach nie są dostępne.

Testy integracyjne korzystają z osobnej bazy tymczasowej. Można je uruchomić poleceniem `mvn test`.
