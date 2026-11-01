# Rockstar Client

Mod klienta dla **Minecraft 1.21.4** (Fabric) z ClickGUI oraz modulami: Watermark, TargetHUD, TargetESP,
Fullbright, Sprint, Coordinates, HitSounds, FakePlayer, KillEffect.

- **Otwarcie ClickGUI:** `Prawy Shift`
- **Wymagania:** Minecraft 1.21.4, Fabric Loader >= 0.16, Fabric API, Java 21+

## Instalacja (gracz)
1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/) dla 1.21.4 i [Fabric API](https://modrinth.com/mod/fabric-api).
2. Pobierz `.jar` z zakladki **Releases** i wrzuc do folderu `.minecraft/mods`.

## Budowanie z zrodel
```bash
./gradlew build
```
Gotowy plik: `build/libs/rockstar-client-1.0.0.jar` (plik `*-sources.jar` nie jest do instalacji).

## Automatyczny build na GitHubie
Kazdy push uruchamia GitHub Actions, a gotowy `.jar` jest w **Actions -> ostatni run -> Artifacts**.
Zeby wydac release z `.jar`:
```bash
git tag v1.0.0
git push origin v1.0.0
```

## standalone/RockstarGUI.jar
Osobna aplikacja Swing (podglad wygladu GUI, bez Minecrafta): `java -jar standalone/RockstarGUI.jar`
(Windows: `standalone/Uruchom_RockstarGUI.bat`).

## Licencja
Zobacz [LICENSE](LICENSE).
