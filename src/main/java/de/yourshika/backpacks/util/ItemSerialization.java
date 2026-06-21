package de.yourshika.backpacks.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Serialisiert ItemStack-Arrays nach Base64 und zurück. Nutzt das von Bukkit
 * versionierte Format, was über dieselbe Serverversion hinweg sicher ist und
 * keine Items beschädigt.
 */
public final class ItemSerialization {

    private ItemSerialization() {}

    public static String toBase64(ItemStack[] items) {
        if (items == null) items = new ItemStack[0];
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream data = new BukkitObjectOutputStream(out)) {
            data.writeInt(items.length);
            for (ItemStack item : items) {
                data.writeObject(item);
            }
            data.flush();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Inhalt konnte nicht serialisiert werden", ex);
        }
    }

    public static ItemStack[] fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[0];
        try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream data = new BukkitObjectInputStream(in)) {
            int length = data.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) data.readObject();
            }
            return items;
        } catch (Exception ex) {
            throw new IllegalStateException("Inhalt konnte nicht gelesen werden", ex);
        }
    }
}
