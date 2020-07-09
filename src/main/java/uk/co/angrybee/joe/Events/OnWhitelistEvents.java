package uk.co.angrybee.joe.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import uk.co.angrybee.joe.DiscordWhitelister;

// Checks for whitelist removes in-game, so player's cannot use the bot to add them back without an admin/staff member with higher permissions
public class OnWhitelistEvents implements Listener
{
    private enum CommandContext { VANILLA_ADD, EASYWL_ADD, VANILLA_REMOVE, EASYWL_REMOVE }

    @EventHandler
    public void PlayerCommandPreprocessEvent(Player commandCaller, String message)
    {
        // Initial check
        CommandContext commandContext;

        if(message.startsWith("!whitelist add"))
            commandContext = CommandContext.VANILLA_ADD;
        else if(message.startsWith("!easywl add"))
            commandContext = CommandContext.EASYWL_ADD;
        else if(message.startsWith("!whitelist remove"))
            commandContext = CommandContext.VANILLA_REMOVE;
        else if(message.startsWith("!easywl remove"))
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

        // Check for adds to remove player's off the removed list (if they are on it)
        if(commandContext.equals(CommandContext.VANILLA_ADD) && !DiscordWhitelister.useEasyWhitelist)
        {
            // TODO
            // Check removed-list.yml, remove username from there if it exists
            // Check in-game removed list when created and remove from there if it exists
            // Log removal of name from list if it existed
        }
        else if(commandContext.equals(CommandContext.EASYWL_ADD) && DiscordWhitelister.useEasyWhitelist)
        {

        }
        else if(commandContext.equals(CommandContext.VANILLA_REMOVE) && !DiscordWhitelister.useEasyWhitelist)
        {
            // TODO
            // Check if the player is in the whitelist as this runs before the command is executed
            // Add player to in-game removed list and the players' name that removed it
        }
        else if(commandContext.equals(CommandContext.EASYWL_REMOVE) && DiscordWhitelister.useEasyWhitelist)
        {

        }
    }
}
