package dev.smpweapons;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StatusManager {
   public static NamespacedKey FREEZE_SPEED;
   public static NamespacedKey FREEZE_JUMP;
   public static NamespacedKey SILENCE_JUMP;
   public static NamespacedKey DAUNTLESS_KB;
   private static final Map<UUID, Long> unstoppableUntil = new HashMap();
   private final Map<UUID, EnumMap<Status, Long>> until = new HashMap();
   private final Map<UUID, LivingEntity> entities = new HashMap();

   public StatusManager(JavaPlugin var1) {
      FREEZE_SPEED = new NamespacedKey(var1, "freeze_speed");
      FREEZE_JUMP = new NamespacedKey(var1, "freeze_jump");
      SILENCE_JUMP = new NamespacedKey(var1, "silence_jump");
      DAUNTLESS_KB = new NamespacedKey(var1, "dauntless_kb");
   }

   public static void setUnstoppable(Player var0, int var1) {
      unstoppableUntil.put(var0.getUniqueId(), System.currentTimeMillis() + (long)var1 * 50L);
   }

   public static boolean isUnstoppable(Player var0) {
      Long var1 = (Long)unstoppableUntil.get(var0.getUniqueId());
      if (var1 == null) {
         return false;
      } else if (var1 <= System.currentTimeMillis()) {
         unstoppableUntil.remove(var0.getUniqueId());
         return false;
      } else {
         return true;
      }
   }

   public void apply(LivingEntity var1, Status var2, int var3) {
      if (SafeZones.isProtected(var1.getLocation())) return;
      if (var1 instanceof Player var4) {
         if (isUnstoppable(var4)) {
            return;
         }
      }

      long var6 = System.currentTimeMillis() + (long)var3 * 50L;
      this.until.computeIfAbsent(var1.getUniqueId(), id -> new EnumMap<>(Status.class)).merge(var2, var6, Math::max);
      this.entities.put(var1.getUniqueId(), var1);
      this.refresh(var1);
   }

   public boolean has(Entity var1, Status var2) {
      EnumMap var3 = (EnumMap)this.until.get(var1.getUniqueId());
      if (var3 == null) {
         return false;
      } else {
         Long var4 = (Long)var3.get(var2);
         return var4 != null && var4 > System.currentTimeMillis();
      }
   }

   public boolean frozen(Entity var1) {
      return this.has(var1, StatusManager.Status.STUN) || this.has(var1, StatusManager.Status.ROOT);
   }

   public boolean silenced(Entity var1) {
      return this.has(var1, StatusManager.Status.SILENCE) || this.has(var1, StatusManager.Status.ROOT);
   }

   public void tick() {
      long var1 = System.currentTimeMillis();
      Iterator var3 = this.until.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         ((EnumMap<Status, Long>)var4.getValue()).values().removeIf(expiry -> expiry <= var1);
         LivingEntity var5 = (LivingEntity)this.entities.get(var4.getKey());
         if (var5 != null && var5.isValid()) {
            this.refresh(var5);
            if (((EnumMap)var4.getValue()).isEmpty()) {
               this.entities.remove(var4.getKey());
               var3.remove();
            }
         } else {
            if (var5 != null) {
               strip(var5);
            }

            this.entities.remove(var4.getKey());
            var3.remove();
         }
      }

   }

   private void refresh(LivingEntity var1) {
      boolean var2 = this.frozen(var1);
      boolean var3 = this.silenced(var1);
      setModifier(var1, Attribute.MOVEMENT_SPEED, FREEZE_SPEED, (double)-1.0F, Operation.MULTIPLY_SCALAR_1, var2);
      setModifier(var1, Attribute.JUMP_STRENGTH, FREEZE_JUMP, (double)-1.0F, Operation.MULTIPLY_SCALAR_1, var2);
      setModifier(var1, Attribute.JUMP_STRENGTH, SILENCE_JUMP, (double)-0.5F, Operation.MULTIPLY_SCALAR_1, var3 && !var2);
      if (var3 && var1 instanceof Player var4) {
         var4.setCooldown(Material.WIND_CHARGE, 10);
         var4.setCooldown(Material.COBWEB, 10);
      }

   }

   public void clear(LivingEntity var1) {
      this.until.remove(var1.getUniqueId());
      this.entities.remove(var1.getUniqueId());
      strip(var1);
   }

   public void clearAll() {
      for(LivingEntity var2 : new ArrayList<>(this.entities.values())) {
         strip(var2);
      }

      this.until.clear();
      this.entities.clear();
      unstoppableUntil.clear();

      for(Player var4 : Bukkit.getOnlinePlayers()) {
         strip(var4);
      }

   }

   public static void strip(LivingEntity var0) {
      setModifier(var0, Attribute.MOVEMENT_SPEED, FREEZE_SPEED, (double)0.0F, Operation.ADD_NUMBER, false);
      setModifier(var0, Attribute.JUMP_STRENGTH, FREEZE_JUMP, (double)0.0F, Operation.ADD_NUMBER, false);
      setModifier(var0, Attribute.JUMP_STRENGTH, SILENCE_JUMP, (double)0.0F, Operation.ADD_NUMBER, false);
      setModifier(var0, Attribute.KNOCKBACK_RESISTANCE, DAUNTLESS_KB, (double)0.0F, Operation.ADD_NUMBER, false);
   }

   public static void setModifier(LivingEntity var0, Attribute var1, NamespacedKey var2, double var3, AttributeModifier.Operation var5, boolean var6) {
      AttributeInstance var7 = var0.getAttribute(var1);
      if (var7 != null) {
         boolean var8 = var7.getModifier(var2) != null;
         if (var6 && !var8) {
            var7.addModifier(new AttributeModifier(var2, var3, var5));
         } else if (!var6 && var8) {
            var7.removeModifier(var2);
         }

      }
   }

   public static enum Status {
      STUN,
      ROOT,
      SILENCE;

      // $FF: synthetic method
      private static Status[] $values() {
         return new Status[]{STUN, ROOT, SILENCE};
      }
   }
}
