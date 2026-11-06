# DripeuszekBomba (Fabric, Minecraft 1.21.4)

Mod kliencki. Klawisz **.** (kropka) otwiera menu z zakładkami po lewej:
Gracze, Ustawienia, Motywy, Licencja. Klawisz zmienisz w Opcje > Sterowanie.

## Budowanie
Wymagane: JDK 21 i Gradle 8.11+ (albo wygeneruj wrapper: `gradle wrapper`).

    gradle build

Gotowy plik: `build/libs/DripeuszekBomba-1.0.0.jar` (NIE `-sources.jar`).
Wrzuć do `.minecraft/mods` razem z Fabric Loader 1.21.4 i Fabric API.

## Budowanie na GitHubie (bez instalowania czegokolwiek)
1. Załóż konto na github.com i utwórz nowe repozytorium (New repository).
2. Wgraj do niego WSZYSTKIE pliki z tego folderu, razem z folderem `.github`.
3. Wejdź w zakładkę Actions, poczekaj na zielony znaczek (kilka minut).
4. Kliknij ukończone uruchomienie i pobierz "DripeuszekBomba" z sekcji Artifacts.
5. W zipie weź plik BEZ "-sources" w nazwie i wrzuć do .minecraft/mods.
