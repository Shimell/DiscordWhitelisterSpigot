package uk.co.angrybee.joe.events;

import net.dv8tion.jda.api.entities.Guild;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import uk.co.angrybee.joe.configs.MainConfig;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.UserList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

import java.io.IOException;
import java.util.*;


public class OnBanEvent implements Listener
{
    @EventHandler
    public void onCommandBan(PlayerCommandPreprocessEvent e) throws IOException
    {
        // Context
        Player commandCaller = e.getPlayer();
        String message = e.getMessage().toLowerCase();

//        // Check just in case?
//        if(!DiscordWhitelister.useOnBanEvents)
//            return;

        // Check if player is using the ban command
        if(!message.startsWith("/ban"))
            return;

        // Check if the player has permission to use the ban command before proceeding
        // Do not bother checking if the player can ban ips as we get no useful information from it
        if(!commandCaller.hasPermission("bukkit.command.ban.player"))
            return;

        String banTarget = message.substring("/ban".length() + 1).toLowerCase();
        // Remove ban reason if there is one
        if(banTarget.contains(" "))
        {
            banTarget = banTarget.substring(0, banTarget.indexOf(" "));
        }

        // Check if there is a name to query
        if(banTarget.equals(""))
            return;

        // Check if the player has ever joined the server or is on the whitelist
        Server server = DiscordWhitelister.getPlugin().getServer();
        boolean nameInOfflinePlayers = false;

        // Check offline players for banTarget
        OfflinePlayer[] offlinePlayers = server.getOfflinePlayers();
        for(int i = 0; i < server.getOfflinePlayers().length; i++)
        {
            if(offlinePlayers[i].getName().equals(banTarget))
                nameInOfflinePlayers = true;
        }

        boolean nameInOnlinePlayers = false;
        for(Player onlinePlayer : server.getOnlinePlayers())
        {
            if(onlinePlayer.getName().equals(banTarget))
                nameInOnlinePlayers = true;
        }

        if(!nameInOnlinePlayers && !nameInOfflinePlayers)
        {
            if(!WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayer(banTarget)
                || WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayerEasyWhitelist(banTarget))
            {
                DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " is attempting to ban player with the name " + banTarget
                        + " but the user is not on the whitelist nor have they joined the server; doing nothing...");
                return;
            }
        }

        // Assign initial ban target to in-game removed list
        InGameRemovedList.AddUserToStore(banTarget, commandCaller.getDisplayName());

        // Check if the player is whitelisted
        if(!WhitelistedPlayers.usingEasyWhitelist && WhitelistedPlayers.CheckForPlayer(banTarget)
                || WhitelistedPlayers.usingEasyWhitelist && WhitelistedPlayers.CheckForPlayerEasyWhitelist(banTarget))
        {
            boolean idFound = false;

            // Find the Discord Id linked to the whitelisted player
            Set<String> keys = UserList.getUserList().getKeys(false);
            for (String discordId : keys) {
                List<?> registeredUsers = UserList.getRegisteredUsers(discordId);
                for (Object mc_name : registeredUsers) {
                if(mc_name instanceof String && mc_name.equals(banTarget))
                    {
                        // Found the ban target, assign the corresponding Discord id
                        // Remove whitelisted players associated with the discord id
                        for (Object targetWhitelistedPlayer : registeredUsers) {
                            if (targetWhitelistedPlayer instanceof String){
                                DiscordClient.UnWhitelist((String) mc_name);
                                DiscordWhitelister.getPluginLogger().info("Removed " + targetWhitelistedPlayer
                                        + " from the whitelist as they were added by Discord Id: " + discordId);

                                // Add username to the in-game removed list
                                InGameRemovedList.AddUserToStore((String) targetWhitelistedPlayer, commandCaller.getDisplayName());
                            }
                        }
                        // Remove the users whitelisted players from the list
                        UserList.getUserList().set(discordId, null);

                        UserList.SaveStore();

                        // Find all servers bot is in, assign & remove roles
                        for(Guild guild: DiscordClient.javaDiscordAPI.getGuilds())
                        {
                            // Remove the whitelisted role(s)
                            DiscordClient.RemoveRolesFromUser(guild, discordId, Arrays.asList(DiscordClient.whitelistedRoleNames));
                            // Add the banned role(s)
                            DiscordClient.AssignRolesToUser(guild, discordId, (List<String>) DiscordWhitelister.mainConfig.getFileConfiguration().get("banned-roles"));
                        }
                        idFound = true;
                        break;
                    }
                }
            }
            if (!idFound) {
                DiscordWhitelister.getPluginLogger().warning(banTarget + " does not have a linked Discord Id; cannot assign roles!");
            }
        }
    }
}
