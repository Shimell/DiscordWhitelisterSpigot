package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.stores.RemovedList;
import uk.co.angrybee.joe.stores.UserList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CommandRemove {

    public static void ExecuteCommand(SlashCommandInteractionEvent event, String mc_name) {

        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();
        TextChannel channel = event.getChannel().asTextChannel();
        Member member = event.getMember();

        // Remove Command
        if (authorPermissions.isUserCanAddRemove()) {


            final String finalNameToRemove = mc_name.replaceAll(" .*", ""); // The name is everything up to the first space

            if (finalNameToRemove.isEmpty()) {
                if (!DiscordClient.hideInfoCommandReplies)
                    DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.removeCommandInfo);
                return;
            } else {
                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to remove " + finalNameToRemove + " from the whitelist");

                boolean notOnWhitelist = false;

                if (WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayerEasyWhitelist(finalNameToRemove) || !WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayer(finalNameToRemove)) {
                    notOnWhitelist = true;

                    if (!DiscordWhitelister.useCustomMessages) {
                        MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("This user is not on the whitelist",
                                (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), DiscordClient.EmbedMessageType.INFO).build();
                        DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                        // Return below
                    } else {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist-title");
                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                        customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                        MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
                        DiscordClient.ReplyAndRemoveAfterSeconds(event, messageEmbed);
                    }
                }

                // not not on whitelist, nice
                if (!notOnWhitelist) // aka on the whitelist
                {
                    DiscordClient.UnWhitelist(finalNameToRemove);
                    // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                    EmbedBuilder embedBuilderSuccess;

                    if (!DiscordWhitelister.useCustomMessages) {
                        if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("set-removed-message-colour-to-red"))
                            embedBuilderSuccess = DiscordClient.CreateEmbeddedMessage((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), DiscordClient.EmbedMessageType.SUCCESS);
                        else
                            embedBuilderSuccess = DiscordClient.CreateEmbeddedMessage((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), DiscordClient.EmbedMessageType.FAILURE);
                    } else {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("remove-success-title");
                        customTitle = customTitle.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("remove-success");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                        customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                        if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("set-removed-message-colour-to-red"))
                            embedBuilderSuccess = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.SUCCESS);
                        else
                            embedBuilderSuccess = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
                    }

                    if (DiscordWhitelister.showPlayerSkin) {
                        String playerUUID = DiscordClient.minecraftUsernameToUUID(finalNameToRemove);

                        if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-crafatar-for-avatars"))
                            embedBuilderSuccess.setThumbnail("https://minotar.net/armor/bust/" + playerUUID + "/100.png");
                        else
                            embedBuilderSuccess.setThumbnail("https://crafatar.com/avatars/" + playerUUID + "?size=100&default=MHF_Steve&overlay.png");
                    }

                    EmbedBuilder embedBuilderFailure;

                    // No custom message needed
                    embedBuilderFailure = DiscordClient.CreateEmbeddedMessage(("Failed to remove " + finalNameToRemove + " from the whitelist"), (author.getAsMention() + ", failed to remove `" + finalNameToRemove + "` from the whitelist. " +
                            "This should never happen, you may have to remove the player manually and report the issue."), DiscordClient.EmbedMessageType.FAILURE);

                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                    {
                        if (WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayerEasyWhitelist(finalNameToRemove)
                                || !WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayer(finalNameToRemove)) {
                            event.replyEmbeds(embedBuilderSuccess.build()).queue();

                            if (DiscordClient.whitelistedRoleAutoRemove) {
                                List<String> whitelistRoles = new LinkedList<>();

                                Collections.addAll(whitelistRoles, DiscordClient.whitelistedRoleNames);

                                // Find the Discord Id linked to the removed name
                                boolean idFound = false;
                                String targetDiscordId = "";
                                Set<String> keys = UserList.getUserList().getKeys(false);
                                for (String userId : keys) {
                                    List<?> registeredUsers = UserList.getRegisteredUsers(userId);
                                    for (Object wUser : registeredUsers) {
                                        if (wUser.equals(finalNameToRemove)) {
                                            // Found the ban target, assign the corresponding Discord id
                                            targetDiscordId = userId;
                                            boolean namesRemainingAfterRemoval = false;

                                            if ((registeredUsers.size() - 1) > 0) {
                                                namesRemainingAfterRemoval = true;
                                                DiscordWhitelister.getPluginLogger().info("The Discord ID (" + targetDiscordId + ") linked to " + finalNameToRemove + " contains "
                                                        + (registeredUsers.size() - 1) + " more whitelisted user(s), not removing whitelisted roles...");
                                            }

                                            // Find all servers bot is in, remove whitelisted roles
                                            if (!whitelistRoles.isEmpty() && !namesRemainingAfterRemoval) {
                                                for (Guild guild : DiscordClient.javaDiscordAPI.getGuilds()) {
                                                    // Remove the whitelisted role(s)
                                                    DiscordClient.RemoveRolesFromUser(guild, targetDiscordId, whitelistRoles);
                                                    DiscordWhitelister.getPluginLogger().info("Successfully removed whitelisted roles from "
                                                            + targetDiscordId + "(" + finalNameToRemove + ") in guild: " + guild.getName());
                                                }
                                            } else if (whitelistRoles.isEmpty()) {
                                                DiscordWhitelister.getPluginLogger().warning("Cannot remove any whitelisted roles from: " + targetDiscordId + "(" + finalNameToRemove + ") as there are none specified in the config");
                                            }
                                            idFound = true;
                                            break;
                                        }
                                    }
                                }

                                if (!idFound) {
                                    DiscordWhitelister.getPluginLogger().warning("Could not find any Discord id linked to Minecraft name: " + finalNameToRemove + ", therefore cannot remove any roles");
                                }
                                DiscordClient.ClearPlayerFromUserList(finalNameToRemove);
                            }

                            // if the name is not on the removed list
                            if (!RemovedList.CheckStoreForPlayer(finalNameToRemove)) {
                                RemovedList.getRemovedPlayers().set(finalNameToRemove, author.getId());
                                RemovedList.SaveStore();
                            }
                        } else {
                            DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderFailure.build());
                        }

                        return null;
                    });
                    return;
                }
                return;
            }

        }

        if (authorPermissions.isUserCanAdd() && !authorPermissions.isUserCanAddRemove()) {
            String higherPermRoles = DiscordWhitelister.mainConfig.getFileConfiguration().getList("add-remove-roles").toString();
            higherPermRoles = higherPermRoles.replaceAll("\\[", "");
            higherPermRoles = higherPermRoles.replaceAll("]", "");

            EmbedBuilder embedBuilderInfo;

            if (!DiscordWhitelister.useCustomMessages) {
                embedBuilderInfo = DiscordClient.CreateEmbeddedMessage("Insufficient Permissions", (author.getAsMention() + ", you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: "
                        + higherPermRoles + "; or get the owner to move your role to 'add-remove-roles' in the config."), DiscordClient.EmbedMessageType.INFO);
            } else {
                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-remove-title");
                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-remove");
                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                customMessage = customMessage.replaceAll("\\{AddRemoveRoles}", higherPermRoles);

                embedBuilderInfo = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO);
            }

            DiscordClient.ReplyAndRemoveAfterSeconds(event, embedBuilderInfo.build());
            return;
        }

        // if the user doesn't have any allowed roles
        DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
        //TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
    }

}