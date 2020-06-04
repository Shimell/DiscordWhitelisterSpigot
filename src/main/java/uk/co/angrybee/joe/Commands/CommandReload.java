package uk.co.angrybee.joe.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.util.logging.Logger;

public class CommandReload implements CommandExecutor
{

    // /dwreload
    // hot reload command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        Logger pluginLogger = DiscordWhitelister.getPlugin().getLogger();

        sender.sendMessage("[DW] Attempting to reload...");
        pluginLogger.info("[DW] " + sender.getName() + " has requested a hot reload");

        pluginLogger.info("[DW] Shutting down client...");
        boolean shutDownTriggerSuccessful = DiscordClient.ShutdownClient();

        if(!shutDownTriggerSuccessful)
        {
            pluginLogger.info("[DW] Failed to trigger shutdown on client");
            sender.sendMessage("[DW] Failed to reload Discord client (Reason: Failed to trigger shutdown on client)");
            return false;
        }

        // initializes client
        int initSuccess = DiscordWhitelister.InitBot();

        if(initSuccess == 1)
        {
            pluginLogger.info("[DW] Failed to re-initialize client");
            sender.sendMessage("[DW] Failed to reload Discord client (Reason: Failed to re-initialize client)");
            return false;
        }

        pluginLogger.info("[DW] Successfully restarted Discord client");
        sender.sendMessage("[DW] Successfully restarted Discord client");
        return true;
    }
}
