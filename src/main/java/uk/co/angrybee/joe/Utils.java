package uk.co.angrybee.joe;

import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import static uk.co.angrybee.joe.DiscordWhitelister.*;

public class Utils {

    // Check if player is vanished
    public static boolean isVanished(Player player) {

        // For Essentials
        if(essentialsPlugin != null) {
            //getPluginLogger().info("Checking via EssX if " + player.getDisplayName() + " is vanished: " + essentialsPlugin.getUser(player).isVanished());
            //getPluginLogger().info("Checking via EssX for list of vanished players: " + essentialsPlugin.getVanishedPlayers());
            if(essentialsPlugin.getUser(player).isVanished()) return true;
        }

        // For SuperVanish / PremiumVanish
        if(VanishAPI.getPlugin() != null) {
            //getPluginLogger().info("Checking via SV if " + player.getDisplayName() + " is vanished: " + VanishAPI.isInvisible(player));
            //getPluginLogger().info("Checking via SV for list of vanished players: " + VanishAPI.getAllInvisiblePlayers());
            if(VanishAPI.isInvisible(player)) return true;
        }

        // For VanishNoPacket
        if(vanishNoPacketPlugin != null) {
            //getPluginLogger().info("Checking via VNP if " + player.getDisplayName() + " is vanished: " + vanishNoPacketPlugin.getManager().isVanished(player));
            //getPluginLogger().info("Checking via VNP for list of vanished players: " + vanishNoPacketPlugin.getManager().getVanishedPlayers());
            if(vanishNoPacketPlugin.getManager().isVanished(player)) return true;
        }

        // For others (maybe)
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }

        // Otherwise, player is not vanished
        return false;

    }

}

