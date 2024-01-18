package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.RemovedList;

public class CommandClearban {
    public static void ExecuteCommand(SlashCommandInteractionEvent event, String mc_user) {

        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();
        TextChannel channel = event.getChannel().asTextChannel();


        if (authorPermissions.isUserCanUseClear()) {
            // Check if empty command
/*
                if (!hideInfoCommandReplies) {
                    return;
                }

                // Send info message
                if (!DiscordWhitelister.getUseCustomPrefixes()) {
                    MessageEmbed messageEmbed = CreateEmbeddedMessage("Clear Ban Command", "Usage: `!clearban <minecraftUsername>`\n", DiscordClient.EmbedMessageType.INFO).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                } else {
                    MessageEmbed messageEmbed = CreateEmbeddedMessage("Clear Ban Command", "Usage: `" + customClearBanPrefix + " <minecraftUsername>`\n", DiscordClient.EmbedMessageType.INFO).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                }

                return;
*/

            // If command is not empty check for args
            // String[] splitMessage = messageContents.toLowerCase().trim().split(" ");

            // Check both removed lists for target name
            boolean nameFoundInLists = false;

            // Remove name from removed list if found
            if (RemovedList.CheckStoreForPlayer(mc_user)) {
                RemovedList.getRemovedPlayers().set(mc_user, null);
                RemovedList.SaveStore();

                nameFoundInLists = true;
            }

            if (InGameRemovedList.CheckStoreForPlayer(mc_user)) {
                InGameRemovedList.RemoveUserFromStore(mc_user);

                nameFoundInLists = true;
            }

            if (nameFoundInLists) {

                if (!DiscordWhitelister.useCustomMessages) {
                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(("Successfully Cleared `" + mc_user + "`"),
                            (author.getAsMention() + " has successfully cleared `" + mc_user + "` from the removed list(s)."), DiscordClient.EmbedMessageType.SUCCESS).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                } else {
                    String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-success-title");
                    String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-success-message");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                    customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
                    customTitle = customTitle.replaceAll("\\{MinecraftUsername}", mc_user);

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                }

            } else {
                if (!DiscordWhitelister.useCustomMessages) {
                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(("Failed to Clear `" + mc_user + "`"),
                            (author.getAsMention() + ", `" + mc_user + "` cannot be found in any of the removed lists!"), DiscordClient.EmbedMessageType.FAILURE).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                } else {
                    String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-failure-title");
                    String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-failure-message");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                    customMessage = customMessage.replaceAll("\\{MinecraftUsername}", mc_user);
                    customTitle = customTitle.replaceAll("\\{MinecraftUsername}", mc_user);

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                }

            }
        } else {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
        }
    }
}

