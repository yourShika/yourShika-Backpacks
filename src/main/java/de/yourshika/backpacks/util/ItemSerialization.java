package de.yourshika.backpacks.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Serialisiert ItemStack-Arrays nach Base64 und zurück.
 *
 * <p><strong>Datensicherheit über Updates hinweg (MUSS):</strong> Es wird das
 * von Paper bereitgestellte, <em>versionierte</em> Byte-Format
 * ({@link ItemStack#serializeAsBytes()} / {@link ItemStack#deserializeBytes(byte[])})
 * verwendet. Dieses Format enthält die Minecraft-Datenversion und wird beim
 * Einlesen automatisch über den DataFixer auf die aktuelle Version migriert –
 * dadurch gehen Items (inklusive beliebiger Custom-NBT / Daten-Components)
 * <em>auch nach Server- oder Plugin-Updates nicht verloren und werden nicht
 * beschädigt</em>.</p>
 *
 * <p>Ältere Daten im früheren Java-Serialisierungsformat
 * ({@link BukkitObjectInputStream}) werden weiterhin gelesen (Legacy-Fallback)
 * und beim nächsten Speichern automatisch in das neue Format überführt.</p>
 */
public final class ItemSerialization {

    /** Magic-Header des neuen Formats ("YSB2"). */
    private static final int MAGIC = 0x59534232;
    private static final int FORMAT_VERSION = 2;

    private ItemSerialization() {}

    public static String toBase64(ItemStack[] items) {
        if (items == null) items = new ItemStack[0];
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(MAGIC);
            data.writeInt(FORMAT_VERSION);
            data.writeInt(items.length);
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir()) {
                    data.writeBoolean(false);
                } else {
                    byte[] bytes = item.serializeAsBytes();
                    data.writeBoolean(true);
                    data.writeInt(bytes.length);
                    data.write(bytes);
                }
            }
            data.flush();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Inhalt konnte nicht serialisiert werden", ex);
        }
    }

    public static ItemStack[] fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[0];
        byte[] raw = Base64.getDecoder().decode(base64);

        // Neues Format anhand des Magic-Headers erkennen.
        if (raw.length >= 4 && readInt(raw, 0) == MAGIC) {
            return readNewFormat(raw);
        }
        // Andernfalls Legacy-Format (altes Bukkit-Java-Serialisierungsformat) lesen.
        return readLegacyFormat(raw);
    }

    private static ItemStack[] readNewFormat(byte[] raw) {
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(raw))) {
            data.readInt(); // MAGIC
            data.readInt(); // FORMAT_VERSION
            int length = data.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                if (!data.readBoolean()) {
                    items[i] = null;
                    continue;
                }
                int len = data.readInt();
                byte[] bytes = new byte[len];
                data.readFully(bytes);
                // Pro Slot absichern: ein einzelnes beschädigtes Item darf NICHT das
                // Einlesen des ganzen Rucksacks abbrechen (B4). Da jeder Eintrag
                // längen-präfixiert ist, bleibt der Stream nach dem Überspringen
                // korrekt ausgerichtet – der betroffene Slot wird leer.
                try {
                    items[i] = ItemStack.deserializeBytes(bytes);
                } catch (Exception ex) {
                    items[i] = null;
                    org.bukkit.Bukkit.getLogger().warning(
                            "[yourShika Backpacks] Beschädigtes Item in Slot " + i
                            + " übersprungen: " + ex.getMessage());
                }
            }
            return items;
        } catch (Exception ex) {
            throw new IllegalStateException("Inhalt konnte nicht gelesen werden (neues Format)", ex);
        }
    }

    private static ItemStack[] readLegacyFormat(byte[] raw) {
        try (BukkitObjectInputStream data = new BukkitObjectInputStream(new ByteArrayInputStream(raw))) {
            int length = data.readInt();
            List<ItemStack> items = new ArrayList<>(Math.max(0, length));
            for (int i = 0; i < length; i++) {
                items.add((ItemStack) data.readObject());
            }
            return items.toArray(new ItemStack[0]);
        } catch (Exception ex) {
            throw new IllegalStateException("Inhalt konnte nicht gelesen werden (Legacy-Format)", ex);
        }
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }
}
