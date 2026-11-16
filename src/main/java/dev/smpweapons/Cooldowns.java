package dev.smpweapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class Cooldowns {
   private final Map<UUID, Map<String, Long>> ends = new HashMap();

   public long remaining(Player var1, String var2) {
      Map var3 = (Map)this.ends.get(var1.getUniqueId());
      if (var3 == null) {
         return 0L;
      } else {
         Long var4 = (Long)var3.get(var2);
         return var4 == null ? 0L : Math.max(0L, var4 - System.currentTimeMillis());
      }
   }

   public void set0(Player var1, String var2, long var3) {
      ((Map)this.ends.computeIfAbsent(var1.getUniqueId(), (var0) -> new HashMap())).put(var2, System.currentTimeMillis() + var3);
   }

   public static String format(long var0) {
      long var2 = (var0 + 999L) / 1000L;
      long var4 = var2 / 60L;
      var2 %= 60L;
      return var4 > 0L ? var4 + "m " + var2 + "s" : var2 + "s";
   }

   public void set(Player var1, String var2, long var3) {
      this.set0(var1, var2, WeaponsConfig.cooldown(var2, var3));
   }
}
