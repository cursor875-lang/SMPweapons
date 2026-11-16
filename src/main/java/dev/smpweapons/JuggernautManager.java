package dev.smpweapons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class JuggernautManager implements Listener {
   private final SMPWeapons plugin;
   private final Map<String, Long> cooldowns = new HashMap();
   private final Map<UUID, Double> rage = new HashMap();
   private final Map<UUID, Long> lastStand = new HashMap();
   private final Map<UUID, Long> unstoppable = new HashMap();
   private final Map<UUID, Double> fallStart = new HashMap();
   private final Set<UUID> poundAirborne = new HashSet();
   private final Map<UUID, BukkitTask> chargeTasks = new HashMap();
   private final Set<UUID> rageActive = new HashSet();
   private final Map<UUID, Long> rageUntil = new HashMap();
   private final Map<UUID, Long> lastRageHit = new HashMap();
   private final Map<UUID, BossBar> rageBars = new HashMap();
   private static final long RAGE_MILLIS = 8000L;
   private final Map<UUID, Double> originalMaxHealth = new HashMap();
   private final Map<UUID, BossBar> eventBars = new HashMap();
   private final Map<UUID, Long> eventEnds = new HashMap();

   public JuggernautManager(SMPWeapons var1) {
      this.plugin = var1;
   }

   public boolean fullSet(Player var1) {
      ItemStack[] var2 = var1.getInventory().getArmorContents();
      return var2.length == 4 && Items.identify(var2[3]) == CustomItem.JUGGERNAUT_HELMET && Items.identify(var2[2]) == CustomItem.JUGGERNAUT_CHESTPLATE && Items.identify(var2[1]) == CustomItem.JUGGERNAUT_LEGGINGS && Items.identify(var2[0]) == CustomItem.JUGGERNAUT_BOOTS;
   }

   public void tick() {
      for(Player var2 : Bukkit.getOnlinePlayers()) {
         boolean var3 = this.fullSet(var2);
         this.potion(var2, PotionEffectType.RESISTANCE, 1, var3);
         this.potion(var2, PotionEffectType.SLOWNESS, 0, var3);
         StatusManager.setModifier(var2, Attribute.KNOCKBACK_RESISTANCE, new NamespacedKey(this.plugin, "juggernaut_knockback"), (double)0.0F, Operation.ADD_NUMBER, false);
         StatusManager.setModifier(var2, Attribute.MAX_HEALTH, new NamespacedKey(this.plugin, "juggernaut_health"), (double)20.0F, Operation.ADD_NUMBER, var3);
         if (var3 && var2.getHealth() > var2.getAttribute(Attribute.MAX_HEALTH).getValue()) {
            var2.setHealth(var2.getAttribute(Attribute.MAX_HEALTH).getValue());
         }

         this.updateRage(var2, var3 || Items.holding(var2, CustomItem.JUGGERNAUT_HAMMER));
         BossBar var4 = (BossBar)this.eventBars.get(var2.getUniqueId());
         if (var4 != null) {
            var4.setProgress(Math.max((double)0.0F, Math.min((double)1.0F, var2.getHealth() / Math.max((double)1.0F, var2.getAttribute(Attribute.MAX_HEALTH).getValue()))));
         }
      }

      long var7 = System.currentTimeMillis();

      for(UUID var10 : new HashSet<>(this.eventEnds.keySet())) {
         if ((Long)this.eventEnds.get(var10) <= var7) {
            this.stopEvent(Bukkit.getPlayer(var10));
         }
      }

      for(Map.Entry var11 : this.eventBars.entrySet()) {
         for(Player var6 : Bukkit.getOnlinePlayers()) {
            if (!((BossBar)var11.getValue()).getPlayers().contains(var6)) {
               ((BossBar)var11.getValue()).addPlayer(var6);
            }
         }
      }

   }

   private void potion(Player var1, PotionEffectType var2, int var3, boolean var4) {
      PotionEffect var5 = var1.getPotionEffect(var2);
      if (var4) {
         if (var5 == null || var5.getAmplifier() < var3 || var5.getAmplifier() == var3 && !var5.isInfinite()) {
            var1.addPotionEffect(new PotionEffect(var2, -1, var3, true, false, true));
         }
      } else if (var5 != null && var5.isInfinite() && var5.isAmbient() && var5.getAmplifier() == var3) {
         var1.removePotionEffect(var2);
      }

   }

   public void command(CommandSender var1, String[] var2) {
      if (var2.length == 2 && var2[0].equalsIgnoreCase("start")) {
         if (!var1.hasPermission("smpweapons.admin")) {
            var1.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
         } else {
            Player var3 = Bukkit.getPlayerExact(var2[1]);
            if (var3 == null) {
               var1.sendMessage(Component.text("Player not online.", NamedTextColor.RED));
            } else {
               this.stopEvent(var3);
               this.originalMaxHealth.put(var3.getUniqueId(), var3.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
               var3.getAttribute(Attribute.MAX_HEALTH).setBaseValue(this.fullSet(var3) ? (double)60.0F : (double)80.0F);
               var3.setHealth(var3.getAttribute(Attribute.MAX_HEALTH).getValue());
               var3.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0, false, false, false));
               BossBar var4 = Bukkit.createBossBar("Juggernaut: " + var3.getName(), BarColor.YELLOW, BarStyle.SOLID, new BarFlag[0]);

               for(Player var6 : Bukkit.getOnlinePlayers()) {
                  var4.addPlayer(var6);
               }

               this.eventBars.put(var3.getUniqueId(), var4);
               this.eventEnds.put(var3.getUniqueId(), Long.MAX_VALUE);
               Bukkit.broadcast(Component.text(var3.getName() + " has become the Juggernaut!", NamedTextColor.GOLD));
               var1.sendMessage(Component.text("Juggernaut event started for " + var3.getName() + ".", NamedTextColor.GREEN));
            }
         }
      } else {
         var1.sendMessage(Component.text("Usage: /juggernaut start <player>", NamedTextColor.RED));
      }
   }

   private void stopEvent(Player var1) {
      if (var1 != null) {
         UUID var2 = var1.getUniqueId();
         this.eventEnds.remove(var2);
         BossBar var3 = (BossBar)this.eventBars.remove(var2);
         if (var3 != null) {
            var3.removeAll();
         }

         Double var4 = (Double)this.originalMaxHealth.remove(var2);
         if (var4 != null) {
            var1.getAttribute(Attribute.MAX_HEALTH).setBaseValue(var4);
            var1.setHealth(Math.min(var1.getHealth(), var4));
         }

         this.clearGlow(var1);
      }
   }

   private void clearGlow(Player var1) {
      PotionEffect var2 = var1.getPotionEffect(PotionEffectType.GLOWING);
      if (var2 != null && var2.isInfinite() && !var2.isAmbient() && !var2.hasParticles() && !var2.hasIcon()) {
         var1.removePotionEffect(PotionEffectType.GLOWING);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onDeath(PlayerDeathEvent var1) {
      Player var2 = var1.getEntity();
      UUID var3 = var2.getUniqueId();
      if (this.eventBars.containsKey(var3)) {
         this.eventEnds.remove(var3);
         BossBar var4 = (BossBar)this.eventBars.remove(var3);
         if (var4 != null) {
            var4.removeAll();
         }

         Double var5 = (Double)this.originalMaxHealth.remove(var3);
         if (var5 != null) {
            AttributeInstance var6 = var2.getAttribute(Attribute.MAX_HEALTH);
            if (var6 != null) {
               var6.setBaseValue(var5);
            }
         }

         this.clearGlow(var2);
         Bukkit.broadcast(Component.text("The Juggernaut " + var2.getName() + " has been defeated! The event is over.", NamedTextColor.GOLD));
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent var1) {
      for(BossBar var3 : this.eventBars.values()) {
         var3.addPlayer(var1.getPlayer());
      }

      if (!this.eventBars.containsKey(var1.getPlayer().getUniqueId())) {
         this.clearGlow(var1.getPlayer());
      }

   }

   private boolean isRaging(Player var1) {
      Long var2 = (Long)this.rageUntil.get(var1.getUniqueId());
      return var2 != null && var2 > System.currentTimeMillis();
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onRageDamage(EntityDamageEvent var1) {
      Entity var3 = var1.getEntity();
      if (var3 instanceof Player var2) {
         if (!this.isRaging(var2)) {
            if (var1.getCause() != DamageCause.VOID && var1.getCause() != DamageCause.KILL) {
               if (!this.fullSet(var2) && !Items.holding(var2, CustomItem.JUGGERNAUT_HAMMER)) {
                  return;
               }

               double var8 = var1.getDamage();
               if (var8 <= (double)0.0F) {
                  return;
               }

               UUID var5 = var2.getUniqueId();
               this.lastRageHit.put(var5, System.currentTimeMillis());
               double var6 = Math.min((double)100.0F, (Double)this.rage.getOrDefault(var5, (double)0.0F) + var8 * (double)4.0F);
               this.rage.put(var5, var6);
               if (var6 >= (double)100.0F) {
                  this.activateRage(var2);
               }

               return;
            }

            return;
         }
      }

   }

   private void activateRage(Player var1) {
      UUID var2 = var1.getUniqueId();
      this.rage.put(var2, (double)0.0F);
      this.rageActive.add(var2);
      this.rageUntil.put(var2, System.currentTimeMillis() + 8000L);
      var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.0F, 0.6F);
      var1.getWorld().playSound(var1.getLocation(), Sound.ITEM_TOTEM_USE, 0.6F, 0.5F);
      var1.getWorld().spawnParticle(Particle.FLAME, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 60, (double)0.5F, 0.8, (double)0.5F, 0.05);
      var1.getWorld().spawnParticle(Particle.LAVA, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 15, (double)0.5F, 0.8, (double)0.5F, (double)0.0F);
      var1.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0, false, false, true));
      var1.sendMessage(Component.text("RAGE! +50% damage and healing on every hit for 8 seconds!", NamedTextColor.GOLD));
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         this.rageActive.remove(var2);
         Player var2x = Bukkit.getPlayer(var2);
         if (var2x != null) {
            var2x.sendActionBar(Component.text("Rage faded", NamedTextColor.GRAY));
         }

      }, 160L);
   }

   private void updateRage(Player var1, boolean var2) {
      UUID var3 = var1.getUniqueId();
      BossBar var4 = (BossBar)this.rageBars.get(var3);
      boolean var5 = this.isRaging(var1);
      if (!var2 && !var5) {
         if (var4 != null) {
            var4.removeAll();
            this.rageBars.remove(var3);
         }

      } else {
         if (var4 == null) {
            var4 = Bukkit.createBossBar("Rage", BarColor.RED, BarStyle.SEGMENTED_10, new BarFlag[0]);
            var4.addPlayer(var1);
            this.rageBars.put(var3, var4);
         }

         long var6 = System.currentTimeMillis();
         if (var5) {
            double var8 = (double)((Long)this.rageUntil.get(var3) - var6) / (double)8000.0F;
            String var10001 = String.valueOf(ChatColor.GOLD);
            var4.setTitle(var10001 + String.valueOf(ChatColor.BOLD) + "RAGE!");
            var4.setColor(BarColor.YELLOW);
            var4.setProgress(Math.max((double)0.0F, Math.min((double)1.0F, var8)));
            var1.getWorld().spawnParticle(Particle.FLAME, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 4, 0.3, 0.6, 0.3, 0.01);
         } else {
            double var10 = (Double)this.rage.getOrDefault(var3, (double)0.0F);
            if (var10 > (double)0.0F && var6 - (Long)this.lastRageHit.getOrDefault(var3, 0L) > 6000L) {
               var10 = Math.max((double)0.0F, var10 - (double)1.0F);
               this.rage.put(var3, var10);
            }

            String var11 = String.valueOf(ChatColor.RED);
            var4.setTitle(var11 + "Rage " + (int)var10 + "%");
            var4.setColor(BarColor.RED);
            var4.setProgress(Math.max((double)0.0F, Math.min((double)1.0F, var10 / (double)100.0F)));
         }

      }
   }

   @EventHandler
   public void onRageQuit(PlayerQuitEvent var1) {
      UUID var2 = var1.getPlayer().getUniqueId();
      BossBar var3 = (BossBar)this.rageBars.remove(var2);
      if (var3 != null) {
         var3.removeAll();
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void damage(EntityDamageEvent var1) {
      Entity var3 = var1.getEntity();
      if (var3 instanceof Player var2) {
         if (this.fullSet(var2) && var1.getCause() == DamageCause.FALL) {
            var1.setDamage(var1.getDamage() * (double)0.75F);
         }

         if (!this.eventBars.containsKey(var2.getUniqueId()) && Items.holding(var2, CustomItem.JUGGERNAUT_HAMMER) && var1.getFinalDamage() >= var2.getHealth() && System.currentTimeMillis() - (Long)this.lastStand.getOrDefault(var2.getUniqueId(), 0L) >= 300000L) {
            var1.setCancelled(true);
            var2.setHealth((double)2.0F);
            var2.setNoDamageTicks(60);
            this.lastStand.put(var2.getUniqueId(), System.currentTimeMillis());

            for(Entity var4 : var2.getNearbyEntities((double)6.0F, (double)4.0F, (double)6.0F)) {
               if (var4 instanceof LivingEntity) {
                  LivingEntity var5 = (LivingEntity)var4;
                  if (var5 != var2) {
                     SafeZones.trackAbilityMotion(var5, 2400L);
                     var5.setVelocity(var5.getLocation().toVector().subtract(var2.getLocation().toVector()).normalize().multiply(1.7).setY(0.8));
                  }
               }
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void combat(EntityDamageByEntityEvent var1) {
      Entity var4 = var1.getDamager();
      Player var10000;
      if (var4 instanceof Player var3) {
         var10000 = var3;
      } else {
         var10000 = null;
      }

      Player var2 = var10000;
      if (var2 != null && Items.holding(var2, CustomItem.JUGGERNAUT_HAMMER)) {
         var4 = var1.getEntity();
         if (var4 instanceof LivingEntity) {
            LivingEntity var10 = (LivingEntity)var4;
            if (ThreadLocalRandom.current().nextDouble() < 0.2 && var10 instanceof Player) {
               Player var13 = (Player)var10;
               ItemStack[] var5 = var13.getInventory().getArmorContents();
               ArrayList var6 = new ArrayList();

               for(int var7 = 0; var7 < var5.length; ++var7) {
                  if (var5[var7] != null && var5[var7].getType().getMaxDurability() > 0) {
                     var6.add(var7);
                  }
               }

               if (!var6.isEmpty()) {
                  ItemStack var15 = var5[(Integer)var6.get(ThreadLocalRandom.current().nextInt(var6.size()))];
                  ItemMeta var8 = var15.getItemMeta();
                  if (var8 instanceof Damageable) {
                     Damageable var9 = (Damageable)var8;
                     var9.setDamage(Math.min(var15.getType().getMaxDurability() - 1, var9.getDamage() + Math.max(1, var15.getType().getMaxDurability() / 10)));
                     var15.setItemMeta(var8);
                  }
               }
            }
         }
      }

      if (var2 != null && var1.getEntity() instanceof LivingEntity && this.isRaging(var2)) {
         var1.setDamage(var1.getDamage() * (double)1.5F);
         var2.heal((double)2.0F);
         var2.getWorld().spawnParticle(Particle.FLAME, var1.getEntity().getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 10, 0.3, (double)0.5F, 0.3, 0.02);
      }

      var4 = var1.getEntity();
      if (var4 instanceof Player var11) {
         if (var11.isBlocking() && Items.holding(var11, CustomItem.FORTRESS_SHIELD) && var2 != null) {
            var2.damage(var1.getFinalDamage() * 0.3);
         }
      }

   }

   @EventHandler
   public void swap(PlayerSwapHandItemsEvent var1) {
      Player var2 = var1.getPlayer();
      CustomItem var3 = Items.identify(var2.getInventory().getItemInMainHand());
      if (var3 == null) {
         var3 = Items.identify(var2.getInventory().getItemInOffHand());
      }

      boolean var4 = var3 != null && var3 != CustomItem.JUGGERNAUT_HAMMER && var3 != CustomItem.FORTRESS_SHIELD && var3 != CustomItem.JUGGERNAUT_HELMET && var3 != CustomItem.JUGGERNAUT_CHESTPLATE && var3 != CustomItem.JUGGERNAUT_LEGGINGS && var3 != CustomItem.JUGGERNAUT_BOOTS;
      if (!var4) {
         if (this.fullSet(var2) && !Items.holding(var2, CustomItem.JUGGERNAUT_HAMMER) && !Items.holding(var2, CustomItem.FORTRESS_SHIELD)) {
            var1.setCancelled(true);
            if (var2.isSneaking()) {
               this.unstoppable(var2);
            } else {
               this.earthquake(var2);
            }

         } else {
            if (Items.holding(var2, CustomItem.JUGGERNAUT_HAMMER)) {
               var1.setCancelled(true);
               if (var2.isSneaking()) {
                  this.ironCharge(var2);
               } else {
                  this.groundPound(var2);
               }
            } else if (Items.holding(var2, CustomItem.FORTRESS_SHIELD)) {
               var1.setCancelled(true);
               this.wallUp(var2);
            }

         }
      }
   }

   private boolean ready0(Player var1, String var2, long var3) {
      if (!ConfigFiles.abilityEnabled(var2) || !SafeZones.guardAbility(var1)) return false;
      SafeZones.trackAbilityMotion(var1, 13000L);
      long var5 = System.currentTimeMillis();
      long var7 = (Long)this.cooldowns.getOrDefault(String.valueOf(var1.getUniqueId()) + ":" + var2, 0L);
      if (var7 > var5) {
         var1.sendActionBar(Component.text(var2 + " cooldown: " + (var7 - var5 + 999L) / 1000L + "s", NamedTextColor.RED));
         return false;
      } else {
         this.cooldowns.put(String.valueOf(var1.getUniqueId()) + ":" + var2, var5 + var3);
         return true;
      }
   }

   private void earthquake(Player var1) {
      if (this.ready(var1, "earthquake", 60000L)) {
         var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.6F);
         var1.getWorld().spawnParticle(Particle.BLOCK, var1.getLocation(), 90, (double)3.0F, 0.2, (double)3.0F, Material.STONE.createBlockData());

         for(Entity var3 : var1.getNearbyEntities((double)8.0F, (double)5.0F, (double)8.0F)) {
            if (var3 instanceof LivingEntity) {
               LivingEntity var4 = (LivingEntity)var3;
               if (var4 != var1) {
                  SafeZones.trackAbilityMotion(var4, 2400L);
                  var4.setVelocity(new Vector((double)0.0F, 1.45, (double)0.0F));
               }
            }
         }

      }
   }

   private void unstoppable(Player var1) {
      if (this.ready(var1, "unstoppable", 120000L)) {
         this.unstoppable.put(var1.getUniqueId(), System.currentTimeMillis() + 6000L);
         StatusManager.setUnstoppable(var1, 120);
         var1.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 1, true, false, true));
         var1.getWorld().spawnParticle(Particle.CRIT, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 45, (double)1.0F, 0.7, (double)1.0F, 0.2);
      }
   }

   private void groundPound(Player var1) {
      if (this.ready(var1, "groundpound", 45000L)) {
         this.fallStart.put(var1.getUniqueId(), var1.getLocation().getY());
         var1.setVelocity(new Vector((double)0.0F, 1.15, (double)0.0F));
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (var1.isOnline()) {
               var1.setVelocity(new Vector((double)0.0F, -2.2, (double)0.0F));
            }
         }, 12L);
      }
   }

   @EventHandler
   public void move(PlayerMoveEvent var1) {
      Player var2 = var1.getPlayer();
      if ((Long)this.unstoppable.getOrDefault(var2.getUniqueId(), 0L) > System.currentTimeMillis() && var1.getTo() != null) {
         for(Block var6 : new Block[]{var1.getTo().getBlock(), var1.getTo().clone().add((double)0.0F, (double)1.0F, (double)0.0F).getBlock()}) {
            if (var6.getType() == Material.COBWEB) {
               if (SafeZones.canModify(var2, var6)) var6.setType(Material.AIR);
            }
         }
      }

      if (this.fallStart.containsKey(var2.getUniqueId()) && !var2.isOnGround()) {
         this.poundAirborne.add(var2.getUniqueId());
      }

      if (this.fallStart.containsKey(var2.getUniqueId()) && this.poundAirborne.contains(var2.getUniqueId()) && var2.isOnGround() && !var2.isDead()) {
         this.poundAirborne.remove(var2.getUniqueId());
         double var8 = Math.max((double)0.0F, (Double)this.fallStart.remove(var2.getUniqueId()) - var2.getLocation().getY());
         var2.getWorld().spawnParticle(Particle.BLOCK, var2.getLocation(), 80, (double)2.0F, 0.2, (double)2.0F, Material.DEEPSLATE.createBlockData());

         for(Entity var10 : var2.getNearbyEntities((double)5.0F, (double)3.0F, (double)5.0F)) {
            if (var10 instanceof LivingEntity) {
               LivingEntity var7 = (LivingEntity)var10;
               if (var7 != var2) {
                  if (SafeZones.canAttack(var2, var7)) var7.damage(Math.min((double)16.0F, (double)4.0F + var8 * 0.8), var2);
               }
            }
         }
      }

   }

   private void ironCharge(final Player var1) {
      if (this.ready(var1, "ironcharge", 60000L)) {
         final Vector var2 = var1.getLocation().getDirection().normalize();
         BukkitTask var3 = Bukkit.getScheduler().runTaskTimer(this.plugin, new Runnable() {
            int n = 0;

            public void run() {
               if (var1.isOnline() && this.n++ < 15) {
                  var1.setVelocity(var2.clone().multiply(0.9).setY(0.12));

                  for(Entity var2x : var1.getNearbyEntities(1.3, 1.3, 1.3)) {
                     if (var2x instanceof LivingEntity) {
                        LivingEntity var3 = (LivingEntity)var2x;
                        if (var3 != var1) {
                           if (SafeZones.canAttack(var1, var3)) var3.damage((double)5.0F, var1);
                           SafeZones.trackAbilityMotion(var3, 2400L);
                           var3.setVelocity(var2.clone().multiply(0.8).setY(0.3));
                        }
                     }
                  }

               } else {
                  JuggernautManager.this.chargeTasks.remove(var1.getUniqueId());
               }
            }
         }, 0L, 1L);
         this.chargeTasks.put(var1.getUniqueId(), var3);
      }
   }

   private void wallUp(Player var1) {
      if (!SafeZones.guardAbility(var1)) return;
      if (this.ready(var1, "wallup", 90000L)) {
         Location var2 = var1.getLocation().getBlock().getLocation().add(var1.getLocation().getDirection().setY(0).normalize().multiply(2));
         ArrayList<Block> var3 = new ArrayList<>();

         for(int var4 = -2; var4 <= 2; ++var4) {
            for(int var5 = 0; var5 < 3; ++var5) {
               Block var6 = var2.clone().add((double)var4, (double)var5, (double)0.0F).getBlock();
               if (var6.getType().isAir() && SafeZones.canModify(var1, var6)) {
                  var6.setType(Math.abs(var4) != 2 && var5 != 0 ? Material.OBSIDIAN : Material.IRON_BARS);
                  var3.add(var6);
               }
            }
         }

         Bukkit.getScheduler().runTaskLater(this.plugin, () -> var3.forEach((var0) -> {
               if (var0.getType() == Material.IRON_BARS || var0.getType() == Material.OBSIDIAN) {
                  var0.setType(Material.AIR);
               }

            }), 160L);
      }
   }

   public void shutdown() {
      for(BossBar var2 : this.rageBars.values()) {
         var2.removeAll();
      }

      this.rageBars.clear();

      for(BukkitTask var5 : this.chargeTasks.values()) {
         var5.cancel();
      }

      for(Player var6 : Bukkit.getOnlinePlayers()) {
         this.stopEvent(var6);
         StatusManager.setModifier(var6, Attribute.KNOCKBACK_RESISTANCE, new NamespacedKey(this.plugin, "juggernaut_knockback"), (double)0.0F, Operation.ADD_NUMBER, false);
         StatusManager.setModifier(var6, Attribute.MAX_HEALTH, new NamespacedKey(this.plugin, "juggernaut_health"), (double)0.0F, Operation.ADD_NUMBER, false);
      }

   }

   private boolean ready(Player var1, String var2, long var3) {
      return this.ready0(var1, var2, WeaponsConfig.cooldown(var2, var3));
   }
}
