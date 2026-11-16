package dev.smpweapons;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

public final class WeaponEvents implements Listener, TabExecutor {
   private static final Map<CustomItem, Theme> THEMES = new EnumMap(CustomItem.class);
   private static final Map<CustomItem, Map<Material, Integer>> GATHER_LISTS = new EnumMap(CustomItem.class);
   private final JavaPlugin plugin;
   private final NamespacedKey relicKey;
   private Active active;

   public WeaponEvents(JavaPlugin var1) {
      this.plugin = var1;
      this.relicKey = new NamespacedKey(var1, "weapon_event_relic");
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onDamage(EntityDamageByEntityEvent var1) {
      if (SafeZones.isProtected(var1.getEntity().getLocation()) || SafeZones.isProtected(var1.getDamager().getLocation())) return;
      if (this.active != null) {
         this.active.onDamage(var1);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onDeath(EntityDeathEvent var1) {
      if (SafeZones.isProtected(var1.getEntity().getLocation())) return;
      if (this.active != null) {
         this.active.onDeath(var1);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInteract(PlayerInteractEvent var1) {
      if (this.active != null) {
         this.active.onInteract(var1);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBreak(BlockBreakEvent var1) {
      if (this.active != null) {
         this.active.onBreak(var1);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlace(BlockPlaceEvent var1) {
      if (this.active != null) {
         this.active.onPlace(var1);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onEntityExplode(EntityExplodeEvent var1) {
      if (this.active != null) {
         List<Block> var10000 = var1.blockList();
         Active var10001 = this.active;
         Objects.requireNonNull(var10001);
         var10000.removeIf(var10001::protects);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onBlockExplode(BlockExplodeEvent var1) {
      if (this.active != null) {
         List<Block> var10000 = var1.blockList();
         Active var10001 = this.active;
         Objects.requireNonNull(var10001);
         var10000.removeIf(var10001::protects);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onMove(PlayerMoveEvent var1) {
      if (this.active != null && var1.hasChangedBlock()) {
         this.active.onMove(var1);
      }

   }

   @EventHandler
   public void onQuit(PlayerQuitEvent var1) {
      if (this.active != null) {
         this.active.onQuit(var1);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onDrop(PlayerDropItemEvent var1) {
      if (this.active != null) {
         this.active.onDrop(var1);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onFish(PlayerFishEvent var1) {
      if (this.active != null) {
         this.active.onFish(var1);
      }

   }

   @EventHandler
   public void onProjectileHit(ProjectileHitEvent var1) {
      if (this.active != null) {
         this.active.onProjectileHit(var1);
      }

   }

   boolean isRelic(ItemStack var1) {
      return var1 != null && var1.hasItemMeta() && var1.getItemMeta().getPersistentDataContainer().has(this.relicKey);
   }

   private static boolean ownInventory(InventoryView var0) {
      InventoryType var1 = var0.getTopInventory().getType();
      return var1 == InventoryType.CRAFTING || var1 == InventoryType.CREATIVE;
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onRelicClick(InventoryClickEvent var1) {
      boolean var2 = this.isRelic(var1.getCurrentItem()) || this.isRelic(var1.getCursor());
      if (!var2 && var1.getClick() == ClickType.NUMBER_KEY) {
         HumanEntity var4 = var1.getWhoClicked();
         if (var4 instanceof Player) {
            Player var3 = (Player)var4;
            var2 = this.isRelic(var3.getInventory().getItem(var1.getHotbarButton()));
         }
      }

      if (var2) {
         boolean var5 = var1.getCurrentItem() != null && var1.getCurrentItem().getType().name().endsWith("BUNDLE") || var1.getCursor() != null && var1.getCursor().getType().name().endsWith("BUNDLE");
         if (!ownInventory(var1.getView()) || var5) {
            var1.setCancelled(true);
         }

      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onRelicDrag(InventoryDragEvent var1) {
      if (this.isRelic(var1.getOldCursor()) && !ownInventory(var1.getView())) {
         var1.setCancelled(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onRelicHopper(InventoryPickupItemEvent var1) {
      if (this.isRelic(var1.getItem().getItemStack())) {
         var1.setCancelled(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onRelicDespawn(ItemDespawnEvent var1) {
      if (this.isRelic(var1.getEntity().getItemStack())) {
         var1.setCancelled(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onRelicFrame(PlayerInteractEntityEvent var1) {
      PlayerInventory var2 = var1.getPlayer().getInventory();
      if (this.isRelic(var2.getItemInMainHand()) || this.isRelic(var2.getItemInOffHand())) {
         var1.setCancelled(true);
      }

   }

   @EventHandler
   public void onEntitiesLoad(EntitiesLoadEvent var1) {
      for(Entity var3 : var1.getEntities()) {
         if (var3 instanceof Item var4) {
            if (this.isRelic(var4.getItemStack())) {
               boolean var10000;
               label23: {
                  Active var7 = this.active;
                  if (var7 instanceof RelicHold) {
                     RelicHold var6 = (RelicHold)var7;
                     if (var4.equals(var6.dropped)) {
                        var10000 = true;
                        break label23;
                     }
                  }

                  var10000 = false;
               }

               boolean var5 = var10000;
               if (!var5) {
                  var3.remove();
               }
            }
         }
      }

   }

   @EventHandler
   public void onJoin(PlayerJoinEvent var1) {
      if (this.active != null) {
         var1.getPlayer().showBossBar(this.active.bar);
      }

   }

   public void shutdown() {
      if (this.active != null) {
         this.active.stopByAdmin();
      }

   }

   public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
      if (var4.length >= 1 && var4[0].equalsIgnoreCase("list")) {
         var1.sendMessage(Component.text("Weapon events:", NamedTextColor.GOLD));

         for(Map.Entry var11 : THEMES.entrySet()) {
            String var12 = ((CustomItem)var11.getKey()).id;
            var1.sendMessage(Component.text(" " + var12 + " -> " + ((Theme)var11.getValue()).title + " (" + describe(((Theme)var11.getValue()).kind) + ")", NamedTextColor.GRAY));
         }

         return true;
      } else if (var4.length >= 1 && var4[0].equalsIgnoreCase("stop")) {
         if (this.active == null) {
            var1.sendMessage(Component.text("No weapon event is running.", NamedTextColor.RED));
         } else {
            this.active.stopByAdmin();
         }

         return true;
      } else if (var4.length >= 2 && var4[0].equalsIgnoreCase("start")) {
         if (!ConfigFiles.bool("events.yml", "events.enabled", true)) {
            var1.sendMessage("§cSMPWeapons events are disabled in events.yml.");
            return true;
         }
         CustomItem var5 = CustomItem.byId(var4[1]);
         Theme var6 = var5 == null ? null : (Theme)THEMES.get(var5);
         if (var6 == null) {
            var1.sendMessage(Component.text("Unknown item. Use /weaponevent list", NamedTextColor.RED));
            return true;
         } else if (this.active != null) {
            var1.sendMessage(Component.text(this.active.theme.title + " is already running. /weaponevent stop first.", NamedTextColor.RED));
            return true;
         } else {
            Location var7;
            if (var4.length >= 3) {
               Player var9 = Bukkit.getPlayerExact(var4[2]);
               if (var9 == null) {
                  var1.sendMessage(Component.text("Player not online.", NamedTextColor.RED));
                  return true;
               }

               var7 = var9.getLocation();
            } else {
               if (!(var1 instanceof Player)) {
                  var1.sendMessage(Component.text("From the console, add a player to start it at: /weaponevent start <item> <player>", NamedTextColor.RED));
                  return true;
               }

               Player var8 = (Player)var1;
               var7 = var8.getLocation();
            }

            if (!ConfigFiles.bool("events.yml", "events.allow-events-in-protected-region", false) && SafeZones.isProtected(var7)) {
               var1.sendMessage("§cWeapon events cannot start inside the protected spawn region.");
               return true;
            }
            Object var10001;
            switch (var6.kind.ordinal()) {
               case 0 -> var10001 = new KingOfTheHill(var5, var6, var7.getBlock().getLocation().add((double)0.5F, (double)0.0F, (double)0.5F));
               case 1 -> var10001 = new TreasureHunt(var5, var6, var7);
               case 2 -> var10001 = new PvpRace(var5, var6, var7);
               case 3 -> var10001 = new BloodMoon(var5, var6, var7);
               case 4 -> var10001 = new MiningRace(var5, var6, var7);
               case 5 -> var10001 = new CheckpointRace(var5, var6, var7);
               case 6 -> var10001 = new RelicHold(var5, var6, var7);
               case 7 -> var10001 = new Gather(var5, var6, var7);
               case 8 -> var10001 = new LastStanding(var5, var6, var7.getBlock().getLocation().add((double)0.5F, (double)0.0F, (double)0.5F));
               case 9 -> var10001 = new FishingFrenzy(var5, var6, var7);
               case 10 -> var10001 = new SnowballBrawl(var5, var6, var7);
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            this.active = (Active)var10001;
            this.active.begin();
            return true;
         }
      } else {
         var1.sendMessage(Component.text("Usage: /weaponevent start <item> [player]  |  /weaponevent stop  |  /weaponevent list", NamedTextColor.RED));
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
      ArrayList<String> var5 = new ArrayList<>();
      if (var4.length == 1) {
         var5.addAll(List.of("start", "stop", "list"));
      } else if (var4.length == 2 && var4[0].equalsIgnoreCase("start")) {
         for(CustomItem var7 : THEMES.keySet()) {
            var5.add(var7.id);
         }
      } else if (var4.length == 3 && var4[0].equalsIgnoreCase("start")) {
         Bukkit.getOnlinePlayers().forEach((var1x) -> var5.add(var1x.getName()));
      }

      String var8 = var4.length == 0 ? "" : var4[var4.length - 1].toLowerCase(Locale.ROOT);
      var5.removeIf((var1x) -> !var1x.toLowerCase(Locale.ROOT).startsWith(var8));
      return var5;
   }

   private static String describe(Kind var0) {
      String var10000;
      switch (var0.ordinal()) {
         case 0 -> var10000 = "king of the hill";
         case 1 -> var10000 = "treasure hunt";
         case 2 -> var10000 = "PvP kill race";
         case 3 -> var10000 = "monster kill race";
         case 4 -> var10000 = "mining race";
         case 5 -> var10000 = "checkpoint race";
         case 6 -> var10000 = "hold the relic";
         case 7 -> var10000 = "gathering race";
         case 8 -> var10000 = "last one standing";
         case 9 -> var10000 = "fishing contest";
         case 10 -> var10000 = "snowball fight";
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static BossBar.Color barColor(NamedTextColor var0) {
      if (var0 != NamedTextColor.RED && var0 != NamedTextColor.DARK_RED) {
         if (var0 != NamedTextColor.BLUE && var0 != NamedTextColor.AQUA && var0 != NamedTextColor.DARK_AQUA) {
            if (var0 == NamedTextColor.GREEN) {
               return Color.GREEN;
            } else if (var0 != NamedTextColor.YELLOW && var0 != NamedTextColor.GOLD) {
               return var0 != NamedTextColor.DARK_PURPLE && var0 != NamedTextColor.LIGHT_PURPLE ? Color.WHITE : Color.PURPLE;
            } else {
               return Color.YELLOW;
            }
         } else {
            return Color.BLUE;
         }
      } else {
         return Color.RED;
      }
   }

   private static boolean playable(Player var0) {
      return !var0.isDead() && (var0.getGameMode() == GameMode.SURVIVAL || var0.getGameMode() == GameMode.ADVENTURE);
   }

   private static List<Player> playersNear(Location var0, double var1) {
      ArrayList var3 = new ArrayList();

      for(Player var5 : var0.getWorld().getPlayers()) {
         if (playable(var5) && var5.getLocation().distanceSquared(var0) <= var1 * var1) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static Player nearestPlayer(Location var0, double var1) {
      Player var3 = null;
      double var4 = var1 * var1;

      for(Player var7 : playersNear(var0, var1)) {
         double var8 = var7.getLocation().distanceSquared(var0);
         if (var8 <= var4) {
            var3 = var7;
            var4 = var8;
         }
      }

      return var3;
   }

   private static Player playerFrom(Entity var0) {
      if (var0 instanceof Player var5) {
         return var5;
      } else {
         if (var0 instanceof Projectile var1) {
            ProjectileSource var3 = var1.getShooter();
            if (var3 instanceof Player var6) {
               return var6;
            }
         }

         if (var0 instanceof TNTPrimed var4) {
            Entity var7 = var4.getSource();
            if (var7 instanceof Player var2) {
               return var2;
            }
         }

         return null;
      }
   }

   private static Map.Entry<UUID, Integer> leader(Map<UUID, Integer> var0) {
      Map.Entry var1 = null;

      for(Map.Entry var3 : var0.entrySet()) {
         if (var1 == null || (Integer)var3.getValue() > (Integer)var1.getValue()) {
            var1 = var3;
         }
      }

      return var1;
   }

   private static Player onlineLeader(Map<UUID, Integer> var0) {
      Player var1 = null;
      int var2 = 0;

      for(Map.Entry var4 : var0.entrySet()) {
         Player var5 = Bukkit.getPlayer((UUID)var4.getKey());
         if (var5 != null && (Integer)var4.getValue() > var2) {
            var1 = var5;
            var2 = (Integer)var4.getValue();
         }
      }

      return var1;
   }

   private static String nameOf(UUID var0) {
      String var1 = Bukkit.getOfflinePlayer(var0).getName();
      return var1 == null ? "?" : var1;
   }

   private static double flatDistance(Location var0, Location var1) {
      double var2 = var0.getX() - var1.getX();
      double var4 = var0.getZ() - var1.getZ();
      return Math.sqrt(var2 * var2 + var4 * var4);
   }

   private static String direction(Location var0, Location var1) {
      double var2 = Math.toDegrees(Math.atan2(-(var1.getX() - var0.getX()), var1.getZ() - var0.getZ()));
      if (var2 < (double)0.0F) {
         var2 += (double)360.0F;
      }

      String[] var4 = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
      return var4[(int)Math.round(var2 / (double)45.0F) % 8];
   }

   private static String coords(Location var0) {
      int var10000 = var0.getBlockX();
      return "X " + var10000 + ", Y " + var0.getBlockY() + ", Z " + var0.getBlockZ();
   }

   private static String worldName(Location var0) {
      String var10000;
      switch (var0.getWorld().getEnvironment()) {
         case NETHER -> var10000 = "the Nether";
         case THE_END -> var10000 = "the End";
         default -> var10000 = "the Overworld";
      }

      return var10000;
   }

   private static String pretty(Enum<?> var0) {
      String var1 = var0.name().toLowerCase(Locale.ROOT).replace('_', ' ');
      char var10000 = Character.toUpperCase(var1.charAt(0));
      return var10000 + var1.substring(1);
   }

   private static String pretty(Material var0) {
      return pretty(var0);
   }

   private static void setBase(LivingEntity var0, Attribute var1, double var2) {
      AttributeInstance var4 = var0.getAttribute(var1);
      if (var4 != null) {
         var4.setBaseValue(var2);
      }

   }

   private static void ring(Location var0, double var1, Particle var3, int var4) {
      World var5 = var0.getWorld();

      for(int var6 = 0; var6 < var4; ++var6) {
         double var7 = (Math.PI * 2D) * (double)var6 / (double)var4;
         var5.spawnParticle(var3, var0.clone().add(Math.cos(var7) * var1, 0.2, Math.sin(var7) * var1), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
      }

   }

   static {
      THEMES.put(CustomItem.DAUNTLESS_SHIELD, WeaponEvents.Theme.of(WeaponEvents.Kind.KOTH, "Iron Siege", NamedTextColor.GOLD));
      THEMES.put(CustomItem.FORTRESS_SHIELD, WeaponEvents.Theme.of(WeaponEvents.Kind.KOTH, "Fortress Siege", NamedTextColor.GRAY));
      THEMES.put(CustomItem.WINDSTRIKER, WeaponEvents.Theme.of(WeaponEvents.Kind.RACE, "Gale Run", NamedTextColor.AQUA, Material.FEATHER));
      THEMES.put(CustomItem.VOID_REAVER, WeaponEvents.Theme.of(WeaponEvents.Kind.RACE, "Rift Run", NamedTextColor.DARK_PURPLE, Material.ENDER_PEARL));
      THEMES.put(CustomItem.SCULK_REAPER, WeaponEvents.Theme.of(WeaponEvents.Kind.RELIC, "Echo Relic", NamedTextColor.DARK_AQUA, Material.ECHO_SHARD));
      THEMES.put(CustomItem.ABYSSAL_BLADE, WeaponEvents.Theme.of(WeaponEvents.Kind.RELIC, "Abyssal Relic", NamedTextColor.DARK_PURPLE, Material.NETHER_STAR));
      THEMES.put(CustomItem.HORN_OF_HELLSCORCH, WeaponEvents.Theme.of(WeaponEvents.Kind.GATHER, "Blaze Rush", NamedTextColor.RED));
      THEMES.put(CustomItem.PLATE_OF_LIFE, WeaponEvents.Theme.of(WeaponEvents.Kind.GATHER, "Bloom Hunt", NamedTextColor.GREEN));
      THEMES.put(CustomItem.FLOWER_SWORD, WeaponEvents.Theme.of(WeaponEvents.Kind.GATHER, "Flower Festival", NamedTextColor.GREEN));
      THEMES.put(CustomItem.STORMCHARGER, WeaponEvents.Theme.of(WeaponEvents.Kind.LAST_STANDING, "Thunderdome", NamedTextColor.YELLOW));
      THEMES.put(CustomItem.OCEANIC_CROWN, WeaponEvents.Theme.of(WeaponEvents.Kind.FISHING, "Fishing Frenzy", NamedTextColor.BLUE));
      THEMES.put(CustomItem.OCEANIC_LEGGINGS, WeaponEvents.Theme.of(WeaponEvents.Kind.FISHING, "Fishing Frenzy", NamedTextColor.BLUE));
      THEMES.put(CustomItem.OCEANIC_BOOTS, WeaponEvents.Theme.of(WeaponEvents.Kind.FISHING, "Fishing Frenzy", NamedTextColor.BLUE));
      THEMES.put(CustomItem.FROSTBITE_AXE, WeaponEvents.Theme.of(WeaponEvents.Kind.SNOWBALL, "Snowball Brawl", NamedTextColor.AQUA));
      THEMES.put(CustomItem.SILENCED_SHIELD, WeaponEvents.Theme.of(WeaponEvents.Kind.HUNT, "Silent Cache", NamedTextColor.DARK_GRAY, Material.SCULK_SHRIEKER));
      THEMES.put(CustomItem.DRAGON_EGG, WeaponEvents.Theme.of(WeaponEvents.Kind.HUNT, "Dragon Egg Hunt", NamedTextColor.LIGHT_PURPLE, Material.DRAGON_EGG));
      THEMES.put(CustomItem.WORLD_BREAKER, WeaponEvents.Theme.of(WeaponEvents.Kind.MINING, "Mining Frenzy", NamedTextColor.GOLD));

      for(CustomItem var3 : new CustomItem[]{CustomItem.JUGGERNAUT_HELMET, CustomItem.JUGGERNAUT_CHESTPLATE, CustomItem.JUGGERNAUT_LEGGINGS, CustomItem.JUGGERNAUT_BOOTS, CustomItem.JUGGERNAUT_HAMMER}) {
         THEMES.put(var3, WeaponEvents.Theme.of(WeaponEvents.Kind.PVP_RACE, "Arena of Might", NamedTextColor.GOLD));
      }

      THEMES.put(CustomItem.BLOODMOON_KATANA, WeaponEvents.Theme.of(WeaponEvents.Kind.BLOOD_MOON, "Blood Moon", NamedTextColor.DARK_RED));
      LinkedHashMap var6 = new LinkedHashMap();
      var6.put(Material.BLAZE_ROD, 16);
      var6.put(Material.MAGMA_CREAM, 4);
      GATHER_LISTS.put(CustomItem.HORN_OF_HELLSCORCH, var6);
      LinkedHashMap var7 = new LinkedHashMap();

      for(Material var5 : new Material[]{Material.DANDELION, Material.POPPY, Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.ALLIUM, Material.LILY_OF_THE_VALLEY, Material.RED_TULIP}) {
         var7.put(var5, 1);
      }

      GATHER_LISTS.put(CustomItem.PLATE_OF_LIFE, var7);
      GATHER_LISTS.put(CustomItem.FLOWER_SWORD, var7);
   }

   static enum Kind {
      KOTH,
      HUNT,
      PVP_RACE,
      BLOOD_MOON,
      MINING,
      RACE,
      RELIC,
      GATHER,
      LAST_STANDING,
      FISHING,
      SNOWBALL;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{KOTH, HUNT, PVP_RACE, BLOOD_MOON, MINING, RACE, RELIC, GATHER, LAST_STANDING, FISHING, SNOWBALL};
      }
   }

   static record Theme(Kind kind, String title, NamedTextColor color, Material material) {
      static Theme of(Kind var0, String var1, NamedTextColor var2) {
         return new Theme(var0, var1, var2, (Material)null);
      }

      static Theme of(Kind var0, String var1, NamedTextColor var2, Material var3) {
         return new Theme(var0, var1, var2, var3);
      }
   }

   private abstract class Active {
      final CustomItem prize;
      final Theme theme;
      final Location center;
      final BossBar bar;
      final long endsAt;
      BukkitTask task;
      int seconds;

      Active(CustomItem var2, Theme var3, Location var4, int var5) {
         this.prize = var2;
         this.theme = var3;
         this.center = var4;
         this.endsAt = System.currentTimeMillis() + (long)var5 * 60000L;
         this.bar = BossBar.bossBar(Component.text(var3.title, var3.color, new TextDecoration[]{TextDecoration.BOLD}), 1.0F, WeaponEvents.barColor(var3.color), Overlay.PROGRESS);
      }

      void begin() {
         for(Player var2 : Bukkit.getOnlinePlayers()) {
            var2.showBossBar(this.bar);
            var2.playSound(var2.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 0.7F);
         }

         Bukkit.broadcast(Component.empty());
         Bukkit.broadcast(Component.text("⚔ WEAPON EVENT: " + this.theme.title + " ⚔", this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         Bukkit.broadcast(Component.text("Prize: ", NamedTextColor.GRAY).append(this.prizeName()));

         for(String var4 : this.howToWin()) {
            Bukkit.broadcast(Component.text(var4, NamedTextColor.WHITE));
         }

         Bukkit.broadcast(Component.empty());
         this.start();
         if (WeaponEvents.this.active == this) {
            this.task = Bukkit.getScheduler().runTaskTimer(WeaponEvents.this.plugin, this::second, 20L, 20L);
         }
      }

      private void second() {
         ++this.seconds;
         if (System.currentTimeMillis() >= this.endsAt) {
            this.timeUp();
         } else {
            this.tick();
         }
      }

      long secondsLeft() {
         return Math.max(0L, (this.endsAt - System.currentTimeMillis()) / 1000L);
      }

      String timeLeft() {
         long var1 = this.secondsLeft();
         return var1 / 60L + ":" + String.format("%02d", var1 % 60L);
      }

      Component prizeName() {
         return Items.create(this.prize).getItemMeta().displayName();
      }

      abstract List<String> howToWin();

      abstract void start();

      abstract void tick();

      void timeUp() {
         this.finish((Player)null);
      }

      void cleanup() {
      }

      void finish(Player var1) {
         if (this.task != null) {
            this.task.cancel();
         }

         this.cleanup();

         for(Player var3 : Bukkit.getOnlinePlayers()) {
            var3.hideBossBar(this.bar);
         }

         if (WeaponEvents.this.active == this) {
            WeaponEvents.this.active = null;
         }

         if (var1 != null) {
            ItemStack var5 = Items.create(this.prize);
            var1.getInventory().addItem(new ItemStack[]{var5}).values().forEach((var1x) -> var1.getWorld().dropItem(var1.getLocation(), var1x));
            Bukkit.broadcast(((TextComponent)Component.text("⚔ " + var1.getName() + " won " + this.theme.title + " and the ", this.theme.color, new TextDecoration[]{TextDecoration.BOLD}).append(this.prizeName())).append(Component.text("! ⚔", this.theme.color, new TextDecoration[]{TextDecoration.BOLD})));

            for(Player var4 : Bukkit.getOnlinePlayers()) {
               var4.playSound(var4.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
            }

            var1.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, var1.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 80, (double)0.5F, (double)1.0F, (double)0.5F, 0.4);
         } else {
            Bukkit.broadcast(Component.text(this.theme.title + " ended with no winner.", NamedTextColor.GRAY));
         }

      }

      void stopByAdmin() {
         Bukkit.broadcast(Component.text(this.theme.title + " was stopped.", NamedTextColor.GRAY));
         if (this.task != null) {
            this.task.cancel();
         }

         this.cleanup();

         for(Player var2 : Bukkit.getOnlinePlayers()) {
            var2.hideBossBar(this.bar);
         }

         if (WeaponEvents.this.active == this) {
            WeaponEvents.this.active = null;
         }

      }

      void onDamage(EntityDamageByEntityEvent var1) {
      }

      void onDeath(EntityDeathEvent var1) {
      }

      void onInteract(PlayerInteractEvent var1) {
      }

      void onBreak(BlockBreakEvent var1) {
      }

      void onPlace(BlockPlaceEvent var1) {
      }

      void onMove(PlayerMoveEvent var1) {
      }

      void onQuit(PlayerQuitEvent var1) {
      }

      void onDrop(PlayerDropItemEvent var1) {
      }

      void onFish(PlayerFishEvent var1) {
      }

      void onProjectileHit(ProjectileHitEvent var1) {
      }

      boolean protects(Block var1) {
         return false;
      }
   }

   private final class KingOfTheHill extends Active {
      static final int GOAL = 60;
      static final double RADIUS = (double)5.0F;
      final Map<UUID, Integer> score = new HashMap();

      KingOfTheHill(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 15);
      }

      List<String> howToWin() {
         return List.of("Capture the zone at " + WeaponEvents.coords(this.center) + " in " + WeaponEvents.worldName(this.center) + "!", "Stand in it alone to earn points. If two players are in it, nobody scores.", "First to 60 seconds wins, or the highest score after 15 minutes.");
      }

      void start() {
      }

      void tick() {
         World var1 = this.center.getWorld();
         WeaponEvents.ring(this.center.clone().add((double)0.0F, 0.2, (double)0.0F), (double)5.0F, Particle.END_ROD, 50);
         WeaponEvents.ring(this.center.clone().add((double)0.0F, 0.2, (double)0.0F), (double)5.0F, Particle.FLAME, 25);
         ArrayList<Player> var2 = new ArrayList<>();

         for(Player var4 : var1.getPlayers()) {
            if (WeaponEvents.playable(var4)) {
               Location var5 = var4.getLocation();
               double var6 = var5.getX() - this.center.getX();
               double var8 = var5.getZ() - this.center.getZ();
               if (var6 * var6 + var8 * var8 <= (double)25.0F && Math.abs(var5.getY() - this.center.getY()) <= (double)4.0F) {
                  var2.add(var4);
               }
            }
         }

         if (var2.size() == 1) {
            Player var10 = (Player)var2.get(0);
            int var13 = (Integer)this.score.merge(var10.getUniqueId(), 1, Integer::sum);
            var10.sendActionBar(Component.text("Holding the zone: " + var13 + "/60", this.theme.color));
            if (var13 >= 60) {
               this.finish(var10);
               return;
            }
         } else if (var2.size() > 1) {
            for(Player var14 : var2) {
               var14.sendActionBar(Component.text("CONTESTED!", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}));
            }
         }

         Map.Entry var12 = WeaponEvents.leader(this.score);
         String var15 = var12 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var12.getKey()) + " " + String.valueOf(var12.getValue()) + "/60";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var15 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var12 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var12.getValue() / 60.0F));
      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.score));
      }
   }

   private final class TreasureHunt extends Active {
      Block target;
      BlockData original;
      int hints;

      TreasureHunt(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 20);
      }

      List<String> howToWin() {
         String var10000 = WeaponEvents.pretty(this.theme.material);
         return List.of("A " + var10000 + " is hidden on the surface 150-350 blocks from " + WeaponEvents.coords(this.center) + " in " + WeaponEvents.worldName(this.center) + ".", "A hint narrowing it down comes every minute. First to right-click it wins!");
      }

      void start() {
         World var1 = this.center.getWorld();
         ThreadLocalRandom var2 = ThreadLocalRandom.current();
         double var3 = var2.nextDouble((Math.PI * 2D));
         double var5 = var2.nextDouble((double)150.0F, (double)350.0F);
         int var7 = this.center.getBlockX() + (int)(Math.cos(var3) * var5);
         int var8 = this.center.getBlockZ() + (int)(Math.sin(var3) * var5);
         int var9 = var1.getHighestBlockYAt(var7, var8) + 1;
         this.target = var1.getBlockAt(var7, var9, var8);
         this.original = this.target.getBlockData();
         if (!SafeZones.isProtected(this.target.getLocation())) this.target.setType(this.theme.material, false);
      }

      void tick() {
         if (this.target.getType() != this.theme.material) {
            if (!SafeZones.isProtected(this.target.getLocation())) this.target.setType(this.theme.material, false);
         }

         World var1 = this.target.getWorld();
         Location var2 = this.target.getLocation().add((double)0.5F, (double)1.0F, (double)0.5F);

         for(int var3 = 0; var3 < 30; var3 += 2) {
            var1.spawnParticle(this.theme.material == Material.DRAGON_EGG ? Particle.PORTAL : Particle.SCULK_SOUL, var2.clone().add((double)0.0F, (double)var3, (double)0.0F), 2, 0.1, 0.2, 0.1, (double)0.0F);
         }

         var1.spawnParticle(Particle.END_ROD, var2, 3, 0.2, 0.3, 0.2, 0.01);

         for(Player var4 : WeaponEvents.playersNear(var2, (double)40.0F)) {
            var4.playSound(var2, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6F, 1.4F);
         }

         if (this.seconds % 60 == 0) {
            ++this.hints;
            int var12 = Math.max(15, 250 - this.hints * 45);
            ThreadLocalRandom var13 = ThreadLocalRandom.current();
            double var5 = var13.nextDouble((Math.PI * 2D));
            double var7 = var13.nextDouble((double)var12 * 0.6);
            int var9 = this.target.getX() + (int)(Math.cos(var5) * var7);
            int var10 = this.target.getZ() + (int)(Math.sin(var5) * var7);
            Bukkit.broadcast(Component.text("[" + this.theme.title + "] Hint " + this.hints + ": it's within " + var12 + " blocks of X " + var9 + ", Z " + var10, this.theme.color));
         }

         this.bar.name(Component.text(this.theme.title + "  |  Hints given: " + this.hints + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(Math.max(0.0F, Math.min(1.0F, (float)this.secondsLeft() / 1200.0F)));
      }

      void onInteract(PlayerInteractEvent var1) {
         if (var1.getClickedBlock() != null && var1.getClickedBlock().equals(this.target)) {
            var1.setCancelled(true);
            if (var1.getAction() == Action.RIGHT_CLICK_BLOCK || var1.getAction() == Action.LEFT_CLICK_BLOCK) {
               this.finish(var1.getPlayer());
            }

         }
      }

      void onBreak(BlockBreakEvent var1) {
         if (var1.getBlock().equals(this.target)) {
            var1.setCancelled(true);
            this.finish(var1.getPlayer());
         }
      }

      boolean protects(Block var1) {
         return var1.equals(this.target);
      }

      void cleanup() {
         if (this.target != null && this.target.getType() == this.theme.material) {
            this.target.setBlockData(this.original, false);
         }

      }
   }

   private final class PvpRace extends Active {
      static final int GOAL = 10;
      final Map<UUID, Integer> kills = new HashMap();

      PvpRace(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 10);
      }

      List<String> howToWin() {
         return List.of("Player kills count for the next 10 minutes!", "First to 10 kills wins, or the most kills when time runs out.");
      }

      void start() {
      }

      void tick() {
         Map.Entry var1 = WeaponEvents.leader(this.kills);
         String var2 = var1 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var1.getKey()) + " (" + String.valueOf(var1.getValue()) + " kills)";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var2 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(Math.max(0.0F, Math.min(1.0F, (float)this.secondsLeft() / 600.0F)));
      }

      void onDeath(EntityDeathEvent var1) {
         LivingEntity var3 = var1.getEntity();
         if (var3 instanceof Player var2) {
            Player var5 = var2.getKiller();
            if (var5 != null && var5 != var2) {
               int var4 = (Integer)this.kills.merge(var5.getUniqueId(), 1, Integer::sum);
               String var10000 = this.theme.title;
               Bukkit.broadcast(Component.text("[" + var10000 + "] " + var5.getName() + " has " + var4 + " kill" + (var4 == 1 ? "" : "s"), this.theme.color));
               if (var4 >= 10) {
                  this.finish(var5);
               }

            }
         }
      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.kills));
      }
   }

   private final class BloodMoon extends Active {
      static final int GOAL = 40;
      final Map<UUID, Integer> kills = new HashMap();
      final Set<UUID> spawned = new HashSet();
      long originalTime;

      BloodMoon(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 8);
      }

      List<String> howToWin() {
         return List.of("The Blood Moon rises over " + WeaponEvents.worldName(this.center) + "! Blood-red monsters hunt every player.", "Kill monsters: first to 40 wins, or the most kills after 8 minutes.");
      }

      void start() {
         World var1 = this.center.getWorld();
         this.originalTime = var1.getTime();
         var1.setTime(18000L);

         for(Player var3 : var1.getPlayers()) {
            var3.playSound(var3.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6F, 0.5F);
         }

      }

      void tick() {
         World var1 = this.center.getWorld();
         var1.setTime(18000L);
         if (this.seconds % 15 == 0) {
            for(Player var3 : var1.getPlayers()) {
               if (WeaponEvents.playable(var3)) {
                  for(int var4 = 0; var4 < 2; ++var4) {
                     this.spawnBloodMob(var3);
                  }
               }
            }
         }

         for(Player var7 : var1.getPlayers()) {
            if (this.seconds % 3 == 0) {
               var7.spawnParticle(Particle.DUST, var7.getLocation().add((double)0.0F, (double)2.5F, (double)0.0F), 6, (double)3.0F, (double)1.0F, (double)3.0F, (double)0.0F, new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 0, 0), 2.0F));
            }
         }

         Map.Entry var6 = WeaponEvents.leader(this.kills);
         String var8 = var6 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var6.getKey()) + " (" + String.valueOf(var6.getValue()) + ")";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var8 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(Math.max(0.0F, Math.min(1.0F, (float)this.secondsLeft() / 480.0F)));
      }

      private void spawnBloodMob(Player var1) {
         ThreadLocalRandom var2 = ThreadLocalRandom.current();
         double var3 = var2.nextDouble((Math.PI * 2D));
         double var5 = var2.nextDouble((double)8.0F, (double)15.0F);
         World var7 = var1.getWorld();
         int var8 = var1.getLocation().getBlockX() + (int)(Math.cos(var3) * var5);
         int var9 = var1.getLocation().getBlockZ() + (int)(Math.sin(var3) * var5);
         int var10 = var7.getHighestBlockYAt(var8, var9) + 1;
         if (!(Math.abs((double)var10 - var1.getLocation().getY()) > (double)12.0F)) {
            EntityType[] var11 = new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK, EntityType.SPIDER};
            Entity var12 = var7.spawnEntity(new Location(var7, (double)var8 + (double)0.5F, (double)var10, (double)var9 + (double)0.5F), var11[var2.nextInt(var11.length)]);
            if (var12 instanceof LivingEntity) {
               LivingEntity var13 = (LivingEntity)var12;
               var13.customName(Component.text("Blood " + WeaponEvents.pretty(var12.getType()), NamedTextColor.DARK_RED));
               var13.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 0, false, false, false));
               var13.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0, false, false, false));
               if (var13.getEquipment() != null) {
                  var13.getEquipment().setHelmet(new ItemStack(Material.RED_STAINED_GLASS));
                  var13.getEquipment().setHelmetDropChance(0.0F);
               }

               if (var13 instanceof Mob) {
                  Mob var14 = (Mob)var13;
                  var14.setTarget(var1);
               }

               this.spawned.add(var13.getUniqueId());
            }
         }
      }

      void onDeath(EntityDeathEvent var1) {
         if (var1.getEntity() instanceof Monster) {
            Player var2 = var1.getEntity().getKiller();
            if (var2 != null) {
               int var3 = (Integer)this.kills.merge(var2.getUniqueId(), 1, Integer::sum);
               var2.sendActionBar(Component.text("Blood Moon kills: " + var3 + "/40", this.theme.color));
               this.spawned.remove(var1.getEntity().getUniqueId());
               if (var3 >= 40) {
                  this.finish(var2);
               }

            }
         }
      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.kills));
      }

      void cleanup() {
         for(UUID var2 : this.spawned) {
            Entity var3 = Bukkit.getEntity(var2);
            if (var3 != null) {
               var3.remove();
            }
         }

         this.spawned.clear();
         this.center.getWorld().setTime(this.originalTime);
      }
   }

   private final class MiningRace extends Active {
      static final int GOAL = 150;
      final Map<UUID, Integer> points = new HashMap();
      final Set<Block> placed = new HashSet();

      MiningRace(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 10);
      }

      List<String> howToWin() {
         return List.of("Mine ores for points for the next 10 minutes!", "Coal/copper/redstone/lapis/quartz 1, iron 2, gold 3, diamond/emerald 10, ancient debris 15.", "First to 150 points wins, or the most points when time runs out.");
      }

      void start() {
      }

      void tick() {
         Map.Entry var1 = WeaponEvents.leader(this.points);
         String var2 = var1 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var1.getKey()) + " (" + String.valueOf(var1.getValue()) + "/150)";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var2 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var1 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var1.getValue() / 150.0F));
      }

      private int value(Material var1) {
         if (var1 == Material.ANCIENT_DEBRIS) {
            return 15;
         } else if (!Tag.DIAMOND_ORES.isTagged(var1) && !Tag.EMERALD_ORES.isTagged(var1)) {
            if (Tag.GOLD_ORES.isTagged(var1)) {
               return 3;
            } else if (Tag.IRON_ORES.isTagged(var1)) {
               return 2;
            } else {
               return !Tag.COAL_ORES.isTagged(var1) && !Tag.COPPER_ORES.isTagged(var1) && !Tag.REDSTONE_ORES.isTagged(var1) && !Tag.LAPIS_ORES.isTagged(var1) && var1 != Material.NETHER_QUARTZ_ORE ? 0 : 1;
            }
         } else {
            return 10;
         }
      }

      void onPlace(BlockPlaceEvent var1) {
         if (this.value(var1.getBlock().getType()) > 0) {
            this.placed.add(var1.getBlock());
         }

      }

      void onBreak(BlockBreakEvent var1) {
         int var2 = this.value(var1.getBlock().getType());
         if (var2 != 0 && !this.placed.remove(var1.getBlock())) {
            Player var3 = var1.getPlayer();
            int var4 = (Integer)this.points.merge(var3.getUniqueId(), var2, Integer::sum);
            var3.sendActionBar(Component.text("+" + var2 + "  (" + var4 + "/150 points)", this.theme.color));
            if (var4 >= 150) {
               this.finish(var3);
            }

         }
      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.points));
      }
   }

   private final class CheckpointRace extends Active {
      static final int COUNT = 5;
      final List<Location> points = new ArrayList();
      final Map<UUID, Integer> progress = new HashMap();

      CheckpointRace(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 15);
      }

      List<String> howToWin() {
         return List.of("Race through 5 checkpoints in order, starting from " + WeaponEvents.coords(this.center) + "!", "Follow your beam of light; your action bar shows the distance to your next checkpoint.", "First to reach the last one wins. You have 15 minutes.");
      }

      void start() {
         World var1 = this.center.getWorld();
         ThreadLocalRandom var2 = ThreadLocalRandom.current();
         Location var3 = this.center;

         for(int var4 = 0; var4 < 5; ++var4) {
            double var5 = var2.nextDouble((Math.PI * 2D));
            double var7 = var2.nextDouble((double)60.0F, (double)110.0F);
            int var9 = var3.getBlockX() + (int)(Math.cos(var5) * var7);
            int var10 = var3.getBlockZ() + (int)(Math.sin(var5) * var7);
            Location var11 = new Location(var1, (double)var9 + (double)0.5F, (double)(var1.getHighestBlockYAt(var9, var10) + 1), (double)var10 + (double)0.5F);
            this.points.add(var11);
            var3 = var11;
         }

      }

      void tick() {
         for(Player var2 : this.center.getWorld().getPlayers()) {
            if (WeaponEvents.playable(var2)) {
               int var3 = (Integer)this.progress.getOrDefault(var2.getUniqueId(), 0);
               Location var4 = (Location)this.points.get(var3);

               for(int var5 = 0; var5 < 40; var5 += 2) {
                  var2.spawnParticle(this.theme.material == Material.ENDER_PEARL ? Particle.PORTAL : Particle.END_ROD, var4.clone().add((double)0.0F, (double)var5, (double)0.0F), 2, 0.15, 0.3, 0.15, (double)0.0F);
               }

               var2.spawnParticle(Particle.CLOUD, var4, 15, (double)1.5F, 0.2, (double)1.5F, (double)0.0F);
               var2.sendActionBar(Component.text("Checkpoint " + (var3 + 1) + "/5: " + (int)WeaponEvents.flatDistance(var2.getLocation(), var4) + " blocks " + WeaponEvents.direction(var2.getLocation(), var4), this.theme.color));
            }
         }

         Map.Entry var6 = WeaponEvents.leader(this.progress);
         String var7 = var6 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var6.getKey()) + " (" + String.valueOf(var6.getValue()) + "/5)";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var7 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var6 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var6.getValue() / 5.0F));
      }

      void onMove(PlayerMoveEvent var1) {
         Player var2 = var1.getPlayer();
         if (WeaponEvents.playable(var2) && var2.getWorld() == this.center.getWorld()) {
            int var3 = (Integer)this.progress.getOrDefault(var2.getUniqueId(), 0);
            Location var4 = (Location)this.points.get(var3);
            if (!(WeaponEvents.flatDistance(var1.getTo(), var4) > (double)4.0F) && !(Math.abs(var1.getTo().getY() - var4.getY()) > (double)8.0F)) {
               ++var3;
               this.progress.put(var2.getUniqueId(), var3);
               var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F + (float)var3 * 0.25F);
               var2.getWorld().spawnParticle(Particle.FIREWORK, var4.clone().add((double)0.0F, (double)1.0F, (double)0.0F), 40, (double)0.5F, (double)1.0F, (double)0.5F, 0.1);
               if (var3 >= 5) {
                  this.finish(var2);
               } else {
                  String var10000 = this.theme.title;
                  Bukkit.broadcast(Component.text("[" + var10000 + "] " + var2.getName() + " reached checkpoint " + var3 + "/5", this.theme.color));
               }
            }
         }
      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.progress));
      }
   }

   private final class RelicHold extends Active {
      static final int GOAL = 120;
      final Map<UUID, Integer> held = new HashMap();
      Item dropped;

      RelicHold(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 15);
      }

      List<String> howToWin() {
         return List.of("The " + this.theme.title + " has appeared at " + WeaponEvents.coords(this.center) + "!", "Pick it up and keep it: you score 1 point per second while you carry it.", "The carrier glows and their location is announced. Kill them to make them drop it.", "First to 120 seconds wins, or the highest score after 15 minutes.");
      }

      ItemStack relic() {
         ItemStack var1 = new ItemStack(this.theme.material);
         ItemMeta var2 = var1.getItemMeta();
         var2.displayName(Component.text(this.theme.title, this.theme.color, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false));
         var2.lore(List.of((TextComponent)Component.text("Event relic: hold it to win the prize", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
         var2.setEnchantmentGlintOverride(true);
         var2.getPersistentDataContainer().set(WeaponEvents.this.relicKey, PersistentDataType.BYTE, (byte)1);
         var1.setItemMeta(var2);
         return var1;
      }

      void dropAt(Location var1) {
         this.dropped = var1.getWorld().dropItem(var1.clone().add((double)0.0F, (double)0.5F, (double)0.0F), this.relic());
         this.protect(this.dropped);
      }

      void protect(Item var1) {
         var1.setInvulnerable(true);
         var1.setUnlimitedLifetime(true);
         var1.setGlowing(true);
      }

      Player holder() {
         for(Player var2 : Bukkit.getOnlinePlayers()) {
            for(ItemStack var6 : var2.getInventory().getContents()) {
               if (WeaponEvents.this.isRelic(var6)) {
                  return var2;
               }
            }
         }

         return null;
      }

      void start() {
         this.dropAt(this.center);
      }

      void tick() {
         Player var1 = this.holder();
         if (var1 != null) {
            int var2 = (Integer)this.held.merge(var1.getUniqueId(), 1, Integer::sum);
            var1.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, false));
            var1.sendActionBar(Component.text("You hold the relic: " + var2 + "/120", this.theme.color));
            if (this.seconds % 10 == 0) {
               String var10000 = this.theme.title;
               Bukkit.broadcast(Component.text("[" + var10000 + "] " + var1.getName() + " has the relic at " + WeaponEvents.coords(var1.getLocation()) + " (" + var2 + "/120)", this.theme.color));
            }

            if (var2 >= 120) {
               this.finish(var1);
               return;
            }
         } else if (this.dropped != null && this.dropped.isValid()) {
            Location var5 = this.dropped.getLocation();

            for(int var3 = 0; var3 < 30; var3 += 2) {
               var5.getWorld().spawnParticle(Particle.END_ROD, var5.clone().add((double)0.0F, (double)var3, (double)0.0F), 1, 0.1, 0.2, 0.1, (double)0.0F);
            }
         } else if (this.dropped == null || this.dropped.isDead()) {
            this.dropAt(this.center);
            Bukkit.broadcast(Component.text("[" + this.theme.title + "] The relic returned to " + WeaponEvents.coords(this.center), this.theme.color));
         }

         Map.Entry var6 = WeaponEvents.leader(this.held);
         String var7 = var1 == null ? "nobody" : var1.getName();
         String var4 = var6 == null ? "-" : WeaponEvents.nameOf((UUID)var6.getKey()) + " " + String.valueOf(var6.getValue()) + "/120";
         this.bar.name(Component.text(this.theme.title + "  |  Holder: " + var7 + "  |  Best: " + var4 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var6 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var6.getValue() / 120.0F));
      }

      void takeFrom(Player var1) {
         for(ItemStack var5 : var1.getInventory().getContents()) {
            if (WeaponEvents.this.isRelic(var5)) {
               var5.setAmount(0);
            }
         }

      }

      void onDeath(EntityDeathEvent var1) {
         LivingEntity var3 = var1.getEntity();
         if (var3 instanceof Player var2) {
            boolean var8 = var1.getDrops().removeIf(WeaponEvents.this::isRelic);

            for(ItemStack var7 : var2.getInventory().getContents()) {
               if (WeaponEvents.this.isRelic(var7)) {
                  var8 = true;
                  var7.setAmount(0);
               }
            }

            if (var8) {
               this.dropAt(var2.getLocation());
            }

         }
      }

      void onQuit(PlayerQuitEvent var1) {
         Player var2 = var1.getPlayer();

         for(ItemStack var6 : var2.getInventory().getContents()) {
            if (WeaponEvents.this.isRelic(var6)) {
               var6.setAmount(0);
               this.dropAt(var2.getLocation());
               return;
            }
         }

      }

      void onDrop(PlayerDropItemEvent var1) {
         if (WeaponEvents.this.isRelic(var1.getItemDrop().getItemStack())) {
            this.dropped = var1.getItemDrop();
            this.protect(this.dropped);
         }

      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.held));
      }

      void cleanup() {
         for(Player var2 : Bukkit.getOnlinePlayers()) {
            this.takeFrom(var2);
         }

         if (this.dropped != null) {
            this.dropped.remove();
         }

      }
   }

   private final class Gather extends Active {
      final Map<Material, Integer> needs;
      final Map<UUID, Integer> percent = new HashMap();

      Gather(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 10);
         this.needs = (Map)WeaponEvents.GATHER_LISTS.get(var2);
      }

      List<String> howToWin() {
         StringJoiner var1 = new StringJoiner(", ");
         this.needs.forEach((var1x, var2) -> var1.add(var2 + "x " + WeaponEvents.pretty(var1x)));
         return List.of("Collect: " + String.valueOf(var1), "First to have all of it in their inventory wins. You have 10 minutes.");
      }

      void start() {
      }

      void tick0() {
         int var1 = this.needs.values().stream().mapToInt(Integer::intValue).sum();

         for(Player var3 : Bukkit.getOnlinePlayers()) {
            if (WeaponEvents.playable(var3)) {
               EnumMap<Material, Integer> var4 = new EnumMap<>(Material.class);

               for(ItemStack var8 : var3.getInventory().getContents()) {
                  if (var8 != null && this.needs.containsKey(var8.getType())) {
                     var4.merge(var8.getType(), var8.getAmount(), Integer::sum);
                  }
               }

               int var11 = 0;

               for(Map.Entry var14 : this.needs.entrySet()) {
                  var11 += Math.min((Integer)var14.getValue(), (Integer)var4.getOrDefault(var14.getKey(), 0));
               }

               int var13 = var11 * 100 / var1;
               if (var13 > 0) {
                  this.percent.put(var3.getUniqueId(), var13);
               }

               if (var11 >= var1) {
                  this.finish(var3);
                  return;
               }
            }
         }

         Map.Entry var9 = WeaponEvents.leader(this.percent);
         String var10 = var9 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var9.getKey()) + " (" + String.valueOf(var9.getValue()) + "%)";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var10 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var9 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var9.getValue() / 100.0F));
      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.percent));
      }

      void tick() {
         WeaponsConfig.gatherProgress(this);
         this.tick0();
      }
   }

   private final class LastStanding extends Active {
      static final double RADIUS = (double)25.0F;
      static final int JOIN_SECONDS = 30;
      final Set<UUID> alive = new LinkedHashSet();
      boolean fighting;

      LastStanding(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 12);
      }

      List<String> howToWin() {
         return List.of("Get within 25 blocks of " + WeaponEvents.coords(this.center) + " in the next 30 seconds to join!", "Then lightning starts striking the arena. Leave the arena or die and you're out.", "Last player standing wins.");
      }

      void start() {
         World var1 = this.center.getWorld();
         var1.setStorm(true);
         var1.setThundering(true);
      }

      boolean inside(Player var1) {
         return var1.getWorld() == this.center.getWorld() && WeaponEvents.flatDistance(var1.getLocation(), this.center) <= (double)25.0F && Math.abs(var1.getLocation().getY() - this.center.getY()) <= (double)20.0F;
      }

      void tick() {
         World var1 = this.center.getWorld();
         WeaponEvents.ring(this.center, (double)25.0F, Particle.ELECTRIC_SPARK, 120);
         if (!this.fighting) {
            for(Player var13 : var1.getPlayers()) {
               if (WeaponEvents.playable(var13) && this.inside(var13) && this.alive.add(var13.getUniqueId())) {
                  Bukkit.broadcast(Component.text("[" + this.theme.title + "] " + var13.getName() + " entered the arena!", this.theme.color));
               }
            }

            int var11 = 30 - this.seconds;
            this.bar.name(Component.text(this.theme.title + "  |  Get in the arena! Starts in " + var11 + "s  |  " + this.alive.size() + " joined", this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
            this.bar.progress(Math.max(0.0F, (float)var11 / 30.0F));
            if (var11 <= 0) {
               if (this.alive.size() < 2) {
                  Bukkit.broadcast(Component.text("[" + this.theme.title + "] Not enough players joined (need 2).", NamedTextColor.GRAY));
                  this.finish((Player)null);
                  return;
               }

               this.fighting = true;
               Bukkit.broadcast(Component.text("[" + this.theme.title + "] FIGHT! Stay in the arena!", this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
               var1.playSound(this.center, Sound.ITEM_TRIDENT_THUNDER, 3.0F, 1.0F);
            }

         } else {
            for(UUID var3 : new ArrayList<>(this.alive)) {
               Player var4 = Bukkit.getPlayer(var3);
               if (var4 == null || var4.isDead() || !this.inside(var4)) {
                  this.eliminate(var3, var4 == null ? "left the game" : "left the arena");
               }
            }

            if (this.alive.size() <= 1) {
               this.finish(this.alive.isEmpty() ? null : Bukkit.getPlayer((UUID)this.alive.iterator().next()));
            } else {
               int var9 = Math.max(1, 4 - this.seconds / 60);
               if (this.seconds % var9 == 0) {
                  ArrayList var12 = new ArrayList(this.alive);
                  Player var14 = Bukkit.getPlayer((UUID)var12.get(ThreadLocalRandom.current().nextInt(var12.size())));
                  if (var14 != null) {
                     ThreadLocalRandom var5 = ThreadLocalRandom.current();
                     Location var6 = var14.getLocation().add(var5.nextDouble((double)-3.0F, (double)3.0F), (double)0.0F, var5.nextDouble((double)-3.0F, (double)3.0F));
                     var1.strikeLightningEffect(var6);

                     for(Player var8 : WeaponEvents.playersNear(var6, (double)2.5F)) {
                        if (!SafeZones.isProtected(var8.getLocation())) var8.damage((double)5.0F, DamageSource.builder(DamageType.LIGHTNING_BOLT).build());
                     }
                  }
               }

               this.bar.name(Component.text(this.theme.title + "  |  " + this.alive.size() + " players left  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
               this.bar.progress(Math.max(0.0F, Math.min(1.0F, (float)this.secondsLeft() / 720.0F)));
            }
         }
      }

      void eliminate(UUID var1, String var2) {
         if (this.alive.remove(var1)) {
            String var10000 = this.theme.title;
            Bukkit.broadcast(Component.text("[" + var10000 + "] " + WeaponEvents.nameOf(var1) + " is out (" + var2 + ")! " + this.alive.size() + " left.", this.theme.color));
         }
      }

      void onDeath(EntityDeathEvent var1) {
         if (this.fighting) {
            LivingEntity var3 = var1.getEntity();
            if (var3 instanceof Player) {
               Player var2 = (Player)var3;
               this.eliminate(var2.getUniqueId(), "died");
            }
         }

      }

      void timeUp() {
         Player var1 = null;

         for(UUID var3 : this.alive) {
            Player var4 = Bukkit.getPlayer(var3);
            if (var4 != null && (var1 == null || var4.getHealth() > var1.getHealth())) {
               var1 = var4;
            }
         }

         this.finish(this.fighting ? var1 : null);
      }

      void cleanup() {
         this.center.getWorld().setStorm(false);
         this.center.getWorld().setThundering(false);
      }
   }

   private final class FishingFrenzy extends Active {
      static final int GOAL = 25;
      final Map<UUID, Integer> fish = new HashMap();

      FishingFrenzy(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 8);
      }

      List<String> howToWin() {
         return List.of("Grab a fishing rod! Every fish you catch counts (cod, salmon, tropical fish, pufferfish).", "First to 25 fish wins, or the most fish after 8 minutes.");
      }

      void start() {
      }

      void tick() {
         Map.Entry var1 = WeaponEvents.leader(this.fish);
         String var2 = var1 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var1.getKey()) + " (" + String.valueOf(var1.getValue()) + "/25)";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var2 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var1 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var1.getValue() / 25.0F));
      }

      void onFish(PlayerFishEvent var1) {
         if (var1.getState() == State.CAUGHT_FISH) {
            Entity var3 = var1.getCaught();
            if (var3 instanceof Item) {
               Item var2 = (Item)var3;
               Material var6 = var2.getItemStack().getType();
               if (var6 != Material.COD && var6 != Material.SALMON && var6 != Material.TROPICAL_FISH && var6 != Material.PUFFERFISH) {
                  return;
               }

               Player var4 = var1.getPlayer();
               int var5 = (Integer)this.fish.merge(var4.getUniqueId(), 1, Integer::sum);
               var4.sendActionBar(Component.text("Fish caught: " + var5 + "/25", this.theme.color));
               if (var5 >= 25) {
                  this.finish(var4);
               }

               return;
            }
         }

      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.fish));
      }
   }

   private final class SnowballBrawl extends Active {
      static final int GOAL = 30;
      final Map<UUID, Integer> hits = new HashMap();

      SnowballBrawl(CustomItem var2, Theme var3, Location var4) {
         super(var2, var3, var4, 5);
      }

      List<String> howToWin() {
         return List.of("Snowball fight! Everyone gets 16 snowballs every 15 seconds.", "Each hit on another player is a point and freezes them a little.", "First to 30 hits wins, or the most hits after 5 minutes.");
      }

      void start() {
         this.handOut();
      }

      void handOut() {
         for(Player var2 : Bukkit.getOnlinePlayers()) {
            if (WeaponEvents.playable(var2)) {
               var2.getInventory().addItem(new ItemStack[]{new ItemStack(Material.SNOWBALL, 16)}).values().forEach((var1) -> var2.getWorld().dropItem(var2.getLocation(), var1));
            }
         }

      }

      void tick() {
         if (this.seconds % 15 == 0) {
            this.handOut();
         }

         Map.Entry var1 = WeaponEvents.leader(this.hits);
         String var2 = var1 == null ? "nobody yet" : WeaponEvents.nameOf((UUID)var1.getKey()) + " (" + String.valueOf(var1.getValue()) + "/30)";
         this.bar.name(Component.text(this.theme.title + "  |  Leader: " + var2 + "  |  " + this.timeLeft(), this.theme.color, new TextDecoration[]{TextDecoration.BOLD}));
         this.bar.progress(var1 == null ? 0.0F : Math.min(1.0F, (float)(Integer)var1.getValue() / 30.0F));
      }

      void onProjectileHit(ProjectileHitEvent var1) {
         Projectile var4 = var1.getEntity();
         if (var4 instanceof Snowball var2) {
            ProjectileSource var6 = var2.getShooter();
            if (var6 instanceof Player var3) {
               Entity var5 = var1.getHitEntity();
               if (var5 instanceof Player var7) {
                  if (var7 != var3 && WeaponEvents.playable(var7)) {
                     int var8 = (Integer)this.hits.merge(var3.getUniqueId(), 1, Integer::sum);
                     var7.setFreezeTicks(Math.min(var7.getMaxFreezeTicks(), var7.getFreezeTicks() + 40));
                     var7.getWorld().spawnParticle(Particle.SNOWFLAKE, var7.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 15, 0.3, (double)0.5F, 0.3, 0.02);
                     var3.playSound(var3.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.8F, 1.4F);
                     var3.sendActionBar(Component.text("Hits: " + var8 + "/30", this.theme.color));
                     if (var8 >= 30) {
                        this.finish(var3);
                     }

                     return;
                  }
               }

               return;
            }
         }

      }

      void timeUp() {
         this.finish(WeaponEvents.onlineLeader(this.hits));
      }
   }
}
