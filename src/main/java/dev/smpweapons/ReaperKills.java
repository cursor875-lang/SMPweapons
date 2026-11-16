package dev.smpweapons;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReaperKills {
   public static final ReaperKills REAPER = new ReaperKills("reaper_kills.yml");
   public static final ReaperKills KATANA = new ReaperKills("katana_kills.yml");
   private static JavaPlugin plugin;
   private final String fileName;
   private final Map<UUID, Integer> kills = new HashMap();

   private ReaperKills(String var1) {
      this.fileName = var1;
   }

   public static ReaperKills of(CustomItem var0) {
      if (var0 == CustomItem.SCULK_REAPER) {
         return REAPER;
      } else {
         return var0 == CustomItem.BLOODMOON_KATANA ? KATANA : null;
      }
   }

   public static void init(JavaPlugin var0) {
      plugin = var0;
      REAPER.load();
      KATANA.load();
   }

   private File file() {
      return new File(plugin.getDataFolder(), this.fileName);
   }

   private void load() {
      this.kills.clear();
      File var1 = this.file();
      if (var1.exists()) {
         YamlConfiguration var2 = YamlConfiguration.loadConfiguration(var1);

         for(String var4 : var2.getKeys(false)) {
            try {
               this.kills.put(UUID.fromString(var4), var2.getInt(var4));
            } catch (IllegalArgumentException var6) {
            }
         }

      }
   }

   public int get(UUID var1) {
      return (Integer)this.kills.getOrDefault(var1, 0);
   }

   public int add(UUID var1) {
      int var2 = this.get(var1) + 1;
      this.set(var1, var2);
      return var2;
   }

   public void set(UUID var1, int var2) {
      this.kills.put(var1, Math.max(0, var2));
      this.save();
   }

   private void save() {
      if (plugin != null) {
         YamlConfiguration var1 = new YamlConfiguration();
         this.kills.forEach((var1x, var2) -> var1.set(var1x.toString(), var2));

         try {
            plugin.getDataFolder().mkdirs();
            var1.save(this.file());
         } catch (IOException var3) {
            Logger var10000 = plugin.getLogger();
            String var10001 = this.fileName;
            var10000.warning("Could not save " + var10001 + ": " + var3.getMessage());
         }

      }
   }
}
