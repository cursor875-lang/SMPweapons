package dev.smpweapons;

import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Shared, restrained SMP-style chat format. Actual command names and IDs remain ASCII. */
final class Ui {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String[] SMALL = {
        "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ",
        "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "ꜱ", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"
    };

    private Ui() { }

    static String small(String text) {
        if (text == null) return "";
        StringBuilder output = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char lower = Character.toLowerCase(c);
            if (lower >= 'a' && lower <= 'z') output.append(SMALL[lower - 'a']);
            else output.append(c);
        }
        return output.toString();
    }

    /** Keep MiniMessage formatting intact; convert only visible text. */
    static String mini(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder(text.length() * 2);
        boolean inTag = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') inTag = true;
            if (inTag) result.append(c);
            else result.append(small(Character.toString(c)));
            if (c == '>') inTag = false;
        }
        return result.toString();
    }

    static Component prefix() {
        String format = ConfigFiles.string("messages.yml", "messages.prefix",
            "<dark_gray>[<light_purple>ꜱᴍᴘᴡᴇᴀᴘᴏɴꜱ<dark_gray>] <gray>» <white>");
        try { return MM.deserialize(format); }
        catch (IllegalArgumentException exception) {
            return Component.text("[SMPWeapons] » ", NamedTextColor.GRAY);
        }
    }

    static Component line(Component body) { return prefix().append(compact(body)); }
    static Component line(String body) {
        NamedTextColor color = body != null && body.startsWith("§c") ? NamedTextColor.RED
            : body != null && body.startsWith("§a") ? NamedTextColor.GREEN
            : body != null && body.startsWith("§e") ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
        return prefix().append(Component.text(small(stripLegacyColors(body)), color));
    }

    private static Component compact(Component component) {
        if (component instanceof TextComponent text) component = text.content(small(text.content()));
        if (!component.children().isEmpty()) {
            component = component.children(component.children().stream().map(Ui::compact).toList());
        }
        return component;
    }

    static void send(CommandSender sender, String message) {
        sender.sendMessage(prefix().append(Component.text(small(message), NamedTextColor.GRAY)));
    }

    static void ids(CommandSender sender, String label, Collection<String> identifiers) {
        sender.sendMessage(prefix().append(Component.text(small(label) + " ", NamedTextColor.GRAY))
            .append(Component.text(String.join(", ", identifiers), NamedTextColor.WHITE)));
    }

    static void success(CommandSender sender, String message) {
        sender.sendMessage(prefix().append(Component.text(small(message), NamedTextColor.GREEN)));
    }

    static void error(CommandSender sender, String message) {
        sender.sendMessage(prefix().append(Component.text(small(message), NamedTextColor.RED)));
    }

    static void format(CommandSender sender, String... commands) {
        sender.sendMessage(prefix().append(Component.text(small("The correct format is"), NamedTextColor.GRAY)));
        for (String command : commands) {
            sender.sendMessage(Component.text("  » ", NamedTextColor.DARK_GRAY)
                .append(Component.text(command, NamedTextColor.WHITE)));
        }
    }

    private static String stripLegacyColors(String input) {
        return input == null ? "" : input.replaceAll("(?i)§[0-9a-fk-orx]", "");
    }
}
