package dev.smpweapons;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

final class WeaponsConfig {
   private static YamlConfiguration cfg;
   private static File file;
   private static long loadedStamp = Long.MIN_VALUE;
   private static long nextCheck;
   private static final Map<Object, Map<UUID, String>> SEEN = new WeakHashMap();
   private static Method playable;
   static final String DEFAULT = "weapons:\n  dauntless_shield: true\n  windstriker: true\n  horn_of_hellscorch: true\n  abyssal_blade: true\n  stormcharger: true\n  plate_of_life: true\n  silenced_shield: true\n  oceanic_crown: true\n  sculk_reaper: true\n  world_breaker: true\n  oceanic_leggings: true\n  oceanic_boots: true\n  juggernaut_helmet: true\n  juggernaut_chestplate: true\n  juggernaut_leggings: true\n  juggernaut_boots: true\n  juggernaut_hammer: true\n  fortress_shield: true\n  dragon_egg: true\n  frostbite_axe: true\n  void_reaver: true\n  bloodmoon_katana: true\ncooldowns:\n  behemoth_bash: 45\n  parry: 120\n  wind_rush: 45\n  hell_ring: 120\n  abyssal_void: 120\n  cosmic_blessing: 150\n  divine_smite: 90\n  natures_wrath: 120\n  silent_binding: 120\n  shadow_stalker: 90\n  tidal_wave: 50\n  riptide: 90\n  sonic_beam: 30\n  echo_shell: 120\n  sculk_hook: 20\n  sculk_hook_miss: 3\n  dragons_breath: 45\n  dragons_awakening: 120\n  glacial_break: 16\n  rift_pull: 22\n  killing_intent: 40\n  crescent_slash: 30\n  groundpound: 45\n  ironcharge: 60\n  earthquake: 60\n  unstoppable: 120\n  wallup: 90\nevents:\n  gather-progress-messages: true\n";

   private WeaponsConfig() {
   }

   private static YamlConfiguration cfg() {
      long var0 = System.currentTimeMillis();
      if (cfg != null && var0 < nextCheck) {
         return cfg;
      } else {
         nextCheck = var0 + 2000L;

         try {
            if (file == null) {
               Plugin var2 = Bukkit.getPluginManager().getPlugin("SMPWeapons");
               if (var2 == null) {
                  return cfg;
               }

               File var3 = var2.getDataFolder();
               var3.mkdirs();
               file = new File(var3, "config.yml");
               if (!file.exists()) {
                  Files.writeString(file.toPath(), "weapons:\n  dauntless_shield: true\n  windstriker: true\n  horn_of_hellscorch: true\n  abyssal_blade: true\n  stormcharger: true\n  plate_of_life: true\n  silenced_shield: true\n  oceanic_crown: true\n  sculk_reaper: true\n  world_breaker: true\n  oceanic_leggings: true\n  oceanic_boots: true\n  juggernaut_helmet: true\n  juggernaut_chestplate: true\n  juggernaut_leggings: true\n  juggernaut_boots: true\n  juggernaut_hammer: true\n  fortress_shield: true\n  dragon_egg: true\n  frostbite_axe: true\n  void_reaver: true\n  bloodmoon_katana: true\ncooldowns:\n  behemoth_bash: 45\n  parry: 120\n  wind_rush: 45\n  hell_ring: 120\n  abyssal_void: 120\n  cosmic_blessing: 150\n  divine_smite: 90\n  natures_wrath: 120\n  silent_binding: 120\n  shadow_stalker: 90\n  tidal_wave: 50\n  riptide: 90\n  sonic_beam: 30\n  echo_shell: 120\n  sculk_hook: 20\n  sculk_hook_miss: 3\n  dragons_breath: 45\n  dragons_awakening: 120\n  glacial_break: 16\n  rift_pull: 22\n  killing_intent: 40\n  crescent_slash: 30\n  groundpound: 45\n  ironcharge: 60\n  earthquake: 60\n  unstoppable: 120\n  wallup: 90\nevents:\n  gather-progress-messages: true\n", StandardCharsets.UTF_8);
               }
            }

            long var7 = file.lastModified();
            if (cfg == null || var7 != loadedStamp) {
               boolean var4 = cfg != null;
               cfg = YamlConfiguration.loadConfiguration(file);
               loadedStamp = var7;
               Plugin var5 = Bukkit.getPluginManager().getPlugin("SMPWeapons");
               if (var4 && var5 != null) {
                  var5.getLogger().info("config.yml changed, reloaded.");
               }
            }
         } catch (Throwable var6) {
         }

         return cfg;
      }
   }

   static Object filter(Object var0) {
      if (var0 instanceof Enum var1) {
         YamlConfiguration var2 = cfg();
         if (var2 == null) {
            return var0;
         } else {
            String var10001 = var1.name().toLowerCase(Locale.ROOT);
            return ConfigFiles.weaponEnabled(var10001) ? var0 : null;
         }
      } else {
         return var0;
      }
   }

