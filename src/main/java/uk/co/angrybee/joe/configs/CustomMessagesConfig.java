package uk.co.angrybee.joe.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import uk.co.angrybee.joe.DiscordWhitelister;

import java.io.File;

// custom-messages.yml
public class CustomMessagesConfig extends Config
{
    public CustomMessagesConfig(){
        fileName = "custom-messages.yml";
        file = new File(DiscordWhitelister.getPlugin().getDataFolder(), fileName);
        fileConfiguration = new YamlConfiguration();
    }

    public void ConfigSetup()
    {
        if(!file.exists())
            CreateConfig();

        LoadConfigFile();
        CheckEntries();
        SaveConfig();
    }

    private void CheckEntries()
    {
        /* TODO: add a YAML comment (#) explaining the config file params
        NOTE: only {params} in the original messages will be evaluated. For example: using {MaxWhitelistAmount} in the "insufficient-permissions" String will not work as it was never in the original message.
        {Sender} == author.getAsMention(), {RemainingWhitelists} == (maxWhitelistAmount - timesWhitelisted), {MaxWhitelistAmount} == maxWhitelistAmount,
        {MinecraftUsername} == finalNameToAdd/Remove, {StaffMember} == DiscordWhitelister.getRemovedList().get(finalNameToAdd), {AddRemoveRoles} = DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles")
        Internal error messages & info messages will remain uneditable. No need to add custom remove failure messages as it should never happen */

        if(file.exists())
        {
            CheckEntry("insufficient-permissions-title", "Insufficient Permissions");
            CheckEntry("insufficient-permissions", "{Sender}, you do not have permission to use this command.");

            CheckEntry("insufficient-permissions-remove-title", "Insufficient Permissions");
            CheckEntry("insufficient-permissions-remove", "{Sender}, you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: {AddRemoveRoles}; or get the owner to move your role to 'add-remove-roles' in the config.");

            CheckEntry("no-whitelists-remaining-title", "No Whitelists Remaining");
            CheckEntry("no-whitelists-remaining", "{Sender}, unable to whitelist. You have **{RemainingWhitelists} out of {MaxWhitelistAmount}** whitelists remaining.");

            CheckEntry("invalid-characters-warning-title", "Invalid Username");
            CheckEntry("invalid-characters-warning", "{Sender}, the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**.");

            CheckEntry("invalid-length-warning-title", "Invalid Username");
            CheckEntry("invalid-length-warning", "{Sender}, the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**.");

            CheckEntry("already-on-whitelist-title", "User already on the whitelist");
            CheckEntry("already-on-whitelist", "{Sender}, cannot add user as `{MinecraftUsername}` is already on the whitelist!");

            CheckEntry("user-was-removed-title", "This user was previously removed by a staff member");
            CheckEntry("user-was-removed", "{Sender}, this user was previously removed by a staff member ({StaffMember}). Please ask a user with higher permissions to add this user.");

            CheckEntry("user-was-removed-in-game-title", "This user was previously removed by a staff member");
            CheckEntry("user-was-removed-in-game", "{Sender}, this user was previously removed by a staff member in-game ({StaffMember}). Please ask a user with higher permissions to add this user.");

            CheckEntry("whitelists-remaining-title", "Whitelists Remaining");
            CheckEntry("whitelists-remaining", "You have **{RemainingWhitelists} out of {MaxWhitelistAmount}** whitelists remaining.");

            CheckEntry("whitelist-success-title", "{MinecraftUsername} is now whitelisted!");
            CheckEntry("whitelist-success", "{Sender} has added `{MinecraftUsername}` to the whitelist.");

            CheckEntry("whitelist-failure-title", "Failed to whitelist");
            CheckEntry("whitelist-failure", "{Sender}, failed to add `{MinecraftUsername}` to the whitelist. This is most likely due to an invalid Minecraft username.");

            CheckEntry("user-not-on-whitelist-title", "This user is not on the whitelist");
            CheckEntry("user-not-on-whitelist", "{Sender}, cannot remove user as `{MinecraftUsername}` is not on the whitelist!");

            CheckEntry("remove-success-title", "{MinecraftUsername} has been removed");
            CheckEntry("remove-success", "{Sender} has removed {MinecraftUsername} from the whitelist");

            CheckEntry("banned-title", "You have been banned!");
            CheckEntry("banned-message", "{Sender}, you cannot use this bot as you have been banned!");

            CheckEntry("clear-name-success-title", "Successfully Cleared `{MinecraftUsername}`");
            CheckEntry("clear-name-success-message", "{Sender} successfully cleared username `{MinecraftUsername}` from {DiscordID}'s whitelisted users.");

            CheckEntry("clear-name-failure-title",  "{MinecraftUsername} not Found");
            CheckEntry("clear-name-failure-message", "{Sender}, could not find name `{MinecraftUsername}` to clear in user list.");

            CheckEntry("clear-ban-success-title",  "Successfully Cleared `{MinecraftUsername}`");
            CheckEntry("clear-ban-success-message", "{Sender} has successfully cleared `{MinecraftUsername}` from the removed list(s).");

            CheckEntry("clear-ban-failure-title",  "Failed to clear `{MinecraftUsername}`");
            CheckEntry("clear-ban-failure-message", "{Sender}, `{MinecraftUsername}` cannot be found in any of the removed lists!");

            CheckEntry("instructional-message-title", "How to Whitelist");
            CheckEntry("instructional-message", "Use `/whitelist add <minecraftUsername>` to whitelist yourself. In the case of whitelisting an incorrect name, please contact a staff member to clear it from the whitelist.");

            CheckEntry("whitelist-clear-success-title",  "Successfully Removed {Sender}'s Whitelisted Entries");
            CheckEntry("whitelist-clear-success-message", "{Sender} successfully removed the following users from the whitelist: \n{RemovedEntries}\nYou now have **{MaxWhitelistAmount} whitelist(s) remaining**.");

            CheckEntry("whitelist-clear-failure-title",  "No Entries to Remove");
            CheckEntry("whitelist-clear-failure-message", "{Sender}, you do not have any whitelisted entries to remove. ");

            CheckEntry("command-channel-title", "This Channel is for Commands Only");
            CheckEntry("command-channel-message", "{Sender}, this channel is for commands only, please use another channel.");
        }
    }
}
