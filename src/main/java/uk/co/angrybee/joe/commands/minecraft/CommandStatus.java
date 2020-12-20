package uk.co.angrybee.joe.commands.minecraft;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.VersionInfo;

public class CommandStatus implements CommandExecutor {

    // /dw
    // version & status command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        String discordOnlineStatus = DiscordClient.getOnlineStatus();
        if (discordOnlineStatus.toLowerCase().equals("connected")) {
            discordOnlineStatus = ChatColor.GREEN + discordOnlineStatus;
        } else {
            discordOnlineStatus = ChatColor.RED + discordOnlineStatus;
        }
        sender.sendMessage("[DW] DiscordWhiteLister runs on: " + VersionInfo.getLongVersion());
        sender.sendMessage("[DW] Discord Bot: " + discordOnlineStatus);
        return true;
    }
}
