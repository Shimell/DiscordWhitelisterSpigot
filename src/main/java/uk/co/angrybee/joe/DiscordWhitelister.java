package uk.co.angrybee.joe;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DiscordWhitelister extends JavaPlugin
{
    private static File whitelisterBotConfigFile;
    private static File userListFile;
    private static File removedListFile;

    private static FileConfiguration whitelisterBotConfig;
    private static FileConfiguration userList;
    private static FileConfiguration removedList;

    // easy whitelist
    public static Plugin easyWhitelist;

    private String botToken;

    private boolean configCreated = false;
    private boolean userListCreated = false;
    private boolean removedListCreated = false;

    public static boolean useEasyWhitelist = false;

    private boolean botEnabled;

    private static JavaPlugin thisPlugin;

    @Override
    public void onEnable()
    {
        thisPlugin = this;
        whitelisterBotConfig = new YamlConfiguration();
        userList = new YamlConfiguration();
        removedList = new YamlConfiguration();

        ConfigSetup();

        botToken = getWhitelisterBotConfig().getString("discord-bot-token");
        botEnabled = getWhitelisterBotConfig().getBoolean("bot-enabled");

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
            getLogger().info("Initializing Discord client...");

            // set add & remove roles
            DiscordClient.allowedToAddRemoveRoles = new String[getWhitelisterBotConfig().getList("add-remove-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToAddRemoveRoles.length; ++roles)
            {
                DiscordClient.allowedToAddRemoveRoles[roles] = getWhitelisterBotConfig().getList("add-remove-roles").get(roles).toString();
            }

            // set add roles
            DiscordClient.allowedToAddRoles = new String[getWhitelisterBotConfig().getList("add-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToAddRoles.length; ++roles)
            {
                DiscordClient.allowedToAddRoles[roles] = getWhitelisterBotConfig().getList("add-roles").get(roles).toString();
            }

            // set limited add roles
            DiscordClient.allowedToAddLimitedRoles = new String[getWhitelisterBotConfig().getList("limited-add-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToAddLimitedRoles.length; ++roles)
            {
                DiscordClient.allowedToAddLimitedRoles[roles] = getWhitelisterBotConfig().getList("limited-add-roles").get(roles).toString();
            }

            // easy whitelist check
            if(getWhitelisterBotConfig().getBoolean("use-easy-whitelist"))
            {
                getLogger().info("Checking for Easy Whitelist...");
                if(getServer().getPluginManager().getPlugin("EasyWhitelist") != null)
                {
                    getLogger().info("Easy Whitelist found! Will use over default whitelist command.");
                    easyWhitelist = getServer().getPluginManager().getPlugin("EasyWhitelist");
                    useEasyWhitelist = true;
                }
                else
                {
                    getLogger().warning("Easy Whitelist was not found but is enabled in the config. " +
                            "Falling back to default whitelist command.");
                }
            }

            int initializeSuccess = DiscordClient.InitializeClient(botToken);

            if(initializeSuccess == 0)
            {
                getLogger().info("Successfully initialized Discord client.");
            }
            else if(initializeSuccess == 1)
            {
                getLogger().severe("Discord Client failed to initialize, please check if your config file is valid.");
            }
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

    public static File getRemovedListFile()
    {
        return removedListFile;
    }

    public void ConfigSetup()
    {
        whitelisterBotConfigFile = new File(getDataFolder(), "discord-whitelister.yml");
        userListFile = new File(getDataFolder(), "user-list.yml");
        removedListFile = new File(getDataFolder(), "removed-list.yml");

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

        // check if entries exist in config file and add them if they are not. only log entry creation if it is not first time set up
        if(whitelisterBotConfigFile.exists())
        {
            if(getWhitelisterBotConfig().get("discord-bot-token") == null)
            {
                getWhitelisterBotConfig().set("discord-bot-token", "Discord bot token goes here, you can find it here: " +
                        "https://discordapp.com/developers/applications/");

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'discord-bot-token' was not found, adding it to the config...");
                }
            }

            // allowed to add and remove from the whitelist
            if(getWhitelisterBotConfig().get("add-remove-roles") == null)
            {
                List<String> tempAddRemoveRoles = Arrays.asList("Owner", "Admin");
                getWhitelisterBotConfig().set("add-remove-roles", tempAddRemoveRoles);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'add-remove-roles' was not found, adding it to the config...");
                }
            }

            // only allowed to add to the whitelist
            if(getWhitelisterBotConfig().get("add-roles") == null)
            {
                List<String> tempAddRoles = Arrays.asList("Mod", "Whitelister");
                getWhitelisterBotConfig().set("add-roles", tempAddRoles);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'add-roles' was not found, adding it to the config...");
                }
            }

            if(getWhitelisterBotConfig().get("username-validation") == null)
            {
                getWhitelisterBotConfig().set("username-validation", true);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'username-validation' was not found, adding it to the config...");
                }
            }

            if(getWhitelisterBotConfig().get("removed-list-enabled") == null)
            {
                getWhitelisterBotConfig().set("removed-list-enabled", true);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'removed-list-enabled' was not found, adding it to the config...");
                }
            }

            // if the limited whitelist feature should be enabled
            if(getWhitelisterBotConfig().get("limited-whitelist-enabled") == null)
            {
                getWhitelisterBotConfig().set("limited-whitelist-enabled", true);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'limited-whitelist-enabled' was not found, adding it to the config...");
                }
            }

            // the amount of times a non-staff user is allowed to whitelist
            if(getWhitelisterBotConfig().get("max-whitelist-amount") == null)
            {
                getWhitelisterBotConfig().set("max-whitelist-amount", 3);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'max-whitelist-amount' was not found, adding it to the config...");
                }
            }

            // roles that are allowed whitelist a limited amount of times
            if(getWhitelisterBotConfig().get("limited-add-roles") == null)
            {
                List<String> tempLimitedRoles = Arrays.asList("VIP", "LimitedWhitelister");
                getWhitelisterBotConfig().set("limited-add-roles", tempLimitedRoles);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'limited-add-roles' was not found, adding it to the config...");
                }
            }

            if(getWhitelisterBotConfig().get("target-text-channels") == null)
            {
                List<String> tempChannelIds = Arrays.asList("445666834382061569", "488450157881327616");
                getWhitelisterBotConfig().set("target-text-channels", tempChannelIds);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'target-text-channels' was not found, adding it to the config...");
                }
            }

            // easy whitelist support
            if(getWhitelisterBotConfig().get("use-easy-whitelist") == null)
            {
                getWhitelisterBotConfig().set("use-easy-whitelist", false);

                if (!configCreated) {
                    getPlugin().getLogger().warning("Entry 'use-easy-whitelist' was not found, adding it to the config...");
                }
            }

            if(getWhitelisterBotConfig().get("bot-enabled") == null)
            {
                getWhitelisterBotConfig().set("bot-enabled", true);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'bot-enabled' was not found, adding it to the config...");
                }
            }

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

        if(removedListCreated)
        {
            //getRemovedList().set("minecraftUsername", "discordRemoverID");
            try
            {
                getRemovedList().save(removedListFile.getPath());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
