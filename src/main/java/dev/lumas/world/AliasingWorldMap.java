package dev.lumas.world;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.World;

/**
 * A drop-in replacement for CraftServer.worlds that transparently resolves a
 * legacy/base world name to a world actually registered under the "worlds_" prefix.
 * <p>
 *   getWorld("resource")  ->  resolves the world stored as "worlds_resource"
 *   getWorld("worlds_resource")  ->  still resolves directly
 * <p>
 * This is a pure delegate, every entry physically lives in the underlying map exactly
 * once, so getWorlds()/values()/entrySet() are unchanged (no duplicate worlds).
 * <p>
 * Note: the resolved World still reports its real name (e.g. "worlds_resource") from
 * World#getName(). Aliasing only affects lookup, not identity.
 */
public final class AliasingWorldMap implements Map<String, World> {

    static final String PREFIX = "worlds_"; // TODO: configurable

    private final Map<String, World> delegate;
    private final Logger log;

    private final Set<String> resolvedOnce = ConcurrentHashMap.newKeySet();

    public AliasingWorldMap(Map<String, World> delegate, Logger log) {
        this.delegate = delegate;
        this.log = log;
    }

    public Map<String, World> delegate() {
        return this.delegate;
    }

    private static String prefixed(Object key) {
        if (!(key instanceof String s)) return null;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith(PREFIX)) return null; // already prefixed, no fallback needed
        return PREFIX + lower;
    }

    @Override
    public World get(Object key) {
        World direct = this.delegate.get(key);
        if (direct != null) return direct;
        String alt = prefixed(key);
        if (alt == null) return null;
        World viaPrefix = this.delegate.get(alt);
        if (viaPrefix != null && this.resolvedOnce.add((String) key)) {
            this.log.info("Resolved legacy world name '" + key + "' -> '" + alt + "'");
        }
        return viaPrefix;
    }

    @Override
    public World getOrDefault(Object key, World defaultValue) {
        World v = get(key);
        return (v != null) ? v : defaultValue;
    }

    @Override
    public boolean containsKey(Object key) {
        if (this.delegate.containsKey(key)) return true;
        String alt = prefixed(key);
        return alt != null && this.delegate.containsKey(alt);
    }

    @Override public int size() {
        return this.delegate.size();
    }
    @Override public boolean isEmpty() {
        return this.delegate.isEmpty();
    }
    @Override public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public World put(String key, World value) {
        World previous = this.delegate.put(key, value);
        if (previous == null) {
            this.log.info("World added to map: '" + key + "'"
                    + (isPrefixed(key) ? " (resolvable as '" + key.substring(PREFIX.length()) + "')" : ""));
        }
        return previous;
    }

    @Override
    public World remove(Object key) {
        World removed = this.delegate.remove(key);
        if (removed != null) {
            this.log.info("World removed from map: '" + key + "'");
            // Drop the one-time resolution flag so a later reload logs again.
            if (key instanceof String s && isPrefixed(s)) {
                this.resolvedOnce.remove(s.substring(PREFIX.length()));
            }
        }
        return removed;
    }

    @Override public void putAll(Map<? extends String, ? extends World> m) {
        for (Entry<? extends String, ? extends World> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        this.log.info("World map cleared (" + this.delegate.size() + " entries)");
        this.delegate.clear();
        this.resolvedOnce.clear();
    }

    @Override public Set<String> keySet() {
        return this.delegate.keySet();
    }
    @Override public Collection<World> values() {
        return this.delegate.values();
    }
    @Override public Set<Entry<String, World>> entrySet() {
        return this.delegate.entrySet();
    }
    @Override public boolean equals(Object o) {
        return this.delegate.equals(o);
    }
    @Override public int hashCode() {
        return this.delegate.hashCode();
    }
    @Override public String toString() {
        return this.delegate.toString();
    }

    private static boolean isPrefixed(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).startsWith(PREFIX);
    }
}