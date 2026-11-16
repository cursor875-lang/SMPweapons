package dev.smpweapons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

/** Only /swa is exposed; Bukkit namespace fallbacks are hidden from suggestions. */
public final class Commands implements TabExecutor, Listener {
    private final TrustManager trust;
    private final Abilities abilities;
    private final StatusManager status;
    private final JuggernautManager juggernaut;
    private final WeaponEvents weaponEvents;
    private final BloomHunt bloomHunt;
    private final SafeZones safeZones;
    private static final Map<String, Object[]> ABILITIES = new LinkedHashMap<>();

    public Commands(TrustManager trust, Abilities abilities, StatusManager status,
                    JuggernautManager juggernaut, WeaponEvents weaponEvents,
                    BloomHunt bloomHunt, SafeZones safeZones) {
        this.trust = trust;
        this.abilities = abilities;
        this.status = status;
        this.juggernaut = juggernaut;
        this.weaponEvents = weaponEvents;
        this.bloomHunt = bloomHunt;
        this.safeZones = safeZones;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpweapons.admin")) {
            Ui.error(sender, "You don't have permission to use this command.");
            return true;
        }
        admin(sender, args);
        return true;
    }

    private void admin(CommandSender sender, String[] args) {
        if (args.length == 0) { help(sender); return; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "list" -> Ui.ids(sender, "Available items:", Arrays.stream(CustomItem.values())
                .map(item -> item.id).toList());
            case "give" -> changeItems(sender, args, true);
            case "remove" -> changeItems(sender, args, false);
            case "levelup" -> levelUp(sender, args);
            case "ability" -> ability(sender, Arrays.copyOfRange(args, 1, args.length));
            case "reload" -> {
                if (args.length > 2 || args.length == 2 && !args[1].equalsIgnoreCase("config")) {
                    Ui.format(sender, "/swa reload", "/swa reload config"); return;
                }
                ConfigFiles.reload();
                Ui.success(sender, "Configuration reloaded. Existing items keep their stored data.");
            }
            case "regioncheck" -> {
                Ui.send(sender, "WorldGuard protection: all named regions.");
                if (sender instanceof Player player) {
                    Ui.send(sender, "Your location: " + (SafeZones.isProtected(player.getLocation())
                        ? "inside a protected region" : "outside protected regions"));
                }
                Ui.send(sender, "Innerspawn and all other named regions are covered automatically.");
            }
            case "event" -> event(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> help(sender);
        }
    }

    private void help(CommandSender sender) {
        Ui.send(sender, "Admin commands");
        Ui.format(sender,
            "/swa give <player> <item>",
            "/swa remove <player> <item>",
            "/swa list", "/swa levelup <player> <weapon> <kills>",
            "/swa reload config", "/swa event list", "/swa regioncheck",
            "/swa ability <ability>");
        Ui.send(sender, "Items: any item ID, or All. Level-up weapons: Reaper, Katana.");
    }

    private void changeItems(CommandSender sender, String[] args, boolean give) {
        if (args.length != 3) {
            Ui.format(sender, "/swa " + (give ? "give" : "remove") + " <player> <item>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { Ui.error(sender, "That player is not online."); return; }
        CustomItem selected = args[2].equalsIgnoreCase("all") ? null : CustomItem.byId(args[2]);
        if (!args[2].equalsIgnoreCase("all") && selected == null) {
            Ui.error(sender, "Unknown item.");
            Ui.format(sender, "/swa list");
            return;
        }
        int changed = 0;
        if (give) {
            for (CustomItem item : CustomItem.values()) {
                if (selected != null && selected != item) continue;
                if (!ConfigFiles.weaponEnabled(item.id)) continue;
                for (ItemStack leftover : target.getInventory().addItem(Items.create(item)).values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                }
                changed++;
            }
        } else {
            ItemStack[] inventory = target.getInventory().getContents();
            for (int slot = 0; slot < inventory.length; slot++) {
                CustomItem id = Items.identify0(inventory[slot]);
                if (id != null && (selected == null || id == selected)) {
                    target.getInventory().setItem(slot, null);
                    changed++;
                }
            }
        }
        Ui.success(sender, (give ? "Gave " : "Removed ") + changed + " item stack(s) "
            + (give ? "to " : "from ") + target.getName() + ".");
        if (!target.equals(sender)) Ui.send(target, "An admin "
            + (give ? "gave you " : "removed ") + changed + " custom item stack(s).");
    }

    private void levelUp(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 4) {
            Ui.format(sender, "/swa levelup <player> <weapon> <kills>");
            Ui.send(sender, "Weapons: Reaper, Katana. Kills is optional."); return;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) { Ui.error(sender, "That player is not online."); return; }
        String weapon = "reaper";
        String killsText = null;
        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("reaper") || args[2].equalsIgnoreCase("katana")) {
                weapon = args[2].toLowerCase(Locale.ROOT);
                if (args.length == 4) killsText = args[3];
            } else if (args.length == 3) killsText = args[2];
            else { Ui.format(sender, "/swa levelup <player> <weapon> <kills>"); Ui.send(sender, "Weapons: Reaper, Katana."); return; }
        }
        boolean katana = weapon.equals("katana");
        int kills = katana ? 3 : 5;
        if (killsText != null) {
            try { kills = Math.max(0, Integer.parseInt(killsText)); }
            catch (NumberFormatException ex) { Ui.error(sender, "Kills must be a number."); return; }
        }
        (katana ? ReaperKills.KATANA : ReaperKills.REAPER).set(player.getUniqueId(), kills);
        Items.refreshKillLore(player);
        Ui.success(sender, "Set " + player.getName() + "'s " + (katana ? "katana" : "reaper") + " kills to " + kills + ".");
    }

    private void event(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Ui.send(sender, "Events: Bloom Hunt, Juggernaut, and themed weapon events.");
            Ui.ids(sender, "Weapon events:", weaponEvents.eventIds());
            Ui.format(sender, "/swa event start bloomhunt", "/swa event start juggernaut <player>",
                "/swa event start <event> <player>", "/swa event stop <event>");
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("stop")) {
            String which = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "all";
            if (!List.of("all", "bloomhunt", "bloom", "juggernaut", "weapon", "weapons").contains(which)) {
                Ui.format(sender, "/swa event stop <event>");
                Ui.send(sender, "Events: Bloom Hunt, Juggernaut, Weapon, All.");
                return;
            }
            boolean stopped = false;
            if ((which.equals("all") || which.equals("bloomhunt") || which.equals("bloom")) && bloomHunt.running()) {
                bloomHunt.stop(sender); stopped = true;
            }
            if ((which.equals("all") || which.equals("juggernaut")) && juggernaut.eventRunning()) {
                juggernaut.stopAllEvents(sender); stopped = true;
            }
            if ((which.equals("all") || which.equals("weapon") || which.equals("weapons")) && weaponEvents.eventRunning()) {
                weaponEvents.handleAdminEvent(sender, new String[] {"stop"}); stopped = true;
            }
            if (!stopped) Ui.send(sender, "No matching event is running.");
            return;
        }
        int offset = action.equals("start") ? 1 : 0;
        if (args.length <= offset) { Ui.format(sender, "/swa event start <event> <player>"); return; }
        String event = args[offset].toLowerCase(Locale.ROOT);
        if (event.equals("bloomhunt") || event.equals("bloom") && args.length > offset + 1 && args[offset + 1].equalsIgnoreCase("hunt")) {
            if (anyEventRunning()) { Ui.error(sender, "Stop the current event before starting another."); return; }
            bloomHunt.start(sender); return;
        }
        if (event.equals("juggernaut")) {
            if (args.length != offset + 2) { Ui.format(sender, "/swa event start juggernaut <player>"); return; }
            if (anyEventRunning()) { Ui.error(sender, "Stop the current event before starting another."); return; }
            juggernaut.command(sender, new String[] {"start", args[offset + 1]}); return;
        }
        if (anyEventRunning()) { Ui.error(sender, "Stop the current event before starting another."); return; }
        String[] forwarded = new String[args.length - offset + 1];
        forwarded[0] = "start";
        System.arraycopy(args, offset, forwarded, 1, args.length - offset);
        weaponEvents.handleAdminEvent(sender, forwarded);
    }

    private boolean anyEventRunning() {
        return bloomHunt.running() || juggernaut.eventRunning() || weaponEvents.eventRunning();
    }

    private void ability(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { Ui.error(sender, "Only players can use abilities."); return; }
        Object[] info = args.length != 1 ? null : ABILITIES.get(args[0].toLowerCase(Locale.ROOT));
        if (info == null) {
            Ui.format(sender, "/swa ability <ability>"); return;
        }
        CustomItem item = (CustomItem) info[0];
        boolean wearable = item == CustomItem.PLATE_OF_LIFE || item == CustomItem.OCEANIC_CROWN;
        boolean hasItem = wearable ? Items.wearing(player, item)
            : item == CustomItem.HORN_OF_HELLSCORCH ? Items.carrying(player, item) : Items.holding(player, item);
        if (!hasItem) { Ui.error(sender, "You must " + (wearable ? "wear" : "hold") + " that item first."); return; }
        if (status.has(player, StatusManager.Status.STUN)) { Ui.error(sender, "You can't use abilities while stunned."); return; }
        if (info[1] == null) abilities.sculkHook(player);
        else abilities.trigger(player, item, (Boolean)info[1]);
    }

    @EventHandler
    public void hideNamespacedCommands(PlayerCommandSendEvent event) {
        event.getCommands().removeIf(name -> name.toLowerCase(Locale.ROOT).startsWith("smpweapons:"));
    }

    @EventHandler(ignoreCancelled=true)
    public void blockNamespacedCommands(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase(Locale.ROOT);
        if (message.startsWith("/smpweapons:") || message.startsWith("/smpweapon:")) {
            event.setCancelled(true);
            Ui.format(event.getPlayer(), "/swa help");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("smpweapons.admin")) return List.of();
        if (args.length == 1) return match(args, List.of("help", "give", "remove", "levelup", "list", "reload", "event", "regioncheck", "ability"));
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("levelup"))) return match(args, playerNames());
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
            List<String> items = new ArrayList<>(List.of("all"));
            for (CustomItem item : CustomItem.values()) items.add(item.id);
            return match(args, items);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("levelup")) return match(args, List.of("reaper", "katana"));
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) return match(args, List.of("config"));
        if (args.length == 2 && args[0].equalsIgnoreCase("ability")) return match(args, new ArrayList<>(ABILITIES.keySet()));
        if (args[0].equalsIgnoreCase("event")) {
            if (args.length == 2) return match(args, List.of("list", "start", "stop", "bloomhunt", "juggernaut"));
            if (args.length == 3 && args[1].equalsIgnoreCase("start")) {
                List<String> events = new ArrayList<>(List.of("bloomhunt", "juggernaut"));
                for (CustomItem item : CustomItem.values()) events.add(item.id);
                return match(args, events);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("stop")) return match(args, List.of("bloomhunt", "juggernaut", "weapon"));
            if (args.length == 4 && args[2].equalsIgnoreCase("juggernaut")) return match(args, playerNames());
            if (args.length == 4 && args[1].equalsIgnoreCase("start")) return match(args, playerNames());
        }
        return List.of();
    }

    private static List<String> playerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
    private static List<String> match(String[] args, List<String> options) {
        String term = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(term)).toList();
    }

    static {
        ABILITIES.put("behemothbash", new Object[]{CustomItem.DAUNTLESS_SHIELD, false});
        ABILITIES.put("parry", new Object[]{CustomItem.DAUNTLESS_SHIELD, true});
        ABILITIES.put("windrush", new Object[]{CustomItem.WINDSTRIKER, false});
        ABILITIES.put("hellring", new Object[]{CustomItem.HORN_OF_HELLSCORCH, false});
        ABILITIES.put("abyssalvoid", new Object[]{CustomItem.ABYSSAL_BLADE, false});
        ABILITIES.put("cosmicblessing", new Object[]{CustomItem.ABYSSAL_BLADE, true});
        ABILITIES.put("divinesmite", new Object[]{CustomItem.STORMCHARGER, false});
        ABILITIES.put("natureswrath", new Object[]{CustomItem.PLATE_OF_LIFE, false});
        ABILITIES.put("silentbinding", new Object[]{CustomItem.SILENCED_SHIELD, false});
        ABILITIES.put("shadowstalker", new Object[]{CustomItem.SILENCED_SHIELD, true});
        ABILITIES.put("tidalwave", new Object[]{CustomItem.OCEANIC_CROWN, false});
        ABILITIES.put("riptide", new Object[]{CustomItem.OCEANIC_CROWN, true});
        ABILITIES.put("sonicbeam", new Object[]{CustomItem.SCULK_REAPER, false});
        ABILITIES.put("echoshell", new Object[]{CustomItem.SCULK_REAPER, true});
        ABILITIES.put("sculkhook", new Object[]{CustomItem.SCULK_REAPER, null});
        ABILITIES.put("killingintent", new Object[]{CustomItem.BLOODMOON_KATANA, false});
        ABILITIES.put("crescentslash", new Object[]{CustomItem.BLOODMOON_KATANA, true});
        ABILITIES.put("dragonsbreath", new Object[]{CustomItem.DRAGON_EGG, false});
        ABILITIES.put("dragonsawakening", new Object[]{CustomItem.DRAGON_EGG, true});
        ABILITIES.put("glacialbreak", new Object[]{CustomItem.FROSTBITE_AXE, false});
        ABILITIES.put("riftpull", new Object[]{CustomItem.VOID_REAVER, false});
    }
}
