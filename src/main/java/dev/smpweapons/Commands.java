package dev.smpweapons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class Commands implements TabExecutor {
   private final TrustManager trust;
   private final Abilities abilities;
   private final StatusManager status;
   private final JuggernautManager juggernaut;
   private static final Map<String, Object[]> ABILITIES = new LinkedHashMap();

   public Commands(TrustManager var1, Abilities var2, StatusManager var3, JuggernautManager var4) {
      this.trust = var1;
      this.abilities = var2;
      this.status = var3;
      this.juggernaut = var4;
   }

   private void abilityCmd(CommandSender var1, String[] var2) {
      if (var1 instanceof Player var3) {
         Object[] var4 = var2.length < 1 ? null : (Object[])ABILITIES.get(var2[0].toLowerCase(Locale.ROOT));
         if (var4 == null) {
            var3.sendMessage(Component.text("Usage: /ability <" + String.join("|", ABILITIES.keySet()) + ">", NamedTextColor.RED));
         } else {
            CustomItem var5 = (CustomItem)var4[0];
            boolean var6 = var5 == CustomItem.PLATE_OF_LIFE || var5 == CustomItem.OCEANIC_CROWN;
            boolean var7 = var6 ? Items.wearing(var3, var5) : (var5 == CustomItem.HORN_OF_HELLSCORCH ? Items.carrying(var3, var5) : Items.holding(var3, var5));
            if (!var7) {
               var3.sendMessage(Component.text("You need to " + (var6 ? "wear" : "hold") + " the right item for that.", NamedTextColor.RED));
            } else if (!this.status.has(var3, StatusManager.Status.STUN)) {
               if (var4[1] == null) {
                  this.abilities.sculkHook(var3);
               } else {
                  this.abilities.trigger(var3, var5, (Boolean)var4[1]);
               }

            }
         }
      }
   }

   public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
      switch (var2.getName().toLowerCase(Locale.ROOT)) {
         case "smpweapons" -> this.admin(var1, var4);
         case "trust" -> this.trustCmd(var1, var4, true);
         case "untrust" -> this.trustCmd(var1, var4, false);
         case "trustlist" -> this.trustList(var1);
         case "ability" -> this.abilityCmd(var1, var4);
         case "juggernaut" -> this.juggernaut.command(var1, var4);
      }

      return true;
   }

   private void admin(CommandSender var1, String[] var2) {
      if (var2.length >= 1 && var2[0].equalsIgnoreCase("list")) {
         StringJoiner var8 = new StringJoiner(", ");

         for(CustomItem var7 : CustomItem.values()) {
            var8.add(var7.id);
         }

         var1.sendMessage(Component.text("Items: " + String.valueOf(var8), NamedTextColor.GOLD));
      } else if (var2.length >= 2 && var2[0].equalsIgnoreCase("levelup")) {
         this.levelUp(var1, var2);
      } else if (var2.length >= 3 && var2[0].equalsIgnoreCase("give")) {
         Player var3 = Bukkit.getPlayerExact(var2[1]);
         if (var3 == null) {
            var1.sendMessage(Component.text("Player not online.", NamedTextColor.RED));
         } else {
            ArrayList<CustomItem> var4 = new ArrayList<>();
            if (var2[2].equalsIgnoreCase("all")) {
               var4.addAll(List.of(CustomItem.values()));
            } else {
               CustomItem var5 = CustomItem.byId(var2[2]);
               if (var5 == null) {
                  var1.sendMessage(Component.text("Unknown item. Use /smpweapons list", NamedTextColor.RED));
                  return;
               }

               var4.add(var5);
            }

            for(CustomItem var6 : var4) {
               var3.getInventory().addItem(new ItemStack[]{Items.create(var6)}).values().forEach((var1x) -> var3.getWorld().dropItem(var3.getLocation(), var1x));
            }

            var1.sendMessage(Component.text("Gave " + var4.size() + " item(s) to " + var3.getName(), NamedTextColor.GREEN));
         }
      } else {
         var1.sendMessage(Component.text("Usage: /smpweapons give <player> <item|all>  |  /smpweapons levelup <player> [kills]  |  /smpweapons list", NamedTextColor.RED));
      }
   }

   private void levelUp(CommandSender var1, String[] var2) {
      Player var3 = Bukkit.getPlayerExact(var2[1]);
      if (var3 == null) {
         var1.sendMessage(Component.text("Player not online.", NamedTextColor.RED));
      } else {
         String var4 = "reaper";
         String var5 = null;
         if (var2.length >= 3) {
            if (!var2[2].equalsIgnoreCase("reaper") && !var2[2].equalsIgnoreCase("katana")) {
               var5 = var2[2];
            } else {
               var4 = var2[2].toLowerCase(Locale.ROOT);
               if (var2.length >= 4) {
                  var5 = var2[3];
               }
            }
         }

         boolean var6 = var4.equals("katana");
         int var7 = var6 ? 3 : 5;
         if (var5 != null) {
            try {
               var7 = Math.max(0, Integer.parseInt(var5));
            } catch (NumberFormatException var9) {
               var1.sendMessage(Component.text("Kills must be a number.", NamedTextColor.RED));
               return;
            }
         }

         (var6 ? ReaperKills.KATANA : ReaperKills.REAPER).set(var3.getUniqueId(), var7);
         Items.refreshKillLore(var3);
         String var8 = var6 ? "Bloodmoon Katana" : "Sculk Reaper";
         var1.sendMessage(Component.text("Set " + var3.getName() + "'s " + var8 + " kills to " + var7 + ".", NamedTextColor.GREEN));
         if (var1 != var3) {
            var3.sendMessage(Component.text("Your " + var8 + " kills were set to " + var7 + ".", NamedTextColor.DARK_AQUA));
         }

      }
   }

   private void trustCmd(CommandSender var1, String[] var2, boolean var3) {
      if (var1 instanceof Player var4) {
         if (var2.length < 1) {
            var4.sendMessage(Component.text("Usage: /" + (var3 ? "trust" : "untrust") + " <player>", NamedTextColor.RED));
         } else {
            Object var5 = Bukkit.getPlayerExact(var2[0]);
            if (var5 == null) {
               var5 = Bukkit.getOfflinePlayerIfCached(var2[0]);
            }

            if (var5 == null) {
               var4.sendMessage(Component.text("Unknown player.", NamedTextColor.RED));
            } else if (((OfflinePlayer)var5).getUniqueId().equals(var4.getUniqueId())) {
               var4.sendMessage(Component.text("You can't trust yourself.", NamedTextColor.RED));
            } else {
               boolean var6 = var3 ? this.trust.add(var4.getUniqueId(), ((OfflinePlayer)var5).getUniqueId()) : this.trust.remove(var4.getUniqueId(), ((OfflinePlayer)var5).getUniqueId());
               String var7 = ((OfflinePlayer)var5).getName() == null ? var2[0] : ((OfflinePlayer)var5).getName();
               if (!var6) {
                  var4.sendMessage(Component.text(var7 + (var3 ? " is already trusted." : " wasn't trusted."), NamedTextColor.YELLOW));
               } else {
                  var4.sendMessage(Component.text((var3 ? "Trusted " : "Untrusted ") + var7, NamedTextColor.GREEN));
               }

            }
         }
      } else {
         var1.sendMessage("Players only.");
      }
   }

   private void trustList(CommandSender var1) {
      if (var1 instanceof Player var2) {
         Set<UUID> var3 = this.trust.list(var2.getUniqueId());
         if (var3.isEmpty()) {
            var2.sendMessage(Component.text("You don't trust anyone yet. /trust <player>", NamedTextColor.GRAY));
         } else {
            StringJoiner var4 = new StringJoiner(", ");

            for(UUID var6 : var3) {
               String var7 = Bukkit.getOfflinePlayer(var6).getName();
               var4.add(var7 == null ? var6.toString() : var7);
            }

            var2.sendMessage(Component.text("Trusted: " + String.valueOf(var4), NamedTextColor.GREEN));
         }
      }
   }

   public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
      String var5 = var2.getName().toLowerCase(Locale.ROOT);
      ArrayList<String> var6 = new ArrayList<>();
      if (var5.equals("smpweapons")) {
         if (var4.length == 1) {
            var6.addAll(List.of("give", "levelup", "list"));
         } else if (var4.length != 2 || !var4[0].equalsIgnoreCase("give") && !var4[0].equalsIgnoreCase("levelup")) {
            if (var4.length == 3 && var4[0].equalsIgnoreCase("levelup")) {
               var6.addAll(List.of("reaper", "katana"));
            } else if (var4.length == 4 && var4[0].equalsIgnoreCase("levelup")) {
               var6.addAll(List.of("1", "2", "3", "4", "5"));
            } else if (var4.length == 3 && var4[0].equalsIgnoreCase("give")) {
               var6.add("all");

               for(CustomItem var10 : CustomItem.values()) {
                  var6.add(var10.id);
               }
            }
         } else {
            Bukkit.getOnlinePlayers().forEach((var1x) -> var6.add(var1x.getName()));
         }
      } else if (var5.equals("juggernaut")) {
         if (var4.length == 1) {
            var6.add("start");
         } else if (var4.length == 2 && var4[0].equalsIgnoreCase("start")) {
            Bukkit.getOnlinePlayers().forEach((var1x) -> var6.add(var1x.getName()));
         }
      } else if (var5.equals("ability") && var4.length == 1) {
         var6.addAll(ABILITIES.keySet());
      } else if ((var5.equals("trust") || var5.equals("untrust")) && var4.length == 1) {
         Bukkit.getOnlinePlayers().forEach((var1x) -> var6.add(var1x.getName()));
      }

      String var11 = var4.length == 0 ? "" : var4[var4.length - 1].toLowerCase(Locale.ROOT);
      var6.removeIf((var1x) -> !var1x.toLowerCase(Locale.ROOT).startsWith(var11));
      return var6;
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
