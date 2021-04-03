package uk.co.angrybee.joe.configs;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.IOException;

public class Config {

    protected String fileName;
    protected File file;
    protected FileConfiguration fileConfiguration;
    protected boolean fileCreated = false;

    public FileConfiguration getFileConfiguration() { return fileConfiguration; }


    protected void CreateConfig()
    {
        try
        {
            file.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        DiscordWhitelister.getPluginLogger().info("Created file " + fileName);
        fileCreated = true;
    }

    protected void LoadConfigFile()
    {
        try
        {
            fileConfiguration.load(file);
        }
        catch (IOException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    protected void SaveConfig()
    {
        try
        {
            fileConfiguration.save(file.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void CheckEntry(String entryName, Object passedValue)
    {
        if(fileConfiguration.get(entryName) == null)
        {
            fileConfiguration.set(entryName, passedValue);

            if(!fileCreated)
                DiscordWhitelister.getPluginLogger().warning("Entry '" + entryName + "' was not found, adding it to "+fileName);
        }
    }

}
