package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.UserList;

import java.util.List;
import java.util.Set;

public class CommandClearname {
    public static void ExecuteCommand(SlashCommandInteractionEvent event, String mc_name) {

        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();

        // Clear Whitelists command
        // /clearname <targetName>


        // Check permissions
        if (!authorPermissions.isUserCanUseClear()) {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
            return;
        }  // Don't have permission



        /*
                if (DiscordClient.hideInfoCommandReplies) {
            return;
        }
        MessageEmbed messageEmbed;

        if (!DiscordWhitelister.getUseCustomPrefixes()) {
            messageEmbed = DiscordClient.CreateEmbeddedMessage("Clear Name Command", "Usage: `/clearname <minecraftUsername>`\n", DiscordClient.EmbedMessageType.INFO).build();
        } else {
            messageEmbed = DiscordClient.CreateEmbeddedMessage("Clear Name Command", "Usage: `" + DiscordClient.customClearNamePrefix + " <minecraftUsername>`\n", DiscordClient.EmbedMessageType.INFO).build();
        }
        DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);

        return;
*/


        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to clear " + mc_name + " from the whitelist");

        // Search for target name & linked ID
        boolean nameFound = false;
        String targetDiscordId = "";
        Set<String> keys = UserList.getUserList().getKeys(false);
        // Make sure the user list is not empty
        if (keys.size() > 0) {
            for (String userid : keys) {
                List<?> registeredUsers = UserList.getRegisteredUsers(userid);
                if (registeredUsers.contains(mc_name)) {
                    nameFound = true;
                    targetDiscordId = userid;
                    if (registeredUsers.size() > 1) {
                        registeredUsers.remove(mc_name); // Clear name
                        // Set the updated list in the config
                        UserList.getUserList().set(userid, registeredUsers);
                    } else { // Remove entirely

                        UserList.getUserList().set(userid, null);
                    }
                    UserList.SaveStore();
                    if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("unwhitelist-and-clear-perms-on-name-clear")) {
                        // Remove name from the whitelist
                        DiscordClient.UnWhitelist(mc_name);
                    }
                    break;
                }
            }
        }
        MessageEmbed messageEmbed;
        if (nameFound) {
            // Success message
            if (DiscordWhitelister.useCustomMessages) {
                String clearNameTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-success-title");
                String clearNameMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-success-message");

                clearNameMessage = clearNameMessage.replaceAll("\\{Sender}", author.getAsMention());
                clearNameMessage = clearNameMessage.replaceAll("\\{MinecraftUsername}", mc_name);
                clearNameMessage = clearNameMessage.replaceAll("\\{DiscordID}", "<@" + targetDiscordId + ">");

                clearNameTitle = clearNameTitle.replaceAll("\\{MinecraftUsername}", mc_name);
                messageEmbed = DiscordClient.CreateEmbeddedMessage(clearNameTitle, clearNameMessage, DiscordClient.EmbedMessageType.SUCCESS).build();
            } else {
                messageEmbed = DiscordClient.CreateEmbeddedMessage("Successfully Cleared Name", (author.getAsMention() + " successfully cleared username `" + mc_name +
                        "` from <@" + targetDiscordId + ">'s whitelisted users."), DiscordClient.EmbedMessageType.SUCCESS).build();
            }

        } else {
            // Name not found
            if (!DiscordWhitelister.useCustomMessages) {

                messageEmbed =
                        DiscordClient.CreateEmbeddedMessage((mc_name + " not Found"),
                                (author.getAsMention() + ", could not find name " + mc_name + " to clear in user list."), DiscordClient.EmbedMessageType.FAILURE).build();
            } else {
                String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-failure-title");
                String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-failure-message");
                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_name);
                customTitle = customTitle.replaceAll("\\{MinecraftUsername}", mc_name);

                messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
            }

        }
        DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
    }

}
