package uk.co.angrybee.joe.configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class PermissionsConfig
{
    private static File permissionsConfigFile;
    private static FileConfiguration permissionsConfig;

    public static FileConfiguration getPermissionsConfig() { return permissionsConfig; }

    private static boolean permissionsFileCreated = false;

    public static void ConfigSetup()
    {
        permissionsConfigFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "on-whitelist-permissions.yml");
        permissionsConfig = new YamlConfiguration();

        if(!permissionsConfigFile.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
    }

    private static void CreateConfig()
    {
        try
        {
            permissionsConfigFile.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        DiscordWhitelister.getPluginLogger().info("on whitelist permissions file created at: " + permissionsConfigFile.getPath());
        permissionsFileCreated = true;
    }

    private static void LoadConfigFile()
    {
        try
        {
            permissionsConfig.load(permissionsConfigFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntries()
    {
        if(permissionsConfigFile.exists())
        {
            // test permission
            CheckEntry("perms-on-whitelist", Collections.singletonList("bukkit.command.tps"));
        }
    }

    private static void SaveConfig()
    {
        try
        {
            permissionsConfig.save(permissionsConfigFile.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntry(String entryName, Object passedValue)
    {
        if(permissionsConfig.get(entryName) == null)
        {
            permissionsConfig.set(entryName, passedValue);

            if(!permissionsFileCreated)
                DiscordWhitelister.getPluginLogger().warning("Entry '" + entryName + "' was not found, adding it to on-whitelist-permissions.yml...");
        }
    }
}
