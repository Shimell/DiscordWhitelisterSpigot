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
        if(event.getPlayer().hasPermission("discordsrv.silentjoin") ||
                event.getPlayer().hasPermission("discordsrv.silentquit") ||
                event.getPlayer().hasPermission("sv.joinvanished")) {
            DiscordWhitelister.getPlugin().getLogger().info("Player " + event.getPlayer().getDisplayName() + " joined with silent joining/quitting permission, not incrementing player count");
            return;
        }
        DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event)
    {
        if(event.getPlayer().hasPermission("discordsrv.silentjoin") ||
                event.getPlayer().hasPermission("discordsrv.silentquit") ||
                event.getPlayer().hasPermission("sv.joinvanished")) {
            DiscordWhitelister.getPlugin().getLogger().info("Player " + event.getPlayer().getDisplayName() + " quit with silent joining/quitting permission, not decrementing player count");
            return;
        }
        DiscordClient.SetPlayerCountStatus(DiscordWhitelister.getOnlineUsers() - 1);
    }
}
