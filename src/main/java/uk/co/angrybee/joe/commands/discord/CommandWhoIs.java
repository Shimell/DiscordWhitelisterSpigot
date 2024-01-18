package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.UserList;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.*;
public class CommandWhoIs {
    public static void ExecuteCommand(SlashCommandInteractionEvent event, String mc_name) {

        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();
        TextChannel channel = event.getChannel().asTextChannel();

        if (!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd()) {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
            return;
        }
/*
        int amountOfArgs = 0;
        StringBuilder exampleCommand = new StringBuilder();
        if (DiscordWhitelister.getUseCustomPrefixes()) {
            for (int i = 0; i < DiscordClient.customWhoIsPrefix.length; i++) {
                exampleCommand.append(DiscordClient.customWhoIsPrefix[i]).append(" ");
            }
        } else {
            for (int i = 0; i < DiscordClient.whitelistWhoIsPrefix.length; i++) {
                exampleCommand.append(DiscordClient.whitelistWhoIsPrefix[i]).append(" ");
            }
        }
        exampleCommand.append("<minecraftUsername>");

        MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("Too many arguments",
                (author.getAsMention() + ", expected 1 argument but found " + amountOfArgs + ".\n" +
                        "Example: " + exampleCommand.toString()), DiscordClient.EmbedMessageType.FAILURE).build();

        DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
*/

        boolean idFound = false;
        // Find the Discord Id linked to the whitelisted player
        Set<String> keys = UserList.getUserList().getKeys(false);
        for (
                String discordId : keys) {
            List<?> registeredUsers = UserList.getRegisteredUsers(discordId);
            for (Object name : registeredUsers) {
                if (name.equals(mc_name)) {
                    String userAsMention = "<@!" + discordId + ">"; // use this in-case the user has left the discord ? over using fetched member
                    StringBuilder usersWhitelistedPlayers = new StringBuilder();
                    for (Object targetWhitelistedPlayer : registeredUsers) {
                        if (targetWhitelistedPlayer instanceof String)
                            usersWhitelistedPlayers.append("- ").append((String) targetWhitelistedPlayer).append("\n");
                    }

                    EmbedBuilder idFoundMessage = DiscordClient.CreateEmbeddedMessage(("Found account linked to `" + mc_name + "`"),
                            (author.getAsMention() + ", the Minecraft username: `" + mc_name + "` is linked to " + userAsMention +
                                    ".\n\n Here is a list of their whitelisted players:\n" + usersWhitelistedPlayers),
                            DiscordClient.EmbedMessageType.SUCCESS);

                    User fetchedUser = DiscordClient.javaDiscordAPI.getUserById(discordId);

                    if (fetchedUser != null)
                        idFoundMessage.setThumbnail(fetchedUser.getAvatarUrl());
                    else
                        DiscordWhitelister.getPluginLogger().warning("Failed to fetch avatar linked to Discord ID: " + discordId);

                    DiscordClient.ReplyAndRemoveAfterSeconds(event, idFoundMessage.build());
                    idFound = true;
                    break;
                }
            }
        }
        if (!idFound) {
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(("Could not find an account linked to `" + mc_name + "`"),
                    (author.getAsMention() + ", the name: `" + mc_name +
                            "` could not be found in the users list. Please make sure that the Minecraft name is valid and whitelisted + linked to an ID before."),
                    DiscordClient.EmbedMessageType.FAILURE).build();

            DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
        }
    }
}