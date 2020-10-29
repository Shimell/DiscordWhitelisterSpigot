package uk.co.angrybee.joe.Stores;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;

//getRemovedList().set("minecraftUsername", "discordRemoverID");

// Stores removed players that were removed through Discord
// removed-list.yml
public class RemovedList
{
    private static File removePlayersFile;
    private static FileConfiguration removedPlayersConfig;

    public static FileConfiguration getRemovedPlayers() { return removedPlayersConfig; }

    public static void StoreSetup()
    {
        removePlayersFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "removed-list.yml");
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

    // Returns true if the player is in the store/list
    public static boolean CheckStoreForPlayer(String nameToCheck)
    {
        LoadStore();
        return removedPlayersConfig.get(nameToCheck) != null;
    }
}
