package dev.smpweapons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

/** Permanent fifth-hit cobweb + stationary, configured flowering sanctuary. */
public final class FlowerSword implements Listener {
    private final SMPWeapons plugin;
    private final TrustManager trust;
    private final Map<UUID, Integer> hitCounts = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<Block, Material> liveFlowers = new HashMap<>();
    private final List<BukkitRunnable> activeFields = new ArrayList<>();
    private final Random rng = new Random();

    FlowerSword(SMPWeapons plugin, TrustManager trust) { this.plugin = plugin; this.trust = trust; }
    private boolean holding(Player player) {
        return ConfigFiles.flowerFlag("enabled", true) && Items.inMainHand(player, CustomItem.FLOWER_SWORD);
    }
    private boolean targetable(Player source, Entity target) {
        if (!(target instanceof LivingEntity living) || living.isDead() || target.equals(source)) return false;
        if (target instanceof Player player) {
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR || player.getGameMode() == org.bukkit.GameMode.CREATIVE) return false;
            if (!ConfigFiles.flowerFlag("sanctuary.damage-trusted-players", false) && trust.trusts(source.getUniqueId(), player.getUniqueId())) return false;
        }
        return SafeZones.canAttack(source, target);
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onSneakLeftClick(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.isSneaking() && holding(player)) activate(player);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onFifthHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        if (!holding(player) || event.getFinalDamage() <= 0 || !targetable(player, target)) return;
        if (player.isSneaking()) activate(player);
        int every = Math.max(1, ConfigFiles.flowerInt("snare.every-hits", 5));
        int count = hitCounts.merge(player.getUniqueId(), 1, Integer::sum);
        if (count < every) return;
        hitCounts.put(player.getUniqueId(), 0);
        int duration = Math.max(1, ConfigFiles.flowerInt("snare.slowness-seconds", 3)) * 20;
        int amp = Math.max(0, ConfigFiles.flowerInt("snare.slowness-amplifier", 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amp, false, true, true));
        if (ConfigFiles.flowerFlag("snare.permanent-web", true)) {
            Block webBlock = target.getLocation().getBlock();
            if (webBlock.getType().isAir() && SafeZones.canModify(player, webBlock)) {
                webBlock.setType(Material.COBWEB, false);
            }
        }
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 16, .4, .5, .4);
        player.sendActionBar(Component.text("Nature's Snare!", NamedTextColor.GREEN));
    }

    private void activate(Player caster) {
        if (!holding(caster) || !SafeZones.guardAbility(caster)) return;
        long now = System.currentTimeMillis();
        long until = cooldownUntil.getOrDefault(caster.getUniqueId(), 0L);
        if (now < until) {
            long seconds = (until - now + 999L) / 1000L;
            caster.sendActionBar(Component.text("Flowering Sanctuary: " + seconds + "s cooldown", NamedTextColor.RED));
            return;
        }
        int duration = Math.max(1, ConfigFiles.flowerInt("sanctuary.duration-seconds", 10));
        int cooldown = Math.max(0, ConfigFiles.flowerInt("sanctuary.cooldown-seconds", 30));
        double radius = Math.max(1, ConfigFiles.flowerNumber("sanctuary.radius-blocks", 5));
        Location center = caster.getLocation().clone();
        cooldownUntil.put(caster.getUniqueId(), now + cooldown * 1000L);
        plantFlowers(caster, center, radius, duration);
        caster.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        caster.sendActionBar(Component.text("Flowering Sanctuary activated!", NamedTextColor.GREEN));
        BukkitRunnable field = new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ >= duration || !caster.isOnline()) { activeFields.remove(this); cancel(); return; }
                if (SafeZones.isProtected(center)) { activeFields.remove(this); cancel(); return; }
                renderField(center, radius);
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity.getLocation().distanceSquared(center) > radius * radius) continue;
                    if (entity == caster) continue;
                    if (!targetable(caster, entity)) continue;
                    LivingEntity enemy = (LivingEntity) entity;
                    int slowAmp = Math.max(0, ConfigFiles.flowerInt("sanctuary.slowness-amplifier", 1));
                    int slowSecs = Math.max(1, ConfigFiles.flowerInt("sanctuary.slowness-seconds", 2));
                    int blindnessSecs = Math.max(1, ConfigFiles.flowerInt("sanctuary.blindness-seconds", 10));
                    int blindnessAmp = Math.max(0, ConfigFiles.flowerInt("sanctuary.blindness-amplifier", 0));
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowSecs * 20, slowAmp));
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessSecs * 20, blindnessAmp));
                    double damage = Math.max(0, ConfigFiles.flowerNumber("sanctuary.damage-health-per-second", 1));
                    if (damage > 0) enemy.damage(damage, caster);
                }
                if (!SafeZones.isProtected(caster.getLocation()) && caster.getWorld().equals(center.getWorld())
                    && caster.getLocation().distanceSquared(center) <= radius * radius) {
                    int regenSecs = Math.max(1, ConfigFiles.flowerInt("sanctuary.regeneration-seconds", 3));
                    int regenAmp = Math.max(0, ConfigFiles.flowerInt("sanctuary.regeneration-amplifier", 1));
                    caster.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenSecs * 20, regenAmp));
                }
            }
        };
        activeFields.add(field);
        field.runTaskTimer(plugin, 0, 20);
    }

    private void renderField(Location center, double radius) {
        int particles = Math.min(500, Math.max(0, ConfigFiles.flowerInt("sanctuary.particles-per-second", 170)));
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(111, 218, 150), 1.1f);
        Particle.DustOptions pink = new Particle.DustOptions(Color.fromRGB(239, 158, 196), 1.1f);
        for (int i = 0; i < particles; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double d = Math.sqrt(rng.nextDouble()) * radius;
            Location p = center.clone().add(Math.cos(a) * d, .25 + rng.nextDouble() * 1.7, Math.sin(a) * d);
            if (!SafeZones.isProtected(p)) center.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, i % 2 == 0 ? green : pink);
        }
    }

    private void plantFlowers(Player caster, Location center, double radius, int duration) {
        if (!ConfigFiles.flowerFlag("sanctuary.remove-flowers-afterward", true)) return;
        List<String> choices = ConfigFiles.strings("flower-sword.yml", "flower-sword.sanctuary.flowers", List.of("POPPY", "DANDELION", "RED_TULIP", "WHITE_TULIP"));
        int max = Math.min(200, Math.max(0, ConfigFiles.flowerInt("sanctuary.maximum-flower-blocks", 80)));
        int attempts = Math.min(1000, Math.max(max, ConfigFiles.flowerInt("sanctuary.flower-placement-attempts", 240)));
        Map<Block, Material> planted = new HashMap<>();
        for (int a = 0; a < attempts && planted.size() < max && !choices.isEmpty(); a++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(rng.nextDouble()) * radius;
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            for (int y = center.getBlockY() + 2; y >= center.getBlockY() - 3; y--) {
                Block block = center.getWorld().getBlockAt(x, y, z);
                Material ground = block.getRelative(org.bukkit.block.BlockFace.DOWN).getType();
                if (!block.getType().isAir() || !switch (ground) {
                    case GRASS_BLOCK, DIRT, COARSE_DIRT, PODZOL, MOSS_BLOCK, ROOTED_DIRT -> true;
                    default -> false;
                }) continue;
                if (!SafeZones.canModify(caster, block) || liveFlowers.containsKey(block)) continue;
                Material flower = Material.matchMaterial(choices.get(rng.nextInt(choices.size())));
                if (flower == null || !org.bukkit.Tag.FLOWERS.isTagged(flower)) continue;
                block.setType(flower, false);
                planted.put(block, flower);
                liveFlowers.put(block, flower);
                break;
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (var entry : planted.entrySet()) {
                Block block = entry.getKey();
                if (block.getType() == entry.getValue()) block.setType(Material.AIR, false);
                liveFlowers.remove(block);
            }
        }, duration * 20L);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (ConfigFiles.flowerFlag("prevent-mob-targeting", true) && event.getEntity() instanceof Monster
            && event.getTarget() instanceof Player player && holding(player) && !SafeZones.isProtected(player.getLocation())) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player) || !holding(player) || !ConfigFiles.flowerFlag("prevent-mob-damage", true)) return;
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Monster && !SafeZones.isProtected(player.getLocation())) event.setCancelled(true);
        } else if (damager instanceof Monster && !SafeZones.isProtected(player.getLocation())) event.setCancelled(true);
    }

    private boolean flower(ItemStack item) { return Items.identify0(item) == CustomItem.FLOWER_SWORD; }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEnchant(EnchantItemEvent event) {
        if (ConfigFiles.flowerFlag("enchantment-lock.enchanting-table", true) && flower(event.getItem())) event.setCancelled(true);
    }
    @EventHandler public void onAnvil(PrepareAnvilEvent event) {
        if (ConfigFiles.flowerFlag("enchantment-lock.anvil", true) && (flower(event.getInventory().getItem(0)) || flower(event.getInventory().getItem(1)))) event.setResult(null);
    }
    @EventHandler public void onSmithing(PrepareSmithingEvent event) {
        if (!ConfigFiles.flowerFlag("enchantment-lock.smithing-table", true)) return;
        for (ItemStack ingredient : event.getInventory().getContents()) {
            if (flower(ingredient)) { event.setResult(null); return; }
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onUpgradeClick(InventoryClickEvent event) {
        InventoryType type = event.getView().getTopInventory().getType();
        if (type != InventoryType.ANVIL && type != InventoryType.SMITHING) return;
        int result = type == InventoryType.ANVIL ? 2 : 3;
        if (event.getRawSlot() != result) return;
        if ((type == InventoryType.ANVIL && !ConfigFiles.flowerFlag("enchantment-lock.anvil", true))
            || (type == InventoryType.SMITHING && !ConfigFiles.flowerFlag("enchantment-lock.smithing-table", true))) return;
        for (ItemStack item : event.getView().getTopInventory().getContents()) {
            if (flower(item)) { event.setCancelled(true); return; }
        }
    }
    void shutdown() {
        for (BukkitRunnable active : new ArrayList<>(activeFields)) active.cancel();
        activeFields.clear();
        for (var entry : liveFlowers.entrySet()) {
            if (entry.getKey().getType() == entry.getValue()) entry.getKey().setType(Material.AIR, false);
        }
        liveFlowers.clear();
    }
}
