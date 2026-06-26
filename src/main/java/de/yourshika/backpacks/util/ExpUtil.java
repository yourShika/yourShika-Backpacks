package de.yourshika.backpacks.util;

import org.bukkit.entity.Player;

/**
 * Rechnet zwischen Minecraft-Leveln und absoluten Erfahrungs-Punkten um – für das
 * XP-Storage-Upgrade. Verwendet die offiziellen Vanilla-Formeln, sodass beim
 * Ein-/Auszahlen weder XP entstehen noch verloren gehen.
 */
public final class ExpUtil {

    private ExpUtil() {}

    /** Gesamt-Erfahrungspunkte, die bis zum Erreichen von {@code level} nötig sind. */
    public static int totalForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Punkte, die ein Aufstieg von {@code level} auf {@code level+1} kostet. */
    public static int pointsToNext(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    /** Aktuelle Gesamt-Erfahrungspunkte eines Spielers (Level + Fortschritt). */
    public static int totalExp(Player player) {
        return totalForLevel(player.getLevel())
                + Math.round(player.getExp() * pointsToNext(player.getLevel()));
    }

    /** Setzt die Erfahrung eines Spielers vollständig auf 0. */
    public static void clear(Player player) {
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);
    }
}
