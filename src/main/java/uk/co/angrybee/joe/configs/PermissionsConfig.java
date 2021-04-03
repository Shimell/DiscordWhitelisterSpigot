package uk.co.angrybee.joe.configs;
import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;
import java.util.Collections;

public class PermissionsConfig extends Config {
    public PermissionsConfig() {
        fileName = "on-whitelist-permissions.yml";
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
            // test permission
            CheckEntry("perms-on-whitelist", Collections.singletonList("bukkit.command.tps"));
        }
    }
}
