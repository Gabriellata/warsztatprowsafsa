# WarsztatPro 0.1

Konsolowy system do obsługi klientów, pojazdów, usług i zleceń serwisowych.

## Wymagania

- Java 21
- Maven 3.8 lub nowszy

## Uruchomienie

W katalogu projektu wykonaj:

```text
mvn test package
java -jar target/WarsztatPro-1.0-SNAPSHOT.jar
```

Przy pierwszym uruchomieniu aplikacja utworzy `data/warsztatpro.db` oraz brakujące tabele. Menu główne pozwala otworzyć każdy moduł i wrócić do menu nadrzędnego. W menu głównym wybierz `K`, aby utworzyć kopię lub wyświetlić dostępne kopie. Kopie trafiają do `backups/`.

Wszystkie kwoty w bazie są przechowywane w groszach. Zlecenia zachowują migawkę nazwy i ceny usługi; późniejsze zmiany cennika nie zmieniają historii.

Testy integracyjne korzystają z osobnej bazy tymczasowej i nie zmieniają danych w `data/warsztatpro.db`.
