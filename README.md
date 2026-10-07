# BetterSouth HUD

Client-HUD für Minecraft Java 26.2 (Fabric) plus Paper-Bridge für die Serverdaten.
Das HUD kann Geld, Wantedpunkte, Drogen-Anzahl, Fraktion, Rang, FPS und Spielzeit
als einzeln verschiebbare und ein-/ausblendbare Widgets anzeigen. Ein
Tasten-Widget zeigt WASD, Leertaste, Shift und Ctrl während des Spielens an;
gedrückte Tasten werden dezent aufgehellt und beim Loslassen sofort wieder
normal dargestellt. Tastendrücke in geöffneten Menüs oder im Chat werden nicht angezeigt.

## Bauen

- Client-Mod: .NET 10 SDK installieren, dann `cd fabric-mod` und
  `.\gradlew.bat build`. Das JAR liegt danach in
  `fabric-mod/build/libs/`.
- Paper-Bridge: `cd paper-bridge` und `mvn package`. Das Plugin-JAR liegt danach in
  `paper-bridge/target/BetterSouth-HUD-Bridge.jar`.

Die Fabric-Mod gehört in den `mods`-Ordner des Spielers; Fabric API muss ebenfalls
installiert sein. Die Bridge gehört in den Paper-Serverordner `plugins`. Auf dem
Server müssen außerdem MoneySystem und Fraktionssystem geladen sein. MoneySystem
benötigt gemäß seiner Plugin-Metadaten zusätzlich LevelSystem.

## Widgets einstellen

Mit `H` öffnet sich zunächst die Studio-Auswahl. Dort kannst du Widgets oder
Hotkeys bearbeiten; `K` öffnet weiterhin direkt das Hotkey-Studio. Im Escape-Menü
gibt es zusätzlich direkt über „Zurück zum Spiel“ den Vanilla-gestalteten Button
„BetterSouth HUD“. Das Schließen-Feld oben rechts ist anklickbar; aus dem
Escape-Menü kehrst du dorthin zurück.
Das Studio ist leicht transparent, sodass das Spiel im Hintergrund sichtbar bleibt.
Links kannst du Widgets ein- oder ausblenden, alle Widgets aktivieren oder das Layout
zurücksetzen. Widgets lassen sich bis auf 50 % verkleinern. In der Vorschau ziehst
du sie zum Positionieren; halte den markierten Seitenrand fest und ziehe, um ein
Widget größer oder kleiner zu machen. Rechts stellst du
für jedes Widget den Hintergrund, die Deckkraft,
die Kontur, das Symbol beziehungsweise Spotify-Cover und die Akzentfarbe ein.
Außerdem kannst du dort Größe und Position zurücksetzen. Mausrad oder Größenregler
passen ebenfalls die Größe an; das Mausrad über dem Deckkraftregler ändert die
Deckkraft. Änderungen werden automatisch in `config/bettersouth-hud.properties` im
Minecraft-Spielverzeichnis gespeichert. Drücke `Esc`, um das Menü zu schließen.

Spieler mit BetterSouth-Mod erhalten in der Spielerliste ein grünes Häkchen.
Diese Markierung erscheint, wenn ihr Client der Server-Bridge seine Anwesenheit
meldet.

## Hotkeys

Drücke `K`, um das Hotkey-Studio zu öffnen. Dort kannst du bis zu neun Hotkeys
benennen, mit einer eigenen Taste belegen, aktivieren oder pausieren und entweder
einen Serverbefehl oder eine Chatnachricht hinterlegen. Beim Drücken der
zugewiesenen Taste sendet der Client den Befehl über die normale Minecraft-
Spielerverbindung an den Server oder die Nachricht über den normalen Chat.
Server-Berechtigungen und Chatregeln gelten weiterhin. Hotkeys funktionieren nur
während des Spiels und werden im Clientprofil gespeichert. Die Tasten belegst
du direkt im Hotkey-Studio über „Taste: … Klicken zum Ändern“.

## Emote-Rad

Mit `G` öffnest du das Emote-Rad. Wähle per Mausklick eines der vier Emotes:
Winken, Jubeln, Klatschen oder Tanzen. Die Paper-Bridge spielt die Animationen
serverseitig ab, sodass auch Spieler ohne BetterSouth-Mod die Armbewegungen,
Partikel und Sounds sehen beziehungsweise hören. Für das Rad wird die Fabric-Mod
benötigt; auf dem Server muss die BetterSouth-Paper-Bridge installiert sein.
Zwischen Emotes gilt eine kurze Abklingzeit.

