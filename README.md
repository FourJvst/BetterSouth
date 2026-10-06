# BetterSouth HUD

Client-HUD für Minecraft Java 26.2 (Fabric) plus Paper-Bridge für die Serverdaten.
Das HUD kann Geld, Wantedpunkte, Drogen-Anzahl, Fraktion, Rang, FPS und Spielzeit
als einzeln verschiebbare und ein-/ausblendbare Widgets anzeigen.

## Bauen

- Client-Mod: `cd fabric-mod` und `.\gradlew.bat build`. Das JAR liegt danach in
  `fabric-mod/build/libs/`.
- Paper-Bridge: `cd paper-bridge` und `mvn package`. Das Plugin-JAR liegt danach in
  `paper-bridge/target/BetterSouth-HUD-Bridge.jar`.

Die Fabric-Mod gehört in den `mods`-Ordner des Spielers; Fabric API muss ebenfalls
installiert sein. Die Bridge gehört in den Paper-Serverordner `plugins`. Auf dem
Server müssen außerdem MoneySystem und Fraktionssystem geladen sein. MoneySystem
benötigt gemäß seiner Plugin-Metadaten zusätzlich LevelSystem.

## Widgets einstellen

Mit `H` öffnet sich das HUD-Studio, ohne das Spiel zu pausieren. Links kannst du
Widgets ein- oder ausblenden und ihre Größe in Prozent anpassen. In der
Bildschirm-Vorschau ziehst du Widgets mit der Maus an die gewünschte Stelle;
das Mausrad skaliert das ausgewählte Widget ebenfalls. Änderungen werden
automatisch in `config/bettersouth-hud.properties` im Minecraft-Spielverzeichnis
gespeichert. Drücke `Esc`, um das Menü zu schließen.

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
