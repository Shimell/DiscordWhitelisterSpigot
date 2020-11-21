package uk.co.angrybee.joe.configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;

// custom-prefixes.yml
public class CustomPrefixConfig
{
    private static File customPrefixesFile;
    private static FileConfiguration customPrefixesConfig;

    public static FileConfiguration getCustomPrefixesConfig() { return customPrefixesConfig; }

    private static boolean customPrefixesFileCreated = false;

    public static void ConfigSetup()
    {
        customPrefixesFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "custom-prefixes.yml");
        customPrefixesConfig = new YamlConfiguration();

        if(!customPrefixesFile.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
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

    private static void LoadConfigFile()
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
            CheckEntry("whitelist-add-prefix", "!whitelist add");

            CheckEntry("whitelist-remove-prefix", "!whitelist remove");

            CheckEntry("clear-name-prefix", "!clearname");

            CheckEntry("limited-whitelist-clear-prefix", "!whitelist clear");

            CheckEntry("clear-ban-prefix", "!clearban");
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

    private static void CheckEntry(String entryName, Object passedValue)
    {
        if(customPrefixesConfig.get(entryName) == null)
        {
            customPrefixesConfig.set(entryName, passedValue);

            if(!customPrefixesFileCreated)
                DiscordWhitelister.getPluginLogger().warning("Entry '" + entryName + "' was not found, adding it to custom-prefixes.yml...");
        }
    }
}