## Spotify-Overlay (Windows)

Das Spotify-Widget zeigt Titel und Interpret des aktiven Spotify-Desktopplayers
an; pausierte Titel werden markiert. Eine Spotify-Anmeldung oder ein Client
Secret ist nicht erforderlich. Unter Windows x64 startet die Mod einen
mitgelieferten, unsichtbaren Begleiter automatisch und beendet ihn mit Minecraft;
Spieler müssen weder .NET installieren noch den Begleiter separat starten. Die
Windows-Mediensteuerung muss für den Spotify-Desktopplayer verfügbar sein. Unter
anderen Betriebssystemen bleibt das Widget ohne Titeldaten. Zum Bauen der Mod
aus dem Quellcode wird das .NET 10 SDK benötigt; das fertige Mod-JAR bringt den
Begleiter bereits mit. Die drei Schaltflächen des Spotify-Widgets steuern
vorherigen Titel, Pause/Fortsetzen und nächsten Titel. Sie sind im `H`-HUD-Studio
und im normalen HUD anklickbar, sobald ein Minecraft-Bildschirm den Mauszeiger
freigibt (zum Beispiel Chat oder Inventar). Die Steuerelemente und der Titeltext
werden gemeinsam mit dem Widget skaliert. Das Widget zeigt außerdem das vom
Windows-Medienplayer bereitgestellte Cover an, sofern eines verfügbar ist.

## Drogen-Erkennung konfigurieren

Falls das optionale `DrugsPlugin` installiert ist, liest die Bridge die
gespeicherten Mengen automatisch über dessen API aus. Andernfalls trage in
`plugins/BetterSouthHudBridge/config.yml` die verwendeten Bukkit-Materialien
und/oder markanten Bestandteile der Item-Anzeigenamen ein. Material oder Name
genügt; jeder passende Inventar-Stack wird nur einmal gezählt und seine Menge
wird addiert. Ohne DrugsPlugin und ohne Matcher zeigt das Widget
„nicht konfiguriert“. Nach einer Änderung der Server-Konfiguration die Bridge
oder den Server neu starten.

## Server-Anbindung

Die Bridge liest das Bargeld über die öffentliche MoneySystem-Methode
`getCashBalance(UUID)`. Fraktion, Rang und Wantedpunkte werden über die aktuell
in der bereitgestellten Fraktionssystem-JAR vorhandenen internen Methoden
abgerufen. Beim Start prüft die Bridge diese Schnittstellen und protokolliert
fehlende oder inkompatible Plugins. Auf dem Netzwerkkanal `bettersouth:hud`
werden nur HUD-Daten des jeweils betroffenen Spielers an dessen Client gesendet.
Die Währung wird aus der `currency`-Einstellung des MoneySystem-Plugins übernommen.


## Spotify-Helper (eingebettete EXE) – Herkunft

Die in der Mod-JAR enthaltene Datei `assets/bettersouth_hud/bettersouth-spotify-helper.exe` ist **keine vorkompilierte Fremddatei**. Sie wird beim Build aus dem Quellcode in `spotify-helper/` erzeugt:

- Quellcode: `spotify-helper/Program.cs`, Projekt `spotify-helper/BetterSouth.SpotifyHelper.csproj` (C#, .NET 10, Lizenz wie die Mod)
- Einzige Abhängigkeit: NuGet `Dubya.WindowsMediaController` (Zugriff auf die Windows-Mediensteuerung / SMTC)
- Build: `dotnet publish spotify-helper -c Release -r win-x64 --self-contained -p:PublishSingleFile=true` (wird von `fabric-mod/build.gradle` automatisch ausgeführt)
- Funktion: liest Titel/Cover der aktuellen Wiedergabe aus Windows und führt Play/Pause/Skip aus. Kein Netzwerkzugriff, kein Login. Kommunikation mit der Mod nur über lokale Dateien in `config/bettersouth-hud/spotify/`.

Reproduzieren: Repository klonen, `.NET 10 SDK` + JDK 25 installieren, `gradlew build` im Ordner `fabric-mod`.
