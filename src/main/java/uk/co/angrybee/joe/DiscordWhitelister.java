package uk.co.angrybee.joe;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DiscordWhitelister extends JavaPlugin
{

    private ServerDiscordClient serverDiscordClient;

    private File whitelisterBotConfigFile;
    private static FileConfiguration whitelisterBotConfig;

    private boolean configCreated = false;

    private String botToken;
    private boolean botEnabled = true;

    private static JavaPlugin thisPlugin;

    @Override
    public void onEnable()
    {
        thisPlugin = this;
        whitelisterBotConfig = new YamlConfiguration();

        ConfigSetup();

        AssignVars();

        if(!botEnabled)
        {
            getLogger().info("Bot is disabled as per the config, doing nothing");
        }
        else if(configCreated)
        {
            getLogger().info("Config newly created, please paste your bot token into the config file, doing nothing until next server start");
        }
        else
        {
            getLogger().info("Initializing Discord client");
            serverDiscordClient = new ServerDiscordClient();
            serverDiscordClient.InitializeClient(botToken);
            getLogger().info("Successfully initialized Discord client");
        }
    }

    public static JavaPlugin getPlugin()
    {
        return thisPlugin;
    }

    public static FileConfiguration getWhitelisterBotConfig()
    {
        return whitelisterBotConfig;
    }

    public void ConfigSetup()
    {
        whitelisterBotConfigFile = new File(getDataFolder(), "discord-whitelister.yml");

        if(!whitelisterBotConfigFile.exists())
        {
            whitelisterBotConfigFile.getParentFile().mkdirs();

            //saveResource(fileString, false); // from example, doesn't seem to work?

            try
            {
                whitelisterBotConfigFile.createNewFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            getLogger().info("Configuration file created at " + whitelisterBotConfigFile.getPath() +
                    ", please edit this else the plugin will not work!");
            configCreated = true;
        }

        try
        {
            getWhitelisterBotConfig().load(whitelisterBotConfigFile);
        }
        catch(IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }

        if(configCreated)
        {
            getWhitelisterBotConfig().set("discord-bot-token", "Discord bot token goes here, you can find it here: " +
                    "https://discordapp.com/developers/applications/");

            List<String> tempSetupRoles = Arrays.asList("Owner", "Admin", "Mod");
            getWhitelisterBotConfig().set("allowed-to-use-roles", tempSetupRoles);

            List<String> tempChannelIds = Arrays.asList("445666834382061569", "488450157881327616");
            getWhitelisterBotConfig().set("target-text-channels", tempChannelIds);

            getWhitelisterBotConfig().set("bot-enabled", true);

            try
            {
                getWhitelisterBotConfig().save((whitelisterBotConfigFile.getPath()));
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void AssignVars()
    {
        botToken = getWhitelisterBotConfig().getString("discord-bot-token");
        botEnabled = getWhitelisterBotConfig().getBoolean("bot-enabled");
    }
}
