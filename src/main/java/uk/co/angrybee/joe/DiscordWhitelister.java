package uk.co.angrybee.joe;

import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.angrybee.joe.Commands.CommandAbout;
import uk.co.angrybee.joe.Commands.CommandReload;
import uk.co.angrybee.joe.Commands.CommandStatus;
import uk.co.angrybee.joe.Configs.CustomMessagesConfig;
import uk.co.angrybee.joe.Configs.CustomPrefixConfig;
import uk.co.angrybee.joe.Configs.MainConfig;
import uk.co.angrybee.joe.Events.JoinLeaveEvents;
import uk.co.angrybee.joe.Events.OnWhitelistEvents;
import uk.co.angrybee.joe.Stores.InGameRemovedList;
import uk.co.angrybee.joe.Stores.RemovedList;
import uk.co.angrybee.joe.Stores.WhitelistedPlayers;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class DiscordWhitelister extends JavaPlugin
{
    private static File userListFile;

    private static FileConfiguration userList;

    public static String botToken;

    private static boolean configCreated = false;
    private static boolean userListCreated = false;

    public static boolean useCustomMessages = false;
    public static boolean useIdForRoles = false;
    public static boolean useCustomPrefixes = false;
    public static boolean showPlayerSkin = true;
    public static boolean showVanishedPlayersInCount = false;
    public static boolean useInGameAddRemoves = true;

    public static boolean botEnabled;

    private static JavaPlugin thisPlugin;
    private static Server thisServer;
    private static Logger pluginLogger;

    // For not counting vanished players when other players join/leave
    private static int vanishedPlayersCount;

    @Override
    public void onEnable()
    {
        thisPlugin = this;
        thisServer = thisPlugin.getServer();
        pluginLogger = thisPlugin.getLogger();

        int initSuccess = InitBot(true);

        if(initSuccess == 0)
        {
            pluginLogger.info("Successfully initialized Discord client");
        }
        else if(initSuccess == 1)
        {
            pluginLogger.severe("Discord Client failed to initialize, please check if your config file is valid");
        }

        this.getCommand("discordwhitelister").setExecutor(new CommandStatus());
        this.getCommand("discordwhitelisterabout").setExecutor(new CommandAbout());
        this.getCommand("discordwhitelisterreload").setExecutor(new CommandReload());
    }

    @Override
    public void onDisable()
    {
        DiscordClient.javaDiscordAPI.shutdownNow();
    }

    public static JavaPlugin getPlugin()
    {
        return thisPlugin;
    }

    public static FileConfiguration getWhitelisterBotConfig() { return MainConfig.getMainConfig(); }

    public static FileConfiguration getUserList()
    {
        return userList;
    }

    public static File getUserListFile()
    {
        return userListFile;
    }

    public static FileConfiguration getCustomMessagesConfig() { return CustomMessagesConfig.getCustomMessagesConfig(); }

    public static Logger getPluginLogger() { return pluginLogger; }

    public static List<?> getRegisteredUsers(String userId) { return userList.getList(userId); }

    public static int getRegisteredUsersCount(String userId) {
        try {
            return getRegisteredUsers(userId).size();
        } catch(NullPointerException ex) {
            return 0;
        }
    }

    public static boolean getUseCustomPrefixes() { return useCustomPrefixes; }

    public static void addRegisteredUser(String userId, String userToAdd) throws IOException {
        List <?> x = getRegisteredUsers(userId);
        List <String> newList = new ArrayList<>();
        for (Object o: x) {
            newList.add(o.toString());
        }
        newList.add(userToAdd);
        getUserList().set(userId, newList);
        getUserList().save(getUserListFile().getPath());
    }

    public static void resetRegisteredUsers(String userId) throws IOException {
        getUserList().set(userId, new ArrayList<>());
        getUserList().save(getUserListFile().getPath());
    }

    public static void addVanishedPlayer() { vanishedPlayersCount++; }

    public static void removeVanishedPlayer() { vanishedPlayersCount--; }

    public static int getOnlineUsers() { return thisPlugin.getServer().getOnlinePlayers().size() - vanishedPlayersCount; }

    public static int getMaximumAllowedPlayers() { return thisPlugin.getServer().getMaxPlayers(); }

    public static int InitBot(boolean firstInit)
    {
        userList = new YamlConfiguration();

        if(firstInit)
            vanishedPlayersCount = 0;

        ConfigSetup();

        botToken = getWhitelisterBotConfig().getString("discord-bot-token");
        botEnabled = getWhitelisterBotConfig().getBoolean("bot-enabled");
        showPlayerSkin = getWhitelisterBotConfig().getBoolean("show-player-skin-on-whitelist");
        configCreated = MainConfig.configCreated;
        showVanishedPlayersInCount = MainConfig.getMainConfig().getBoolean("show-vanished-players-in-player-count");
        useInGameAddRemoves = MainConfig.getMainConfig().getBoolean("add-in-game-adds-and-removes-to-list");


        DiscordClient.whitelistAddPrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-add-prefix");
        DiscordClient.whitelistRemovePrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-remove-prefix");

        if(!botEnabled)
        {
            pluginLogger.info("Bot is disabled as per the config, doing nothing");
        }
        else if(configCreated)
        {
            pluginLogger.info("Config newly created, please paste your bot token into the config file, doing nothing until next server start");
        }
        else
        {
            pluginLogger.info("Initializing Discord client...");

            useIdForRoles = getWhitelisterBotConfig().getBoolean("use-id-for-roles");

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

            // Custom messages check
            useCustomMessages = getWhitelisterBotConfig().getBoolean("use-custom-messages");
            useCustomPrefixes = getWhitelisterBotConfig().getBoolean("use-custom-prefixes");

            int initSuccess = DiscordClient.InitializeClient(botToken);

            if(initSuccess == 1)
                return 1;


            // No need for an if here statement anymore as this code will not run if the client has not been initialized
            // Only attempt to set player count if the bot successfully initialized
            if(getWhitelisterBotConfig().getBoolean("show-player-count"))
            {
                // Register events if enabled
                thisServer.getPluginManager().registerEvents(new JoinLeaveEvents(), thisPlugin);

                // Set initial player count
                DiscordClient.SetPlayerCountStatus(getOnlineUsers());
            }

            // Register whitelist events
            if(useInGameAddRemoves)
                thisServer.getPluginManager().registerEvents(new OnWhitelistEvents(), thisPlugin);

            return 0;
        }

        return 0;
    }

    public static void ConfigSetup()
    {
        File dataFolder = thisPlugin.getDataFolder();
        Logger pluginLogger = thisPlugin.getLogger();

        // Run this first, as it creates the root folder if it does not exist
        MainConfig.ConfigSetup();
        CustomPrefixConfig.ConfigSetup();
        CustomMessagesConfig.ConfigSetup();

        // Init Stores
        InGameRemovedList.StoreSetup();
        RemovedList.StoreSetup();

        WhitelistedPlayers.Setup();

        userListFile = new File(dataFolder, "user-list.yml");

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

            pluginLogger.info("User list created at: " + userListFile.getPath());
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
}
