package uk.co.angrybee.joe.Stores;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import uk.co.angrybee.joe.Configs.MainConfig;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

// For accessing whitelisted players
public class WhitelistedPlayers
{
    private static Plugin easyWhitelist;
    private static FileConfiguration easyWhitelistPlayers;

    private static final Server server = DiscordWhitelister.getPlugin().getServer();
    private static final PluginManager pluginManager = server.getPluginManager();
    private static final Logger pluginLogger = DiscordWhitelister.getPluginLogger();

    public static boolean usingEasyWhitelist;

    public static void Setup()
    {
        // Check if we are using EasyWhitelist
        if(MainConfig.getMainConfig().getBoolean("use-easy-whitelist"))
            GetEasyWhitelist();

        if(usingEasyWhitelist)
        {
            try
            {
                easyWhitelistPlayers = new YamlConfiguration();
                easyWhitelistPlayers.load(new File(easyWhitelist.getDataFolder(), "config.yml"));
            }
            catch (IOException | InvalidConfigurationException e)
            {
                pluginLogger.severe("Failed to load the EasyWhitelist file, reverting back to vanilla whitelist command");
                usingEasyWhitelist = false;
                e.printStackTrace();
            }
        }
    }

    private static void GetEasyWhitelist()
    {
        if(pluginManager.getPlugin("EasyWhitelist") != null)
        {
            pluginLogger.info("Easy Whitelist found; will use over default whitelist command");
            easyWhitelist = pluginManager.getPlugin("EasyWhitelist");
            usingEasyWhitelist = true;
        }
        else
        {
            usingEasyWhitelist = false; // Define this for config hot reloads
            pluginLogger.warning("Easy Whitelist was not found but is enabled in the config. " +
                    "Falling back to default whitelist command");
        }
    }

    public static boolean CheckForPlayer(String playerName)
    {
        for(OfflinePlayer offlinePlayer : server.getWhitelistedPlayers())
        {
            if(offlinePlayer.getName().equalsIgnoreCase(playerName))
                return true;
        }
        return false;
    }

    public static boolean CheckForPlayerEasyWhitelist(String playerName)
    {
        // Check just in case
        if(!usingEasyWhitelist)
            return false;

        // Load changes
        try
        {
            easyWhitelistPlayers.load(new File(easyWhitelist.getDataFolder(), "config.yml"));
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }

        for(String name : easyWhitelistPlayers.getStringList("whitelisted"))
        {
            if(name.equalsIgnoreCase(playerName))
                return true;
        }
        return false;
    }
}
