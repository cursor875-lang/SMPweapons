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
        Location snare = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, snare, 30, .55, .55, .55, .02);
        target.getWorld().spawnParticle(Particle.DUST, snare, 25, .45, .5, .45, 0,
            new Particle.DustOptions(Color.fromRGB(108, 233, 136), 1.1f));
        player.playSound(snare, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
        player.sendActionBar(Ui.line(Component.text(Ui.small("Nature's Snare!"), NamedTextColor.GREEN)));
    }

    private void activate(Player caster) {
        if (!holding(caster) || !SafeZones.guardAbility(caster)) return;
        long now = System.currentTimeMillis();
        long until = cooldownUntil.getOrDefault(caster.getUniqueId(), 0L);
        if (now < until) {
            long seconds = (until - now + 999L) / 1000L;
            caster.sendActionBar(Ui.line(Component.text(Ui.small("Flowering Sanctuary: " + seconds + "s cooldown"), NamedTextColor.RED)));
            return;
        }
        int duration = Math.max(1, ConfigFiles.flowerInt("sanctuary.duration-seconds", 10));
        int cooldown = Math.max(0, ConfigFiles.flowerInt("sanctuary.cooldown-seconds", 30));
        double radius = Math.max(1, ConfigFiles.flowerNumber("sanctuary.radius-blocks", 5));
        Location center = caster.getLocation().clone();
        cooldownUntil.put(caster.getUniqueId(), now + cooldown * 1000L);
        plantFlowers(caster, center, radius, duration);
        caster.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        caster.sendActionBar(Ui.line(Component.text(Ui.small("Flowering Sanctuary activated!"), NamedTextColor.GREEN)));
        BukkitRunnable field = new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ >= duration || !caster.isOnline()) { activeFields.remove(this); cancel(); return; }
                if (SafeZones.isProtected(center)) { activeFields.remove(this); cancel(); return; }
                renderField(center, radius, ticks);
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

    private void renderField(Location center, double radius, int seconds) {
        int particles = Math.min(340, Math.max(0, ConfigFiles.flowerInt("sanctuary.particles-per-second", 170)));
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(91, 221, 142), 1.0f);
        Particle.DustOptions pink = new Particle.DustOptions(Color.fromRGB(245, 156, 207), 1.0f);
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 220, 135), 1.0f);
        // A visible rotating boundary and two spiraling strands; particles cannot enter regions.
        for (int i = 0; i < 80; i++) {
            double angle = i * Math.PI * 2.0 / 80.0 + seconds * 0.19;
            double edge = radius - (i % 4 == 0 ? 0.1 : 0.0);
            Location point = center.clone().add(Math.cos(angle) * edge, 0.22 + Math.sin(angle * 3 + seconds) * 0.15, Math.sin(angle) * edge);
            if (!SafeZones.isProtected(point)) center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                i % 3 == 0 ? pink : green);
        }
        for (int i = 0; i < 30; i++) {
            double angle = i * Math.PI * 2.0 / 30.0 + seconds * 0.65;
            double distance = 0.7 + ((i % 7) / 7.0) * radius;
            Location point = center.clone().add(Math.cos(angle) * distance, 0.5 + (i % 6) * 0.22, Math.sin(angle) * distance);
            if (!SafeZones.isProtected(point)) center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, gold);
        }
        for (int i = 0; i < particles; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(rng.nextDouble()) * radius;
            Location point = center.clone().add(Math.cos(angle) * distance, .2 + rng.nextDouble() * 1.85, Math.sin(angle) * distance);
            if (!SafeZones.isProtected(point)) center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                i % 4 == 0 ? gold : i % 2 == 0 ? green : pink);
        }
    }

    private void plantFlowers(Player caster, Location center, double radius, int duration) {
        List<String> choices = ConfigFiles.strings("flower-sword.yml", "flower-sword.sanctuary.flowers", List.of("POPPY", "DANDELION", "RED_TULIP", "WHITE_TULIP"));
        int max = Math.min(220, Math.max(0, ConfigFiles.flowerInt("sanctuary.maximum-flower-blocks", 120)));
        int attempts = Math.min(3000, Math.max(max, ConfigFiles.flowerInt("sanctuary.flower-placement-attempts", 750)));
        // Remember what was underneath so short grass/ferns can be restored when the field expires.
        Map<Block, Material> planted = new HashMap<>();
        Map<Block, Material> replaced = new HashMap<>();
        for (int a = 0; a < attempts && planted.size() < max && !choices.isEmpty(); a++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(rng.nextDouble()) * radius;
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (Math.pow(x + 0.5 - center.getX(), 2) + Math.pow(z + 0.5 - center.getZ(), 2) > radius * radius) continue;
            // Find a planting surface on small hills and shallow dips as well as flat ground.
            for (int y = center.getBlockY() + 5; y >= center.getBlockY() - 6; y--) {
                Block block = center.getWorld().getBlockAt(x, y, z);
                Material original = block.getType();
                if (!original.isAir() && original != Material.SHORT_GRASS && original != Material.FERN) continue;
                Material ground = block.getRelative(org.bukkit.block.BlockFace.DOWN).getType();
                if (!org.bukkit.Tag.DIRT.isTagged(ground) && ground != Material.MOSS_BLOCK && ground != Material.MUD) continue;
                if (planted.containsKey(block) || liveFlowers.containsKey(block)) continue;
                if (!SafeZones.canModify(caster, block)) continue;
                Material flower = Material.matchMaterial(choices.get(rng.nextInt(choices.size())));
                if (flower == null || !org.bukkit.Tag.FLOWERS.isTagged(flower)) continue;
                block.setType(flower, false);
                planted.put(block, flower);
                replaced.put(block, original);
                liveFlowers.put(block, flower);
                if (plugin.bloomHunt() != null) plugin.bloomHunt().trackArtificial(block);
                break;
            }
        }
        // Leaving flowers in the world is a separate setting; it must never prevent planting.
        if (ConfigFiles.flowerFlag("sanctuary.remove-flowers-afterward", true)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (var entry : planted.entrySet()) {
                    Block block = entry.getKey();
                    // Do not overwrite blocks placed by a player while the sanctuary was active.
                    if (!SafeZones.isProtected(block.getLocation()) && block.getType() == entry.getValue()) {
                        block.setType(replaced.getOrDefault(block, Material.AIR), false);
                    }
                    liveFlowers.remove(block);
                    if (plugin.bloomHunt() != null) plugin.bloomHunt().untrackArtificial(block);
                }
            }, duration * 20L);
        } else {
            for (Block block : planted.keySet()) liveFlowers.remove(block);
        }
        if (planted.isEmpty()) {
            caster.sendActionBar(Ui.line(Component.text(Ui.small("The field is active. Flowers need open grass you can build on."), NamedTextColor.YELLOW)));
        }
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
            if (!SafeZones.isProtected(entry.getKey().getLocation()) && entry.getKey().getType() == entry.getValue()) entry.getKey().setType(Material.AIR, false);
        }
        liveFlowers.clear();
    }
}
