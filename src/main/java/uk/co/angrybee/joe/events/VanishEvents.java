package uk.co.angrybee.joe.events;

import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;

public class VanishEvents {

    // Called when a player exits vanished mode
    public static void onPlayerShow(String playerName) {
        if(!DiscordWhitelister.showVanishedPlayersInCount) {
            DiscordWhitelister.getPlugin().getLogger().info("Player " + playerName + " un-vanished, incrementing player count");
            DiscordWhitelister.removeVanishedPlayer();
            DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers());
        }
    }

    // Called when a player enters vanished mode
    public static void onPlayerHide(String playerName) {
        if(!DiscordWhitelister.showVanishedPlayersInCount) {
            DiscordWhitelister.getPlugin().getLogger().info("Player " + playerName + " vanished, decrementing player count");
            DiscordWhitelister.addVanishedPlayer();
            DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers());
        }
    }
}
