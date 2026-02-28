# EZ Actions Wiki

!!! Tipp „Sprache / 语言“
    Verwenden Sie das **Sprachumschalter**-Symbol in der oberen rechten Navigationsleiste, um die Wiki-Sprache zu ändern.

    使用右上角导航栏中的**语言切换**图标来切换 Wiki 语言.

EZ Actions ist ein clientseitiger Minecraft-Mod, der Ihnen ein schnelles Radialmenü für Aktionen bietet, die Sie ständig verwenden.

Betrachten Sie es als Ihr „Kampf- und Utility-Schnellrad“: einmal die Taste gedrückt halten, einmal bewegen, fertig.

!!! Warnung „Versionsbereich“
    Dieses Wiki wurde für **EZ Actions 2.0.0.0** geschrieben.

    Wenn Sie eine neuere Version verwenden, haben sich möglicherweise einige Funktionen und UI-Details geändert.

???+ Info „TLDR“
    - Erstellen Sie Ihr eigenes Radialmenü mit **Schlüsselaktionen**, **Befehlsaktionen**, **Gegenstandsausrüstungsaktionen** und **Bundles**.
    - Gestalten Sie es mit Farb-/Design-/Animationskonfigurationen.
    - JSON-Import-/Exportmenü für Freigabe und Backups.
    - Mod-Entwickler können alles über die API steuern (einschließlich temporärer Laufzeitradiale).

## Was EZ Actions bewirken kann

- Lösen Sie Vanilla- oder modifizierte Tastenkombinationen aus.
- Führen Sie ein- oder mehrzeilige Befehle aus.
- Rüsten Sie aufgezeichnete Ausrüstungssätze mit exaktem Item-Matching aus (NBT inklusive).
- Organisieren Sie Aktionen in verschachtelten Paketen.
- Verstecken Sie Bundles vor dem Root-Verzeichnis, während Sie sie über die Bundle-Tastenkombination zugänglich halten.
- Fügen Sie benutzerdefinierte Symbole von `config/ezactions/icons` hinzu.
- Erstellen/bearbeiten Sie Menüs im Spiel mit Drag/Drop und Tastaturkürzeln.
- Lassen Sie andere Mods EZ Actions über die öffentliche API steuern.

## Für wen dieses Wiki gedacht ist

- Spieler, die eine klare Einrichtungsanleitung wünschen, ohne den Quellcode lesen zu müssen.
- Power-User, die erweiterte Verhaltensdetails wünschen.
- Modpack-Hersteller und Mod-Entwickler, die vollständige API-Dokumente wünschen.

Auf den meisten Seiten werden Sie erweiterbare „Deep Dive“-Abschnitte sehen. Überspringen Sie sie, wenn Sie nur den praktischen Ablauf wünschen.

## Schnellstart

1. Legen Sie Tastenkombinationen fest für:
   - `Open radial menu`
   - `Open editor`
2. Öffnen Sie den Editor und fügen Sie Ihre erste Aktion hinzu.
3. Halten Sie Ihre Radialtaste im Spiel gedrückt und lassen Sie sie über einem Slice los, um sie auszuführen.
4. Optimieren Sie die visuelle Darstellung im Konfigurationsbildschirm.

## Navigation

Für vollständige Dokumente verwenden Sie das linke Navigationsmenü:

- Hauptmenü-Editor-GUI
- Schlüsselaktion
- Befehlsaktion
- Aktion zum Ausrüsten von Gegenständen
- Bündel
- Import-Export
- Konfiguration
- API

??? „Technischer Hinweis“
    EZ Actions ist vollständig clientseitig. Es ist keine Serverinstallation erforderlich.

    Aktionen hängen immer noch davon ab, was der Server zulässt (z. B. Befehlsberechtigungen).
