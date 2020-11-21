package uk.co.angrybee.joe.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.RemovedList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

import java.io.File;

// Checks for whitelist removes in-game, so player's cannot use the bot to add them back without an admin/staff member with higher permissions
public class OnWhitelistEvents implements Listener
{
    private enum CommandContext { VANILLA_ADD, EASYWL_ADD, VANILLA_REMOVE, EASYWL_REMOVE }

    // TODO: incorporate this into the checkWhitelistJSON function
    private static final File whitelistFile = (new File(".", "whitelist.json"));

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        // Initial check
        CommandContext commandContext;
        Player commandCaller = event.getPlayer();
        String message = event.getMessage();

        if(message.startsWith("/whitelist add"))
            commandContext = CommandContext.VANILLA_ADD;
        else if(message.startsWith("/easywl add"))
            commandContext = CommandContext.EASYWL_ADD;
        else if(message.startsWith("/whitelist remove"))
            commandContext = CommandContext.VANILLA_REMOVE;
        else if(message.startsWith("/easywl remove"))
            commandContext = CommandContext.EASYWL_REMOVE;
        else
            return;

        // Don't proceed to run if the player does not have permission
        if(commandContext.equals(CommandContext.VANILLA_ADD) && !commandCaller.hasPermission("bukkit.command.whitelist.add"))
            return;
        else if(commandContext.equals(CommandContext.EASYWL_ADD) && !commandCaller.hasPermission("easywhitelist.admin"))
            return;
        else if(commandContext.equals(CommandContext.VANILLA_REMOVE) && !commandCaller.hasPermission("bukkit.command.whitelist.remove"))
            return;
        else if(commandContext.equals(CommandContext.EASYWL_REMOVE) && !commandCaller.hasPermission("easywhitelist.admin"))
            return;

        // Determine what command to check

        String targetName = "";

        // Check for adds to remove player's off the removed list (if they are on it)
        if(commandContext.equals(CommandContext.VANILLA_ADD) && !WhitelistedPlayers.usingEasyWhitelist)
        {
            targetName = message.substring("/whitelist add".length() + 1).toLowerCase();
            ClearPlayerFromRemovedLists(targetName, commandCaller);
        }
        else if(commandContext.equals(CommandContext.EASYWL_ADD) && WhitelistedPlayers.usingEasyWhitelist)
        {
            targetName = message.substring("/easywl add".length() + 1).toLowerCase();
            ClearPlayerFromRemovedLists(targetName, commandCaller);
        }
        else if(commandContext.equals(CommandContext.VANILLA_REMOVE) && !WhitelistedPlayers.usingEasyWhitelist)
        {
            targetName = message.substring("/whitelist remove".length() + 1).toLowerCase();

            if(WhitelistedPlayers.CheckForPlayer(targetName))
            {
                InGameRemovedList.AddUserToStore(targetName, commandCaller.getName());
                DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " has added " + targetName + " to the in-game removed list");
            }
        }
        else if(commandContext.equals(CommandContext.EASYWL_REMOVE) && WhitelistedPlayers.usingEasyWhitelist)
        {
            targetName = message.substring("/easywl remove".length() + 1).toLowerCase();

            if(WhitelistedPlayers.CheckForPlayerEasyWhitelist(targetName))
            {
                InGameRemovedList.AddUserToStore(targetName, commandCaller.getName());
                DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " has added " + targetName + " to the in-game removed list");
            }
        }
    }

    private static void ClearPlayerFromRemovedLists(String playerName, Player commandCaller)
    {
        if(RemovedList.CheckStoreForPlayer(playerName))
        {
            DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " is attempting to add " + playerName + ". Removing " + playerName +
                    " from removed-list.yml");
            RemovedList.getRemovedPlayers().set(playerName, null);

            // Save changes
            RemovedList.SaveStore();
        }

        if(InGameRemovedList.CheckStoreForPlayer(playerName))
        {
            InGameRemovedList.RemoveUserFromStore(playerName);
            DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " is attempting to add " + playerName + ". Removing " + playerName +
                    " from in-game-removed-list.yml");
        }
    }
}