   static long cooldown(String key, long fallbackMilliseconds) {
      String actual = key.equals("sculk_hook") && fallbackMilliseconds == 3000L ? "sculk_hook_miss" : key;
      return ConfigFiles.cooldown(actual, (int) (fallbackMilliseconds / 1000L)) * 1000L;
   }

   static void gatherProgress(Object var0) {
      try {
         YamlConfiguration var1 = cfg();
         if (!ConfigFiles.bool("events.yml", "events.gather-progress-messages", true)) {
            return;
         }

         Field var2 = var0.getClass().getDeclaredField("needs");
         var2.setAccessible(true);
         Map<Material, Integer> var3 = (Map<Material, Integer>)var2.get(var0);
         if (var3 == null || var3.isEmpty()) {
            return;
         }

         String var4 = title(var0);
         int var5 = 0;

         for(int var7 : var3.values()) {
            var5 += var7;
         }

         Map<UUID, String> var24 = SEEN.computeIfAbsent(var0, ignored -> new HashMap<>());

         for(Player var8 : Bukkit.getOnlinePlayers()) {
            if (isPlayable(var8)) {
               EnumMap<Material, Integer> var9 = new EnumMap<>(Material.class);
               ItemStack[] var10 = var8.getInventory().getContents();
               if (var10 != null) {
                  for(ItemStack var14 : var10) {
                     if (var14 != null && var3.containsKey(var14.getType())) {
                        var9.merge(var14.getType(), var14.getAmount(), Integer::sum);
                     }
                  }
               }

               LinkedHashMap<Material, Integer> var26 = new LinkedHashMap<>();
               int var27 = 0;

               for(Map.Entry<Material, Integer> var30 : var3.entrySet()) {
                  int var15 = Math.min((Integer)var30.getValue(), (Integer)var9.getOrDefault(var30.getKey(), 0));
                  var26.put((Material)var30.getKey(), var15);
                  var27 += var15;
               }

               String var29 = var26.toString();
               if (!var29.equals(var24.get(var8.getUniqueId()))) {
                  var24.put(var8.getUniqueId(), var29);
                  if (var27 < var5) {
                     Component var31 = Component.text("[" + var4 + "] ", NamedTextColor.GOLD).append(Component.text("Collected " + var27 + "/" + var5, NamedTextColor.YELLOW));
                     Object var32 = Component.text("  ", NamedTextColor.GRAY);
                     Object var16 = Component.text("  Missing: ", NamedTextColor.RED);
                     boolean var17 = true;
                     boolean var18 = true;

                     for(Map.Entry<Material, Integer> var20 : var3.entrySet()) {
                        int var21 = (Integer)var26.get(var20.getKey());
                        int var22 = (Integer)var20.getValue();
                        if (!var17) {
                           var32 = ((Component)var32).append(Component.text(", ", NamedTextColor.GRAY));
                        }

                        var17 = false;
                        String var10001 = pretty(var20.getKey());
                        var32 = ((Component)var32).append(Component.text(var10001 + " " + var21 + "/" + var22, var21 >= var22 ? NamedTextColor.GREEN : NamedTextColor.RED));
                        if (var21 < var22) {
                           if (!var18) {
                              var16 = ((Component)var16).append(Component.text(", ", NamedTextColor.GRAY));
                           }

                           var18 = false;
                           var16 = ((Component)var16).append(Component.text(var22 - var21 + "x " + pretty(var20.getKey()), NamedTextColor.RED));
                        }
                     }

                     var8.sendMessage(Ui.line(var31));
                     var8.sendMessage(Ui.line((Component)var32));
                     var8.sendMessage(Ui.line((Component)var16));
                  }
               }
            }
         }
      } catch (Throwable var23) {
      }

   }

   private static boolean isPlayable(Player var0) {
      try {
         if (playable == null) {
            playable = Class.forName("dev.smpweapons.WeaponEvents").getDeclaredMethod("playable", Player.class);
            playable.setAccessible(true);
         }

         return (Boolean)playable.invoke((Object)null, var0);
      } catch (Throwable var2) {
         return true;
      }
   }

   private static String title(Object var0) {
      try {
         for(Class var1 = var0.getClass(); var1 != null; var1 = var1.getSuperclass()) {
            try {
               Field var2 = var1.getDeclaredField("theme");
               var2.setAccessible(true);
               Object var3 = var2.get(var0);
               Method var4 = var3.getClass().getDeclaredMethod("title");
               var4.setAccessible(true);
               return String.valueOf(var4.invoke(var3));
            } catch (NoSuchFieldException ignored) {
            }
         }
      } catch (Throwable var6) {
      }

      return "Gather";
   }

   private static String pretty(Object var0) {
      String var10000;
      if (var0 instanceof Enum var2) {
         var10000 = var2.name();
      } else {
         var10000 = String.valueOf(var0);
      }

      String var1 = var10000;
      StringBuilder var7 = new StringBuilder();

      for(String var6 : var1.toLowerCase(Locale.ROOT).split("_")) {
         if (!var6.isEmpty()) {
            if (var7.length() > 0) {
               var7.append(' ');
            }

            var7.append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
         }
      }

      return var7.toString();
   }
}
