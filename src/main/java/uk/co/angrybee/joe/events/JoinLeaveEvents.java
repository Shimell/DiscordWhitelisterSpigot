package uk.co.angrybee.joe.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.DiscordClient;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import uk.co.angrybee.joe.Utils;

// Used for showing player count in the discord bots status
public class JoinLeaveEvents implements Listener
{
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        if(!DiscordWhitelister.showVanishedPlayersInCount)
        {
            if(Utils.isVanished(player))
            {
                DiscordWhitelister.getPlugin().getLogger().info("Player " + player.getDisplayName() + " joined while vanished, not incrementing player count");
                DiscordWhitelister.addVanishedPlayer();
                return;
            }
        }
        DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        if(!DiscordWhitelister.showVanishedPlayersInCount)
        {
            if(Utils.isVanished(player))
            {
                DiscordWhitelister.getPlugin().getLogger().info("Player " + player.getDisplayName() + " quit while vanished, not decrementing player count");
                DiscordWhitelister.removeVanishedPlayer();
                return;
            }
        }
        DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers() - 1);
    }
}
