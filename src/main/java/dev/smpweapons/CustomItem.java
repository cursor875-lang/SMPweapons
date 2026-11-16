package dev.smpweapons;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public enum CustomItem {
   DAUNTLESS_SHIELD("dauntless_shield", "<gold><bold>Dauntless Shield", Material.SHIELD, Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Resistance I", "<white> • Take 45% less knockback", "", "<gold>[F] Behemoth Bash <dark_gray>(45s)", "<gray>  Knock back nearby players, 2.5❤ + Slowness IV 5s", "<gold>[Shift+F] Parry <dark_gray>(2m)", "<gray>  Block the next 3 hits for 15s and knock back attackers")),
   WINDSTRIKER("windstriker", "<aqua><bold>WindStriker", Material.MACE, Map.of(Enchantment.WIND_BURST, 3, Enchantment.DENSITY, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(always active)", "<white> • No fall damage", "<white> • Every 10th mace hit gives you Slow Falling 3s", "", "<aqua>[F] Wind Rush <dark_gray>(45s)", "<gray>  Fast 10-block dash, steer with your camera.", "<gray>  Enemies you pass take 2❤ and get launched up")),
   HORN_OF_HELLSCORCH("horn_of_hellscorch", "<red><bold>Horn of Hellscorch", Material.GOAT_HORN, Map.of(), List.of("<gray>Passive <dark_gray>(always active)", "<white> • Fire Resistance", "<white> • Every 15th hit burns the enemy 1.5❤/s for 3s", "", "<red>[F] Hell Ring <dark_gray>(2m)", "<gray>  A 10s ring of hellfire: 1.5❤/s, turns the ground", "<gray>  to nether blocks and drains water (reverts after)")),
   ABYSSAL_BLADE("abyssal_blade", "<dark_purple><bold>Abyssal Blade", Material.NETHERITE_SWORD, Map.of(Enchantment.SHARPNESS, 5, Enchantment.LOOTING, 3, Enchantment.SWEEPING_EDGE, 3, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Speed I", "<white> • Every 15th hit gives Wither + Darkness 5s", "", "<dark_purple>[F] Abyssal Void <dark_gray>(2m)", "<gray>  10s blackhole: pulls everything in 10 blocks,", "<gray>  1❤/s, shreds armor, Blindness + Wither", "<dark_purple>[Shift+F] Cosmic Blessing <dark_gray>(2m 30s)", "<gray>  15s aura: all your potion effects +1 level")),
   STORMCHARGER("stormcharger", "<yellow><bold>StormCharger", Material.NETHERITE_AXE, Map.of(Enchantment.SHARPNESS, 4, Enchantment.EFFICIENCY, 5, Enchantment.FORTUNE, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Haste I", "<white> • Every 10th hit: lightning + 2s stun", "", "<yellow>[F] Divine Smite <dark_gray>(1m 30s)", "<gray>  Stun everyone in a circle for 5s and", "<gray>  strike them with lightning 3 times")),
   PLATE_OF_LIFE("plate_of_life", "<green><bold>Plate of Life", Material.NETHERITE_CHESTPLATE, Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while worn)", "<white> • Regeneration I, Luck V, Hero of the Village V", "<white> • Golden apples give Absorption II", "", "<green>[Double-tap Shift, look ahead] Nature's Wrath <dark_gray>(2m)", "<gray>  Vines grab nearby players for 10s: stun, Hunger V,", "<gray>  Poison V, Nausea, 1.5❤/s. +1 Regen level per hit")),
   SILENCED_SHIELD("silenced_shield", "<dark_gray><bold>Silenced Shield", Material.SHIELD, Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • When your shield is disabled, the attacker gets", "<white>   Darkness 5s and you teleport behind them", "<white> • Silenced enemies: wind charge + cobweb cooldown,", "<white>   50% less jump strength", "", "<dark_gray>[F] Silent Binding <dark_gray>(2m)", "<gray>  Target can't move/jump/use wind charges or cobwebs", "<gray>  for 5s, loses effects, gets Darkness; you tp behind", "<dark_gray>[Shift+F] Shadow Stalker <dark_gray>(1m 30s)", "<gray>  Reveal untrusted players in 100 blocks, Glowing,", "<gray>  Darkness, Hunger; you go invisible for 10s")),
   OCEANIC_CROWN("oceanic_crown", "<blue><bold>Oceanic Crown", Material.NETHERITE_HELMET, Map.of(Enchantment.PROTECTION, 4, Enchantment.RESPIRATION, 3, Enchantment.AQUA_AFFINITY, 1, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while worn)", "<white> • Infinite water breathing", "<white> • Armor takes 25% less durability in water", "", "<blue>[Double-tap Shift, look up] Tidal Wave <dark_gray>(50s)", "<gray>  A wave drags players into a whirlpool for 10s:", "<gray>  2❤/s and 5 armor durability/s", "<blue>[Double-tap Shift, look down] Riptide <dark_gray>(1m 30s)", "<gray>  Ride a wave for 6s; anyone touching it takes 1.5❤/s")),
   SCULK_REAPER("sculk_reaper", "<dark_aqua><bold>Sculk Reaper", Material.NETHERITE_SWORD, Map.of(Enchantment.SHARPNESS, 5, Enchantment.LOOTING, 3, Enchantment.SWEEPING_EDGE, 3, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Every 5th hit makes the enemy bleed (1❤/s for 4s)", "<white> • Immune to Darkness", "<white> • Player kills heal you 3❤", "", "<dark_aqua>[F] Sonic Beam <dark_gray>(30s)", "<gray>  Fire a warden beam (20 blocks): 4❤ through armor", "<dark_aqua>[Shift+F] Echo Shell <dark_gray>(2m, needs 4 kills)", "<gray>  Become undamageable for 8s", "<dark_aqua>[Right-click] Sculk Hook <dark_gray>(20s, needs 5 kills)", "<gray>  Hook a player, pull them to you, 3❤ damage")),
   WORLD_BREAKER("world_breaker", "<gold><bold>Worldbreaker Pickaxe", Material.NETHERITE_PICKAXE, Map.of(Enchantment.EFFICIENCY, 5, Enchantment.FORTUNE, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Mines a 3x3x3 area", "<white> • Uses your pickaxe enchantments for drops")),
   OCEANIC_LEGGINGS("oceanic_leggings", "<blue><bold>Oceanic Leggings", Material.NETHERITE_LEGGINGS, Map.of(Enchantment.PROTECTION, 4, Enchantment.DEPTH_STRIDER, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Oceanic armor set", "<white> • Protection IV", "<white> • Depth Strider III")),
   OCEANIC_BOOTS("oceanic_boots", "<blue><bold>Oceanic Boots", Material.NETHERITE_BOOTS, Map.of(Enchantment.PROTECTION, 4, Enchantment.DEPTH_STRIDER, 3, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Oceanic armor set", "<white> • Protection IV", "<white> • Depth Strider III", "<white> • Feather Falling IV")),
   JUGGERNAUT_HELMET("juggernaut_helmet", "<gold><bold>Juggernaut Helmet", Material.NETHERITE_HELMET, Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Juggernaut armor set", "<white> • Part of the full Juggernaut set", "<white> • Full set: Resistance II, +10 hearts, Slowness I", "<white> • Full set builds Rage when you take hits")),
   JUGGERNAUT_CHESTPLATE("juggernaut_chestplate", "<gold><bold>Juggernaut Chestplate", Material.NETHERITE_CHESTPLATE, Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Juggernaut armor set", "<white> • Part of the full Juggernaut set", "<white> • Full set: Resistance II, +10 hearts, Slowness I", "<white> • Full set builds Rage when you take hits")),
   JUGGERNAUT_LEGGINGS("juggernaut_leggings", "<gold><bold>Juggernaut Leggings", Material.NETHERITE_LEGGINGS, Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Juggernaut armor set", "<white> • Part of the full Juggernaut set", "<white> • Full set: Resistance II, +10 hearts, Slowness I", "<white> • Full set builds Rage when you take hits")),
   JUGGERNAUT_BOOTS("juggernaut_boots", "<gold><bold>Juggernaut Boots", Material.NETHERITE_BOOTS, Map.of(Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Juggernaut armor set", "<white> • Part of the full Juggernaut set", "<white> • Full set: Resistance II, +10 hearts, Slowness I", "<white> • Full set builds Rage when you take hits")),
   JUGGERNAUT_HAMMER("juggernaut_hammer", "<gold><bold>Juggernaut Hammer", Material.MACE, Map.of(Enchantment.DENSITY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Heavy weapon", "<white> • 20% chance to damage enemy armor by 10%", "<white> • Rage: taking hits fills a rage bar. When full:", "<white>   8s of +50% damage and heal 1❤ per hit", "<gold>[F] Ground Pound <dark_gray>(45s)", "<gray>  Slam down; damage scales with fall distance", "<gold>[Shift+F] Iron Charge <dark_gray>(1m)", "<gray>  Charge forward up to 15 blocks")),
   FORTRESS_SHIELD("fortress_shield", "<gold><bold>Fortress Shield", Material.SHIELD, Map.of(Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive: reflect 30% damage while blocking", "<gold>[F] Wall Up <dark_gray>(1m 30s)", "<gray>  Temporary iron bar and obsidian wall for 8s")),
   DRAGON_EGG("dragon_egg", "<dark_purple><bold>Dragon Egg", Material.DRAGON_EGG, Map.of(), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Fire Resistance", "<white> • Immune to dragon's breath damage", "", "<light_purple>[F] Dragon's Breath <dark_gray>(45s)", "<gray>  Unleash a cone of dragon flame: 4❤ + burn", "<gray>  nearby enemies for 4 seconds", "<light_purple>[Shift+F] Dragon's Awakening <dark_gray>(2m)", "<gray>  Gain Strength II, Resistance II and Speed II", "<gray>  for 8 seconds; blast nearby enemies away")),
   FROSTBITE_AXE("frostbite_axe", "<aqua><bold>Frostbite Axe", Material.NETHERITE_AXE, Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive", "<white> • Chance to apply Slowness on hit", "<aqua>[F] Glacial Break <dark_gray>(16s)", "<gray>  Sends out a frost wave that damages and slows enemies")),
   VOID_REAVER("void_reaver", "<dark_purple><bold>Void Reaver", Material.NETHERITE_AXE, Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive", "<white> • Hits build Void Energy", "<dark_purple>[F] Rift Pull <dark_gray>(22s)", "<gray>  Pulls nearby enemies toward you")),
   BLOODMOON_KATANA("bloodmoon_katana", "<dark_red><bold>Bloodmoon Katana", Material.NETHERITE_SWORD, Map.of(Enchantment.SHARPNESS, 5, Enchantment.LOOTING, 3, Enchantment.SWEEPING_EDGE, 3, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), List.of("<gray>Passive <dark_gray>(while held)", "<white> • Lifesteal: heal 15% of melee damage you deal", "<white> • Bloodlust: Speed II while at 4❤ or less", "", "<dark_red>[F] Killing Intent <dark_gray>(40s, needs 3 kills)", "<gray>  For 5s every hit is a critical hit (x1.5 damage)", "<dark_red>[Shift+F] Crescent Slash <dark_gray>(30s)", "<gray>  Fire a blood crescent 15 blocks: 3❤ + Weakness II 3s")),
   FLOWER_SWORD("flower_sword", "<gradient:#8ee2ad:#eda5cf><bold>Flower Sword</bold></gradient>", Material.DIAMOND_SWORD, Map.of(Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 3), List.of("<gray>Passive", "<white> • Hostile mobs will not attack while held", "<white> • Every fifth hit creates a permanent cobweb", "<white> • Slowness II for 3s on fifth hit", "", "<green>[Shift + Left-click] Flowering Sanctuary <dark_gray>(30s)", "<gray>  10s stationary sanctuary: flowers, damage, blindness", "<gray>  Grants Regeneration II while inside"));

   public final String id;
   public final String name;
   public final Material material;
   public final Map<Enchantment, Integer> enchants;
   public final List<String> lore;

   private CustomItem(String var3, String var4, Material var5, Map<Enchantment, Integer> var6, List<String> var7) {
      this.id = var3;
      this.name = var4;
      this.material = var5;
      this.enchants = var6;
      this.lore = var7;
   }

   public static CustomItem byId(String var0) {
      if (var0 == null) {
         return null;
      } else {
         for(CustomItem var4 : values()) {
            if (var4.id.equalsIgnoreCase(var0)) {
               return var4;
            }
         }

         return null;
      }
   }

   // $FF: synthetic method
   private static CustomItem[] $values() {
      return new CustomItem[]{DAUNTLESS_SHIELD, WINDSTRIKER, HORN_OF_HELLSCORCH, ABYSSAL_BLADE, STORMCHARGER, PLATE_OF_LIFE, SILENCED_SHIELD, OCEANIC_CROWN, SCULK_REAPER, WORLD_BREAKER, OCEANIC_LEGGINGS, OCEANIC_BOOTS, JUGGERNAUT_HELMET, JUGGERNAUT_CHESTPLATE, JUGGERNAUT_LEGGINGS, JUGGERNAUT_BOOTS, JUGGERNAUT_HAMMER, FORTRESS_SHIELD, DRAGON_EGG, FROSTBITE_AXE, VOID_REAVER, BLOODMOON_KATANA, FLOWER_SWORD};
   }
}
