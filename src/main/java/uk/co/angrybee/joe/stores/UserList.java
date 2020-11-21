package uk.co.angrybee.joe.stores;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// user-list.yml
public class UserList
{
    private static File userListFile;
    private static FileConfiguration userList;

    public static File getUserListFile() { return userListFile; }
    public static FileConfiguration getUserList()
    {
        return userList;
    }
    public static List<?> getRegisteredUsers(String userId) { return userList.getList(userId); }

    public static void StoreSetup()
    {
        userListFile = new File(DiscordWhitelister.getPlugin().getDataFolder(), "user-list.yml");
        userList = new YamlConfiguration();

        if(!userListFile.exists())
            CreateStore();

        LoadStore();
        SaveStore();
    }

    private static void CreateStore()
    {
        try
        {
            userListFile.createNewFile();
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
            userList.load(userListFile);
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
            userList.save(userListFile.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static int getRegisteredUsersCount(String userId)
    {
        try
        {
            return getRegisteredUsers(userId).size();
        }
        catch(NullPointerException e)
        {
            return 0;
        }
    }

    public static void addRegisteredUser(String userId, String userToAdd) throws IOException
    {
        List <?> x = getRegisteredUsers(userId);
        List <String> newList = new ArrayList<>();

        for (Object o: x)
        {
            newList.add(o.toString());
        }

        newList.add(userToAdd);
        userList.set(userId, newList);
        SaveStore();
    }

    public static void resetRegisteredUsers(String userId) throws IOException
    {
        getUserList().set(userId, null);
        SaveStore();
    }
}
