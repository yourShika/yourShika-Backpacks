package de.yourshika.backpacks.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Soft-Hook auf das <b>BetterPets</b>-Plugin (des selben Autors). Ruft dessen
 * öffentliche, statische API {@code de.kamil.betterpets.api.BetterPetsApi} per
 * <b>Reflection</b> auf – so kompiliert und läuft dieses Plugin ohne harte
 * Abhängigkeit. Ist BetterPets nicht installiert (oder zu alt), liefert alles
 * {@code false}/{@code 0} und das Pet-Booster-Upgrade bleibt einfach wirkungslos.
 */
public final class BetterPetsHook {

    private static final String API = "de.kamil.betterpets.api.BetterPetsApi";

    private static boolean initialized;
    private static boolean available;
    private static Method mIsBooster, mBoosterTier, mBoosterMinutes, mHasActive, mActivate;

    private BetterPetsHook() {}

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("BetterPets")) return;
            Class<?> api = Class.forName(API);
            mIsBooster = api.getMethod("isBoosterItem", ItemStack.class);
            mBoosterTier = api.getMethod("boosterTier", ItemStack.class);
            mBoosterMinutes = api.getMethod("boosterMinutes", ItemStack.class);
            mHasActive = api.getMethod("hasActiveBooster", Player.class);
            mActivate = api.getMethod("activateBooster", Player.class, int.class, int.class);
            available = true;
        } catch (Throwable ignored) {
            available = false; // Plugin fehlt / API-Version passt nicht -> still deaktiviert.
        }
    }

    /** Muss aufgerufen werden, falls BetterPets nach uns (neu)geladen wird. */
    public static synchronized void reset() {
        initialized = false;
        available = false;
        mIsBooster = mBoosterTier = mBoosterMinutes = mHasActive = mActivate = null;
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static boolean isBoosterItem(ItemStack item) {
        if (!isAvailable() || item == null) return false;
        try {
            return (boolean) mIsBooster.invoke(null, item);
        } catch (Throwable ex) {
            return false;
        }
    }

    public static int boosterTier(ItemStack item) {
        if (!isAvailable() || item == null) return 0;
        try {
            return (int) mBoosterTier.invoke(null, item);
        } catch (Throwable ex) {
            return 0;
        }
    }

    public static int boosterMinutes(ItemStack item) {
        if (!isAvailable() || item == null) return 0;
        try {
            return (int) mBoosterMinutes.invoke(null, item);
        } catch (Throwable ex) {
            return 0;
        }
    }

    public static boolean hasActiveBooster(Player player) {
        if (!isAvailable() || player == null) return false;
        try {
            return (boolean) mHasActive.invoke(null, player);
        } catch (Throwable ex) {
            return false;
        }
    }

    /** Aktiviert einen Booster (ohne Item); false, wenn schon einer läuft oder Tier ungültig. */
    public static boolean activateBooster(Player player, int tier, int minutes) {
        if (!isAvailable() || player == null) return false;
        try {
            return (boolean) mActivate.invoke(null, player, tier, minutes);
        } catch (Throwable ex) {
            return false;
        }
    }
}
