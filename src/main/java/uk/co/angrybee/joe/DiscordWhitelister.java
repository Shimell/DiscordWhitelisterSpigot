package uk.co.angrybee.joe;

import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import uk.co.angrybee.joe.Commands.CommandAbout;
import uk.co.angrybee.joe.Commands.CommandReload;
import uk.co.angrybee.joe.Commands.CommandStatus;
import uk.co.angrybee.joe.Events.JoinLeaveEvents;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class DiscordWhitelister extends JavaPlugin
{
    private static File whitelisterBotConfigFile;
    private static File userListFile;
    private static File removedListFile;
    private static File customMessagesFile;

    private static FileConfiguration whitelisterBotConfig;
    private static FileConfiguration userList;
    private static FileConfiguration removedList;
    private static FileConfiguration customMessagesConfig;

    // easy whitelist
    public static Plugin easyWhitelist;

    public static String botToken;

    private static boolean configCreated = false;
    private static boolean userListCreated = false;
    private static boolean removedListCreated = false;
    private static boolean customMessagesCreated = false;

    public static boolean useEasyWhitelist = false;
    public static boolean useCustomMessages = false;
    public static boolean useIdForRoles = false;

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

    public static FileConfiguration getRemovedList() { return removedList; }

    public static File getRemovedListFile()
    {
        return removedListFile;
    }

    public  static FileConfiguration getCustomMessagesConfig() { return customMessagesConfig; }

    public static List<?> getRegisteredUsers(String userId) { return userList.getList(userId); }

    public static int getRegisteredUsersCount(String userId) {
        try {
            return getRegisteredUsers(userId).size();
        } catch(NullPointerException ex) {
            return 0;
        }
    }

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
        whitelisterBotConfig = new YamlConfiguration();
        userList = new YamlConfiguration();
        removedList = new YamlConfiguration();
        customMessagesConfig = new YamlConfiguration();

        if(firstInit)
            vanishedPlayersCount = 0;

        ConfigSetup();

        botToken = getWhitelisterBotConfig().getString("discord-bot-token");
        botEnabled = getWhitelisterBotConfig().getBoolean("bot-enabled");

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

            // easy whitelist check
            if(getWhitelisterBotConfig().getBoolean("use-easy-whitelist"))
            {
                pluginLogger.info("Checking for Easy Whitelist...");
                if(thisServer.getPluginManager().getPlugin("EasyWhitelist") != null)
                {
                    pluginLogger.info("Easy Whitelist found! Will use over default whitelist command.");
                    easyWhitelist = thisServer.getPluginManager().getPlugin("EasyWhitelist");
                    useEasyWhitelist = true;
                }
                else
                {
                    pluginLogger.warning("Easy Whitelist was not found but is enabled in the config. " +
                            "Falling back to default whitelist command.");
                }
            }

            // Custom messages check
            if(getWhitelisterBotConfig().getBoolean("use-custom-messages"))
                useCustomMessages = true;

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

            return 0;
        }

        return 0;
    }

    public static void ConfigSetup()
    {
        File dataFolder = thisPlugin.getDataFolder();
        Logger pluginLogger = thisPlugin.getLogger();

        whitelisterBotConfigFile = new File(dataFolder, "discord-whitelister.yml");
        userListFile = new File(dataFolder, "user-list.yml");
        removedListFile = new File(dataFolder, "removed-list.yml");
        customMessagesFile = new File(dataFolder, "custom-messages.yml");

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

            pluginLogger.info("Configuration file created at: " + whitelisterBotConfigFile.getPath() +
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

            pluginLogger.info("Removed list created at: " + removedListFile.getPath());
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

        if(!customMessagesFile.exists())
        {
            try
            {
                customMessagesFile.createNewFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            pluginLogger.info("Custom messages file has been created at: " + customMessagesFile.getPath());
            customMessagesCreated = true;
        }

        try
        {
            getCustomMessagesConfig().load(customMessagesFile);
        }
        catch(IOException | InvalidConfigurationException e)
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

            // If adding the whitelisted role to the discord user is enabled
            if(getWhitelisterBotConfig().get("whitelisted-role-auto-add") == null)
            {
                getWhitelisterBotConfig().set("whitelisted-role-auto-add", false);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelisted-role-auto-add' was not found, adding it to the config...");
                }
            }

            // If removing the whitelisted role from the discord user is enabled
            if(getWhitelisterBotConfig().get("whitelisted-role-auto-remove") == null)
            {
                getWhitelisterBotConfig().set("whitelisted-role-auto-remove", false);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelisted-role-auto-remove' was not found, adding it to the config...");
                }
            }

            // The name of the role to add/remove to the user
            if(getWhitelisterBotConfig().get("whitelisted-role") == null)
            {
                getWhitelisterBotConfig().set("whitelisted-role", "Whitelisted");

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelisted-role' was not found, adding it to the config...");
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

            if(getWhitelisterBotConfig().get("show-player-count") == null)
            {
                getWhitelisterBotConfig().set("show-player-count", true);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'show-player-count' was not found, adding it to the config...");
                }
            }

            if(getWhitelisterBotConfig().get("use-custom-messages") == null)
            {
                getWhitelisterBotConfig().set("use-custom-messages", false);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'use-custom-messages' was not found, adding it to the config...");
                }
            }

            if(getWhitelisterBotConfig().get("use-id-for-roles") == null)
            {
                getWhitelisterBotConfig().set("use-id-for-roles", false);

                if(!configCreated)
                {
                    getPlugin().getLogger().warning("Entry 'use-id-for-roles' was not found, adding it to the config...");
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

        if(customMessagesFile.exists())
        {
            /* TODO: add a YAML comment (#) explaining the config file params
            NOTE: only {params} in the original messages will be evaluated. For example: using {MaxWhitelistAmount} in the "insufficient-permissions" String will not work as it was never in the original message.
            {Sender} == author.getAsMention(), {RemainingWhitelists} == (maxWhitelistAmount - timesWhitelisted), {MaxWhitelistAmount} == maxWhitelistAmount,
            {MinecraftUsername} == finalNameToAdd/Remove, {StaffMember} == DiscordWhitelister.getRemovedList().get(finalNameToAdd), {AddRemoveRoles} = DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles")
            Internal error messages & info messages will remain uneditable. No need to add custom remove failure messages as it should never happen */

            if(getCustomMessagesConfig().getString("insufficient-permissions-title") == null)
            {
                getCustomMessagesConfig().set("insufficient-permissions-title", "Insufficient Permissions");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'insufficient-permissions-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("insufficient-permissions") == null)
            {
                getCustomMessagesConfig().set("insufficient-permissions", "{Sender}, you do not have permission to use this command.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'insufficient-permissions' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("insufficient-permissions-remove-title") == null)
            {
                getCustomMessagesConfig().set("insufficient-permissions-remove-title", "Insufficient Permissions");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'insufficient-permissions-remove-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("insufficient-permissions-remove") == null)
            {
                getCustomMessagesConfig().set("insufficient-permissions-remove", "{Sender}, you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: {AddRemoveRoles}; or get the owner to move your role to 'add-remove-roles' in the config.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'insufficient-permissions-remove' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("no-whitelists-remaining-title") == null)
            {
                getCustomMessagesConfig().set("no-whitelists-remaining-title", "No Whitelists Remaining");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'no-whitelists-remaining-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("no-whitelists-remaining") == null)
            {
                getCustomMessagesConfig().set("no-whitelists-remaining", "{Sender}, unable to whitelist. You have **{RemainingWhitelists} out of {MaxWhitelistAmount}** whitelists remaining.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'insufficient-permissions' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("invalid-characters-warning-title") == null)
            {
                getCustomMessagesConfig().set("invalid-characters-warning-title", "Invalid Username");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'invalid-characters-warning-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("invalid-characters-warning") == null)
            {
                getCustomMessagesConfig().set("invalid-characters-warning", "{Sender}, the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'invalid-characters-warning' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("invalid-length-warning-title") == null)
            {
                getCustomMessagesConfig().set("invalid-length-warning-title", "Invalid Username");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'invalid-length-warning-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("invalid-length-warning") == null)
            {
                getCustomMessagesConfig().set("invalid-length-warning", "{Sender}, the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'invalid-length-warning' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("already-on-whitelist-title") == null)
            {
                getCustomMessagesConfig().set("already-on-whitelist-title", "User already on the whitelist");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'already-on-whitelist-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("already-on-whitelist") == null)
            {
                getCustomMessagesConfig().set("already-on-whitelist", "{Sender}, cannot add user as `{MinecraftUsername}` is already on the whitelist!");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'already-on-whitelist' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("user-was-removed-title") == null)
            {
                getCustomMessagesConfig().set("user-was-removed-title", "This user was previously removed by a staff member");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'user-was-removed-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("user-was-removed") == null)
            {
                getCustomMessagesConfig().set("user-was-removed", "{Sender}, this user was previously removed by a staff member ({StaffMember}). Please ask a user with higher permissions to add this user.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'user-was-removed' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("whitelists-remaining-title") == null)
            {
                getCustomMessagesConfig().set("whitelists-remaining-title", "Whitelists Remaining");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelists-remaining-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("whitelists-remaining") == null)
            {
                getCustomMessagesConfig().set("whitelists-remaining", "You have **{RemainingWhitelists} out of {MaxWhitelistAmount}** whitelists remaining.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelists-remaining' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("whitelist-success-title") == null)
            {
                getCustomMessagesConfig().set("whitelist-success-title", "{MinecraftUsername} is now whitelisted!");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelist-success-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("whitelist-success") == null)
            {
                getCustomMessagesConfig().set("whitelist-success", "{Sender} has added `{MinecraftUsername}` to the whitelist.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelist-success' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("whitelist-failure-title") == null)
            {
                getCustomMessagesConfig().set("whitelist-failure-title", "Failed to whitelist");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelist-failure-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("whitelist-failure") == null)
            {
                getCustomMessagesConfig().set("whitelist-failure", "{Sender}, failed to add `{MinecraftUsername}` to the whitelist. This is most likely due to an invalid Minecraft username.");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'whitelist-failure' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("user-not-on-whitelist-title") == null)
            {
                getCustomMessagesConfig().set("user-not-on-whitelist-title", "This user is not on the whitelist");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'user-not-on-whitelist-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("user-not-on-whitelist") == null)
            {
                getCustomMessagesConfig().set("user-not-on-whitelist", "{Sender}, cannot remove user as `{MinecraftUsername}` is not on the whitelist!");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'user-not-on-whitelist' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("remove-success-title") == null)
            {
                getCustomMessagesConfig().set("remove-success-title", "{MinecraftUsername} has been removed");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'remove-success-title' was not found, adding it to the config...");
                }
            }

            if(getCustomMessagesConfig().getString("remove-success") == null)
            {
                getCustomMessagesConfig().set("remove-success", "{Sender} has removed {MinecraftUsername} from the whitelist");

                if(!customMessagesCreated)
                {
                    getPlugin().getLogger().warning("Entry 'remove-success' was not found, adding it to the config...");
                }
            }

            try
            {
                getCustomMessagesConfig().save((customMessagesFile.getPath()));
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
