package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.hook.PlaceholderHook;

/**
 * PlaceholderAPI-Modul. Registriert die {@code %ysbp_*%}-Platzhalter, solange
 * das Modul experimentell aktiviert und PlaceholderAPI installiert ist.
 */
public final class PlaceholderModule extends AbstractModule {

    private PlaceholderHook hook;

    public PlaceholderModule(YourShikaBackpacks plugin) {
        super(plugin, "placeholderapi", "PlaceholderAPI",
                "Platzhalter %ysbp_count%, %ysbp_highest_tier%, %ysbp_open%", "PlaceholderAPI");
    }

    @Override
    protected void onEnable() throws Throwable {
        hook = new PlaceholderHook(plugin, plugin.manager(), plugin.tiers());
        hook.register();
    }

    @Override
    protected void onDisable() {
        if (hook != null) {
            try {
                hook.unregister();
            } catch (Throwable ignored) {
            }
            hook = null;
        }
    }
}
