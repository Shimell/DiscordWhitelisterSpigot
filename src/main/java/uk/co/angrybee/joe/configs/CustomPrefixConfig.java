package uk.co.angrybee.joe.configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;

// custom-prefixes.yml
public class CustomPrefixConfig extends Config
{
    public CustomPrefixConfig() {
        file = new File(DiscordWhitelister.getPlugin().getDataFolder(), "custom-prefixes.yml");
        fileConfiguration = new YamlConfiguration();
    }

    public void ConfigSetup()
    {


        if(!file.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
    }

    private void CheckEntries()
    {
        if(file.exists())
        {
            CheckEntry("whitelist-add-prefix", "/whitelist add");

            CheckEntry("whitelist-remove-prefix", "/whitelist remove");

            CheckEntry("clear-name-prefix", "/clearname");

            CheckEntry("limited-whitelist-clear-prefix", "/whitelist clear");

            CheckEntry("clear-ban-prefix", "!clearban");

            CheckEntry("whitelist-whois-prefix", "/whitelist whois");
        }
    }
}
