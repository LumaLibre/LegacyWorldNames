package dev.lumas.world;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegacyWorldNames extends JavaPlugin {

    private Field worldsField;
    private AliasingWorldMap installed;

    @Override
    public void onLoad() {
        try {
            install();
            getLogger().info("Legacy world-name aliasing installed (worlds_<name> resolvable as <name>)");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to install legacy world-name aliasing", t);
        }
    }

    @Override
    public void onDisable() {
        uninstall();
    }

    @SuppressWarnings("unchecked")
    private void install() throws Exception {
        Server server = Bukkit.getServer();

        Field field = findWorldsField(server.getClass());
        if (field == null) {
            throw new NoSuchFieldException("CraftServer.worlds not found on " + server.getClass());
        }
        field.setAccessible(true);

        Object current = field.get(server);
        if (current instanceof AliasingWorldMap) {
            // Already installed, unsafe though
            this.worldsField = field;
            this.installed = (AliasingWorldMap) current;
            return;
        }
        if (!(current instanceof Map)) {
            throw new IllegalStateException("CraftServer.worlds is not a Map: " + current);
        }

        AliasingWorldMap wrapper = new AliasingWorldMap((Map<String, World>) current, getLogger());
        field.set(server, wrapper);

        Object after = field.get(server);
        if (after != wrapper) {
            throw new IllegalStateException("Field write did not stick; aliasing not active");
        }

        this.worldsField = field;
        this.installed = wrapper;
    }

    private void uninstall() {
        if (this.worldsField == null || this.installed == null) return;
        try {
            Object current = this.worldsField.get(Bukkit.getServer());
            // Only restore if our wrapper is still the one in place; never clobber someone else's.
            if (current == this.installed) {
                this.worldsField.set(Bukkit.getServer(), this.installed.delegate());
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Failed to uninstall legacy world-name aliasing", t);
        } finally {
            this.installed = null;
        }
    }

    private static Field findWorldsField(Class<?> c) {
        while (c != null) {
            try {
                return c.getDeclaredField("worlds");
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}