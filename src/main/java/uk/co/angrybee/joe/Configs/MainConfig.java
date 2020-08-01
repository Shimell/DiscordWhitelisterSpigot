package uk.co.angrybee.joe.Configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

// discord-whitelister.yml
public class MainConfig
{
    private static File whitelisterBotConfigFile;
    private static FileConfiguration whitelisterBotConfig;

    public static FileConfiguration getMainConfig() { return whitelisterBotConfig; }

    public static boolean configCreated = false;

    public static void ConfigSetup()
    {
        whitelisterBotConfigFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "discord-whitelister.yml");
        whitelisterBotConfig = new YamlConfiguration();

        // Create root folder for configs if it does not exist
        if(!whitelisterBotConfigFile.getParentFile().exists())
            whitelisterBotConfigFile.getParentFile().mkdirs();

        if(!whitelisterBotConfigFile.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
    }

    private static void CreateConfig()
    {
        try
        {
            whitelisterBotConfigFile.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        DiscordWhitelister.getPluginLogger().info("Configuration file created at: " + whitelisterBotConfigFile.getPath() +
                ", please edit this else the plugin will not work!");
        configCreated = true;
    }

    private static void LoadConfigFile()
    {
        try
        {
            whitelisterBotConfig.load(whitelisterBotConfigFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntries()
    {
        CheckEntry("bot-enabled", true);

        CheckEntry("discord-bot-token",
                "Discord bot token goes here, you can find it here: https://discordapp.com/developers/applications/" );

        CheckEntry("use-id-for-roles", false);

        // Allowed to add and remove from the whitelist
        CheckEntry("add-remove-roles", Arrays.asList("Owner", "Admin"));

        // Only allowed to add to the whitelist
        CheckEntry("add-roles", Arrays.asList("Mod", "Whitelister"));

        // Roles that are allowed whitelist a limited amount of times
        CheckEntry("limited-add-roles", Collections.singletonList("LimitedWhitelister"));

        // The roles to add/remove when whitelisted/removed
        CheckEntry("whitelisted-roles", Collections.singletonList("Whitelisted"));

        CheckEntry("target-text-channels", Arrays.asList("000000000000000000", "111111111111111111"));

        // EasyWhitelist support (https://www.spigotmc.org/resources/easywhitelist-name-based-whitelist.65222/)
        CheckEntry("use-easy-whitelist", false);

        // If adding the whitelisted role to the discord user is enabled
        CheckEntry("whitelisted-role-auto-add", false);

        // If removing the whitelisted role from the discord user is enabled
        CheckEntry("whitelisted-role-auto-remove", false);

        // If the limited whitelist feature should be enabled
        CheckEntry("limited-whitelist-enabled", true);

        // The amount of times a non-staff user is allowed to whitelist
        CheckEntry("max-whitelist-amount", 3);

        CheckEntry("username-validation", true);

        CheckEntry("removed-list-enabled", true);

        CheckEntry("add-in-game-adds-and-removes-to-list", true);

        CheckEntry("use-custom-messages", false);

        CheckEntry("use-custom-prefixes", false);

        CheckEntry("show-player-skin-on-whitelist", true);

        CheckEntry("show-player-count", true);

        CheckEntry("show-vanished-players-in-player-count", false);

        // Disable checking if username exists
        CheckEntry("offline-mode", false);

        // Remove old role entry if found, move role to new array (for people with v1.3.6 or below)
        if(whitelisterBotConfig.get("whitelisted-role") != null)
        {
            DiscordWhitelister.getPluginLogger().warning("Found whitelisted-role entry, moving over to whitelisted-roles. Please check your config to make sure the change is correct");
            // Get the role from the old entry
            String whitelistedRoleTemp = whitelisterBotConfig.getString("whitelisted-role");
            // Assign role from old entry to new entry as a list
            whitelisterBotConfig.set("whitelisted-roles", Collections.singletonList(whitelistedRoleTemp));

            // Remove now un-used entry
            whitelisterBotConfig.set("whitelisted-role", null);

            // Note to users that id for roles now affects the new entry
            if(whitelisterBotConfig.getBoolean("use-id-for-roles"))
            {
                DiscordWhitelister.getPluginLogger().severe("You have 'use-id-for-roles' enabled please change the whitelisted-roles to ids as they now follow this setting");
            }
        }
    }

    private static void SaveConfig()
    {
        try
        {
            whitelisterBotConfig.save(whitelisterBotConfigFile.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void CheckEntry(String entryName, Object passedValue)
    {
        if(whitelisterBotConfig.get(entryName) == null)
        {
            whitelisterBotConfig.set(entryName, passedValue);

            if(!configCreated)
                DiscordWhitelister.getPluginLogger().warning("Entry '" + entryName + "' was not found, adding it to the config...");
        }
    }
}
