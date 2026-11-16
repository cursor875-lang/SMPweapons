package dev.smpweapons;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/** Ten natural flowers in shuffled order; animated opening and per-player next-flower spins. */
final class BloomHunt implements Listener {
    private static final List<Material> FLOWERS = List.of(
        Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.OXEYE_DAISY,
        Material.AZURE_BLUET, Material.ALLIUM, Material.RED_TULIP, Material.ORANGE_TULIP,
        Material.WHITE_TULIP, Material.PINK_TULIP);
    private static final String[] LABELS = {
        "Dandelion", "Poppy", "Blue Orchid", "Oxeye Daisy", "Azure Bluet", "Allium",
        "Red Tulip", "Orange Tulip", "White Tulip", "Pink Tulip"
    };
    private final SMPWeapons plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> stages = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, BukkitTask> rolling = new HashMap<>();
    private final Map<UUID, String> rollingLabels = new HashMap<>();
    private final Set<String> placed = new HashSet<>();
    private final List<Material> sequence = new ArrayList<>();
    private BukkitTask countdownTask;
    private BukkitTask openingTask;
    private BukkitTask displayTask;
    private int countdown;
    private int round;
    private String openingLabel = "";
    private boolean countingDown;
    private boolean opening;
    private boolean live;

    BloomHunt(SMPWeapons plugin) { this.plugin = plugin; }
    boolean running() { return countingDown || opening || live; }
    void trackArtificial(Block block) { placed.add(key(block)); }
    void untrackArtificial(Block block) { placed.remove(key(block)); }

