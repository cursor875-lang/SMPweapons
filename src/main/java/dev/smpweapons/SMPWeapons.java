package dev.smpweapons;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class SMPWeapons extends JavaPlugin {
   private TrustManager trust;
   private StatusManager status;
   private Abilities abilities;
   private JuggernautManager juggernaut;
   private WeaponEvents weaponEvents;
   private SafeZones safeZones;
   private FlowerSword flowerSword;

   StatusManager statusManager() { return this.status; }

   public void onEnable() {
      ConfigFiles.init(this);
      this.safeZones = new SafeZones(this);
      this.getServer().getPluginManager().registerEvents(this.safeZones, this);
      Items.init(this);
      ReaperKills.init(this);
      this.trust = new TrustManager(this);
      this.trust.load();
      this.status = new StatusManager(this);
      Cooldowns var1 = new Cooldowns();
      this.abilities = new Abilities(this, var1, this.trust, this.status);
      this.juggernaut = new JuggernautManager(this);
      this.getServer().getPluginManager().registerEvents(this.juggernaut, this);
      this.getServer().getPluginManager().registerEvents(new ItemListener(this, this.abilities, this.status), this);
      this.flowerSword = new FlowerSword(this, this.trust);
      this.getServer().getPluginManager().registerEvents(this.flowerSword, this);
      this.getServer().getPluginManager().registerEvents(new NoDragonEgg(this), this);
      this.weaponEvents = new WeaponEvents(this);
      this.getServer().getPluginManager().registerEvents(this.weaponEvents, this);
      PluginCommand var2 = this.getCommand("weaponevent");
      if (var2 != null) {
         var2.setExecutor(this.weaponEvents);
         var2.setTabCompleter(this.weaponEvents);
      }

      Commands var3 = new Commands(this.trust, this.abilities, this.status, this.juggernaut);

      for(String var7 : new String[]{"smpweapons", "trust", "untrust", "trustlist", "ability", "juggernaut"}) {
         PluginCommand var8 = this.getCommand(var7);
         if (var8 != null) {
            var8.setExecutor(var3);
            var8.setTabCompleter(var3);
         }
      }

      this.getServer().getScheduler().runTaskTimer(this, new PassiveTask(), 20L, 10L);
      BukkitScheduler var10000 = this.getServer().getScheduler();
      StatusManager var10002 = this.status;
      Objects.requireNonNull(var10002);
      var10000.runTaskTimer(this, var10002::tick, 5L, 5L);
      var10000 = this.getServer().getScheduler();
      JuggernautManager var10 = this.juggernaut;
      Objects.requireNonNull(var10);
      var10000.runTaskTimer(this, var10::tick, 20L, 10L);
      this.getLogger().info("SMPWeapons enabled with " + CustomItem.values().length + " items, including Flower Sword.");
      if (!safeZones.existsInAnyLoadedWorld()) {
         getLogger().warning("WorldGuard region '" + ConfigFiles.string("protection.yml", "protection.worldguard-region", "spawn2") + "' was not found in any loaded world. Configure it before enabling abilities.");
      }
      if (getCommand("smpweaponsconfig") != null) {
         getCommand("smpweaponsconfig").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
               ConfigFiles.reload();
               sender.sendMessage("§aSMPWeapons YAML files reloaded. Existing spawned items keep their metadata.");
            } else if (args.length > 0 && args[0].equalsIgnoreCase("regioncheck")) {
               sender.sendMessage("§eProtected region: " + ConfigFiles.string("protection.yml", "protection.worldguard-region", "spawn2")
                   + " | found: " + safeZones.existsInAnyLoadedWorld());
            } else {
               sender.sendMessage("§eUsage: /smpweaponsconfig <reload|regioncheck>");
            }
            return true;
         });
      }
      // Do not modify global console logging.
   }

   public void onDisable() {
      if (flowerSword != null) flowerSword.shutdown();
      if (this.weaponEvents != null) {
         this.weaponEvents.shutdown();
      }

      if (this.abilities != null) {
         this.abilities.shutdown();
      }

      if (this.juggernaut != null) {
         this.juggernaut.shutdown();
      }

      if (this.status != null) {
         this.status.clearAll();
      }

      if (this.trust != null) {
         this.trust.save();
      }

      // Nothing to uninstall from the global logger.
   }
}
