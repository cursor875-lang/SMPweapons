package dev.smpweapons;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class TrustManager {
   private final JavaPlugin plugin;
   private final File file;
   private final Map<UUID, Set<UUID>> trusted = new HashMap();

   public TrustManager(JavaPlugin var1) {
      this.plugin = var1;
      this.file = new File(var1.getDataFolder(), "trust.yml");
   }

   public void load() {
      this.trusted.clear();
      if (this.file.exists()) {
         YamlConfiguration var1 = YamlConfiguration.loadConfiguration(this.file);

         for(String var3 : var1.getKeys(false)) {
            try {
               HashSet var4 = new HashSet();

               for(String var6 : var1.getStringList(var3)) {
                  var4.add(UUID.fromString(var6));
               }

               this.trusted.put(UUID.fromString(var3), var4);
            } catch (IllegalArgumentException var7) {
            }
         }

      }
   }

   public void save() {
      YamlConfiguration var1 = new YamlConfiguration();
      this.trusted.forEach((var1x, var2) -> {
         ArrayList var3 = new ArrayList();

         for(UUID var5 : var2) {
            var3.add(var5.toString());
         }

         var1.set(var1x.toString(), var3);
      });

      try {
         this.plugin.getDataFolder().mkdirs();
         var1.save(this.file);
      } catch (IOException var3) {
         this.plugin.getLogger().warning("Could not save trust.yml: " + var3.getMessage());
      }

   }

   public boolean trusts(UUID var1, UUID var2) {
      Set var3 = (Set)this.trusted.get(var1);
      return var3 != null && var3.contains(var2);
   }

   public boolean add(UUID var1, UUID var2) {
      boolean var3 = ((Set)this.trusted.computeIfAbsent(var1, (var0) -> new HashSet())).add(var2);
      if (var3) {
         this.save();
      }

      return var3;
   }

   public boolean remove(UUID var1, UUID var2) {
      Set var3 = (Set)this.trusted.get(var1);
      boolean var4 = var3 != null && var3.remove(var2);
      if (var4) {
         this.save();
      }

      return var4;
   }

   public Set<UUID> list(UUID var1) {
      return (Set)this.trusted.getOrDefault(var1, Collections.emptySet());
   }
}
