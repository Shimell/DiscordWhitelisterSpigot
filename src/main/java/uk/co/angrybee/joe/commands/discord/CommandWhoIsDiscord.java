package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.UserList;

import java.util.List;
import java.util.Set;

public class CommandWhoIsDiscord {
    public static void ExecuteCommand(SlashCommandEvent event, Member target) {

        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();

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
        String userAsMention = "<@!" + target.getId() + ">"; // use this in-case the user has left the discord ? over using fetched member
        for (String discordId : keys) {
            if (discordId.equals(target.getId())) {
                List<?> registeredUsers = UserList.getRegisteredUsers(discordId);
                StringBuilder usersWhitelistedPlayers = new StringBuilder();
                for (Object targetWhitelistedPlayer : registeredUsers) {
                    if (targetWhitelistedPlayer instanceof String)
                        usersWhitelistedPlayers.append("- ").append((String) targetWhitelistedPlayer).append("\n");
                }

                EmbedBuilder idFoundMessage = DiscordClient.CreateEmbeddedMessage(("Found usernames linked to " + userAsMention),
                        (author.getAsMention() + ", the user " + userAsMention + " has the following usernames linked to their account:\n" + usersWhitelistedPlayers),
                        DiscordClient.EmbedMessageType.SUCCESS);

                DiscordClient.ReplyAndRemoveAfterSeconds(event, idFoundMessage.build());
                return;


            }
        }
        MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(("Could not find an usernames linked to " + target.getEffectiveName()),
                (author.getAsMention() + ", the user " + userAsMention +
                        " could not be found in the users list."),
                DiscordClient.EmbedMessageType.FAILURE).build();

        DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);

    }
}