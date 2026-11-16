package dev.smpweapons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class Abilities {
   private static final Particle.DustOptions BLACK;
   private static final Particle.DustOptions WHITE;
   private static final Particle.DustOptions VOID;
   private final SMPWeapons plugin;
   private final Cooldowns cooldowns;
   private final TrustManager trust;
   private final StatusManager status;
   private final Random rng = new Random();
   private final Map<UUID, Integer> parryHits = new HashMap();
   private final Map<UUID, Long> parryUntil = new HashMap();
   private final Map<UUID, Long> noFallUntil = new HashMap();
   private final Map<UUID, Map<PotionEffectType, Integer>> boosted = new HashMap();
   private final Set<Runnable> pendingReverts = new HashSet();
   private final Map<UUID, BukkitTask> shadowGlowTasks = new HashMap();
   private final Map<UUID, Integer> voidEnergy = new HashMap();
   private static final Particle.DustOptions BLOOD;
   private final Map<UUID, Long> autoCritUntil = new HashMap();
   private final Map<UUID, Long> invulnerableUntil = new HashMap();

   public Abilities(SMPWeapons var1, Cooldowns var2, TrustManager var3, StatusManager var4) {
      this.plugin = var1;
      this.cooldowns = var2;
      this.trust = var3;
      this.status = var4;
   }

   public void trigger(Player var1, CustomItem var2, boolean var3) {
      if (!SafeZones.guardAbility(var1)) return;
      SafeZones.trackAbilityMotion(var1, 13000L);
      switch (var2) {
         case DAUNTLESS_SHIELD:
            if (var3) {
               this.parry(var1);
            } else {
               this.behemothBash(var1);
            }
            break;
         case WINDSTRIKER:
            this.windRush(var1);
            break;
         case HORN_OF_HELLSCORCH:
            this.hellRing(var1);
            break;
         case ABYSSAL_BLADE:
            if (var3) {
               this.cosmicBlessing(var1);
            } else {
               this.abyssalVoid(var1);
            }
            break;
         case STORMCHARGER:
            this.divineSmite(var1);
            break;
         case PLATE_OF_LIFE:
            this.naturesWrath(var1);
            break;
         case SILENCED_SHIELD:
            if (var3) {
               this.shadowStalker(var1);
            } else {
               this.silentBinding(var1);
            }
            break;
         case OCEANIC_CROWN:
            if (var3) {
               this.riptide(var1);
            } else {
               this.tidalWave(var1);
            }
            break;
         case SCULK_REAPER:
            if (var3) {
               this.echoShell(var1);
            } else {
               this.sonicBeam(var1);
            }
            break;
         case BLOODMOON_KATANA:
            if (var3) {
               this.crescentSlash(var1);
            } else {
               this.killingIntent(var1);
            }
            break;
         case DRAGON_EGG:
            if (var3) {
               this.dragonsAwakening(var1);
            } else {
               this.dragonsBreath(var1);
            }
            break;
         case FROSTBITE_AXE:
            this.glacialBreak(var1);
            break;
         case VOID_REAVER:
            this.riftPull(var1);
      }

   }

   public void axePassiveHit(Player var1, LivingEntity var2, CustomItem var3, EntityDamageByEntityEvent var4) {
      if (!SafeZones.canAttack(var1, var2)) return;
      switch (var3) {
         case FROSTBITE_AXE:
            if (this.rng.nextDouble() < (double)0.25F) {
               var2.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
            break;
         case VOID_REAVER:
            Map<UUID, Integer> var10001 = this.voidEnergy;
            UUID var10002 = var1.getUniqueId();
            var1.sendActionBar(Ui.line(Component.text("Void Energy: " + String.valueOf(var10001.merge(var10002, 1, (var0, var1x) -> Math.min(5, var0 + var1x))) + "/5", NamedTextColor.DARK_PURPLE)));
      }

   }

   private void glacialBreak(Player var1) {
      if (this.ready(var1, "glacial_break", 16, "Glacial Break")) {
         Location var2 = var1.getLocation();
         Vector var3 = var2.getDirection().setY(0).normalize();
         World var4 = var1.getWorld();
         HashSet var5 = new HashSet();
         var4.playSound(var2, Sound.BLOCK_GLASS_BREAK, 1.5F, 0.6F);
         this.every(1L, 9, (var6) -> {
            Location var7 = var2.clone().add(var3.clone().multiply(var6 + 1)).add((double)0.0F, 0.8, (double)0.0F);
            var4.spawnParticle(Particle.SNOWFLAKE, var7, 18, 1.1, (double)0.5F, 1.1, 0.02);

            for(LivingEntity var9 : this.targetsNear(var1, var7, ConfigFiles.stat("glacial_break", "target-radius", 1.7))) {
               if (var5.add(var9.getUniqueId())) {
                  this.hurt(var9, ConfigFiles.stat("glacial_break", "damage-health", 4.0), var1, DamageType.MAGIC);
                  var9.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
               }
            }

         }, (Runnable)null);
      }
   }

   private void riftPull(Player var1) {
      if (this.ready(var1, "rift_pull", 22, "Rift Pull")) {
         int var2 = (Integer)this.voidEnergy.getOrDefault(var1.getUniqueId(), 0);
         this.voidEnergy.put(var1.getUniqueId(), 0);
         Location var3 = var1.getLocation().add(var1.getLocation().getDirection().multiply(4));
         World var4 = var1.getWorld();
         var4.playSound(var3, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5F, 0.5F);

         for(int var5 = 1; var5 <= 5; ++var5) {
            this.ring(var3.clone().add((double)0.0F, 0.3, (double)0.0F), (double)var5, Particle.PORTAL, var5 * 10);
         }

         for(LivingEntity var6 : this.targetsNear(var1, var3, ConfigFiles.stat("rift_pull", "radius", 7.0))) {
            Vector var7 = var3.toVector().subtract(var6.getLocation().toVector());
            if (var7.lengthSquared() > 0.01) {
               var6.setVelocity(var7.normalize().multiply((double)1.0F + (double)var2 * 0.12).setY((double)0.25F));
            }

            this.hurt(var6, (double)(2 + var2), var1, DamageType.MAGIC);
         }

      }
   }

   private void root(LivingEntity var1, int var2) {
      var1.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, var2, 10));
      var1.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, var2, 128));
   }

   private void killingIntent(Player var1) {
      int var2 = ReaperKills.KATANA.get(var1.getUniqueId());
      if (var2 < 3) {
         var1.sendActionBar(Ui.line(Component.text("Killing Intent unlocks at 3 kills with the katana (" + var2 + "/3)", NamedTextColor.RED)));
      } else if (this.ready(var1, "killing_intent", 40, "Killing Intent")) {
         this.autoCritUntil.put(var1.getUniqueId(), System.currentTimeMillis() + 5000L);
         World var3 = var1.getWorld();
         var3.playSound(var1.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8F, 1.6F);
         var3.playSound(var1.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.5F, 0.5F);
         this.every(4L, 25, (var2x) -> {
            if (var1.isOnline()) {
               Location var3x = var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
               var3.spawnParticle(Particle.DUST, var3x, 5, 0.4, 0.6, 0.4, (double)0.0F, BLOOD);
               var3.spawnParticle(Particle.CRIT, var3x, 3, 0.4, 0.6, 0.4, 0.05);
            }
         }, () -> {
            if (var1.isOnline()) {
               var1.sendActionBar(Ui.line(Component.text("Killing Intent faded", NamedTextColor.GRAY)));
            }

         });
      }
   }

   public boolean hasAutoCrit(Player var1) {
      Long var2 = (Long)this.autoCritUntil.get(var1.getUniqueId());
      return var2 != null && var2 > System.currentTimeMillis();
   }

   private void crescentSlash(Player var1) {
      if (this.ready(var1, "crescent_slash", 30, "Crescent Slash")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getEyeLocation().subtract((double)0.0F, 0.3, (double)0.0F);
         Vector var4 = var3.getDirection().normalize();
         Vector var5 = var4.clone().crossProduct(new Vector(0, 1, 0));
         if (var5.lengthSquared() < 0.01) {
            var5 = new Vector(1, 0, 0);
         }

         var5.normalize();
         final Vector slashSide = var5.clone();
         var2.playSound(var3, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
         var2.playSound(var3, Sound.ITEM_TRIDENT_THROW, 1.5F, 0.6F);
         HashSet var7 = new HashSet();
         boolean[] var8 = new boolean[]{false};
         this.every(1L, 15, (var8x) -> {
            if (!var8[0]) {
               Location var9 = var3.clone().add(var4.clone().multiply(var8x + 1));
               if (!var9.getBlock().isPassable()) {
                  var8[0] = true;
               } else {
                  for(double var10 = -1.6; var10 <= 1.6; var10 += 0.2) {
                     Location var12 = var9.clone().add(slashSide.clone().multiply(var10)).subtract(var4.clone().multiply(var10 * var10 * (double)0.25F));
                     var2.spawnParticle(Particle.DUST, var12, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, BLOOD);
                  }

                  var2.spawnParticle(Particle.SWEEP_ATTACK, var9, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);

                  for(LivingEntity var11 : this.targetsNear(var1, var9, 1.8)) {
                     if (var7.add(var11.getUniqueId())) {
                        this.hurt(var11, (double)6.0F, var1, DamageType.MAGIC);
                        var11.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                        var2.spawnParticle(Particle.DAMAGE_INDICATOR, var11.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 6, 0.3, 0.4, 0.3, 0.1);
                     }
                  }

               }
            }
         }, (Runnable)null);
      }
   }

   private void dragonsBreath(Player var1) {
      if (this.ready(var1, "dragons_breath", 45, "Dragon's Breath")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getEyeLocation();
         Vector var4 = var3.getDirection().normalize();
         HashSet var5 = new HashSet();
         var2.playSound(var3, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2F, 1.25F);
         var2.playSound(var3, Sound.ITEM_FIRECHARGE_USE, 1.5F, 0.7F);

         for(double var6 = (double)1.0F; var6 <= (double)9.0F; var6 += (double)0.5F) {
            Location var8 = var3.clone().add(var4.clone().multiply(var6));
            if (!var8.getBlock().isPassable()) {
               break;
            }

            double var9 = 0.35 + var6 * 0.13;
            this.dragonBreath(var8, 8, var9, var9 * 0.55, var9, 0.015);
            var2.spawnParticle(Particle.FLAME, var8, 4, var9 * (double)0.5F, var9 * 0.4, var9 * (double)0.5F, 0.02);

            for(LivingEntity var12 : this.targetsNear(var1, var8, (double)1.25F + var6 * 0.06)) {
               if (var5.add(var12.getUniqueId())) {
                  this.hurt(var12, (double)8.0F, var1, DamageType.MAGIC);
                  var12.setFireTicks(Math.max(var12.getFireTicks(), 80));
                  var2.spawnParticle(Particle.DAMAGE_INDICATOR, var12.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 8, 0.3, 0.4, 0.3, 0.1);
               }
            }
         }

      }
   }

   private void dragonBreath(Location var1, int var2, double var3, double var5, double var7, double var9) {
      World var11 = var1.getWorld();
      Class var12 = Particle.DRAGON_BREATH.getDataType();

      try {
         if (var12 == Float.class) {
            var11.spawnParticle(Particle.DRAGON_BREATH, var1, var2, var3, var5, var7, var9, 1.0F);
         } else if (var12 == Void.class) {
            var11.spawnParticle(Particle.DRAGON_BREATH, var1, var2, var3, var5, var7, var9);
         } else {
            var11.spawnParticle(Particle.REVERSE_PORTAL, var1, var2, var3, var5, var7, var9);
         }
      } catch (IllegalArgumentException var14) {
         var11.spawnParticle(Particle.REVERSE_PORTAL, var1, var2, var3, var5, var7, var9);
      }

   }

   private void dragonsAwakening(Player var1) {
      if (this.ready(var1, "dragons_awakening", 120, "Dragon's Awakening")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getLocation();
         var1.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 1, true, true, true));
         var1.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1, true, true, true));
         var1.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1, true, true, true));
         var1.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 160, 0, true, true, true));
         var2.playSound(var3, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5F, 0.7F);
         var2.playSound(var3, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.4F);

         for(int var4 = 1; var4 <= 6; ++var4) {
            int var5 = 0;

            for(int var6 = var4 * 10; var5 < var6; ++var5) {
               double var7 = (Math.PI * 2D) * (double)var5 / (double)var6;
               this.dragonBreath(var3.clone().add(Math.cos(var7) * (double)var4, 0.2, Math.sin(var7) * (double)var4), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            }

            this.ring(var3.clone().add((double)0.0F, (double)0.25F, (double)0.0F), (double)var4, Particle.FLAME, var4 * 8);
         }

         for(LivingEntity var10 : this.targetsNear(var1, var3, (double)6.0F)) {
            Vector var11 = var10.getLocation().toVector().subtract(var3.toVector());
            if (var11.lengthSquared() < 0.01) {
               var11 = new Vector(0, 0, 1);
            }

            var10.setVelocity(var11.normalize().multiply(1.35).setY(0.65));
            this.hurt(var10, (double)4.0F, var1, DamageType.MAGIC);
         }

         this.every(4L, 40, (var3x) -> {
            if (var1.isOnline()) {
               Location var4 = var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
               this.dragonBreath(var4, 5, 0.45, 0.65, 0.45, 0.01);
               var2.spawnParticle(Particle.FLAME, var4, 3, 0.35, 0.55, 0.35, 0.01);
            }
         }, () -> {
            if (var1.isOnline()) {
               var1.sendActionBar(Ui.line(Component.text("Dragon's Awakening faded", NamedTextColor.GRAY)));
            }

         });
      }
   }

   private void sonicBeam(Player var1) {
      if (this.ready(var1, "sonic_beam", 30, "Sonic Beam")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getEyeLocation();
         Vector var4 = var3.getDirection().normalize();
         var2.playSound(var3, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5F, 1.2F);
         var2.playSound(var3, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0F, 1.0F);
         HashSet var5 = new HashSet();

         for(double var6 = (double)1.0F; var6 <= (double)20.0F; ++var6) {
            Location var8 = var3.clone().add(var4.clone().multiply(var6));
            if (!var8.getBlock().isPassable()) {
               break;
            }

            var2.spawnParticle(Particle.SONIC_BOOM, var8, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);

            for(LivingEntity var10 : this.targetsNear(var1, var8, 1.3)) {
               if (var5.add(var10.getUniqueId())) {
                  this.hurt(var10, (double)8.0F, var1, DamageType.SONIC_BOOM);
                  var10.setVelocity(var4.clone().multiply(1.2).setY(0.4));
               }
            }
         }

      }
   }

   private void echoShell(Player var1) {
      if (Items.heldReaperKills(var1) < 4) {
         var1.sendActionBar(Ui.line(Component.text("Echo Shell unlocks at 4 player kills", NamedTextColor.RED)));
      } else if (this.ready(var1, "echo_shell", 120, "Echo Shell")) {
         this.invulnerableUntil.put(var1.getUniqueId(), System.currentTimeMillis() + 8000L);
         World var2 = var1.getWorld();
         var2.playSound(var1.getLocation(), Sound.ENTITY_WARDEN_ROAR, 1.5F, 1.0F);
         this.every(4L, 40, (var3) -> {
            if (var1.isOnline()) {
               Location var4 = var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
               var2.spawnParticle(Particle.SCULK_SOUL, var4, 4, (double)0.5F, 0.8, (double)0.5F, 0.01);
               this.ring(var1.getLocation().add((double)0.0F, 0.1, (double)0.0F), (double)1.0F, Particle.SOUL_FIRE_FLAME, 14);
               if (var3 % 5 == 0) {
                  var2.playSound(var4, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0F, 1.0F);
               }

            }
         }, () -> {
            if (var1.isOnline()) {
               var1.sendActionBar(Ui.line(Component.text("Echo Shell faded", NamedTextColor.GRAY)));
            }

         });
      }
   }

   public boolean isInvulnerable(Player var1) {
      Long var2 = (Long)this.invulnerableUntil.get(var1.getUniqueId());
      return var2 != null && var2 > System.currentTimeMillis();
   }

   public void sculkHook(Player var1) {
      if (!SafeZones.guardAbility(var1)) return;
      if (Items.heldReaperKills(var1) < 5) {
         var1.sendActionBar(Ui.line(Component.text("Sculk Hook unlocks at 5 player kills", NamedTextColor.RED)));
      } else if (this.cooldowns.remaining(var1, "sculk_hook") > 0L) {
         var1.sendActionBar(Ui.line(Component.text("Sculk Hook is on cooldown: " + Cooldowns.format(this.cooldowns.remaining(var1, "sculk_hook")), NamedTextColor.RED)));
      } else {
         World var2 = var1.getWorld();
         Location var3 = var1.getEyeLocation();
         Vector var4 = var3.getDirection().normalize();
         RayTraceResult var5 = var2.rayTrace(var3, var4, (double)25.0F, FluidCollisionMode.NEVER, true, 0.6, (var2x) -> var2x instanceof Player && this.isTarget(var1, var2x));
         double var6 = var5 == null ? (double)25.0F : var5.getHitPosition().distance(var3.toVector());

         for(double var8 = (double)0.5F; var8 <= var6; var8 += (double)0.5F) {
            var2.spawnParticle(Particle.SCULK_SOUL, var3.clone().add(var4.clone().multiply(var8)), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
         }

         var2.playSound(var3, Sound.ENTITY_FISHING_BOBBER_THROW, 1.5F, 0.5F);
         if (var5 != null) {
            Entity var9 = var5.getHitEntity();
            if (var9 instanceof Player) {
               Player var12 = (Player)var9;
               this.cooldowns.set(var1, "sculk_hook", 20000L);
               var1.sendActionBar(Ui.line(Component.text("Hooked " + var12.getName() + "!", NamedTextColor.GREEN)));
               this.hurt(var12, (double)6.0F, var1, DamageType.MAGIC);
               Vector var13 = var1.getLocation().toVector().subtract(var12.getLocation().toVector());
               double var10 = var13.length();
               if (var10 > (double)1.5F) {
                  var12.setVelocity(var13.normalize().multiply(Math.min((double)2.5F, (double)0.5F + var10 * 0.12)).setY(0.45));
               }

               var2.playSound(var12.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.5F, 1.0F);
               var2.spawnParticle(Particle.SONIC_BOOM, var12.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
               return;
            }
         }

         this.cooldowns.set(var1, "sculk_hook", 3000L);
         var1.sendActionBar(Ui.line(Component.text("Sculk Hook missed", NamedTextColor.GRAY)));
      }
   }

   public void bleed(Player var1, LivingEntity var2) {
      if (!SafeZones.canAttack(var1, var2)) return;
      BlockData var3 = Material.REDSTONE_BLOCK.createBlockData();
      if (var2 instanceof Player var4) {
         var4.sendActionBar(Ui.line(Component.text("You are bleeding!", NamedTextColor.DARK_RED)));
      }

      this.every(5L, 16, (var4x) -> {
         if (var2.isValid()) {
            var2.getWorld().spawnParticle(Particle.BLOCK, var2.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 8, 0.3, (double)0.5F, 0.3, (double)0.0F, var3);
            if (var4x % 4 == 0) {
               this.hurt(var2, (double)2.0F, var1, DamageType.MAGIC);
            }

         }
      }, (Runnable)null);
   }

   private void behemothBash(Player var1) {
      if (this.ready(var1, "behemoth_bash", 45, "Behemoth Bash")) {
         Location var2 = var1.getLocation();
         World var3 = var1.getWorld();
         var3.playSound(var2, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5F, 0.6F);
         var3.playSound(var2, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
         var3.spawnParticle(Particle.EXPLOSION, var2, 3, (double)1.0F, 0.2, (double)1.0F, (double)0.0F);

         for(LivingEntity var5 : this.targetsNear(var1, var2, ConfigFiles.stat("behemoth_bash", "radius", 6.0))) {
            this.hurt(var5, ConfigFiles.stat("behemoth_bash", "damage-health", 5.0), var1, DamageType.MAGIC);
            Vector var6 = var5.getLocation().toVector().subtract(var2.toVector()).setY(0);
            if (var6.lengthSquared() < 0.01) {
               var6 = var2.getDirection().setY(0);
            }

            if (var6.lengthSquared() < 0.01) {
               var6 = new Vector(0, 0, 1);
            }

            var5.setVelocity(var6.normalize().multiply(1.8).setY(0.6));
            var5.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
         }

         this.every(2L, 10, (var2x) -> {
            double radius = 0.6 + (double)var2x * 0.6;
            this.ring(var2.clone().add((double)0.0F, 0.15, (double)0.0F), radius, Particle.CLOUD, (int)(radius * (double)8.0F));
            this.ring(var2.clone().add((double)0.0F, 0.15, (double)0.0F), radius, Particle.CRIT, (int)(radius * (double)5.0F));
         }, (Runnable)null);
      }
   }

   private void parry(Player var1) {
      if (this.ready(var1, "parry", 120, "Parry")) {
         UUID var2 = var1.getUniqueId();
         long var3 = System.currentTimeMillis() + 15000L;
         this.parryHits.put(var2, 3);
         this.parryUntil.put(var2, var3);
         var1.getWorld().playSound(var1.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 1.4F);
         var1.sendActionBar(Ui.line(Component.text("Parry ready: 3 hits", NamedTextColor.GOLD)));
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Long var5 = (Long)this.parryUntil.get(var2);
            if (var5 != null && var5 == var3) {
               this.endParry(var1, false);
            }

         }, 300L);
      }
   }

   public boolean tryParry(Player var1, Entity var2) {
      UUID var3 = var1.getUniqueId();
      Long var4 = (Long)this.parryUntil.get(var3);
      if (var4 == null) {
         return false;
      } else if (System.currentTimeMillis() > var4) {
         this.endParry(var1, false);
         return false;
      } else {
         int var5 = (Integer)this.parryHits.merge(var3, -1, Integer::sum);
         World var6 = var1.getWorld();
         var6.playSound(var1.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.2F, 0.8F);
         var6.playSound(var1.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4F, 1.8F);
         var6.spawnParticle(Particle.ENCHANTED_HIT, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 25, 0.4, (double)0.5F, 0.4, 0.3);
         if (var2 != null && var2 != var1) {
            Vector var7 = var2.getLocation().toVector().subtract(var1.getLocation().toVector()).setY(0);
            if (var7.lengthSquared() < 0.01) {
               var7 = var1.getLocation().getDirection().setY(0);
            }

            if (var7.lengthSquared() > 0.01) {
               var2.setVelocity(var7.normalize().multiply(1.4).setY(0.45));
            }
         }

         if (var5 <= 0) {
            this.endParry(var1, true);
         } else {
            var1.sendActionBar(Ui.line(Component.text("Parried! " + var5 + " left", NamedTextColor.GOLD)));
         }

         return true;
      }
   }

   private void endParry(Player var1, boolean var2) {
      this.parryHits.remove(var1.getUniqueId());
      this.parryUntil.remove(var1.getUniqueId());
      if (var1.isOnline()) {
         var1.sendActionBar(Ui.line(Component.text(var2 ? "Parry used up" : "Parry ended", NamedTextColor.GRAY)));
      }

   }

   private void windRush(Player var1) {
      if (this.ready(var1, "wind_rush", 45, "Wind Rush")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getLocation();
         var2.playSound(var3, Sound.ENTITY_BREEZE_WIND_BURST, 2.0F, 0.8F);
         var2.playSound(var3, Sound.ENTITY_BREEZE_SHOOT, 1.5F, 1.2F);
         var2.spawnParticle(Particle.GUST_EMITTER_SMALL, var3.clone().add((double)0.0F, (double)0.5F, (double)0.0F), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
         this.noFallUntil.put(var1.getUniqueId(), System.currentTimeMillis() + 5000L);
         HashSet var4 = new HashSet();
         boolean[] var5 = new boolean[]{false};
         Vector[] var6 = new Vector[]{var1.getLocation().getDirection()};
         this.every(1L, 10, (var6x) -> {
            if (!var5[0] && var1.isOnline() && !var1.isDead()) {
               Vector var7 = var1.getLocation().getDirection();
               var7.setY(Math.max(-0.4, Math.min((double)0.5F, var7.getY())));
               if (!(var7.lengthSquared() < 0.01)) {
                  var7.normalize();
                  var6[0] = var7.clone();
                  Vector var8 = var7.clone().setY(0);
                  if (var8.lengthSquared() > 0.01) {
                     Location var9 = var1.getLocation().add(var8.normalize().multiply(0.9));
                     if (!var9.getBlock().isPassable() && !var9.clone().add((double)0.0F, (double)1.0F, (double)0.0F).getBlock().isPassable()) {
                        var5[0] = true;
                        var1.setVelocity(new Vector((double)0.0F, 0.3, (double)0.0F));
                        return;
                     }
                  }

                  var1.setVelocity(var7.clone().multiply((double)1.0F));
                  var1.setFallDistance(0.0F);
                  Location var12 = var1.getLocation().add((double)0.0F, 0.8, (double)0.0F);
                  var2.spawnParticle(Particle.CLOUD, var12, 10, 0.3, 0.3, 0.3, 0.03);
                  var2.spawnParticle(Particle.SMALL_GUST, var12, 4, 0.4, 0.4, 0.4, (double)0.0F);
                  if (var6x % 2 == 0) {
                     var2.spawnParticle(Particle.GUST, var12, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
                  }

                  for(LivingEntity var11 : this.targetsNear(var1, var12, 2.2)) {
                     if (var4.add(var11.getUniqueId())) {
                        this.hurt(var11, (double)4.0F, var1, DamageType.MAGIC);
                        var11.setVelocity(var7.clone().multiply(0.4).setY(0.9));
                        var2.playSound(var11.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.2F, 1.0F);
                        var2.spawnParticle(Particle.GUST, var11.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
                     }
                  }

               }
            }
         }, () -> {
            if (var1.isOnline() && !var5[0]) {
               var1.setVelocity(var6[0].clone().multiply(0.35).setY(0.55));
               var2.spawnParticle(Particle.GUST_EMITTER_SMALL, var1.getLocation(), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            }
         });
      }
   }

   public void windStrikerProc(Player var1) {
      var1.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
      var1.getWorld().spawnParticle(Particle.GUST, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
   }

   public boolean isFallImmune(Player var1) {
      Long var2 = (Long)this.noFallUntil.get(var1.getUniqueId());
      return var2 != null && var2 > System.currentTimeMillis();
   }

   public void hellscorchProc(Player var1, LivingEntity var2) {
      if (!SafeZones.canAttack(var1, var2)) return;
      this.every(4L, 15, (var3) -> {
         if (var2.isValid()) {
            Location var4 = var2.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
            var2.getWorld().spawnParticle(Particle.FLAME, var4, 6, 0.4, 0.8, 0.4, 0.01);
            var2.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, var4, 6, 0.4, 0.8, 0.4, 0.01);
            if (var3 % 5 == 0) {
               this.hurt(var2, (double)3.0F, var1, DamageType.MAGIC);
            }

         }
      }, (Runnable)null);
   }

   private void hellRing(Player var1) {
      if (this.ready(var1, "hell_ring", 120, "Hell Ring")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getLocation().getBlock().getLocation().add((double)0.5F, (double)0.0F, (double)0.5F);
         var2.playSound(var3, Sound.ITEM_GOAT_HORN_SOUND_7, 3.0F, 0.7F);
         var2.playSound(var3, Sound.ENTITY_BLAZE_SHOOT, 1.5F, 0.6F);
         final LinkedHashMap<Block, BlockData[]> var4 = new LinkedHashMap<>();
         int var5 = var3.getBlockX();
         int var6 = var3.getBlockY();
         int var7 = var3.getBlockZ();

         for(int var8 = -8; var8 <= 8; ++var8) {
            for(int var9 = -8; var9 <= 8; ++var9) {
               if (var8 * var8 + var9 * var9 <= 64) {
                  for(int var10 = -4; var10 <= 5; ++var10) {
                     Block var11 = var2.getBlockAt(var5 + var8, var6 + var10, var7 + var9);
                     BlockData var12 = this.netherize(var11);
                     if (var12 != null && SafeZones.canModify(var1, var11)) {
                        var4.put(var11, new BlockData[]{var11.getBlockData(), var12});
                        var11.setBlockData(var12, false);
                     }
                  }
               }
            }
         }

         Runnable var13 = new Runnable() {
            public void run() {
               for(Map.Entry var2 : var4.entrySet()) {
                  Block var3 = (Block)var2.getKey();
                  if (!SafeZones.isProtected(var3.getLocation()) && var3.getType() == ((BlockData[])var2.getValue())[1].getMaterial()) {
                     var3.setBlockData(((BlockData[])var2.getValue())[0], false);
                  }
               }

               Abilities.this.pendingReverts.remove(this);
            }
         };
         this.pendingReverts.add(var13);
         this.every(10L, 20, (var4x) -> {
            this.ring(var3.clone().add((double)0.0F, 0.3, (double)0.0F), (double)8.0F, Particle.FLAME, 60);
            this.ring(var3.clone().add((double)0.0F, 0.8, (double)0.0F), (double)8.0F, Particle.SOUL_FIRE_FLAME, 60);
            this.ring(var3.clone().add((double)0.0F, 0.3, (double)0.0F), (double)8.0F, Particle.LAVA, 10);
            var2.spawnParticle(Particle.LARGE_SMOKE, var3.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 15, (double)4.0F, (double)0.5F, (double)4.0F, 0.01);
            if (var4x % 2 == 0) {
               var2.playSound(var3, Sound.BLOCK_FIRE_AMBIENT, 1.5F, 0.8F);

               for(LivingEntity target : this.targetsNear(var1, var3, ConfigFiles.stat("hell_ring", "radius", 9.0))) {
                  Location targetLocation = target.getLocation();
                  double dx = targetLocation.getX() - var3.getX();
                  double dz = targetLocation.getZ() - var3.getZ();
                  if (!(dx * dx + dz * dz > (double)64.0F) && !(Math.abs(targetLocation.getY() - var3.getY()) > (double)5.0F)) {
                     this.hurt(target, ConfigFiles.stat("hell_ring", "damage-health", 3.0), var1, DamageType.MAGIC);
                     var2.spawnParticle(Particle.FLAME, targetLocation.add((double)0.0F, (double)1.0F, (double)0.0F), 10, 0.3, 0.6, 0.3, 0.02);
                  }
               }
            }

         }, var13);
      }
   }

   private BlockData netherize(Block var1) {
      Material var2 = var1.getType();
      BlockData var3 = var1.getBlockData();
      if (var2 != Material.WATER && var2 != Material.BUBBLE_COLUMN && var2 != Material.KELP && var2 != Material.KELP_PLANT && var2 != Material.SEAGRASS && var2 != Material.TALL_SEAGRASS) {
         if (var3 instanceof Waterlogged) {
            Waterlogged var4 = (Waterlogged)var3;
            if (var4.isWaterlogged()) {
               Waterlogged var10 = (Waterlogged)var4.clone();
               var10.setWaterlogged(false);
               return var10;
            }
         }

         boolean var8 = this.rng.nextBoolean();
         if (Tag.LOGS.isTagged(var2)) {
            BlockData var9 = (var8 ? Material.WARPED_STEM : Material.CRIMSON_STEM).createBlockData();
            if (var3 instanceof Orientable) {
               Orientable var6 = (Orientable)var3;
               if (var9 instanceof Orientable) {
                  Orientable var7 = (Orientable)var9;
                  var7.setAxis(var6.getAxis());
               }
            }

            return var9;
         } else if (Tag.LEAVES.isTagged(var2)) {
            return (var8 ? Material.WARPED_WART_BLOCK : Material.NETHER_WART_BLOCK).createBlockData();
         } else {
            Material var10000;
            switch (var2) {
               case GRASS_BLOCK:
               case MYCELIUM:
               case PODZOL:
               case MOSS_BLOCK:
                  var10000 = var8 ? Material.WARPED_NYLIUM : Material.CRIMSON_NYLIUM;
                  break;
               case DIRT:
               case COARSE_DIRT:
               case ROOTED_DIRT:
               case MUD:
               case FARMLAND:
               case DIRT_PATH:
               case CLAY:
                  var10000 = Material.NETHERRACK;
                  break;
               case STONE:
               case COBBLESTONE:
               case ANDESITE:
               case DIORITE:
               case GRANITE:
               case TUFF:
               case CALCITE:
               case DEEPSLATE:
               case COBBLED_DEEPSLATE:
                  var10000 = this.rng.nextDouble() < 0.3 ? Material.BASALT : Material.BLACKSTONE;
                  break;
               case SAND:
               case RED_SAND:
                  var10000 = Material.SOUL_SAND;
                  break;
               case GRAVEL:
                  var10000 = Material.SOUL_SOIL;
                  break;
               case SANDSTONE:
               case RED_SANDSTONE:
                  var10000 = Material.SMOOTH_BASALT;
                  break;
               case ICE:
               case PACKED_ICE:
               case BLUE_ICE:
               case SNOW_BLOCK:
                  var10000 = Material.MAGMA_BLOCK;
                  break;
               case SNOW:
                  var10000 = Material.AIR;
                  break;
               default:
                  var10000 = null;
            }

            Material var5 = var10000;
            return var5 == null ? null : var5.createBlockData();
         }
      } else {
         return Material.AIR.createBlockData();
      }
   }

   public void abyssalProc(LivingEntity var1) {
      if (SafeZones.isProtected(var1.getLocation())) return;
      var1.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
      var1.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
      var1.getWorld().spawnParticle(Particle.SQUID_INK, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 20, 0.3, (double)0.5F, 0.3, 0.05);
      var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.7F, 0.6F);
   }

   private void abyssalVoid(Player var1) {
      if (this.ready(var1, "abyssal_void", 120, "Abyssal Void")) {
         World var2 = var1.getWorld();
         RayTraceResult var3 = var1.rayTraceBlocks((double)15.0F);
         Location var4 = var3 != null ? var3.getHitPosition().toLocation(var2).add((double)0.0F, (double)2.0F, (double)0.0F) : var1.getEyeLocation().add(var1.getEyeLocation().getDirection().multiply(8));
         var2.playSound(var4, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.0F, 0.5F);
         var2.playSound(var4, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.5F);
         this.every(1L, 200, (var4x) -> {
            double var5 = (double)var4x * 0.3;

            for(int var7 = 0; var7 < 4; ++var7) {
               for(double var8 = 0.8; var8 <= (double)4.0F; var8 += 0.6) {
                  double var10 = var5 + (double)var7 * Math.PI / (double)2.0F + var8 * 0.8;
                  Location var12 = var4.clone().add(Math.cos(var10) * var8, (double)0.0F, Math.sin(var10) * var8);
                  var2.spawnParticle(Particle.DUST, var12, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, var7 % 2 == 0 ? VOID : BLACK);
               }
            }

            var2.spawnParticle(Particle.DUST, var4, 8, 0.35, 0.35, 0.35, (double)0.0F, VOID);
            var2.spawnParticle(Particle.PORTAL, var4, 25, (double)3.0F, (double)3.0F, (double)3.0F, 1.2);
            var2.spawnParticle(Particle.REVERSE_PORTAL, var4, 4, 0.2, 0.2, 0.2, 0.02);
            if (var4x % 20 == 0) {
               var2.playSound(var4, Sound.BLOCK_PORTAL_AMBIENT, 1.2F, 0.5F);
            }

            for(Entity var14 : var2.getNearbyEntities(var4, (double)10.0F, (double)10.0F, (double)10.0F)) {
               if (!(var14.getLocation().distanceSquared(var4) > (double)100.0F) && (var14 instanceof Item || this.isTarget(var1, var14))) {
                  Vector var9 = var4.toVector().subtract(var14.getLocation().toVector());
                  double var15 = var9.length();
                  if (var15 > 0.7) {
                     var14.setVelocity(var9.normalize().multiply(Math.min(0.6, 0.15 + ((double)10.0F - var15) * 0.04)));
                  } else {
                     var14.setVelocity(new Vector());
                  }

                  if (var4x % 20 == 0 && var14 instanceof LivingEntity) {
                     LivingEntity var16 = (LivingEntity)var14;
                     this.hurt(var16, (double)2.0F, var1, DamageType.MAGIC);
                     this.shredArmor(var16, 10);
                     var16.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                     var16.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                  }
               }
            }

         }, () -> {
            var2.spawnParticle(Particle.REVERSE_PORTAL, var4, 200, 0.1, 0.1, 0.1, (double)1.0F);
            var2.playSound(var4, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5F, 0.5F);
         });
      }
   }

   private void cosmicBlessing(Player var1) {
      if (this.ready(var1, "cosmic_blessing", 150, "Cosmic Blessing")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
         var2.playSound(var3, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.5F);
         var2.playSound(var3, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5F, 0.6F);

         for(int var4 = 0; var4 < 120; ++var4) {
            Vector var5 = (new Vector(this.rng.nextGaussian(), this.rng.nextGaussian(), this.rng.nextGaussian())).normalize().multiply((double)1.5F + this.rng.nextDouble());
            var2.spawnParticle(Particle.DUST, var3.clone().add(var5), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, var4 % 2 == 0 ? BLACK : WHITE);
         }

         this.endBlessing(var1);
         Map<PotionEffectType, Integer> var6 = new HashMap<>();
         this.boosted.put(var1.getUniqueId(), var6);
         this.every(5L, 60, (var4x) -> {
            if (var1.isOnline() && !var1.isDead()) {
               double var5 = (double)var4x * 0.4;
               Location var7 = var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
               var2.spawnParticle(Particle.DUST, var7.clone().add(Math.cos(var5) * 0.9, (double)0.0F, Math.sin(var5) * 0.9), 2, 0.05, 0.05, 0.05, (double)0.0F, BLACK);
               var2.spawnParticle(Particle.DUST, var7.clone().add(-Math.cos(var5) * 0.9, (double)0.0F, -Math.sin(var5) * 0.9), 2, 0.05, 0.05, 0.05, (double)0.0F, WHITE);
               this.boostEffects(var1, var6);
            }
         }, () -> this.endBlessing(var1));
      }
   }

   private void boostEffects(Player var1, Map<PotionEffectType, Integer> var2) {
      for(PotionEffect var4 : new ArrayList<>(var1.getActivePotionEffects())) {
         Integer var5 = (Integer)var2.get(var4.getType());
         if ((var5 == null || var4.getAmplifier() != var5) && var4.getAmplifier() < 254) {
            int var6 = var4.getAmplifier() + 1;
            var1.addPotionEffect(new PotionEffect(var4.getType(), var4.getDuration(), var6, var4.isAmbient(), var4.hasParticles(), var4.hasIcon()));
            var2.put(var4.getType(), var6);
         }
      }

   }

   private void endBlessing(Player var1) {
      Map<PotionEffectType, Integer> var2 = this.boosted.remove(var1.getUniqueId());
      if (var2 != null && var1.isOnline()) {
         var2.forEach((var1x, var2x) -> {
            PotionEffect var3 = var1.getPotionEffect(var1x);
            if (var3 != null && var3.getAmplifier() == var2x) {
               var1.removePotionEffect(var1x);
               if (var2x - 1 >= 0) {
                  var1.addPotionEffect(new PotionEffect(var1x, var3.getDuration(), var2x - 1, var3.isAmbient(), var3.hasParticles(), var3.hasIcon()));
               }

            }
         });
      }
   }

   public void stormProc(Player var1, LivingEntity var2) {
      if (!SafeZones.canAttack(var1, var2)) return;
      if (var2.isValid()) {
         var2.getWorld().strikeLightningEffect(var2.getLocation());
         this.hurt(var2, (double)5.0F, var1, DamageType.LIGHTNING_BOLT);
         this.status.apply(var2, StatusManager.Status.STUN, 40);
         this.sparks(var2, 20);
      }
   }

   private void divineSmite(Player var1) {
      if (this.ready(var1, "divine_smite", 90, "Divine Smite")) {
         World var2 = var1.getWorld();
         Location var3 = var1.getLocation();
         var2.playSound(var3, Sound.ITEM_TRIDENT_THUNDER, 2.0F, 1.0F);
         List<LivingEntity> var4 = this.targetsNear(var1, var3, ConfigFiles.stat("divine_smite", "radius", 7.0));

         for(LivingEntity var6 : var4) {
            this.status.apply(var6, StatusManager.Status.STUN, 100);
            var2.strikeLightningEffect(var6.getLocation());
         }

         this.every(2L, 50, (var5) -> {
            if (var5 % 5 == 0) {
               this.ring(var3.clone().add((double)0.0F, 0.2, (double)0.0F), (double)7.0F, Particle.ELECTRIC_SPARK, 70);
               this.ring(var3.clone().add((double)0.0F, 0.2, (double)0.0F), (double)7.0F, Particle.END_ROD, 25);
            }

            for(LivingEntity var7 : var4) {
               if (var7.isValid()) {
                  var2.spawnParticle(Particle.ELECTRIC_SPARK, var7.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 8, 0.4, 0.8, 0.4, 0.05);
                  if (var5 == 10 || var5 == 25 || var5 == 40) {
                     var2.strikeLightningEffect(var7.getLocation());
                     this.hurt(var7, ConfigFiles.stat("divine_smite", "damage-health", 5.0), var1, DamageType.LIGHTNING_BOLT);
                  }
               }
            }

         }, (Runnable)null);
      }
   }

   private void sparks(LivingEntity var1, int var2) {
      this.every(2L, var2, (var1x) -> {
         if (var1.isValid()) {
            var1.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 6, 0.4, 0.8, 0.4, 0.05);
         }

      }, (Runnable)null);
   }

   private void naturesWrath(Player var1) {
      if (this.ready(var1, "natures_wrath", 120, "Nature's Wrath")) {
         World var2 = var1.getWorld();
         var2.playSound(var1.getLocation(), Sound.BLOCK_MOSS_PLACE, 2.0F, 0.5F);
         var2.playSound(var1.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.5F, 0.6F);
         List<LivingEntity> var3 = this.targetsNear(var1, var1.getLocation(), (double)8.0F);
         BlockData var4 = Material.VINE.createBlockData();

         for(LivingEntity var6 : var3) {
            this.status.apply(var6, StatusManager.Status.STUN, 200);
            var6.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 4));
            var6.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4));
            var6.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
         }

         if (!var3.isEmpty()) {
            var1.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, var3.size()));
         }

         var1.sendActionBar(Ui.line(Component.text("Nature's Wrath grabbed " + var3.size() + " target(s)", NamedTextColor.GREEN)));
         this.every(4L, 50, (var5) -> {
            for(LivingEntity var7 : var3) {
               if (var7.isValid()) {
                  Location var8 = var7.getLocation();
                  double var9 = (double)var5 * 0.6;

                  for(double var11 = (double)0.0F; var11 < (double)2.0F; var11 += (double)0.25F) {
                     Location var13 = var8.clone().add(Math.cos(var9 + var11 * (double)3.0F) * 0.6, var11, Math.sin(var9 + var11 * (double)3.0F) * 0.6);
                     var2.spawnParticle(Particle.BLOCK, var13, 2, 0.05, 0.05, 0.05, (double)0.0F, var4);
                  }

                  var2.spawnParticle(Particle.SPORE_BLOSSOM_AIR, var8.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 3, 0.4, 0.6, 0.4, (double)0.0F);
                  if (var5 % 5 == 0) {
                     this.hurt(var7, ConfigFiles.stat("natures_wrath", "damage-health", 3.0), var1, DamageType.MAGIC);
                  }
               }
            }

         }, (Runnable)null);
      }
   }

   public void silencedShieldProc(Player var1, Player var2) {
      if (!SafeZones.canAttack(var1, var2)) return;
      if (var2.isValid() && var1.isValid()) {
         var2.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
         this.status.apply(var2, StatusManager.Status.SILENCE, 100);
         this.teleportBehind(var1, var2);
      }
   }

   private void silentBinding(Player var1) {
      Entity var2 = var1.getTargetEntity(30);
      if (var2 instanceof Player var3) {
         if (this.isTarget(var1, var3)) {
            if (!this.ready(var1, "silent_binding", 120, "Silent Binding")) {
               return;
            }

            for(PotionEffect var5 : new ArrayList<>(var3.getActivePotionEffects())) {
               var3.removePotionEffect(var5.getType());
            }

            var3.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
            this.status.apply(var3, StatusManager.Status.ROOT, 100);
            this.status.apply(var3, StatusManager.Status.SILENCE, 100);
            this.teleportBehind(var1, var3);
            var3.sendActionBar(Ui.line(Component.text("You have been bound...", NamedTextColor.DARK_GRAY)));
            World var6 = var3.getWorld();
            var6.playSound(var3.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 1.5F, 0.6F);
            this.every(4L, 25, (var3x) -> {
               if (var3.isValid()) {
                  this.ring(var3.getLocation().add((double)0.0F, 0.1, (double)0.0F), 0.8, Particle.SMOKE, 12);
                  var6.spawnParticle(Particle.SCULK_SOUL, var3.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 2, 0.3, (double)0.5F, 0.3, 0.01);
               }
            }, (Runnable)null);
            return;
         }
      }

      var1.sendActionBar(Ui.line(Component.text("Look at a player to bind them", NamedTextColor.RED)));
   }

   private void shadowStalker(Player var1) {
      if (this.ready(var1, "shadow_stalker", 90, "Shadow Stalker")) {
         Location var2 = var1.getLocation();
         ArrayList<Player> var3 = new ArrayList<>();

         for(Player var5 : var1.getWorld().getPlayers()) {
            if (var5 != var1 && var5.getGameMode() != GameMode.SPECTATOR && !this.trust.trusts(var1.getUniqueId(), var5.getUniqueId()) && !(var5.getLocation().distanceSquared(var2) > (double)10000.0F)) {
               var3.add(var5);
            }
         }

         var3.sort(Comparator.comparingDouble((var1x) -> var1x.getLocation().distanceSquared(var2)));

         for(Player var14 : var3) {
            var14.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
            var14.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0));
            var14.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
            this.status.apply(var14, StatusManager.Status.SILENCE, 200);
            var14.sendMessage(Ui.line(Component.text("You feel like you're being watched...", NamedTextColor.DARK_GRAY, new TextDecoration[]{TextDecoration.ITALIC})));
            var14.playSound(var14.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.5F);
         }

         var1.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false, true));
         var1.getWorld().spawnParticle(Particle.SQUID_INK, var2.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 30, 0.4, 0.6, 0.4, 0.02);
         int var13 = Math.max(9, Math.min(54, (var3.size() + 8) / 9 * 9));
         StalkerMenu var15 = new StalkerMenu();
         Inventory var6 = Bukkit.createInventory(var15, var13, Component.text("Shadow Stalker - " + var3.size() + " player(s)", NamedTextColor.DARK_GRAY));
         var15.inventory = var6;

         for(int var7 = 0; var7 < Math.min(var3.size(), var13); ++var7) {
            Player var8 = (Player)var3.get(var7);
            ItemStack var9 = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta var10 = (SkullMeta)var9.getItemMeta();
            var10.setOwningPlayer(var8);
            var10.displayName(Component.text(var8.getName(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            Location var11 = var8.getLocation();
            int var10001 = (int)var11.distance(var2);
            Component var16 = line("Distance: " + var10001 + " blocks (" + direction(var2, var11) + ")");
            int var10002 = var11.getBlockX();
            Component var17 = line("Coords: " + var10002 + ", " + var11.getBlockY() + ", " + var11.getBlockZ());
            Object[] var10004 = new Object[]{var8.getHealth() / (double)2.0F};
            var10.lore(List.of(var16, var17, line("Health: " + String.format("%.1f", var10004) + " ❤")));
            var9.setItemMeta(var10);
            var6.setItem(var7, var9);
         }

         var1.openInventory(var6);
      }
   }

   private static Component line(String var0) {
      return Component.text(var0, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
   }

   private static String direction(Location var0, Location var1) {
      double var2 = var1.getX() - var0.getX();
      double var4 = var1.getZ() - var0.getZ();
      double var6 = Math.toDegrees(Math.atan2(-var2, var4));
      if (var6 < (double)0.0F) {
         var6 += (double)360.0F;
      }

      String[] var8 = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
      return var8[(int)Math.round(var6 / (double)45.0F) % 8];
   }

   private void tidalWave(Player var1) {
      if (this.ready(var1, "tidal_wave", 50, "Tidal Wave")) {
         World var2 = var1.getWorld();
         Vector var3 = var1.getLocation().getDirection().setY(0);
         if (var3.lengthSquared() < 0.01) {
            var3 = new Vector(0, 0, 1);
         }

         Location var4 = var1.getLocation().add(var3.normalize().multiply(7));
         var2.playSound(var4, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 2.0F, 0.5F);
         var2.playSound(var4, Sound.BLOCK_CONDUIT_ACTIVATE, 2.0F, 0.8F);
         this.every(1L, 200, (var4x) -> {
            if (var4x < 20) {
               double var5 = (double)14.0F - (double)var4x * (double)0.5F;

               for(double var7 = (double)0.0F; var7 < (Math.PI * 2D); var7 += 0.15) {
                  for(int var9 = 0; var9 < 4; ++var9) {
                     Location var10 = var4.clone().add(Math.cos(var7) * var5, (double)var9, Math.sin(var7) * var5);
                     var2.spawnParticle(var9 == 3 ? Particle.CLOUD : Particle.FALLING_WATER, var10, 1, 0.1, 0.1, 0.1, (double)0.0F);
                  }
               }
            }

            double var16 = (double)var4x * 0.3;

            for(int var17 = 0; var17 < 3; ++var17) {
               for(double var8 = (double)0.5F; var8 <= (double)6.0F; var8 += (double)0.5F) {
                  double var20 = var16 + (double)var17 * 2.094 + var8 * 0.6;
                  var2.spawnParticle(Particle.SPLASH, var4.clone().add(Math.cos(var20) * var8, 0.2 + ((double)6.0F - var8) * 0.1, Math.sin(var20) * var8), 2, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
               }
            }

            if (var4x % 2 == 0) {
               var2.spawnParticle(Particle.FALLING_WATER, var4, 20, (double)5.0F, (double)0.5F, (double)5.0F, (double)0.0F);
            }

            if (var4x % 20 == 0) {
               var2.playSound(var4, Sound.ENTITY_GENERIC_SPLASH, 1.5F, 0.6F);
            }

            double var18 = var4x < 20 ? 0.45 : 0.22;

            for(LivingEntity var21 : this.targetsNear(var1, var4, (double)14.0F)) {
               Vector var11 = var4.toVector().subtract(var21.getLocation().toVector()).setY(0);
               double var12 = var11.length();
               Vector var14 = new Vector(-var11.getZ(), (double)0.0F, var11.getX());
               Vector var15 = new Vector();
               if (var12 > (double)0.5F) {
                  var15.add(var11.clone().normalize().multiply(var18));
               }

               if (var14.lengthSquared() > 0.001) {
                  var15.add(var14.normalize().multiply(0.3));
               }

               var15.setY(Math.max(var21.getVelocity().getY(), -0.2));
               var21.setVelocity(var15);
               if (var4x % 20 == 0) {
                  this.hurt(var21, (double)4.0F, var1, DamageType.MAGIC);
                  this.shredArmor(var21, 5);
               }
            }

         }, (Runnable)null);
      }
   }

   private void riptide(Player var1) {
      if (this.ready(var1, "riptide", 90, "Riptide")) {
         World var2 = var1.getWorld();
         HashMap var3 = new HashMap();
         this.noFallUntil.put(var1.getUniqueId(), System.currentTimeMillis() + 9000L);
         var2.playSound(var1.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5F, 1.0F);
         this.every(1L, 120, (var4) -> {
            if (var1.isOnline() && !var1.isDead()) {
               Vector var5 = var1.getLocation().getDirection().setY(0);
               if (!(var5.lengthSquared() < 0.01)) {
                  var5.normalize();
                  Vector var6 = var5.clone().multiply(0.65);
                  Block var7 = var1.getLocation().add(var5.clone().multiply(0.8)).getBlock();
                  double var8 = var1.getVelocity().getY();
                  if (!var7.isPassable() && var7.getRelative(BlockFace.UP).isPassable()) {
                     var8 = 0.42;
                  } else if (var8 < -0.1) {
                     var8 = -0.1;
                  }

                  var6.setY(var8);
                  var1.setVelocity(var6);
                  var1.setFallDistance(0.0F);
                  Location var10 = var1.getLocation();
                  var2.spawnParticle(Particle.SPLASH, var10, 20, 0.9, 0.1, 0.9, (double)0.0F);
                  var2.spawnParticle(Particle.FALLING_WATER, var10.clone().add((double)0.0F, 0.4, (double)0.0F), 8, (double)1.0F, 0.3, (double)1.0F, (double)0.0F);
                  var2.spawnParticle(Particle.CLOUD, var10.clone().subtract(var5), 3, 0.4, 0.2, 0.4, 0.02);
                  var2.spawnParticle(Particle.DOLPHIN, var10, 6, 0.8, 0.3, 0.8, (double)0.0F);
                  if (var4 % 10 == 0) {
                     var2.playSound(var10, Sound.ENTITY_GENERIC_SPLASH, 0.8F, 1.2F);
                  }

                  for(LivingEntity var12 : this.targetsNear(var1, var10, (double)2.5F)) {
                     int var13 = (Integer)var3.getOrDefault(var12.getUniqueId(), -100);
                     if (var4 - var13 >= 20) {
                        var3.put(var12.getUniqueId(), var4);
                        this.hurt(var12, (double)3.0F, var1, DamageType.MAGIC);
                        var12.setVelocity(var5.clone().multiply(0.8).setY(0.35));
                     }
                  }

               }
            }
         }, () -> {
            if (var1.isOnline()) {
               var2.playSound(var1.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0F, 1.0F);
            }

         });
      }
   }

   private boolean ready(Player var1, String var2, int var3, String var4) {
      if (!ConfigFiles.abilityEnabled(var2) || !SafeZones.guardAbility(var1)) return false;
      long var5 = this.cooldowns.remaining(var1, var2);
      if (var5 > 0L) {
         var1.sendActionBar(Ui.line(Component.text(var4 + " is on cooldown: " + Cooldowns.format(var5), NamedTextColor.RED)));
         return false;
      } else {
         this.cooldowns.set(var1, var2, (long)var3 * 1000L);
         var1.sendActionBar(Ui.line(Component.text("Used " + var4 + "!", NamedTextColor.GREEN)));
         return true;
      }
   }

   public boolean isTarget(Player var1, Entity var2) {
      if (!SafeZones.canAttack(var1, var2)) return false;
      SafeZones.trackAbilityMotion(var2, 2000L);
      if (var2 instanceof LivingEntity var3) {
         if (var2 != var1 && !var3.isDead() && !(var2 instanceof ArmorStand)) {
            if (var2 instanceof Player) {
               Player var4 = (Player)var2;
               if (var4.getGameMode() == GameMode.CREATIVE || var4.getGameMode() == GameMode.SPECTATOR) {
                  return false;
               }

               if (this.trust.trusts(var1.getUniqueId(), var4.getUniqueId())) {
                  return false;
               }
            }

            return true;
         }
      }

      return false;
   }

   private List<LivingEntity> targetsNear(Player var1, Location var2, double var3) {
      ArrayList var5 = new ArrayList();

      for(LivingEntity var7 : var2.getWorld().getNearbyLivingEntities(var2, var3)) {
         if (var7.getLocation().distanceSquared(var2) <= var3 * var3 && this.isTarget(var1, var7)) {
            var5.add(var7);
         }
      }

      return var5;
   }

   private void hurt(LivingEntity var1, double var2, Player var4, DamageType var5) {
      if (var1.isValid() && SafeZones.canAttack(var4, var1)) {
         var1.setNoDamageTicks(0);
         var1.damage(var2, DamageSource.builder(var5).withCausingEntity(var4).withDirectEntity(var4).build());
      }
   }

   private void shredArmor(LivingEntity var1, int var2) {
      if (!SafeZones.isProtected(var1.getLocation()) && var1.getEquipment() != null) {
         for(EquipmentSlot var6 : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack var7 = var1.getEquipment().getItem(var6);
            if (var7 != null && !var7.isEmpty()) {
               var1.damageItemStack(var6, var2);
            }
         }

      }
   }

   private void teleportBehind(Player var1, LivingEntity var2) {
      if (!SafeZones.canAttack(var1, var2)) return;
      Location var3 = var2.getLocation();
      Vector var4 = var3.getDirection().setY(0);
      if (var4.lengthSquared() < 0.01) {
         var4 = new Vector(0, 0, 1);
      }

      Location var5 = var3.clone().add(var4.normalize().multiply((double)-1.5F));
      if (!SafeZones.isProtected(var5) && var5.getBlock().isPassable() && var5.clone().add((double)0.0F, (double)1.0F, (double)0.0F).getBlock().isPassable()) {
         var5.setDirection(var3.toVector().subtract(var5.toVector()));
         var1.getWorld().spawnParticle(Particle.SQUID_INK, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 20, 0.3, (double)0.5F, 0.3, 0.02);
         var1.teleport(var5);
         var1.getWorld().playSound(var5, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.6F);
      }
   }

   private void ring(Location var1, double var2, Particle var4, int var5) {
      World var6 = var1.getWorld();

      for(int var7 = 0; var7 < var5; ++var7) {
         double var8 = (Math.PI * 2D) * (double)var7 / (double)var5;
         var6.spawnParticle(var4, var1.clone().add(Math.cos(var8) * var2, (double)0.0F, Math.sin(var8) * var2), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
      }

   }

   private BukkitTask every(long var1, final int var3, final IntConsumer var4, final Runnable var5) {
      return (new BukkitRunnable() {
         int i = 0;

         public void run() {
            if (this.i >= var3) {
               this.cancel();
               if (var5 != null) {
                  var5.run();
               }

            } else {
               var4.accept(this.i++);
            }
         }
      }).runTaskTimer(this.plugin, 0L, var1);
   }

   public void onQuit(Player var1) {
      this.endBlessing(var1);
      this.parryHits.remove(var1.getUniqueId());
      this.parryUntil.remove(var1.getUniqueId());
   }

   public void shutdown() {
      for(Runnable var2 : new ArrayList<>(this.pendingReverts)) {
         var2.run();
      }

      for(Player var4 : Bukkit.getOnlinePlayers()) {
         this.endBlessing(var4);
      }

   }

   static {
      BLACK = new Particle.DustOptions(Color.BLACK, 1.6F);
      WHITE = new Particle.DustOptions(Color.WHITE, 1.6F);
      VOID = new Particle.DustOptions(Color.fromRGB(20, 0, 30), 2.4F);
      BLOOD = new Particle.DustOptions(Color.fromRGB(170, 0, 20), 1.4F);
   }

   public static final class StalkerMenu implements InventoryHolder {
      Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
