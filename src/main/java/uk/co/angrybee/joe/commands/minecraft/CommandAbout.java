package uk.co.angrybee.joe.commands.minecraft;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandAbout implements CommandExecutor {

    // /dwabout
    // about command
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage("[DW] DiscordWhiteLister by JoeShimell\n" + ChatColor.GREEN + "https://github.com/JoeShimell/DiscordWhitelisterSpigot");
        return true;
    }
}
