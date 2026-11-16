package dev.smpweapons;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** Defensive guards for the protected WorldGuard spawn2 region. */
public final class SafeZones implements Listener {
    private static SafeZones instance;
    private final SMPWeapons plugin;
    private final Map<UUID, Long> abilityMotion = new HashMap<>();

    SafeZones(SMPWeapons plugin) {
        this.plugin = plugin;
        instance = this;
    }
    static boolean enabled() {
        return ConfigFiles.bool("protection.yml", "protection.enabled", true);
    }
    static boolean isProtected(Location location) {
        if (!enabled() || location == null || location.getWorld() == null) return false;
        String regionName = ConfigFiles.string("protection.yml", "protection.worldguard-region", "spawn2");
        try {
            RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
            if (manager == null) return ConfigFiles.bool("protection.yml", "protection.fail-closed-if-worldguard-unavailable", true);
            ProtectedRegion region = manager.getRegion(regionName);
            if (region == null) return false;
            return region.contains(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        } catch (RuntimeException ex) {
            if (instance != null) instance.plugin.getLogger().warning("WorldGuard lookup failed: " + ex.getMessage());
            return ConfigFiles.bool("protection.yml", "protection.fail-closed-if-worldguard-unavailable", true);
        }
    }
    static boolean canAttack(Player caster, Entity target) {
        return caster != null && target != null && !isProtected(caster.getLocation()) && !isProtected(target.getLocation());
    }
    static boolean canModify(Player owner, Block block) {
        if (owner == null || block == null) return false;
        if (ConfigFiles.bool("protection.yml", "protection.block-custom-block-changes", true) && (isProtected(owner.getLocation()) || isProtected(block.getLocation()))) return false;
        if (!ConfigFiles.bool("protection.yml", "protection.respect-all-worldguard-build-flags", true)) return true;
        try {
            var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            var bukkitLocation = BukkitAdapter.adapt(block.getLocation());
            var localPlayer = WorldGuardPlugin.inst().wrapPlayer(owner);
            return query.testState(bukkitLocation, localPlayer, Flags.BUILD);
        } catch (RuntimeException ex) { return false; }
    }
    static boolean guardAbility(Player caster) {
        if (ConfigFiles.bool("protection.yml", "protection.block-abilities-starting-inside", true) && isProtected(caster.getLocation())) {
            caster.sendMessage("§cAbilities are disabled inside the protected spawn region.");
            return false;
        }
        return true;
    }
    static void trackAbilityMotion(Entity entity, long milliseconds) {
        if (instance != null && entity instanceof Player player) {
            instance.abilityMotion.put(player.getUniqueId(), System.currentTimeMillis() + milliseconds);
        }
    }
    private boolean movingByAbility(Player player) {
        Long until = abilityMotion.get(player.getUniqueId());
        if (until == null) return false;
        if (until < System.currentTimeMillis()) { abilityMotion.remove(player.getUniqueId()); return false; }
        return true;
    }
    boolean existsInAnyLoadedWorld() {
        if (!enabled()) return true;
        String name = ConfigFiles.string("protection.yml", "protection.worldguard-region", "spawn2");
        for (World world : Bukkit.getWorlds()) {
            try {
                var manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (manager != null && manager.hasRegion(name)) return true;
            } catch (RuntimeException ignored) { }
        }
        return false;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        if (ConfigFiles.bool("protection.yml", "protection.cancel-damage-inside", true) && isProtected(event.getEntity().getLocation())) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEffect(EntityPotionEffectEvent event) {
        if (!ConfigFiles.bool("protection.yml", "protection.cancel-harmful-potions-inside", true) || !isProtected(event.getEntity().getLocation()) || event.getNewEffect() == null) return;
        PotionEffectType type = event.getNewEffect().getType();
        if (type.equals(PotionEffectType.BLINDNESS) || type.equals(PotionEffectType.SLOWNESS) || type.equals(PotionEffectType.POISON)
           || type.equals(PotionEffectType.WITHER) || type.equals(PotionEffectType.DARKNESS) || type.equals(PotionEffectType.WEAKNESS)
           || type.equals(PotionEffectType.HUNGER) || type.equals(PotionEffectType.NAUSEA)) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (!event.hasChangedBlock() && !movingByAbility(event.getPlayer())) return;
        if (!isProtected(event.getFrom()) && isProtected(event.getTo())) {
            if (plugin.statusManager() != null) plugin.statusManager().clear(event.getPlayer());
            if (ConfigFiles.bool("protection.yml", "protection.remove-negative-effects-on-entry", true)) {
                for (PotionEffectType type : new PotionEffectType[] {PotionEffectType.SLOWNESS, PotionEffectType.BLINDNESS,
                        PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.DARKNESS,
                        PotionEffectType.WEAKNESS, PotionEffectType.HUNGER, PotionEffectType.NAUSEA}) {
                    event.getPlayer().removePotionEffect(type);
                }
            }
        }
        if (!movingByAbility(event.getPlayer())) return;
        if (!isProtected(event.getFrom()) && isProtected(event.getTo()) && ConfigFiles.bool("protection.yml", "protection.block-ability-push-into-region", true)) {
            event.setTo(event.getFrom());
            event.getPlayer().setVelocity(new Vector());
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onVelocity(PlayerVelocityEvent event) {
        if (ConfigFiles.bool("protection.yml", "protection.block-ability-push-into-region", true)
            && isProtected(event.getPlayer().getLocation())) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null && !isProtected(event.getFrom()) && isProtected(event.getTo())
            && movingByAbility(event.getPlayer())
            && ConfigFiles.bool("protection.yml", "protection.block-ability-teleport-into-region", true)) event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!ConfigFiles.bool("protection.yml", "protection.cancel-explosions-in-region", true)) return;
        if (isProtected(event.getLocation())) { event.setCancelled(true); return; }
        event.blockList().removeIf(block -> isProtected(block.getLocation()));
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (!ConfigFiles.bool("protection.yml", "protection.cancel-explosions-in-region", true)) return;
        if (isProtected(event.getBlock().getLocation())) { event.setCancelled(true); return; }
        event.blockList().removeIf(block -> isProtected(block.getLocation()));
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHit(ProjectileHitEvent event) {
        if (!ConfigFiles.bool("protection.yml", "protection.guard-projectile-hits", true)) return;
        if (isProtected(event.getEntity().getLocation()) || event.getHitEntity() != null && isProtected(event.getHitEntity().getLocation())
            || event.getHitBlock() != null && isProtected(event.getHitBlock().getLocation())) event.getEntity().remove();
    }
    @EventHandler(ignoreCancelled=true)
    public void onCustomProjectile(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player player) || Items.identify(player.getInventory().getItemInMainHand()) == null) return;
        if (isProtected(projectile.getLocation())) { event.setCancelled(true); return; }
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (!projectile.isValid() || ticks++ >= 200) { cancel(); return; }
                if (isProtected(projectile.getLocation())) { projectile.remove(); cancel(); }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