    void start(CommandSender sender) {
        if (!ConfigFiles.bool("events.yml", "events.enabled", true)
            || !ConfigFiles.bool("events.yml", "events.bloomhunt.enabled", true)) {
            Ui.error(sender, "Bloom Hunt is disabled in events.yml.");
            return;
        }
        if (running()) { Ui.error(sender, "Bloom Hunt is already running."); return; }
        clear();
        sequence.addAll(FLOWERS);
        Collections.shuffle(sequence);
        countingDown = true;
        countdown = Math.max(0, ConfigFiles.integer("events.yml", "events.bloomhunt.countdown-seconds", 10));
        int currentRound = round;
        Bukkit.broadcast(Ui.line(Component.text(Ui.small("Bloom Hunt  »  First to collect all 10 natural flowers wins."), NamedTextColor.LIGHT_PURPLE)));
        // Action bars update much more frequently than the countdown, like the Skript version.
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> display(currentRound), 0L, 3L);
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentRound != round || !countingDown) return;
            if (countdown <= 0) {
                countdownTask.cancel();
                countdownTask = null;
                openSpin(currentRound);
                return;
            }
            if (countdown <= 3 || countdown % 5 == 0) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.1f);
                }
            }
            countdown--;
        }, 0L, 20L);
        Ui.success(sender, "Bloom Hunt countdown started.");
    }

    private void openSpin(int currentRound) {
        if (currentRound != round) return;
        countingDown = false;
        opening = true;
        int steps = Math.max(1, Math.min(48, ConfigFiles.integer("events.yml", "events.bloomhunt.roll-steps", 16)));
        final int[] step = {0};
        openingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (round != currentRound || !opening) return;
            if (step[0]++ >= steps) {
                openingTask.cancel();
                openingTask = null;
                opening = false;
                live = true;
                Bukkit.broadcast(Ui.line(Component.text(Ui.small("Bloom Hunt started. Find your assigned flower!"), NamedTextColor.LIGHT_PURPLE)));
                for (Player player : Bukkit.getOnlinePlayers()) register(player, false);
                return;
            }
            openingLabel = pretty(FLOWERS.get(random.nextInt(FLOWERS.size())));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.35f,
                    0.85f + (step[0] / (float) steps) * 0.6f);
            }
        }, 0L, 4L);
    }

    private void register(Player player, boolean animate) {
        if (!live || !player.isOnline()) return;
        int stage = stages.computeIfAbsent(player.getUniqueId(), ignored -> 0);
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored ->
            Bukkit.createBossBar(Ui.small("Bloom Hunt"), BarColor.PINK, BarStyle.SEGMENTED_10));
        bar.addPlayer(player);
        updateBar(player);
        if (animate) spinFor(player, stage);
        else reveal(player, stage);
    }

    private void spinFor(Player player, int stage) {
        UUID id = player.getUniqueId();
        BukkitTask previous = rolling.remove(id);
        if (previous != null) previous.cancel();
        rollingLabels.put(id, "...");
        int currentRound = round;
        int steps = Math.max(1, Math.min(48, ConfigFiles.integer("events.yml", "events.bloomhunt.roll-steps", 16)));
        final int[] step = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!live || round != currentRound || !player.isOnline()) {
                rolling.remove(id);
                rollingLabels.remove(id);
                task[0].cancel();
                return;
            }
            if (step[0]++ >= steps) {
                rolling.remove(id);
                rollingLabels.remove(id);
                task[0].cancel();
                reveal(player, stage);
                return;
            }
            rollingLabels.put(id, pretty(FLOWERS.get(random.nextInt(FLOWERS.size()))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f,
                0.8f + (step[0] / (float) steps) * 0.65f);
            if (step[0] % 3 == 0) player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1.25, 0), 4, 0.55, 0.35, 0.55, 0.01);
        }, 0L, 4L);
        rolling.put(id, task[0]);
    }

    private void reveal(Player player, int stage) {
        if (!live || !player.isOnline() || stage >= sequence.size()) return;
        String name = pretty(sequence.get(stage));
        Ui.send(player, "Your flower: " + name + "  »  " + (stage + 1) + "/10");
        player.showTitle(Title.title(
            Component.text(Ui.small(name), NamedTextColor.LIGHT_PURPLE),
            Component.text(Ui.small("Find this flower! " + (stage + 1) + "/10"), NamedTextColor.WHITE),
            Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1100), Duration.ofMillis(300))));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.2f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1, 0), 18, .65, .55, .65, .01);
    }

    private void display(int currentRound) {
        if (currentRound != round || !running()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            String message;
            if (countingDown) message = "Bloom Hunt  »  Starting in " + (countdown + 1) + "s";
            else if (opening) message = "Bloom Hunt  »  Rolling  »  " + openingLabel;
            else {
                UUID id = player.getUniqueId();
                if (rollingLabels.containsKey(id)) message = "Bloom Hunt  »  Rolling  »  " + rollingLabels.get(id);
                else {
                    int stage = stages.getOrDefault(id, 0);
                    message = stage < sequence.size()
                        ? "Bloom Hunt  »  Find " + pretty(sequence.get(stage)) + "  (" + (stage + 1) + "/10)"
                        : "Bloom Hunt  »  Finished";
                }
            }
            player.sendActionBar(Component.text(Ui.small(message), NamedTextColor.LIGHT_PURPLE));
            if (live) updateBar(player);
        }
    }

    private void updateBar(Player player) {
        BossBar bar = bars.get(player.getUniqueId());
        if (bar == null) return;
        int done = stages.getOrDefault(player.getUniqueId(), 0);
        int best = 0;
        String leader = null;
        for (Map.Entry<UUID, Integer> entry : stages.entrySet()) {
            Player candidate = Bukkit.getPlayer(entry.getKey());
            if (candidate != null && entry.getValue() > best) {
                best = entry.getValue(); leader = candidate.getName();
            }
        }
        String lead = best > 0 && leader != null ? leader + " is in the lead!" : "No leader yet";
        bar.setTitle(Ui.small(lead + "  »  You are " + (done * 10) + "% complete"));
        bar.setProgress(Math.max(0, Math.min(1, done / 10.0)));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event) {
        if (FLOWERS.contains(event.getBlockPlaced().getType())) placed.add(key(event.getBlockPlaced()));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onFertilize(BlockFertilizeEvent event) {
        for (BlockState state : event.getBlocks()) {
            if (FLOWERS.contains(state.getType())) placed.add(key(state.getBlock()));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        String location = key(event.getBlock());
        boolean wasPlaced = placed.remove(location);
        if (!live || SafeZones.isProtected(event.getBlock().getLocation())) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (rolling.containsKey(id)) return;
        int stage = stages.getOrDefault(id, -1);
        if (stage < 0 || stage >= sequence.size() || event.getBlock().getType() != sequence.get(stage)) return;
        if (wasPlaced) { Ui.error(player, "Placed flowers don't count. Find a natural flower."); return; }
        int completed = stage + 1;
        stages.put(id, completed);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.85f, 1.45f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1.15, 0), 30, .7, .7, .7, .03);
        Ui.success(player, "Found! " + completed + "/10 flowers.");
        if (completed == sequence.size()) {
            Bukkit.broadcast(Ui.line(Component.text(
                Ui.small(player.getName() + " won Bloom Hunt with all 10 flowers!"), NamedTextColor.GREEN)));
            String reward = ConfigFiles.string("events.yml", "events.bloomhunt.reward-item", "");
            CustomItem item = CustomItem.byId(reward);
            if (item != null && ConfigFiles.weaponEnabled(item.id)) {
                for (ItemStack stack : player.getInventory().addItem(Items.create(item)).values())
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
            }
            clear();
        } else spinFor(player, completed);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (live) Bukkit.getScheduler().runTaskLater(plugin, () -> register(event.getPlayer(), true), 2L);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) bar.removeAll();
        BukkitTask task = rolling.remove(id);
        if (task != null) task.cancel();
        rollingLabels.remove(id);
    }

    void stop(CommandSender sender) {
        if (!running()) { Ui.send(sender, "No Bloom Hunt is running."); return; }
        clear();
        Bukkit.broadcast(Ui.line(Component.text(Ui.small("Bloom Hunt has been stopped."), NamedTextColor.RED)));
    }
    void shutdown() { clear(); }

    private void clear() {
        round++;
        countingDown = false;
        opening = false;
        live = false;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (openingTask != null) { openingTask.cancel(); openingTask = null; }
        if (displayTask != null) { displayTask.cancel(); displayTask = null; }
        for (BukkitTask task : rolling.values()) task.cancel();
        rolling.clear();
        rollingLabels.clear();
        for (BossBar bar : bars.values()) bar.removeAll();
        bars.clear();
        stages.clear();
        sequence.clear();
        // Keep placed block coordinates across rounds, so planted flowers never count.
    }
    private static String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
    private static String pretty(Material material) {
        int i = FLOWERS.indexOf(material);
        return i >= 0 ? LABELS[i] : material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
