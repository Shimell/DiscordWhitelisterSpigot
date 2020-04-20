package uk.co.angrybee.joe.Events;

import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.DiscordClient;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// Used for showing player count in the discord bots status
public class JoinLeaveEvents implements Listener
{
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event)
    {
        DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers() - 1);
    }
}
