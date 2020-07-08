package uk.co.angrybee.joe.Configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;

public class CustomPrefixConfig
{
    static File customPrefixesFile;
    static FileConfiguration customPrefixesConfig;

    static boolean customPrefixesFileCreated = false;

    public static String whitelistAddPrefix;
    public static String whitelistRemovePrefix;

    public static void ConfigSetup()
    {
        customPrefixesFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "custom-prefixes.yml");
        customPrefixesConfig = new YamlConfiguration();

        if(!customPrefixesFile.exists())
        {
            CreateConfig();
        }

        LoadConfig();
        CheckEntries();
        SaveConfig();
        AssignStrings();
    }

    private static void CreateConfig()
    {
        try
        {
            customPrefixesFile.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        DiscordWhitelister.getPluginLogger().info("Custom Prefixes file created at: " + customPrefixesFile.getPath());
        customPrefixesFileCreated = true;
    }

    private static void LoadConfig()
    {
        try
        {
            customPrefixesConfig.load(customPrefixesFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntries()
    {
        if(customPrefixesFile.exists())
        {
            if(customPrefixesConfig.getString("whitelist-add-prefix") == null)
            {
                customPrefixesConfig.set("whitelist-add-prefix", "!whitelist add");

                if(!customPrefixesFileCreated)
                {
                    DiscordWhitelister.getPluginLogger().warning("Entry 'whitelist-add-prefix' was not found, adding it to the config...");
                }
            }

            if(customPrefixesConfig.getString("whitelist-remove-prefix") == null)
            {
                customPrefixesConfig.set("whitelist-remove-prefix", "!whitelist remove");

                if(!customPrefixesFileCreated)
                {
                    DiscordWhitelister.getPluginLogger().warning("Entry 'whitelist-remove-prefix' was not found, adding it to the config...");
                }
            }
        }
    }

    private static void SaveConfig()
    {
        try
        {
            customPrefixesConfig.save(customPrefixesFile.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void AssignStrings()
    {
        whitelistAddPrefix = customPrefixesConfig.getString("whitelist-add-prefix");
        whitelistRemovePrefix = customPrefixesConfig.getString("whitelist-remove-prefix");
    }
}
