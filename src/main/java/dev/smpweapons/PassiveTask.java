package dev.smpweapons;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PassiveTask implements Runnable {
   public void run() {
      for(Player var2 : Bukkit.getOnlinePlayers()) {
         if (SafeZones.isProtected(var2.getLocation())) {
            for (PotionEffectType effectType : new PotionEffectType[] {
                PotionEffectType.RESISTANCE, PotionEffectType.SPEED, PotionEffectType.STRENGTH,
                PotionEffectType.HASTE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.REGENERATION,
                PotionEffectType.LUCK, PotionEffectType.HERO_OF_THE_VILLAGE, PotionEffectType.WATER_BREATHING
            }) {
                PotionEffect existing = var2.getPotionEffect(effectType);
                if (existing != null && existing.isInfinite() && existing.isAmbient() && !existing.hasParticles()) {
                    var2.removePotionEffect(effectType);
                }
            }
            continue;
         }
         Items.makeItemsUnbreakable(var2);
         Items.upgradeDragonEgg(var2);
         Items.refreshKillLore(var2);
         boolean var3 = Items.holding(var2, CustomItem.DAUNTLESS_SHIELD);
         effect(var2, PotionEffectType.RESISTANCE, 0, var3);
         StatusManager.setModifier(var2, Attribute.KNOCKBACK_RESISTANCE, StatusManager.DAUNTLESS_KB, ConfigFiles.number("passives.yml", "passives.dauntless-shield.knockback-resistance", 0.45), Operation.ADD_NUMBER, var3);
         int var4 = Items.heldReaperKills(var2);
         effect(var2, PotionEffectType.SPEED, 0, Items.holding(var2, CustomItem.ABYSSAL_BLADE) || var4 >= 1);
         effect(var2, PotionEffectType.STRENGTH, 1, var4 >= 2);
         effect(var2, PotionEffectType.SPEED, 1, Items.holding(var2, CustomItem.BLOODMOON_KATANA) && var2.getHealth() <= (double)8.0F);
         effect(var2, PotionEffectType.HASTE, 0, Items.holding(var2, CustomItem.STORMCHARGER));
         boolean var5 = Items.holding(var2, CustomItem.DRAGON_EGG);
         effect(var2, PotionEffectType.FIRE_RESISTANCE, 0, var5 || Items.carrying(var2, CustomItem.HORN_OF_HELLSCORCH) || var4 >= 3);
         if (var4 >= 0) {
            var2.removePotionEffect(PotionEffectType.DARKNESS);
         }

         boolean var6 = Items.wearing(var2, CustomItem.PLATE_OF_LIFE);
         effect(var2, PotionEffectType.REGENERATION, 0, var6);
         effect(var2, PotionEffectType.LUCK, 4, var6);
         effect(var2, PotionEffectType.HERO_OF_THE_VILLAGE, 4, var6);
         effect(var2, PotionEffectType.WATER_BREATHING, 0, Items.wearing(var2, CustomItem.OCEANIC_CROWN));
      }

   }

   private static void effect(Player var0, PotionEffectType var1, int var2, boolean var3) {
      PotionEffect var4 = var0.getPotionEffect(var1);
      if (var3) {
         if (var4 == null || var4.getAmplifier() < var2 || var4.getAmplifier() == var2 && !var4.isInfinite()) {
            var0.addPotionEffect(new PotionEffect(var1, -1, var2, true, false, true));
         }
      } else if (var4 != null && var4.isInfinite() && var4.isAmbient() && var4.getAmplifier() == var2) {
         var0.removePotionEffect(var1);
      }

   }
}
