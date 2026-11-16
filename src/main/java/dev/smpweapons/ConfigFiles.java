package dev.smpweapons;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

/** Strongly typed access to the modular configuration files. */
final class ConfigFiles {
    private static SMPWeapons plugin;
    private static final Map<String, YamlConfiguration> configs = new HashMap<>();
    private static final List<String> FILES = List.of("config.yml", "weapons.yml", "abilities.yml", "passives.yml", "events.yml", "protection.yml", "flower-sword.yml", "messages.yml");

    private ConfigFiles() { }

    static void init(SMPWeapons owner) {
        plugin = owner;
        reload();
    }

    static void reload() {
        for (String file : FILES) {
            File target = new File(plugin.getDataFolder(), file);
            if (!target.exists()) plugin.saveResource(file, false);
            configs.put(file, YamlConfiguration.loadConfiguration(target));
        }
    }

    static YamlConfiguration of(String filename) { return configs.get(filename); }
    static boolean bool(String file, String path, boolean fallback) {
        var c = of(file);
        return c == null ? fallback : c.getBoolean(path, fallback);
    }
    static int integer(String file, String path, int fallback) {
        var c = of(file);
        return c == null ? fallback : c.getInt(path, fallback);
    }
    static double number(String file, String path, double fallback) {
        var c = of(file);
        return c == null ? fallback : c.getDouble(path, fallback);
    }
    static String string(String file, String path, String fallback) {
        var c = of(file);
        return c == null ? fallback : c.getString(path, fallback);
    }
    static List<String> strings(String file, String path, List<String> fallback) {
        var c = of(file);
        return c == null || !c.isList(path) ? fallback : c.getStringList(path);
    }

    static boolean weaponEnabled(String id) { return bool("weapons.yml", "weapons." + id + ".enabled", true); }
    static boolean abilityEnabled(String key) { return bool("abilities.yml", "abilities." + key + ".enabled", true); }
    static int cooldown(String key, int fallbackSeconds) {
        return Math.max(0, integer("abilities.yml", "abilities." + key + ".cooldown-seconds", fallbackSeconds));
    }
    static double stat(String ability, String parameter, double fallback) {
        return number("abilities.yml", "abilities." + ability + "." + parameter, fallback);
    }
    static int statInt(String ability, String parameter, int fallback) {
        return integer("abilities.yml", "abilities." + ability + "." + parameter, fallback);
    }
    static int flowerInt(String parameter, int fallback) {
        return integer("flower-sword.yml", "flower-sword." + parameter, fallback);
    }
    static double flowerNumber(String parameter, double fallback) {
        return number("flower-sword.yml", "flower-sword." + parameter, fallback);
    }
    static boolean flowerFlag(String parameter, boolean fallback) {
        return bool("flower-sword.yml", "flower-sword." + parameter, fallback);
    }
}
