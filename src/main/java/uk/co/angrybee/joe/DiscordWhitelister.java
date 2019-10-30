package uk.co.angrybee.joe;

import net.dv8tion.jda.api.entities.ISnowflake;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class DiscordWhitelister extends JavaPlugin
{

    private ServerDiscordClient serverDiscordClient;

    private File whitelisterBotConfigFile;
    static private File userListFile;
    private File removedListFile;

    private static FileConfiguration whitelisterBotConfig;
    private static FileConfiguration userList;
    private static FileConfiguration removedList;

//    private LinkedHashMap<String, Integer> userInfo;

    private String botToken;

    private boolean configCreated = false;
    private boolean userListCreated = false;
    private boolean removedListCreated = false;

    private boolean botEnabled;
    public boolean limitedAddRolesEnabled;

    private static JavaPlugin thisPlugin;

    @Override
    public void onEnable()
    {
        thisPlugin = this;
        whitelisterBotConfig = new YamlConfiguration();
        userList = new YamlConfiguration();
        removedList = new YamlConfiguration();

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

            // set add & remove roles
            serverDiscordClient.allowedToAddRemoveRoles = new String[DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles").size()];
            for(int roles = 0; roles < serverDiscordClient.allowedToAddRemoveRoles.length; ++roles)
            {
                serverDiscordClient.allowedToAddRemoveRoles[roles] = DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles").get(roles).toString();
            }

            // set add roles
            serverDiscordClient.allowedToAddRoles = new String[DiscordWhitelister.getWhitelisterBotConfig().getList("add-roles").size()];
            for(int roles = 0; roles < serverDiscordClient.allowedToAddRoles.length; ++roles)
            {
                serverDiscordClient.allowedToAddRoles[roles] = DiscordWhitelister.getWhitelisterBotConfig().getList("add-roles").get(roles).toString();
            }

            // set limited add roles
            serverDiscordClient.allowedToAddLimitedRoles = new String[DiscordWhitelister.getWhitelisterBotConfig().getList("limited-add-roles").size()];
            for(int roles = 0; roles < serverDiscordClient.allowedToAddLimitedRoles.length; ++roles)
            {
                serverDiscordClient.allowedToAddLimitedRoles[roles] = DiscordWhitelister.getWhitelisterBotConfig().getList("limited-add-roles").get(roles).toString();
            }

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

    public static FileConfiguration getUserList()
    {
        return userList;
    }

    public static File getUserListFile()
    {
        return userListFile;
    }

    public static FileConfiguration getRemovedList()
    {
        return  removedList;
    }

    public void ConfigSetup()
    {
        whitelisterBotConfigFile = new File(getDataFolder(), "discord-whitelister.yml");
        userListFile = new File(getDataFolder(), "user-list.yml");
        removedListFile = new File(getDataFolder(), "removed-list");

        if(!whitelisterBotConfigFile.getParentFile().exists())
        {
            whitelisterBotConfigFile.getParentFile().mkdirs();
        }

        if(!whitelisterBotConfigFile.exists())
        {
            try
            {
                whitelisterBotConfigFile.createNewFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            getLogger().info("Configuration file created at: " + whitelisterBotConfigFile.getPath() +
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

        if(!userListFile.exists())
        {
            try
            {
                userListFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            getLogger().info("User list created at: " + userListFile.getPath());
            userListCreated = true;
        }

        try
        {
            getUserList().load(userListFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }

        if(!removedListFile.exists())
        {
            try
            {
                removedListFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            getLogger().info("Removed list created at: " + removedListFile.getPath());
            removedListCreated = true;
        }

        try
        {
            getRemovedList().load(removedListFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }

        if(configCreated)
        {
            getWhitelisterBotConfig().set("discord-bot-token", "Discord bot token goes here, you can find it here: " +
                    "https://discordapp.com/developers/applications/");

            // allowed to add and remove from the whitelist
            List<String> tempAddRemoveRoles = Arrays.asList("Owner", "Admin");
            getWhitelisterBotConfig().set("add-remove-roles", tempAddRemoveRoles);

            // only allowed to add to the whitelist
            List<String> tempAddRoles = Arrays.asList("Mod", "Whitelister");
            getWhitelisterBotConfig().set("add-roles", tempAddRoles);

            // if the limited whitelist feature should be enabled
            getWhitelisterBotConfig().set("limited-whitelist-enabled", true);

            // the amount of times a non-staff user is allowed to whitelist
            getWhitelisterBotConfig().set("max-whitelist-amount", 3);

            // roles that are allowed whitelist a limited amount of times
            List<String> tempLimitedRoles = Arrays.asList("VIP", "LimitedWhitelister");
            getWhitelisterBotConfig().set("limited-add-roles", tempLimitedRoles);

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

        if(userListCreated)
        {
            try
            {
                getUserList().save(userListFile.getPath());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void AssignVars()
    {
        botToken = getWhitelisterBotConfig().getString("discord-bot-token");
        botEnabled = getWhitelisterBotConfig().getBoolean("bot-enabled");
        limitedAddRolesEnabled = getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled");
    }
}
