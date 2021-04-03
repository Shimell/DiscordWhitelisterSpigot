package uk.co.angrybee.joe.commands.discord;

import com.sun.org.apache.xpath.internal.operations.Bool;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.AccountTypeException;
import org.yaml.snakeyaml.Yaml;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.configs.MainConfig;
import uk.co.angrybee.joe.stores.UserList;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CommandWhoIs {
    public static void ExecuteCommand(MessageReceivedEvent messageReceivedEvent, String[] splitMessage) {
        AuthorPermissions authorPermissions = new AuthorPermissions(messageReceivedEvent);
        User author = messageReceivedEvent.getAuthor();
        TextChannel channel = messageReceivedEvent.getTextChannel();

        if (!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd()) {
            DiscordClient.QueueAndRemoveAfterSeconds(channel, DiscordClient.CreateInsufficientPermsMessage(author));
            return;
        }

        // TODO make 1 function like this that multiple commands can call on
        if (DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length > (DiscordClient.customWhoIsPrefix.length + 1)
                || !DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length > (DiscordClient.whitelistWhoIsPrefix.length + 1)) {
            int amountOfArgs = 0;
            if (DiscordWhitelister.getUseCustomPrefixes())
                amountOfArgs = splitMessage.length - (DiscordClient.customWhoIsPrefix.length);
            else
                amountOfArgs = splitMessage.length - (DiscordClient.whitelistWhoIsPrefix.length);

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

            DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
            return;
        }

        String nameToCheck = "";
        if (DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length >= DiscordClient.customWhoIsPrefix.length + 1)
            nameToCheck = splitMessage[DiscordClient.customWhoIsPrefix.length];

        if (!DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length >= DiscordClient.whitelistWhoIsPrefix.length + 1)
            nameToCheck = splitMessage[DiscordClient.whitelistWhoIsPrefix.length];

        if (DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length == DiscordClient.customWhoIsPrefix.length
                || !DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length == DiscordClient.whitelistWhoIsPrefix.length || nameToCheck.isEmpty()) {
            if (!DiscordClient.hideInfoCommandReplies)
                return;

            DiscordClient.QueueAndRemoveAfterSeconds(channel, DiscordClient.whoIsInfo);
            return;
        }

        boolean idFound = false;
        // Find the Discord Id linked to the whitelisted player
        Set<String> keys = UserList.getUserList().getKeys(false);
        for (String discordId : keys) {
            List<?> registeredUsers = UserList.getRegisteredUsers(discordId);
            for (Object mc_name : registeredUsers) {
                if (mc_name.equals(nameToCheck)) {
                    String userAsMention = "<@!" + discordId + ">"; // use this in-case the user has left the discord ? over using fetched member
                    StringBuilder usersWhitelistedPlayers = new StringBuilder();
                    for (Object targetWhitelistedPlayer : registeredUsers) {
                        if (targetWhitelistedPlayer instanceof String)
                            usersWhitelistedPlayers.append("- ").append((String) targetWhitelistedPlayer).append("\n");
                    }

                    EmbedBuilder idFoundMessage = DiscordClient.CreateEmbeddedMessage(("Found account linked to `" + nameToCheck + "`"),
                            (author.getAsMention() + ", the Minecraft username: `" + nameToCheck + "` is linked to " + userAsMention +
                                    ".\n\n Here is a list of their whitelisted players:\n" + usersWhitelistedPlayers),
                            DiscordClient.EmbedMessageType.SUCCESS);

                    User fetchedUser = DiscordClient.javaDiscordAPI.getUserById(discordId);

                    if (fetchedUser != null)
                        idFoundMessage.setThumbnail(fetchedUser.getAvatarUrl());
                    else
                        DiscordWhitelister.getPluginLogger().warning("Failed to fetch avatar linked to Discord ID: " + discordId);

                    DiscordClient.QueueAndRemoveAfterSeconds(channel, idFoundMessage.build());
                    idFound = true;
                    break;
                }
            }
        }
        if (!idFound) {
            MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(("Could not find an account linked to `" + nameToCheck + "`"),
                    (author.getAsMention() + ", the name: `" + nameToCheck +
                            "` could not be found in the users list. Please make sure that the Minecraft name is valid and whitelisted + linked to an ID before."),
                    DiscordClient.EmbedMessageType.FAILURE).build();

            DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
        }
    }
}