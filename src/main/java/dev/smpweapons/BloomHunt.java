package dev.smpweapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/** A natural-flower scavenger race; players must find and break flowers in order. */
final class BloomHunt implements Listener {
    private static final List<Material> FLOWERS = List.of(
        Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.OXEYE_DAISY,
        Material.AZURE_BLUET, Material.ALLIUM, Material.RED_TULIP, Material.ORANGE_TULIP,
        Material.WHITE_TULIP, Material.PINK_TULIP);
    private final SMPWeapons plugin;
    private final Map<UUID, Integer> stages = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Set<String> placed = new HashSet<>();
    private final List<Material> sequence = new ArrayList<>();
    private BukkitTask timer;
    private int secondsRemaining;
    private boolean countingDown;
    private boolean live;

    BloomHunt(SMPWeapons plugin) { this.plugin = plugin; }
    boolean running() { return countingDown || live; }
    void trackArtificial(Block block) { placed.add(key(block)); }
    void untrackArtificial(Block block) { placed.remove(key(block)); }

    void start(CommandSender sender) {
        if (!ConfigFiles.bool("events.yml", "events.enabled", true)
            || !ConfigFiles.bool("events.yml", "events.bloomhunt.enabled", true)) {
            Ui.error(sender, "Bloom Hunt is disabled in events.yml."); return;
        }
        if (running()) { Ui.error(sender, "Bloom Hunt is already running."); return; }
        clear();
        sequence.addAll(FLOWERS);
        Collections.shuffle(sequence);
        countingDown = true;
        secondsRemaining = Math.max(0, ConfigFiles.integer("events.yml", "events.bloomhunt.countdown-seconds", 10));
        Bukkit.broadcast(Ui.line(Component.text(Ui.small("Bloom Hunt starts in " + secondsRemaining
            + " seconds. First to find all 10 flowers wins!"), NamedTextColor.LIGHT_PURPLE)));
        timer = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!countingDown) return;
            if (secondsRemaining-- > 0) {
                if (secondsRemaining < 5 || secondsRemaining % 5 == 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendActionBar(Component.text(Ui.small("Bloom Hunt starting in " + (secondsRemaining + 1) + "s"), NamedTextColor.LIGHT_PURPLE));
                    }
                }
                return;
            }
            countingDown = false;
            live = true;
            for (Player player : Bukkit.getOnlinePlayers()) register(player);
            Bukkit.broadcast(Ui.line(Component.text(Ui.small("Bloom Hunt has started. Find natural flowers in order."), NamedTextColor.LIGHT_PURPLE)));
        }, 0L, 20L);
        Ui.success(sender, "Bloom Hunt countdown started.");
    }

    void stop(CommandSender sender) {
        if (!running()) { Ui.send(sender, "No Bloom Hunt is running."); return; }
        clear();
        Bukkit.broadcast(Ui.line(Component.text(Ui.small("Bloom Hunt has been stopped."), NamedTextColor.RED)));
    }

    private void register(Player player) {
        if (!live) return;
        int stage = stages.computeIfAbsent(player.getUniqueId(), ignored -> 0);
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored ->
            Bukkit.createBossBar(Ui.small("Bloom Hunt"), BarColor.PINK, BarStyle.SEGMENTED_10));
        bar.addPlayer(player);
        updateBar(player, stage);
        tellTarget(player);
    }

    private void updateBar(Player player, int count) {
        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) return;
        int best = stages.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String leader = "No leader yet";
        if (best > 0) {
            for (Map.Entry<UUID, Integer> entry : stages.entrySet()) {
                if (entry.getValue() == best) {
                    Player winner = Bukkit.getPlayer(entry.getKey());
                    if (winner != null) { leader = winner.getName() + " is ahead"; break; }
                }
            }
        }
        bar.setTitle(Ui.small(leader) + "  »  " + (count * 10) + "%");
        bar.setProgress(Math.min(1.0, count / 10.0));
    }

    private void tellTarget(Player player) {
        int count = stages.getOrDefault(player.getUniqueId(), 0);
        if (count >= sequence.size()) return;
        String name = sequence.get(count).name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        Ui.send(player, "Find " + name + " (" + (count + 1) + "/10)");
        player.sendActionBar(Component.text(Ui.small("Find " + name + "  »  " + (count + 1) + "/10"), NamedTextColor.LIGHT_PURPLE));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if (FLOWERS.contains(event.getBlockPlaced().getType())) placed.add(key(event.getBlockPlaced()));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        String location = key(event.getBlock());
        boolean wasPlaced = placed.remove(location);
        if (!live || SafeZones.isProtected(event.getBlock().getLocation())) return;
        Player player = event.getPlayer();
        int count = stages.getOrDefault(player.getUniqueId(), 0);
        if (count >= sequence.size() || event.getBlock().getType() != sequence.get(count)) return;
        if (wasPlaced) { Ui.error(player, "Placed flowers don't count. Find a natural one."); return; }
        count++;
        stages.put(player.getUniqueId(), count);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        Ui.success(player, "Found! " + count + "/10 flowers.");
        if (count >= sequence.size()) {
            Bukkit.broadcast(Ui.line(Component.text(Ui.small(player.getName() + " won Bloom Hunt by finding all 10 flowers!"), NamedTextColor.GREEN)));
            String reward = ConfigFiles.string("events.yml", "events.bloomhunt.reward-item", "");
            CustomItem item = CustomItem.byId(reward);
            if (item != null && ConfigFiles.weaponEnabled(item.id)) {
                for (ItemStack stack : player.getInventory().addItem(Items.create(item)).values())
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            clear();
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) updateBar(online, stages.getOrDefault(online.getUniqueId(), 0));
        tellTarget(player);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (live) Bukkit.getScheduler().runTaskLater(plugin, () -> register(event.getPlayer()), 2L);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        BossBar bar = bars.remove(event.getPlayer().getUniqueId());
        if (bar != null) bar.removeAll();
    }
    void shutdown() { clear(); }

    private void clear() {
        countingDown = false;
        live = false;
        if (timer != null) { timer.cancel(); timer = null; }
        for (BossBar bar : bars.values()) bar.removeAll();
        bars.clear();
        stages.clear();
        sequence.clear();
    }
    private static String key(Block b) {
        return b.getWorld().getUID() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }
}
