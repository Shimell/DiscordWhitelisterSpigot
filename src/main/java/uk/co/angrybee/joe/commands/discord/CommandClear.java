package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.UserList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CommandClear {
    public static void ExecuteCommand(SlashCommandEvent event) {
        // Clear whitelists for limited-whitelisters
        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();
        TextChannel channel = event.getTextChannel();

        if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("allow-limited-whitelisters-to-unwhitelist-self")) {
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("This Command is disabled",
                    "If staff members need to clear a name from the whitelist please use `/clearname <mcName>`.", DiscordClient.EmbedMessageType.INFO).build();
            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            return;
        }
        // just inform staff, can add custom messages later if really needed
        if (authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserIsBanned() || authorPermissions.isUserCanAdd() && !authorPermissions.isUserIsBanned()) {
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("This Command is Only Available for Limited Whitelister Roles",
                    "If staff members need to clear a name from the whitelist please use `/clearname <mcName>`.", DiscordClient.EmbedMessageType.INFO).build();
            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
            return;
        }

        if (authorPermissions.isUserHasLimitedAdd() && !authorPermissions.isUserIsBanned()) {
            List<?> ls = UserList.getRegisteredUsers(author.getId());

            // check for names whitelisted
            if (ls != null) {
                for (Object minecraftNameToRemove : ls) {
                    DiscordClient.UnWhitelist(minecraftNameToRemove.toString());
                }

                try {
                    UserList.resetRegisteredUsers(author.getId());
                } catch (IOException e) {
                    DiscordWhitelister.getPluginLogger().severe("Failed to remove" + author.getId() + "'s entries.");
                    e.printStackTrace();
                    return;
                }

                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") triggered whitelist clear. " +
                        "Successfully removed their whitelisted entries from the user list.");

                // Log in Discord channel
                if (!DiscordWhitelister.useCustomMessages) {
                    StringBuilder message = new StringBuilder(author.getAsMention() + " successfully removed the following users from the whitelist: \n");
                    for (Object minercaftName : ls) {
                        message.append("- ").append(minercaftName.toString()).append("\n");
                    }
                    message.append("\n You now have **").append(DiscordClient.maxWhitelistAmount).append(" whitelist(s) remaining**.");

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(("Successfully Removed " + author.getName() + "'s Whitelisted Entries"),
                            message.toString(), DiscordClient.EmbedMessageType.FAILURE).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                } else {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-success-title");
                    customTitle = customTitle.replaceAll("\\{Sender}", author.getName());

                    StringBuilder removedNames = new StringBuilder();
                    for (Object minercaftName : ls) {
                        removedNames.append("- ").append(minercaftName.toString()).append("\n");
                    }

                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-success-message");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                    customMessage = customMessage.replaceAll("\\{RemovedEntries}", removedNames.toString());
                    customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(DiscordClient.maxWhitelistAmount));

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.SUCCESS).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                }

                if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("whitelisted-role-auto-remove")) {
                    // Find all servers bot is in, remove whitelisted roles
                    for (int i = 0; i < DiscordClient.javaDiscordAPI.getGuilds().size(); i++) {
                        // Remove the whitelisted role(s)
                        DiscordClient.RemoveRolesFromUser(DiscordClient.javaDiscordAPI.getGuilds().get(i), author.getId(), Arrays.asList(DiscordClient.whitelistedRoleNames));
                    }
                }
            } else {
                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") triggered whitelist clear. " +
                        "Could not remove any whitelisted entries as they do not have any.");

                // Log in Discord channel
                if (!DiscordWhitelister.useCustomMessages) {
                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("No Entries to Remove",
                            (author.getAsMention() + ", you do not have any whitelisted entries to remove."), DiscordClient.EmbedMessageType.FAILURE).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                } else {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-failure-title");
                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-failure-message");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                }
            }
            return;
        }

        if (!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd() && !authorPermissions.isUserHasLimitedAdd() || authorPermissions.isUserIsBanned()) {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
        }

    }
}