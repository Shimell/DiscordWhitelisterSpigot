package uk.co.angrybee.joe.configs;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class OnWhitelistCommandsConfig extends Config {
    OnWhitelistCommandsConfig() {
        fileName = "on-whitelist-commands.yml";
        file = new File(DiscordWhitelister.getPlugin().getDataFolder(), fileName);
        fileConfiguration = new YamlConfiguration();
    }

    public void ConfigSetup() {
        if (!file.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
    }

    private void CheckEntries() {
        if (file.exists()) {
            // Write comments
            if (fileCreated) {
                SaveConfig(); // save and load again
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write("# The list of commands that will be dispatched when a player gets whitelisted. (Use the following syntax: \n"
                            + "# \"%TYPE%:%COMMAND%\", being %TYPE% whether 'CONSOLE' or 'PLAYER' and the command without the slash (/)\n"
                            + "# placeholder %PLAYER% is supported here).\n"
                            + "# NOTE: The 'PLAYER' type will only work if the target whitelisted player is in the server at the time of command dispatch.");

                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LoadConfigFile();
            }

            CheckEntry("on-whitelist-commands", Arrays.asList("CONSOLE:gamemode adventure %PLAYER%", "CONSOLE:say hello testing"));
        }
    }
}
