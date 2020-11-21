package uk.co.angrybee.joe.configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class OnWhitelistCommandsConfig
{
    private static File onWhitelistCommandsConfigFile;
    private static FileConfiguration onWhitelistCommandsConfig;

    public static FileConfiguration getPermissionsConfig() { return onWhitelistCommandsConfig; }

    private static boolean onWhitelistCommandsFileCreated = false;

    public static void ConfigSetup()
    {
        onWhitelistCommandsConfigFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "on-whitelist-commands.yml");
        onWhitelistCommandsConfig = new YamlConfiguration();

        if(!onWhitelistCommandsConfigFile.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
    }

    private static void CreateConfig()
    {
        try
        {
            onWhitelistCommandsConfigFile.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        DiscordWhitelister.getPluginLogger().info("on whitelist commands file created at: " + onWhitelistCommandsConfigFile.getPath());
        onWhitelistCommandsFileCreated = true;
    }

    private static void LoadConfigFile()
    {
        try
        {
            onWhitelistCommandsConfig.load(onWhitelistCommandsConfigFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntries()
    {
        if(onWhitelistCommandsConfigFile.exists())
        {
            // Write comments
            if(onWhitelistCommandsFileCreated)
            {
                SaveConfig(); // save and load again
                try
                {
                    FileWriter fileWriter = new FileWriter(onWhitelistCommandsConfigFile);
                    fileWriter.write("# The list of commands that will be dispatched when a player gets whitelisted. (Use the following syntax: \n"
                    + "# \"%TYPE%:%COMMAND%\", being %TYPE% whether 'CONSOLE' or 'PLAYER' and the command without the slash (/)\n"
                    + "# placeholder %PLAYER% is supported here).\n"
                    + "# NOTE: The 'PLAYER' type will only work if the target whitelisted player is in the server at the time of command dispatch.");

                    fileWriter.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                LoadConfigFile();
            }

            CheckEntry("on-whitelist-commands", Arrays.asList("CONSOLE:gamemode adventure %PLAYER%", "CONSOLE:say hello testing"));
        }
    }

    private static void SaveConfig()
    {
        try
        {
            onWhitelistCommandsConfig.save(onWhitelistCommandsConfigFile.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntry(String entryName, Object passedValue)
    {
        if(onWhitelistCommandsConfig.get(entryName) == null)
        {
            onWhitelistCommandsConfig.set(entryName, passedValue);

            if(!onWhitelistCommandsFileCreated)
                DiscordWhitelister.getPluginLogger().warning("Entry '" + entryName + "' was not found, adding it to on-whitelist-permissions.yml...");
        }
    }
}
