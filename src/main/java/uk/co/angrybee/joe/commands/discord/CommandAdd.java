package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;
import uk.co.angrybee.joe.DiscordWhitelister;
import uk.co.angrybee.joe.configs.MainConfig;
import uk.co.angrybee.joe.configs.OnWhitelistCommandsConfig;
import uk.co.angrybee.joe.configs.PermissionsConfig;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.RemovedList;
import uk.co.angrybee.joe.stores.UserList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandAdd
{
    public static void ExecuteCommand(MessageReceivedEvent messageReceivedEvent, String[] splitMessageCaseSensitive)
    {
        AuthorPermissions authorPermissions = new AuthorPermissions(messageReceivedEvent);
        User author = messageReceivedEvent.getAuthor();
        TextChannel channel = messageReceivedEvent.getTextChannel();
        Member member = messageReceivedEvent.getMember();

        if(DiscordWhitelister.useOnBanEvents && authorPermissions.isUserIsBanned())
        {
            if(!DiscordWhitelister.useCustomMessages)
            {
                MessageEmbed messageEmbed =
                        DiscordClient.CreateEmbeddedMessage("You have been banned!", (author.getAsMention() + ", you cannot use this bot as you have been banned!"), DiscordClient.EmbedMessageType.FAILURE).build();

                DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
            }
            else
            {
                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("banned-title");
                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("banned-message");
                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention()); // Only checking for {Sender}

                MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
                DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
            }
            return;
        }

        // Permission check
        if (!(authorPermissions.isUserCanAddRemove() || authorPermissions.isUserCanAdd() || DiscordClient.limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd()))
        {
            DiscordClient.QueueAndRemoveAfterSeconds(channel, DiscordClient.CreateInsufficientPermsMessage(author));
            return;
        }

        /* if limited whitelist is enabled, check if the user is in the limited whitelister group and add the user to the list
        which records how many times the user has successfully used the whitelist command */
        if (DiscordClient.limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd())
        {
            if (UserList.getUserList().getString(author.getId()) == null)
            {
                UserList.getUserList().set(author.getId(), new ArrayList<String>());
                UserList.SaveStore();
            }
        }

        boolean usedAllWhitelists = false;
        try
        {
            usedAllWhitelists = UserList.getRegisteredUsersCount(author.getId()) >= DiscordClient.maxWhitelistAmount &&
                    !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();
        }
        catch (NullPointerException exception)
        {
            exception.printStackTrace();
        }

        if (authorPermissions.isUserCanAddRemove() || authorPermissions.isUserCanAdd() || DiscordClient.limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd())
        {
            if(DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length > (DiscordClient.customWhitelistAddPrefixSplit.length + 2)
                    || !DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length > (DiscordClient.whitelistAddPrefix.length + 2))
            {
                int amountOfArgs = 0;
                if(DiscordWhitelister.getUseCustomPrefixes())
                    amountOfArgs = splitMessageCaseSensitive.length - (DiscordClient.customWhitelistAddPrefixSplit.length);
                else
                    amountOfArgs = splitMessageCaseSensitive.length - (DiscordClient.whitelistAddPrefix.length);

                StringBuilder exampleCommand = new StringBuilder();
                if(DiscordWhitelister.getUseCustomPrefixes())
                {
                    for(int i = 0; i < DiscordClient.customWhitelistAddPrefixSplit.length; i++)
                    {
                        exampleCommand.append(DiscordClient.customWhitelistAddPrefixSplit[i]).append(" ");
                    }
                }
                else
                {
                    for(int i = 0; i < DiscordClient.whitelistAddPrefix.length; i++)
                    {
                        exampleCommand.append(DiscordClient.whitelistAddPrefix[i]).append(" ");
                    }
                }
                exampleCommand.append("<minecraftUsername> <@discordMention>");

                MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("Too many arguments",
                        (author.getAsMention() + ", expected 1 or 2 arguments but found " + amountOfArgs + ".\n" +
                                "Example: " + exampleCommand.toString() + "\n\n **NOTE:** Only staff members may use the <@discordMention> parameter."),
                                    DiscordClient.EmbedMessageType.FAILURE).build();

                DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                return;
            }

            String nameToAddCaseSensitive = "";
            if(DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length >= DiscordClient.customWhitelistAddPrefixSplit.length + 1)
                nameToAddCaseSensitive = splitMessageCaseSensitive[DiscordClient.customWhitelistAddPrefixSplit.length];

            if(!DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length >= DiscordClient.whitelistAddPrefix.length + 1)
                nameToAddCaseSensitive = splitMessageCaseSensitive[DiscordClient.whitelistAddPrefix.length];

            final String finalNameToAdd = nameToAddCaseSensitive;
            final char[] finalNameToWhitelistChar = finalNameToAdd.toLowerCase().toCharArray(); // Lower case for char check

            int timesWhitelisted = 0;

            boolean onlyHasLimitedAdd = DiscordClient.limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd() &&
                    !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();

            boolean discordIdProvided = false;
            if(DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length == (DiscordClient.customWhitelistAddPrefixSplit.length + 2)
                    || !DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length == (DiscordClient.whitelistAddPrefix.length + 2))
            {
                if(onlyHasLimitedAdd)
                {
                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("Insufficient Permissions",
                            (author.getAsMention() + ", only staff members can manually link Discord IDs. Please only enter your Minecraft name."), DiscordClient.EmbedMessageType.FAILURE).build();

                    DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    return;
                }
                else
                {
                    discordIdProvided = true;
                }
            }

            final boolean finalDiscordIdProvided = discordIdProvided;

            String discordIdStripped = "";
            if(finalDiscordIdProvided)
            {
                discordIdStripped = splitMessageCaseSensitive[DiscordClient.customWhitelistAddPrefixSplit.length + 1];
                discordIdStripped = discordIdStripped.replaceAll("<@!","");
                discordIdStripped = discordIdStripped.replaceAll(">","");
            }

            final String finalDiscordIdStripped = discordIdStripped;
            final Member suppliedMember = (!finalDiscordIdStripped.isEmpty()) ? channel.getGuild().getMemberById(finalDiscordIdStripped) : null;

            if(finalDiscordIdProvided && suppliedMember == null)
            {
                MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("Discord ID not found in Guild or is incorrect",
                        (author.getAsMention() + ", the ID supplied (`" + discordIdStripped + "`) could not be located in this Discord Server/Guild."),
                                DiscordClient.EmbedMessageType.FAILURE).build();

                DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                return;
            }

            // runs after member null check
            if(finalDiscordIdProvided)
            {
                // Create a entry in the user list if needed for supplied id
                if (UserList.getUserList().getString(suppliedMember.getId()) == null)
                {
                    UserList.getUserList().set(suppliedMember.getId(), new ArrayList<String>());
                    UserList.SaveStore();
                }
            }

            if (onlyHasLimitedAdd)
            {
                timesWhitelisted = UserList.getRegisteredUsersCount(author.getId());

                // set to current max in case the max whitelist amount was changed
                if (timesWhitelisted > DiscordClient.maxWhitelistAmount)
                {
                    timesWhitelisted = DiscordClient.maxWhitelistAmount;
                }
            }

            if (onlyHasLimitedAdd && usedAllWhitelists)
            {
                if(!DiscordWhitelister.useCustomMessages)
                {
                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage("No Whitelists Remaining", (author.getAsMention() + ", unable to whitelist. You have **" + (DiscordClient.maxWhitelistAmount - timesWhitelisted)
                            + " out of " + DiscordClient.maxWhitelistAmount + "** whitelists remaining."), DiscordClient.EmbedMessageType.INFO).build();
                    DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                }
                else
                {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining-title");
                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                    customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((DiscordClient.maxWhitelistAmount - timesWhitelisted)));
                    customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(DiscordClient.maxWhitelistAmount));

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
                    DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                }

                return;
            }

            // Command would only be executed if the prefix matched so no need to check contents just length
            if (DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length == DiscordClient.customWhitelistAddPrefixSplit.length
                    || !DiscordWhitelister.getUseCustomPrefixes() && splitMessageCaseSensitive.length == DiscordClient.whitelistAddPrefix.length
                        || finalNameToAdd.isEmpty())
            {
                if(!MainConfig.getMainConfig().getBoolean("hide-info-command-replies"))
                    DiscordClient.QueueAndRemoveAfterSeconds(channel, DiscordClient.addCommandInfo);
            }
            else
            {
                if (DiscordClient.usernameValidation)
                {
                    // Invalid char check
                    for (int a = 0; a < finalNameToWhitelistChar.length; ++a)
                    {
                        if (new String(DiscordClient.validCharacters).indexOf(finalNameToWhitelistChar[a]) == -1)
                        {
                            EmbedBuilder embedBuilderInvalidChar;

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderInvalidChar = DiscordClient.CreateEmbeddedMessage("Invalid Username", (author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**."), DiscordClient.EmbedMessageType.FAILURE);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning");
                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                                embedBuilderInvalidChar = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
                            }

                            if (onlyHasLimitedAdd)
                            {
                                DiscordClient.AddWhitelistRemainingCount(embedBuilderInvalidChar, timesWhitelisted);
                            }

                            DiscordClient.QueueAndRemoveAfterSeconds(channel, embedBuilderInvalidChar.build());
                            return;
                        }
                    }

                    // Length check
                    if (finalNameToAdd.length() < 3 || finalNameToAdd.length() > 16)
                    {
                        EmbedBuilder embedBuilderLengthInvalid;

                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            embedBuilderLengthInvalid = DiscordClient.CreateEmbeddedMessage("Invalid Username", (author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**."), DiscordClient.EmbedMessageType.FAILURE);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning-title");
                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                            embedBuilderLengthInvalid = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
                        }

                        if (onlyHasLimitedAdd)
                        {
                            DiscordClient.AddWhitelistRemainingCount(embedBuilderLengthInvalid, timesWhitelisted);
                        }

                        DiscordClient.QueueAndRemoveAfterSeconds(channel, embedBuilderLengthInvalid.build());
                        return;
                    }
                }

                if (onlyHasLimitedAdd)
                    DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to whitelist: " + finalNameToAdd + ", " + (DiscordClient.maxWhitelistAmount - timesWhitelisted) + " whitelists remaining");
                else
                    DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to whitelist: " + finalNameToAdd);

                boolean alreadyOnWhitelist = false;

                if(WhitelistedPlayers.usingEasyWhitelist)
                {
                    if (WhitelistedPlayers.CheckForPlayerEasyWhitelist(finalNameToAdd))
                    {
                        alreadyOnWhitelist = true;
                    }
                }
                else if (WhitelistedPlayers.CheckForPlayer(finalNameToAdd))
                {
                    alreadyOnWhitelist = true;
                }

                if(alreadyOnWhitelist)
                {
                    if(!DiscordWhitelister.useCustomMessages)
                    {
                        MessageEmbed messageEmbed =
                                DiscordClient.CreateEmbeddedMessage("User already on the whitelist",
                                        (author.getAsMention() + ", cannot add user as `" + finalNameToAdd + "` is already on the whitelist!"), DiscordClient.EmbedMessageType.INFO).build();
                        DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    }
                    else
                    {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist-title");
                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                        customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                        MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.INFO).build();
                        DiscordClient.QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    }
                    return;
                }

                if (RemovedList.CheckStoreForPlayer(finalNameToAdd)) // If the user has been removed before
                {
                    if (onlyHasLimitedAdd)
                    {
                        EmbedBuilder embedBuilderRemovedByStaff;

                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            embedBuilderRemovedByStaff = DiscordClient.CreateEmbeddedMessage("This user was previously removed by a staff member",
                                    (author.getAsMention() + ", this user was previously removed by a staff member (<@" + RemovedList.getRemovedPlayers().get(finalNameToAdd.toLowerCase()) + ">).\n" +
                                            "Please ask a user with higher permissions to add this user.\n"), DiscordClient.EmbedMessageType.FAILURE);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-title");
                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed");
                            String staffMemberMention = "<@" + RemovedList.getRemovedPlayers().get(finalNameToAdd.toLowerCase()) + ">";

                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{StaffMember}", staffMemberMention);

                            embedBuilderRemovedByStaff = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
                        }

                        DiscordClient.AddWhitelistRemainingCount(embedBuilderRemovedByStaff, timesWhitelisted);
                        DiscordClient.QueueAndRemoveAfterSeconds(channel, embedBuilderRemovedByStaff.build());
                        return;
                    }
                    else
                    {
                        RemovedList.getRemovedPlayers().set(finalNameToAdd.toLowerCase(), null);
                        RemovedList.SaveStore();

                        DiscordWhitelister.getPlugin().getLogger().info(finalNameToAdd + " has been removed from the removed list by " + author.getName() + "(" + author.getId() + ")");
                    }
                }

                // In-game list check
                if(DiscordWhitelister.useInGameAddRemoves)
                {
                    if(InGameRemovedList.CheckStoreForPlayer(finalNameToAdd.toLowerCase()))
                    {
                        if(onlyHasLimitedAdd)
                        {
                            EmbedBuilder embedBuilderRemovedByInGameStaff;

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderRemovedByInGameStaff = DiscordClient.CreateEmbeddedMessage("This user was previously removed by a staff member",
                                        (author.getAsMention() + ", this user was previously removed by a staff member in-game (" + InGameRemovedList.getRemovedPlayers().get(finalNameToAdd.toLowerCase()) + ").\n" +
                                                "Please ask a user with higher permissions to add this user.\n"), DiscordClient.EmbedMessageType.FAILURE);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-in-game-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-in-game");
                                String inGameStaffMember = InGameRemovedList.getRemovedPlayers().getString(finalNameToAdd.toLowerCase());

                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                customMessage = customMessage.replaceAll("\\{StaffMember}", inGameStaffMember);

                                embedBuilderRemovedByInGameStaff = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
                            }

                            DiscordClient.AddWhitelistRemainingCount(embedBuilderRemovedByInGameStaff, timesWhitelisted);

                            DiscordClient.QueueAndRemoveAfterSeconds(channel, embedBuilderRemovedByInGameStaff.build());
                            return;
                        }
                        else
                        {
                            InGameRemovedList.RemoveUserFromStore(finalNameToAdd.toLowerCase());

                            DiscordWhitelister.getPlugin().getLogger().info(finalNameToAdd + " has been removed from in-game-removed-list.yml by " + author.getName() + "(" + author.getId() + ")");
                        }
                    }
                }

                /* Do as much as possible off the main server thread.
                convert username into UUID to avoid depreciation and rate limits (according to https://minotar.net/) */
                String playerUUID = DiscordClient.minecraftUsernameToUUID(finalNameToAdd);
                final boolean invalidMinecraftName = playerUUID == null;

                /* Configure success & failure messages here instead of on the main server thread -
                this will run even if the message is never sent, but is a good trade off */
                EmbedBuilder embedBuilderWhitelistSuccess;

                if(!DiscordWhitelister.useCustomMessages)
                {
                    embedBuilderWhitelistSuccess = DiscordClient.CreateEmbeddedMessage((finalNameToAdd + " is now whitelisted!"), (author.getAsMention() + " has added `" + finalNameToAdd + "` to the whitelist.") , DiscordClient.EmbedMessageType.SUCCESS);
                }
                else
                {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success-title");
                    customTitle = customTitle.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                    customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                    embedBuilderWhitelistSuccess = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.SUCCESS);
                }

                if (onlyHasLimitedAdd)
                    DiscordClient.AddWhitelistRemainingCount(embedBuilderWhitelistSuccess, (timesWhitelisted + 1));

                if(DiscordWhitelister.showPlayerSkin)
                {
                    if(!MainConfig.getMainConfig().getBoolean("use-crafatar-for-avatars"))
                        embedBuilderWhitelistSuccess.setThumbnail("https://minotar.net/armor/bust/" + playerUUID + "/100.png");
                    else
                        embedBuilderWhitelistSuccess.setThumbnail("https://crafatar.com/avatars/" + playerUUID + "?size=100&default=MHF_Steve&overlay.png");
                }

                EmbedBuilder embedBuilderWhitelistFailure;

                if(!DiscordWhitelister.useCustomMessages)
                {
                    embedBuilderWhitelistFailure = DiscordClient.CreateEmbeddedMessage("Failed to whitelist",
                            (author.getAsMention() + ", failed to add `" + finalNameToAdd + "` to the whitelist. This is most likely due to an invalid Minecraft username."), DiscordClient.EmbedMessageType.FAILURE);
                }
                else
                {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure-title");

                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                    customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                    embedBuilderWhitelistFailure = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE);
                }

                if (onlyHasLimitedAdd)
                    DiscordClient.AddWhitelistRemainingCount(embedBuilderWhitelistFailure, timesWhitelisted); // was timesWhitelisted + 1 for some reason, change back if it doesn't work correctly

                int tempTimesWhitelisted = timesWhitelisted;
                if (onlyHasLimitedAdd && tempTimesWhitelisted < DiscordClient.maxWhitelistAmount)
                    tempTimesWhitelisted = timesWhitelisted + 1;
                final int finalTimesWhitelisted = tempTimesWhitelisted; // if successful

                AtomicBoolean successfulWhitelist = new AtomicBoolean(false);

                if (!WhitelistedPlayers.usingEasyWhitelist && authorPermissions.isUserCanUseCommand())
                    DiscordClient.ExecuteServerCommand("whitelist add " + finalNameToAdd);
                    if(MainConfig.getMainConfig().getBoolean("use-geyser/floodgate-compatibility")) {
                        addBedrockUser(finalNameToAdd);
                    }

                // have to use !invalidMinecraftName else the easy whitelist plugin will add the name regardless of whether it is valid on not
                if (WhitelistedPlayers.usingEasyWhitelist && !invalidMinecraftName && authorPermissions.isUserCanUseCommand())
                    DiscordClient.ExecuteServerCommand("easywl add " + finalNameToAdd);

                DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                {
                    if(WhitelistedPlayers.usingEasyWhitelist && !invalidMinecraftName && WhitelistedPlayers.CheckForPlayerEasyWhitelist(finalNameToAdd)
                            || !WhitelistedPlayers.usingEasyWhitelist && WhitelistedPlayers.CheckForPlayer(finalNameToAdd))
                    {
                        channel.sendMessage(embedBuilderWhitelistSuccess.build()).queue();

                        // For instructional message
                        successfulWhitelist.set(true);

                        //Assign perms:
                        DiscordClient.AssignPerms(finalNameToAdd);

                        if(DiscordWhitelister.useOnWhitelistCommands)
                        {
                            List<String> commandsToExecute = OnWhitelistCommandsConfig.getPermissionsConfig().getStringList("on-whitelist-commands");
                            for (String command : commandsToExecute)
                            {
                                DiscordClient.CheckAndExecuteCommand(command, finalNameToAdd);
                            }
                        }

                        if(DiscordClient.whitelistedRoleAutoAdd)
                        {
                            List<Role> whitelistRoles = new LinkedList<>();
                            try
                            {
                                if(!DiscordWhitelister.useIdForRoles)
                                {
                                    for (String whitelistedRoleName : DiscordClient.whitelistedRoleNames)
                                    {
                                        // Use channel, get guild instead of JDA so that it is guild specific
                                        List<Role> rolesFoundWithName = channel.getGuild().getRolesByName(whitelistedRoleName, false);
                                        whitelistRoles.addAll(rolesFoundWithName);
                                    }
                                }
                                else
                                {
                                    for (String whitelistedRoleName : DiscordClient.whitelistedRoleNames)
                                    {
                                        if (channel.getGuild().getRoleById(whitelistedRoleName) != null)
                                            whitelistRoles.add(channel.getGuild().getRoleById(whitelistedRoleName));
                                    }
                                }

                                if(!whitelistRoles.isEmpty())
                                {
                                    if(!finalDiscordIdProvided)
                                    {
                                        whitelistRoles.forEach(role ->
                                        {
                                            member.getGuild().addRoleToMember(member, role).queue();
                                        });
                                    }
                                    else
                                    {
                                        whitelistRoles.forEach(role ->
                                        {
                                            member.getGuild().addRoleToMember(suppliedMember, role).queue();
                                        });
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                if(!finalDiscordIdProvided)
                                    DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name/id to " + author.getName() + ", check the config and that the bot has the Manage Roles permission");
                                else
                                    DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name/id to " + suppliedMember.getEffectiveName() + ", check the config and that the bot has the Manage Roles permission");

                                e.printStackTrace();
                            }

                            // Instructional message
                            if(successfulWhitelist.get())
                            {
                                if(MainConfig.getMainConfig().getBoolean("send-instructional-message-on-whitelist"))
                                {
                                    if(!MainConfig.getMainConfig().getBoolean("use-timer-for-instructional-message"))
                                    {
                                        channel.sendMessage(DiscordClient.CreateInstructionalMessage()).queue();
                                    }
                                    else
                                    {
                                        int waitTime = MainConfig.getMainConfig().getInt("timer-wait-time-in-seconds");

                                        // Run on a new thread to not block main thread
                                        Thread whitelisterTimerThread = new Thread(() ->
                                        {
                                            try
                                            {
                                                TimeUnit.SECONDS.sleep(waitTime);
                                                channel.sendMessage(DiscordClient.CreateInstructionalMessage()).queue();
                                            }
                                            catch (InterruptedException e)
                                            {
                                                e.printStackTrace();
                                            }
                                        });

                                        whitelisterTimerThread.start();
                                    }
                                }
                            }
                        }

                        if (onlyHasLimitedAdd || finalDiscordIdProvided)
                        {
                            if(!finalDiscordIdProvided)
                            {
                                UserList.addRegisteredUser(author.getId(), finalNameToAdd.toLowerCase()); // convert to lower case for remove & clearname commands
                            }
                            else
                            {
                                UserList.addRegisteredUser(suppliedMember.getId(), finalNameToAdd.toLowerCase());
                            }

                            if(!finalDiscordIdProvided)
                                DiscordWhitelister.getPluginLogger().info(author.getName() + "(" + author.getId() + ") successfully added " + finalNameToAdd
                                        + " to the whitelist, " + (DiscordClient.maxWhitelistAmount - finalTimesWhitelisted) + " whitelists remaining.");
                            else
                                DiscordWhitelister.getPluginLogger().info(author.getName() + "(" + author.getId() + ") successfully added " + finalNameToAdd
                                        + " to the whitelist and linked " + finalNameToAdd + " to "+ suppliedMember.getEffectiveName() + "(" + suppliedMember.getId() + ").");
                        }
                    }
                    else
                    {
                        DiscordClient.QueueAndRemoveAfterSeconds(channel, embedBuilderWhitelistFailure.build());
                    }
                    return null;
                });
            }
        }
    }

    private static void addBedrockUser(String finalNameToAdd) {
        String bedrockPrefix = MainConfig.getMainConfig().getString("geyser/floodgate prefix");
        String bedrockName = bedrockPrefix + finalNameToAdd;
        if(bedrockName.length() > 16) {
            bedrockName = bedrockName.substring(0, 16);
        }
        //check if we actually NEED to add
        if(finalNameToAdd.length() < bedrockPrefix.length() || !finalNameToAdd.substring(0, bedrockPrefix.length() - 1).equals(bedrockPrefix)) {
            DiscordClient.ExecuteServerCommand("whitelist add " + bedrockName);
        }
    }
}
