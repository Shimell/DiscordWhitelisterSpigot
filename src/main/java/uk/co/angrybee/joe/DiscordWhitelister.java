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

    private File whitelisterBotConfigFile;
    private FileConfiguration whitelisterBotConfig;

    private boolean configCreated = false;

    private String botToken;
    private List allowedRoles;
    private boolean botEnabled = true;

    @Override
    public void onEnable()
    {
        ConfigSetup();

        AssignVars();

        if(!botEnabled)
        {
            getLogger().info("Bot is disabled as per the config, doing nothing");
        }
        else if(configCreated)
        {
            getLogger().info("Config newly created, token will be in-valid, doing nothing until next server start");
        }
        else
        {
            getLogger().info("Initializing Discord client");
            ServerDiscordClient serverDiscordClient = new ServerDiscordClient();
            serverDiscordClient.InitializeClient(botToken);
            getLogger().info("Successfully initialized Discord client");
        }
    }

    public FileConfiguration getWhitelisterBotConfig()
    {
        return this.whitelisterBotConfig;
    }

    public void ConfigSetup()
    {
        String fileString = "discord-whitelister.yml";

        whitelisterBotConfigFile = new File(getDataFolder(), fileString);

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

        whitelisterBotConfig = new YamlConfiguration();

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
        allowedRoles = getWhitelisterBotConfig().getList("allowed-to-use-roles");
        botEnabled = getWhitelisterBotConfig().getBoolean("bot-enabled");
    }

}
