package dev.smpweapons;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class Items {
   private static NamespacedKey key;
   private static NamespacedKey killsKey;
   private static final MiniMessage MM = MiniMessage.miniMessage();

   private Items() {
   }

   public static void init(JavaPlugin var0) {
      key = new NamespacedKey(var0, "custom_item");
      killsKey = new NamespacedKey(var0, "reaper_kills");
   }

   public static ItemStack create(CustomItem var0) {
      YamlConfiguration wc = ConfigFiles.of("weapons.yml");
      String path = "weapons." + var0.id;
      Material material = Material.matchMaterial(ConfigFiles.string("weapons.yml", path + ".material", var0.material.name()));
      if (material == null) material = var0.material;
      ItemStack var1 = new ItemStack(material);
      ItemMeta var2 = var1.getItemMeta();
      var2.displayName(MM.deserialize("<!italic>" + ConfigFiles.string("weapons.yml", path + ".display-name", var0.name)));
      if (wc != null && wc.isConfigurationSection(path + ".enchantments")) {
         var enchants = wc.getConfigurationSection(path + ".enchantments");
         for (String enchantKey : enchants.getKeys(false)) {
            Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantKey.toLowerCase(java.util.Locale.ROOT)));
            if (enchant != null) {
               int level = enchants.getInt(enchantKey, 0);
               if (level > 0) var2.addEnchant(enchant, Math.min(255, level), true);
            }
         }
      } else {
         var0.enchants.forEach((var1x, var2x) -> var2.addEnchant(var1x, var2x, true));
      }
      if (var0.enchants.isEmpty()) {
         var2.setEnchantmentGlintOverride(true);
      }

      var2.getPersistentDataContainer().set(key, PersistentDataType.STRING, var0.id);
      if (var0 == CustomItem.SCULK_REAPER) {
         var2.getPersistentDataContainer().set(killsKey, PersistentDataType.INTEGER, 0);
      }

      var2.setUnbreakable(ConfigFiles.bool("weapons.yml", path + ".unbreakable", true));
      var2.lore(buildLore(var0, 0));
      var1.setItemMeta(var2);
      return var1;
   }

   public static boolean isArmorMaterial(Material var0) {
      String var1 = var0.name();
      return var1.endsWith("_HELMET") || var1.endsWith("_CHESTPLATE") || var1.endsWith("_LEGGINGS") || var1.endsWith("_BOOTS");
   }

   public static void upgradeDragonEgg(Player var0) {
      PlayerInventory var1 = var0.getInventory();
      ItemStack var2 = var1.getItemInMainHand();
      ItemStack var3 = var1.getItemInOffHand();
      if (var2.getType() == Material.DRAGON_EGG && identify(var2) == null) {
         ItemStack var4 = create(CustomItem.DRAGON_EGG);
         var4.setAmount(var2.getAmount());
         var1.setItemInMainHand(var4);
      }

      if (var3.getType() == Material.DRAGON_EGG && identify(var3) == null) {
         ItemStack var5 = create(CustomItem.DRAGON_EGG);
         var5.setAmount(var3.getAmount());
         var1.setItemInOffHand(var5);
      }

   }

   public static void makeItemsUnbreakable(Player var0) {
      for(ItemStack var4 : var0.getInventory().getContents()) {
         if (var4 != null && identify(var4) != null) {
            ItemMeta var5 = var4.getItemMeta();
            if (ConfigFiles.bool("weapons.yml", "weapons." + identify(var4).id + ".unbreakable", true) && !var5.isUnbreakable()) {
               var5.setUnbreakable(true);
               var4.setItemMeta(var5);
            }
         }
      }

   }

   private static List<Component> buildLore(CustomItem var0, int var1) {
      ArrayList var2 = new ArrayList();

      for(String var4 : ConfigFiles.strings("weapons.yml", "weapons." + var0.id + ".lore", var0.lore)) {
         var2.add(MM.deserialize("<!italic>" + var4));
      }

      if (var0 == CustomItem.SCULK_REAPER) {
         String[] var6 = new String[]{"Permanent Speed I", "Permanent Strength II", "Permanent Fire Resistance", "Unlock Echo Shell", "Unlock Sculk Hook"};
         var2.add(Component.empty());
         var2.add(MM.deserialize("<!italic><dark_aqua>Your kills: <white>" + var1));

         for(int var7 = 0; var7 < var6.length; ++var7) {
            boolean var5 = var1 >= var7 + 1;
            var2.add(MM.deserialize("<!italic>" + (var5 ? "<green>✔ " : "<dark_gray>✘ ") + "Kill " + (var7 + 1) + ": " + var6[var7]));
         }
      }

      if (var0 == CustomItem.BLOODMOON_KATANA) {
         var2.add(Component.empty());
         var2.add(MM.deserialize("<!italic><dark_red>Your kills: <white>" + var1));
         var2.add(MM.deserialize("<!italic>" + (var1 >= 3 ? "<green>✔ " : "<dark_gray>✘ ") + "Kill 3: Unlock Killing Intent"));
      }

      return var2;
   }

   private static int shownKills(ItemStack var0) {
      Integer var1 = (Integer)var0.getItemMeta().getPersistentDataContainer().get(killsKey, PersistentDataType.INTEGER);
      return var1 == null ? 0 : var1;
   }

   public static void refreshKillLore(Player var0) {
      PlayerInventory var1 = var0.getInventory();

      for(ItemStack var5 : new ItemStack[]{var1.getItemInMainHand(), var1.getItemInOffHand()}) {
         CustomItem var6 = identify(var5);
         ReaperKills var7 = ReaperKills.of(var6);
         if (var7 != null) {
            int var8 = var7.get(var0.getUniqueId());
            if (shownKills(var5) != var8 || !var5.getItemMeta().getPersistentDataContainer().has(killsKey)) {
               ItemMeta var9 = var5.getItemMeta();
               var9.getPersistentDataContainer().set(killsKey, PersistentDataType.INTEGER, var8);
               var9.lore(buildLore(var6, var8));
               var5.setItemMeta(var9);
            }
         }
      }

   }

   public static int heldReaperKills(Player var0) {
      return holding(var0, CustomItem.SCULK_REAPER) ? ReaperKills.REAPER.get(var0.getUniqueId()) : -1;
   }

   public static CustomItem identify0(ItemStack var0) {
      return var0 != null && !var0.isEmpty() && var0.hasItemMeta() ? CustomItem.byId((String)var0.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING)) : null;
   }

   public static boolean holding(Player var0, CustomItem var1) {
      PlayerInventory var2 = var0.getInventory();
      return identify(var2.getItemInMainHand()) == var1 || identify(var2.getItemInOffHand()) == var1;
   }

   public static boolean inMainHand(Player var0, CustomItem var1) {
      return identify(var0.getInventory().getItemInMainHand()) == var1;
   }

   public static boolean wearing(Player var0, CustomItem var1) {
      for(ItemStack var5 : var0.getInventory().getArmorContents()) {
         if (identify(var5) == var1) {
            return true;
         }
      }

      return false;
   }

   public static boolean carrying(Player var0, CustomItem var1) {
      for(ItemStack var5 : var0.getInventory().getContents()) {
         if (identify(var5) == var1) {
            return true;
         }
      }

      return false;
   }

   public static CustomItem identify(ItemStack var0) {
      return (CustomItem)WeaponsConfig.filter(identify0(var0));
   }
}
