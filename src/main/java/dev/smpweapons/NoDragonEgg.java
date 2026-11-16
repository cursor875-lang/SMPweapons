package dev.smpweapons;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class NoDragonEgg implements Listener {
   private final JavaPlugin plugin;

   public NoDragonEgg(JavaPlugin var1) {
      this.plugin = var1;
   }

   @EventHandler
   public void onDragonDeath(EntityDeathEvent var1) {
      if (var1.getEntity() instanceof EnderDragon) {
         final World var2 = var1.getEntity().getWorld();
         if (var2.getEnvironment() == Environment.THE_END) {
            int portalX = 0;
            int portalZ = 0;
            DragonBattle var5 = var2.getEnderDragonBattle();
            if (var5 != null) {
               Location var6 = var5.getEndPortalLocation();
               if (var6 != null) {
                  portalX = var6.getBlockX();
                  portalZ = var6.getBlockZ();
               }
            }

            final int var3 = portalX;
            final int var4 = portalZ;
            (new BukkitRunnable() {
               int runs = 0;

               public void run() {
                  if (++this.runs > 80) {
                     this.cancel();
                  } else {
                     for(int var1 = var3 - 2; var1 <= var3 + 2; ++var1) {
                        for(int var2x = var4 - 2; var2x <= var4 + 2; ++var2x) {
                           int var3x = var2.getHighestBlockYAt(var1, var2x);

                           for(int var4x = var3x; var4x >= Math.max(var2.getMinHeight(), var3x - 8); --var4x) {
                              Block var5 = var2.getBlockAt(var1, var4x, var2x);
                              if (var5.getType() == Material.DRAGON_EGG) {
                                 var5.setType(Material.AIR);
                              }
                           }
                        }
                     }

                  }
               }
            }).runTaskTimer(this.plugin, 10L, 10L);
         }
      }
   }
}
