package uk.co.angrybee.joe.Stores;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;

// Stores removed players that were removed in-game
// in-game-removed-list.yml
public class InGameRemovedList
{
    private static File removePlayersFile;
    private static FileConfiguration removedPlayersConfig;

    public static FileConfiguration getRemovedPlayers() { return removedPlayersConfig; }

    private static boolean storeCreated = false;

    public static void StoreSetup()
    {
        removePlayersFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "in-game-removed-list.yml");
        removedPlayersConfig = new YamlConfiguration();

        if(!removePlayersFile.exists())
            CreateStore();

        LoadStore();
        SaveStore();
    }

    private static void CreateStore()
    {
        try
        {
            removePlayersFile.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void LoadStore()
    {
        try
        {
            removedPlayersConfig.load(removePlayersFile);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    public static void SaveStore()
    {
        try
        {
            removedPlayersConfig.save(removePlayersFile.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void AddUserToStore(String nameToAdd, String commandIssuer)
    {
        if(removedPlayersConfig.get(nameToAdd) != null)
        {
            // Already in store, notify in console and update commandIssuer
            String oldCommandIssuer = removedPlayersConfig.getString(nameToAdd);
            DiscordWhitelister.getPluginLogger().warning(nameToAdd + " is already in the in-game removed list. Updating commandIssuer from " +
                    oldCommandIssuer + " to " + commandIssuer);
        }

        removedPlayersConfig.set(nameToAdd, commandIssuer);
        SaveStore();
    }

    public static void RemoveUserFromStore(String nameToRemove)
    {
        if(removedPlayersConfig.get(nameToRemove) != null)
        {
            removedPlayersConfig.set(nameToRemove, null);
            SaveStore();
        }
        else // Shouldn't ever happen
        {
            DiscordWhitelister.getPluginLogger().warning("Tried to remove " + nameToRemove + " from in-game-removed-list.yml, but " +
                    nameToRemove + " could not be found, doing nothing...");
        }
    }

    // Returns true if the player is in the store/list
    public static boolean CheckStoreForPlayer(String nameToCheck)
    {
        LoadStore();
        return removedPlayersConfig.get(nameToCheck) != null;
    }
}
