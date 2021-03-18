package uk.co.angrybee.joe;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.angrybee.joe.commands.minecraft.CommandAbout;
import uk.co.angrybee.joe.commands.minecraft.CommandReload;
import uk.co.angrybee.joe.commands.minecraft.CommandStatus;
import uk.co.angrybee.joe.configs.*;
import uk.co.angrybee.joe.events.JoinLeaveEvents;
import uk.co.angrybee.joe.events.OnBanEvent;
import uk.co.angrybee.joe.events.OnWhitelistEvents;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.RemovedList;
import uk.co.angrybee.joe.stores.UserList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DiscordWhitelister extends JavaPlugin
{
    public static String botToken;

    private static boolean configCreated = false;

    public static boolean useCustomMessages = false;
    public static boolean useIdForRoles = false;
    public static boolean useCustomPrefixes = false;
    public static boolean showPlayerSkin = true;
    public static boolean showVanishedPlayersInCount = false;
    public static boolean useInGameAddRemoves = true;
    public static boolean useOnBanEvents = true;
    public static boolean useUltraPerms = false;
    public static boolean useLuckPerms = false;
    public static boolean useOnWhitelistCommands = false;
    public static boolean removeUnnecessaryMessages = false;

    public static boolean botEnabled;

    public static String[] bannedRoles;

    private static JavaPlugin thisPlugin;
    private static Server thisServer;
    private static Logger pluginLogger;

    // For not counting vanished players when other players join/leave
    private static int vanishedPlayersCount;

    public static int removeMessageWaitTime = 5;

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

        // Check for leavers if enabled
        DiscordClient.ServerLeaveStartupCheck();
        DiscordClient.RequiredRoleStartupCheck();

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

    public static FileConfiguration getCustomMessagesConfig() { return CustomMessagesConfig.getCustomMessagesConfig(); }

    public static Logger getPluginLogger() { return pluginLogger; }

    public static boolean getUseCustomPrefixes() { return useCustomPrefixes; }

    public static void addVanishedPlayer() { vanishedPlayersCount++; }

    public static void removeVanishedPlayer() { vanishedPlayersCount--; }

    public static int getOnlineUsers() { return thisPlugin.getServer().getOnlinePlayers().size() - vanishedPlayersCount; }

    public static int getMaximumAllowedPlayers() { return thisPlugin.getServer().getMaxPlayers(); }

    public static int InitBot(boolean firstInit)
    {
        if(firstInit)
            vanishedPlayersCount = 0;

        ConfigSetup();

        botToken = MainConfig.getMainConfig().getString("discord-bot-token");
        botEnabled = MainConfig.getMainConfig().getBoolean("bot-enabled");
        showPlayerSkin = MainConfig.getMainConfig().getBoolean("show-player-skin-on-whitelist");
        configCreated = MainConfig.configCreated;
        showVanishedPlayersInCount = MainConfig.getMainConfig().getBoolean("show-vanished-players-in-player-count");
        useInGameAddRemoves = MainConfig.getMainConfig().getBoolean("add-in-game-adds-and-removes-to-list");
        useOnBanEvents = MainConfig.getMainConfig().getBoolean("use-on-ban-events");
        removeUnnecessaryMessages = MainConfig.getMainConfig().getBoolean("remove-unnecessary-messages-from-whitelist-channel");

        removeMessageWaitTime = MainConfig.getMainConfig().getInt("seconds-to-remove-message-from-whitelist-channel");

        // Check for LuckPerms first
        if(MainConfig.getMainConfig().getBoolean("assign-perms-with-luck-perms"))
        {
            if(DiscordWhitelister.getPlugin().getServer().getPluginManager().getPlugin("LuckPerms") != null)
            {
                useLuckPerms = true;
                DiscordWhitelister.getPluginLogger().info("LuckPerms found!");
            }
            else
            {
                DiscordWhitelister.getPluginLogger().warning("LuckPerms was not found but is enabled in the config. Doing nothing...");
                useLuckPerms = false;
            }
        }
        if(MainConfig.getMainConfig().getBoolean("assign-perms-with-ultra-perms"))
        {
            if(DiscordWhitelister.getPlugin().getServer().getPluginManager().getPlugin("UltraPermissions") != null)
            {
                useUltraPerms = true;
                DiscordWhitelister.getPluginLogger().info("Ultra Permissions found!");
            }
            else
            {
                DiscordWhitelister.getPluginLogger().warning("Ultra Permissions was not found but is enabled in the config. Doing nothing...");
                useUltraPerms = false;
            }
        }

        if(MainConfig.getMainConfig().getBoolean("use-on-whitelist-commands"))
            useOnWhitelistCommands = true;

        // TODO: remove in favour of split versions
        DiscordClient.customWhitelistAddPrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-add-prefix").toLowerCase();
        DiscordClient.customWhitelistRemovePrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-remove-prefix").toLowerCase();
        DiscordClient.customClearNamePrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("clear-name-prefix").toLowerCase();
        DiscordClient.customLimitedWhitelistClearPrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("limited-whitelist-clear-prefix").toLowerCase();
        DiscordClient.customClearBanPrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("clear-ban-prefix").toLowerCase();

        // Split versions
        DiscordClient.customWhitelistAddPrefixSplit = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-add-prefix").toLowerCase().trim().split(" ");
        DiscordClient.customWhitelistRemovePrefixSplit = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-remove-prefix").toLowerCase().trim().split(" ");
        DiscordClient.customClearNamePrefixSplit = CustomPrefixConfig.getCustomPrefixesConfig().getString("clear-name-prefix").toLowerCase().trim().split(" ");
        DiscordClient.customLimitedWhitelistClearPrefixSplit = CustomPrefixConfig.getCustomPrefixesConfig().getString("limited-whitelist-clear-prefix").toLowerCase().trim().split(" ");
        DiscordClient.customClearBanPrefixSplit = CustomPrefixConfig.getCustomPrefixesConfig().getString("clear-ban-prefix").toLowerCase().trim().split(" ");
        DiscordClient.customWhoIsPrefix = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-whois-prefix").toLowerCase().trim().split(" ");

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

            // TODO: below role section could be moved to DiscordClient class
            useIdForRoles = MainConfig.getMainConfig().getBoolean("use-id-for-roles");

            // set add & remove roles
            DiscordClient.allowedToAddRemoveRoles = new String[MainConfig.getMainConfig().getList("add-remove-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToAddRemoveRoles.length; ++roles)
            {
                DiscordClient.allowedToAddRemoveRoles[roles] = MainConfig.getMainConfig().getList("add-remove-roles").get(roles).toString();
            }

            // set add roles
            DiscordClient.allowedToAddRoles = new String[MainConfig.getMainConfig().getList("add-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToAddRoles.length; ++roles)
            {
                DiscordClient.allowedToAddRoles[roles] = MainConfig.getMainConfig().getList("add-roles").get(roles).toString();
            }

            // set limited add roles
            DiscordClient.allowedToAddLimitedRoles = new String[MainConfig.getMainConfig().getList("limited-add-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToAddLimitedRoles.length; ++roles)
            {
                DiscordClient.allowedToAddLimitedRoles[roles] = MainConfig.getMainConfig().getList("limited-add-roles").get(roles).toString();
            }

            // Get banned roles
            if(useOnBanEvents)
            {
                List<String> tempBannedRoles = MainConfig.getMainConfig().getStringList("banned-roles");
                bannedRoles = new String[tempBannedRoles.size()];
                for(int i = 0; i < tempBannedRoles.size(); i++)
                {
                    bannedRoles[i] = tempBannedRoles.get(i);
                }
            }

            // Allowed to clear name roles
            DiscordClient.allowedToClearNamesRoles = new String[MainConfig.getMainConfig().getStringList("clear-command-roles").size()];
            for(int roles = 0; roles < DiscordClient.allowedToClearNamesRoles.length; roles++)
            {
                DiscordClient.allowedToClearNamesRoles[roles] = MainConfig.getMainConfig().getStringList("clear-command-roles").get(roles);
            }

            // All roles combined for role check
            DiscordClient.combinedRoles = Stream.of(DiscordClient.allowedToAddRemoveRoles, DiscordClient.allowedToAddRoles,
                    DiscordClient.allowedToAddLimitedRoles, DiscordClient.allowedToClearNamesRoles)
                    .flatMap(Stream::of).toArray(String[]::new);

            // Custom messages check
            useCustomMessages = MainConfig.getMainConfig().getBoolean("use-custom-messages");
            useCustomPrefixes = MainConfig.getMainConfig().getBoolean("use-custom-prefixes");

            int initSuccess = DiscordClient.InitializeClient(botToken);

            if(initSuccess == 1)
                return 1;


            // No need for an if here statement anymore as this code will not run if the client has not been initialized
            // Only attempt to set player count if the bot successfully initialized
            if(MainConfig.getMainConfig().getBoolean("show-player-count"))
            {
                // Register events if enabled
                thisServer.getPluginManager().registerEvents(new JoinLeaveEvents(), thisPlugin);

                // Set initial player count
                DiscordClient.SetPlayerCountStatus(getOnlineUsers());
            }

            // Register whitelist events if enabled
            if(useInGameAddRemoves)
                thisServer.getPluginManager().registerEvents(new OnWhitelistEvents(), thisPlugin);

            // Register ban events if enabled
            if(useOnBanEvents)
                thisServer.getPluginManager().registerEvents(new OnBanEvent(), thisPlugin);

            return 0;
        }

        return 0;
    }

    public static void ConfigSetup()
    {
        // Run this first, as it creates the root folder if it does not exist
        MainConfig.ConfigSetup();

        CustomPrefixConfig.ConfigSetup();
        CustomMessagesConfig.ConfigSetup();
        PermissionsConfig.ConfigSetup();
        OnWhitelistCommandsConfig.ConfigSetup();

        // Init Stores
        UserList.StoreSetup();
        InGameRemovedList.StoreSetup();
        RemovedList.StoreSetup();

        WhitelistedPlayers.Setup();
    }
}
