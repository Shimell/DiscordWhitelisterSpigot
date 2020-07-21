package uk.co.angrybee.joe.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.Stores.InGameRemovedList;

import java.io.File;
import java.io.IOException;

// Checks for whitelist removes in-game, so player's cannot use the bot to add them back without an admin/staff member with higher permissions
public class OnWhitelistEvents implements Listener
{
    private enum CommandContext { VANILLA_ADD, EASYWL_ADD, VANILLA_REMOVE, EASYWL_REMOVE }

    //private static final File whitelistFile = (new File(".", "whitelist.json"));

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) // ? no need to log uuid as the minecraft name is just for reference
    {
        // @NotNull Player commandCaller, @NotNull String message

        // Initial check
        CommandContext commandContext;
        Player commandCaller = event.getPlayer();
        String message = event.getMessage();

        event.getPlayer().chat("test");

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

        commandCaller.chat(commandContext.toString());

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
        // TODO: sort out case sensitive stuff when adding & removing in-game
        // TODO: PM staff member of removal from list

        event.getPlayer().chat("passed check");
        String targetName = "";

        // Check for adds to remove player's off the removed list (if they are on it)
        if(commandContext.equals(CommandContext.VANILLA_ADD) && !DiscordWhitelister.useEasyWhitelist)
        {
            // TODO
            // DONE Check removed-list.yml, remove username from there if it exists
            // DONE Check in-game removed list when created and remove from there if it exists
            // Log removal of name from list if it existed

            targetName = message.substring("/whitelist add".length() + 1).toLowerCase();

            // TODO: change when removed list is moved to stores folder
            if(DiscordWhitelister.getRemovedList().get(targetName) != null)
            {
                DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " is attempting to add " + targetName + ". Removing " + targetName +
                        " from removed-list.yml");
                DiscordWhitelister.getRemovedList().set(targetName, null);

                // Save changes
                // TODO: call save function when created instead of doing it again here
                try
                {
                    DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            if(InGameRemovedList.CheckStoreForPlayer(targetName))
            {
                InGameRemovedList.RemoveUserFromStore(targetName);
                DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " is attempting to add " + targetName + ". Removing " + targetName +
                        " from in-game-removed-list.yml");
            }
        }
        else if(commandContext.equals(CommandContext.EASYWL_ADD) && DiscordWhitelister.useEasyWhitelist)
        {

        }
        else if(commandContext.equals(CommandContext.VANILLA_REMOVE) && !DiscordWhitelister.useEasyWhitelist)
        {
            // TODO
            // DONE Check if the player is in the whitelist as this runs before the command is executed
            // DONE Add player to in-game removed list and the players' name that removed it

            targetName = message.substring("/whitelist remove".length() + 1).toLowerCase();

            // Should this be checked?
            // TODO: incorporate this into the function
            File tempWhitelistJSON = (new File(".", "whitelist.json"));
            if(DiscordClient.checkWhitelistJSON(tempWhitelistJSON, targetName))
            {
                InGameRemovedList.AddUserToStore(targetName, commandCaller.getName());
                DiscordWhitelister.getPluginLogger().info(commandCaller.getName() + " has added " + targetName + " to the in-game removed list");
            }
        }
        else if(commandContext.equals(CommandContext.EASYWL_REMOVE) && DiscordWhitelister.useEasyWhitelist)
        {

        }
    }
}
