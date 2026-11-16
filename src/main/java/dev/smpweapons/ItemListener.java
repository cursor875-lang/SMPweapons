package dev.smpweapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public final class ItemListener implements Listener {
   private final SMPWeapons plugin;
   private final Abilities abilities;
   private final StatusManager status;
   private final Map<UUID, Map<CustomItem, Integer>> hitCounts = new HashMap();
   private final Map<UUID, Long> lastSneak = new HashMap();

   public ItemListener(SMPWeapons var1, Abilities var2, StatusManager var3) {
      this.plugin = var1;
      this.abilities = var2;
      this.status = var3;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onSwap(PlayerSwapHandItemsEvent var1) {
      Player var2 = var1.getPlayer();
      Items.upgradeDragonEgg(var2);
      CustomItem var3 = Items.identify(var2.getInventory().getItemInMainHand());
      if (var3 == null) {
         var3 = Items.identify(var2.getInventory().getItemInOffHand());
      }

      if (var3 == CustomItem.FLOWER_SWORD) return;
      if (var3 == CustomItem.PLATE_OF_LIFE || var3 == CustomItem.OCEANIC_CROWN) {
         var3 = null;
      }

      if (var3 == null && Items.wearing(var2, CustomItem.OCEANIC_CROWN)) {
         var3 = CustomItem.OCEANIC_CROWN;
      }

      if (var3 == null && Items.wearing(var2, CustomItem.PLATE_OF_LIFE)) {
         var3 = CustomItem.PLATE_OF_LIFE;
      }

      if (var3 != null) {
         var1.setCancelled(true);
         if (!this.status.has(var2, StatusManager.Status.STUN)) {
            this.abilities.trigger(var2, var3, var2.isSneaking());
         }
      }
   }

   @EventHandler
   public void onSneak(PlayerToggleSneakEvent var1) {
      if (var1.isSneaking()) {
         Player var2 = var1.getPlayer();
         long var3 = System.currentTimeMillis();
         Long var5 = (Long)this.lastSneak.put(var2.getUniqueId(), var3);
         if (var5 != null && var3 - var5 <= 500L) {
            this.lastSneak.remove(var2.getUniqueId());
            if (!this.status.has(var2, StatusManager.Status.STUN)) {
               boolean var6 = Items.wearing(var2, CustomItem.OCEANIC_CROWN);
               boolean var7 = Items.wearing(var2, CustomItem.PLATE_OF_LIFE);
               float var8 = var2.getLocation().getPitch();
               if (!var6 || !(var8 < -45.0F) && var7) {
                  if (var6 && var8 > 45.0F) {
                     this.abilities.trigger(var2, CustomItem.OCEANIC_CROWN, true);
                  } else if (var7) {
                     this.abilities.trigger(var2, CustomItem.PLATE_OF_LIFE, false);
                  }
               } else {
                  this.abilities.trigger(var2, CustomItem.OCEANIC_CROWN, var8 > 45.0F);
               }

            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = true
   )
   public void onStunnedAttack(EntityDamageByEntityEvent var1) {
      if (var1.getCause() == DamageCause.ENTITY_ATTACK && this.status.has(var1.getDamager(), StatusManager.Status.STUN)) {
         var1.setCancelled(true);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onHit(EntityDamageByEntityEvent var1) {
      if (SafeZones.isProtected(var1.getEntity().getLocation()) || SafeZones.isProtected(var1.getDamager().getLocation())) return;
      Entity var2 = var1.getDamager();
      EntityDamageEvent.DamageCause var3 = var1.getCause();
      Entity var5 = var1.getEntity();
      if (var5 instanceof Player var4) {
         if (var3 == DamageCause.ENTITY_ATTACK || var3 == DamageCause.ENTITY_SWEEP_ATTACK || var3 == DamageCause.PROJECTILE) {
            Entity var10000;
            label74: {
               if (var2 instanceof Projectile) {
                  Projectile var7 = (Projectile)var2;
                  ProjectileSource var8 = var7.getShooter();
                  if (var8 instanceof Entity) {
                     Entity var6 = (Entity)var8;
                     var10000 = var6;
                     break label74;
                  }
               }

               var10000 = var2;
            }

            var5 = var10000;
            if (this.abilities.tryParry(var4, var5)) {
               var1.setCancelled(true);
               return;
            }
         }
      }

      if (var3 == DamageCause.ENTITY_ATTACK && var2 instanceof Player var9) {
         Entity var12 = var1.getEntity();
         if (var12 instanceof LivingEntity var11) {
            CustomItem var13 = Items.identify(var9.getInventory().getItemInMainHand());
            if (var13 == CustomItem.FROSTBITE_AXE || var13 == CustomItem.VOID_REAVER) {
               this.abilities.axePassiveHit(var9, var11, var13, var1);
            }

            if (Items.inMainHand(var9, CustomItem.BLOODMOON_KATANA) && this.abilities.hasAutoCrit(var9) && !var1.isCritical()) {
               var1.setDamage(var1.getDamage() * (double)1.5F);
               var11.getWorld().spawnParticle(Particle.CRIT, var11.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 15, 0.4, (double)0.5F, 0.4, 0.2);
               var11.getWorld().playSound(var11.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
            }

            if (Items.inMainHand(var9, CustomItem.ABYSSAL_BLADE) && this.count(var9, CustomItem.ABYSSAL_BLADE, ConfigFiles.integer("passives.yml", "passives.abyssal-blade.hit-trigger", 15))) {
               this.later(() -> this.abilities.abyssalProc(var11));
            }

            if (Items.inMainHand(var9, CustomItem.STORMCHARGER) && this.count(var9, CustomItem.STORMCHARGER, ConfigFiles.integer("passives.yml", "passives.stormcharger.hit-trigger", 10))) {
               this.later(() -> this.abilities.stormProc(var9, var11));
            }

            if (Items.inMainHand(var9, CustomItem.WINDSTRIKER) && this.count(var9, CustomItem.WINDSTRIKER, ConfigFiles.integer("passives.yml", "passives.windstriker.hit-trigger", 10))) {
               this.later(() -> this.abilities.windStrikerProc(var9));
            }

            if (Items.carrying(var9, CustomItem.HORN_OF_HELLSCORCH) && this.count(var9, CustomItem.HORN_OF_HELLSCORCH, ConfigFiles.integer("passives.yml", "passives.horn-of-hellscorch.hit-trigger", 15))) {
               this.later(() -> this.abilities.hellscorchProc(var9, var11));
            }

            if (Items.inMainHand(var9, CustomItem.SCULK_REAPER) && this.count(var9, CustomItem.SCULK_REAPER, ConfigFiles.integer("passives.yml", "passives.sculk-reaper.hit-trigger", 5))) {
               this.later(() -> this.abilities.bleed(var9, var11));
            }

            return;
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onLifesteal(EntityDamageByEntityEvent var1) {
      if (SafeZones.isProtected(var1.getEntity().getLocation()) || SafeZones.isProtected(var1.getDamager().getLocation())) return;
      if (var1.getCause() == DamageCause.ENTITY_ATTACK) {
         Entity var3 = var1.getDamager();
         if (var3 instanceof Player) {
            Player var2 = (Player)var3;
            if (var1.getEntity() instanceof LivingEntity && Items.inMainHand(var2, CustomItem.BLOODMOON_KATANA)) {
               double var5 = var1.getFinalDamage() * ConfigFiles.number("passives.yml", "passives.bloodmoon-katana.lifesteal-fraction", 0.15);
               if (var5 > (double)0.0F) {
                  var2.heal(var5);
               }

               return;
            }

            return;
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onWorldbreakerMine(BlockBreakEvent var1) {
      Player var2 = var1.getPlayer();
      if (!SafeZones.canModify(var2, var1.getBlock())) return;
      if (Items.inMainHand(var2, CustomItem.WORLD_BREAKER)) {
         Block var3 = var1.getBlock();
         ItemStack var4 = var2.getInventory().getItemInMainHand();

         for(int var5 = -1; var5 <= 1; ++var5) {
            for(int var6 = -1; var6 <= 1; ++var6) {
               for(int var7 = -1; var7 <= 1; ++var7) {
                  if (var5 != 0 || var6 != 0 || var7 != 0) {
                     Block var8 = var3.getRelative(var5, var6, var7);
                     if (SafeZones.canModify(var2, var8) && !var8.getType().isAir() && !(var8.getType().getHardness() < 0.0F) && !(var8.getState() instanceof TileState) && this.isPickaxeBlock(var8.getType())) {
                        var8.breakNaturally(var4);
                     }
                  }
               }
            }
         }

      }
   }

   private boolean isPickaxeBlock(Material var1) {
      return Tag.MINEABLE_PICKAXE.isTagged(var1);
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onDragonEggDamage(EntityDamageEvent var1) {
      Entity var3 = var1.getEntity();
      if (var3 instanceof Player var2) {
         if (Items.holding(var2, CustomItem.DRAGON_EGG)) {
            if (var1.getCause() == DamageCause.DRAGON_BREATH) {
               var1.setCancelled(true);
            }

            return;
         }
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST,
      ignoreCancelled = true
   )
   public void onDamageWhileShelled(EntityDamageEvent var1) {
      Entity var3 = var1.getEntity();
      if (var3 instanceof Player var2) {
         if (this.abilities.isInvulnerable(var2) && var1.getCause() != DamageCause.VOID && var1.getCause() != DamageCause.KILL) {
            var1.setCancelled(true);
         }
      }

   }

   @EventHandler
   public void onPlayerKill(PlayerDeathEvent var1) {
      Player var2 = var1.getPlayer().getKiller();
      if (var2 != null && var2 != var1.getPlayer()) {
         if (Items.inMainHand(var2, CustomItem.BLOODMOON_KATANA)) {
            int var5 = ReaperKills.KATANA.add(var2.getUniqueId());
            Items.refreshKillLore(var2);
            if (var5 == 3) {
               var2.sendMessage(Component.text("Bloodmoon Katana kill 3 - unlocked: Killing Intent (F)", NamedTextColor.DARK_RED));
            } else {
               var2.sendActionBar(Component.text("Bloodmoon Katana kills: " + var5, NamedTextColor.DARK_RED));
            }

         } else if (Items.inMainHand(var2, CustomItem.SCULK_REAPER)) {
            int var3 = ReaperKills.REAPER.add(var2.getUniqueId());
            Items.refreshKillLore(var2);
            var2.heal((double)6.0F);
            String var10000;
            switch (var3) {
               case 1 -> var10000 = "Permanent Speed I";
               case 2 -> var10000 = "Permanent Strength II";
               case 3 -> var10000 = "Permanent Fire Resistance";
               case 4 -> var10000 = "Echo Shell (Shift+F)";
               case 5 -> var10000 = "Sculk Hook (Right-click)";
               default -> var10000 = null;
            }

            String var4 = var10000;
            var2.playSound(var2.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0F, 1.0F);
            if (var4 != null) {
               var2.sendMessage(Component.text("Sculk Reaper kill " + var3 + " - unlocked: " + var4, NamedTextColor.DARK_AQUA));
            } else {
               var2.sendActionBar(Component.text("Sculk Reaper kills: " + var3, NamedTextColor.DARK_AQUA));
            }

         }
      }
   }

   @EventHandler
   public void onRightClick(PlayerInteractEvent var1) {
      if (var1.getHand() == EquipmentSlot.HAND) {
         if (var1.getAction() == Action.RIGHT_CLICK_AIR || var1.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player var2 = var1.getPlayer();
            if (Items.inMainHand(var2, CustomItem.SCULK_REAPER) && !this.status.has(var2, StatusManager.Status.STUN)) {
               this.abilities.sculkHook(var2);
            }
         }
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onDarkness(EntityPotionEffectEvent var1) {
      Entity var3 = var1.getEntity();
      if (var3 instanceof Player var2) {
         if (var1.getNewEffect() != null && var1.getNewEffect().getType().equals(PotionEffectType.DARKNESS) && Items.holding(var2, CustomItem.SCULK_REAPER)) {
            var1.setCancelled(true);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onShieldHit(EntityDamageByEntityEvent var1) {
      Entity var4 = var1.getEntity();
      if (var4 instanceof Player var2) {
         var4 = var1.getDamager();
         if (var4 instanceof Player var3) {
            if (var2.isBlocking() && Items.holding(var2, CustomItem.SILENCED_SHIELD)) {
               if (var2.hasCooldown(Material.SHIELD)) {
                  return;
               }

               this.later(() -> {
                  if (var2.isOnline() && var2.hasCooldown(Material.SHIELD)) {
                     this.abilities.silencedShieldProc(var2, var3);
                  }

               });
               return;
            }

            return;
         }
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onFall(EntityDamageEvent var1) {
      if (var1.getCause() == DamageCause.FALL) {
         Entity var3 = var1.getEntity();
         if (var3 instanceof Player) {
            Player var2 = (Player)var3;
            if (Items.carrying(var2, CustomItem.WINDSTRIKER) || this.abilities.isFallImmune(var2)) {
               var1.setCancelled(true);
            }

            return;
         }
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onEat(PlayerItemConsumeEvent var1) {
      Player var2 = var1.getPlayer();
      if (var1.getItem().getType() == Material.GOLDEN_APPLE && Items.wearing(var2, CustomItem.PLATE_OF_LIFE)) {
         this.later(() -> var2.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1)));
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onItemDamage(PlayerItemDamageEvent var1) {
      Player var2 = var1.getPlayer();
      if (var2.isInWater() && Items.wearing(var2, CustomItem.OCEANIC_CROWN)) {
         boolean var3 = false;

         for(ItemStack var7 : var2.getInventory().getArmorContents()) {
            if (var7 != null && var7.equals(var1.getItem())) {
               var3 = true;
            }
         }

         if (var3) {
            double var8 = (double)var1.getDamage() * (double)0.75F;
            int var9 = (int)var8;
            if (ThreadLocalRandom.current().nextDouble() < var8 - (double)var9) {
               ++var9;
            }

            if (var9 <= 0) {
               var1.setCancelled(true);
            } else {
               var1.setDamage(var9);
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onInteract(PlayerInteractEvent var1) {
      if (this.status.has(var1.getPlayer(), StatusManager.Status.STUN)) {
         var1.setCancelled(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onPlace(BlockPlaceEvent var1) {
      Player var2 = var1.getPlayer();
      if (this.status.has(var2, StatusManager.Status.STUN) || var1.getBlock().getType() == Material.COBWEB && this.status.silenced(var2)) {
         var1.setCancelled(true);
      }

      if (Items.identify(var1.getItemInHand()) == CustomItem.DRAGON_EGG) {
         var1.setCancelled(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onLaunch(ProjectileLaunchEvent var1) {
      ProjectileSource var3 = var1.getEntity().getShooter();
      if (var3 instanceof Player var2) {
         if (this.status.has(var2, StatusManager.Status.STUN) || var1.getEntity() instanceof WindCharge && this.status.silenced(var2)) {
            var1.setCancelled(true);
         }

      }
   }

   @EventHandler
   public void onMenuClick(InventoryClickEvent var1) {
      if (var1.getView().getTopInventory().getHolder() instanceof Abilities.StalkerMenu) {
         var1.setCancelled(true);
      }

   }

   @EventHandler
   public void onMenuDrag(InventoryDragEvent var1) {
      if (var1.getView().getTopInventory().getHolder() instanceof Abilities.StalkerMenu) {
         var1.setCancelled(true);
      }

   }

   @EventHandler
   public void onJoin(PlayerJoinEvent var1) {
      StatusManager.strip(var1.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent var1) {
      Player var2 = var1.getPlayer();
      this.status.clear(var2);
      this.abilities.onQuit(var2);
      this.lastSneak.remove(var2.getUniqueId());
   }

   private boolean count(Player var1, CustomItem var2, int var3) {
      int var4 = this.hitCounts.computeIfAbsent(var1.getUniqueId(), id -> new HashMap<>()).merge(var2, 1, Integer::sum);
      if (var4 >= var3) {
         ((Map)this.hitCounts.get(var1.getUniqueId())).put(var2, 0);
         return true;
      } else {
         return false;
      }
   }

   private void later(Runnable var1) {
      Bukkit.getScheduler().runTask(this.plugin, var1);
   }
}
