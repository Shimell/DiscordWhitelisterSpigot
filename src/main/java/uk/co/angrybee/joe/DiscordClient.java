package uk.co.angrybee.joe;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import uk.co.angrybee.joe.commands.discord.CommandAdd;
import uk.co.angrybee.joe.commands.discord.CommandWhoIs;
import uk.co.angrybee.joe.configs.*;
import uk.co.angrybee.joe.commands.discord.CommandInfo;
import uk.co.angrybee.joe.events.ShutdownEvents;
import uk.co.angrybee.joe.stores.InGameRemovedList;
import uk.co.angrybee.joe.stores.RemovedList;
import uk.co.angrybee.joe.stores.UserList;
import uk.co.angrybee.joe.stores.WhitelistedPlayers;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

// handles Discord interaction
public class DiscordClient extends ListenerAdapter
{
    public static String[] allowedToAddRemoveRoles;
    public static String[] allowedToAddRoles;
    public static String[] allowedToAddLimitedRoles;
    public static String[] allowedToClearNamesRoles;

    public static String[] combinedRoles;

    private static String[] targetTextChannels;

    // TODO: remove in favour of split versions
    public static String customWhitelistAddPrefix;
    public static String customWhitelistRemovePrefix;
    public static String customClearNamePrefix;
    public static String customLimitedWhitelistClearPrefix;
    public static String customClearBanPrefix;

    public static String[] customWhitelistAddPrefixSplit;
    public static String[] customWhitelistRemovePrefixSplit;
    public static String[] customClearNamePrefixSplit;
    public static String[] customLimitedWhitelistClearPrefixSplit;
    public static String[] customClearBanPrefixSplit;
    public static String[] customWhoIsPrefix;

    // TODO: move to own class, references to non custom prefix
    public static final String[] whitelistInfoPrefix = {"!whitelist"};
    public static final String[] whitelistAddPrefix = {"!whitelist", "add"};
    public static final String[] whitelistRemovePrefix = {"!whitelist", "remove"};
    public static final String[] whitelistClearPrefix = {"!whitelist", "clear"};
    public static final String[] whitelistWhoIsPrefix = {"!whitelist", "whois"};
    public static final String[] clearNamePrefix = {"!clearname"};
    public static final String[] clearBanPrefix = {"!clearban"};

    public static MessageEmbed botInfo;
    public static MessageEmbed addCommandInfo;
    public static MessageEmbed removeCommandInfo;
    public static MessageEmbed whoIsInfo;

    public static int maxWhitelistAmount;

    public static boolean limitedWhitelistEnabled;
    public static boolean usernameValidation;

    public static boolean whitelistedRoleAutoAdd;
    public static boolean whitelistedRoleAutoRemove;
    public static String[] whitelistedRoleNames;
    public static boolean hideInfoCommandReplies = false;

    private static boolean checkForMissingRole = false;
    private static boolean checkAllRoles = false;
    private static String roleToCheck;

    public static final char[] validCharacters = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h',
            'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_'};

    public static JDA javaDiscordAPI;

    public static int InitializeClient(String clientToken)
    {
        AssignVars();
        BuildStrings();

        try
        {
            javaDiscordAPI = JDABuilder.createDefault(clientToken)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setBulkDeleteSplittingEnabled(false)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOTE)
                    .setContextEnabled(true)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .addEventListeners(new DiscordClient())
                    .addEventListeners(new ShutdownEvents())
                    .build();

            javaDiscordAPI.awaitReady();

            return 0;
        }
        catch (LoginException | InterruptedException e)
        {
            e.printStackTrace();
            return 1;
        }
        catch (IllegalStateException e)
        {
            // Don't print exception related to disallowed intents, already handled
            if(!e.getMessage().startsWith("Was shutdown trying to await status"))
                e.printStackTrace();

            return 1;
        }
    }

    public static boolean ShutdownClient()
    {
        javaDiscordAPI.shutdownNow();

        return javaDiscordAPI.getStatus() == JDA.Status.SHUTTING_DOWN || javaDiscordAPI.getStatus() == JDA.Status.SHUTDOWN;
    }

    private static void AssignVars()
    {
        FileConfiguration mainConfig = DiscordWhitelister.mainConfig.getFileConfiguration();

        // assign vars here instead of every time a message is received, as they do not change
        targetTextChannels = new String[mainConfig.getList("target-text-channels").size()];
        for (int i = 0; i < targetTextChannels.length; ++i)
        {
            targetTextChannels[i] = mainConfig.getList("target-text-channels").get(i).toString();
        }

        maxWhitelistAmount = mainConfig.getInt("max-whitelist-amount");
        limitedWhitelistEnabled = mainConfig.getBoolean("limited-whitelist-enabled");
        usernameValidation = mainConfig.getBoolean("username-validation");

        // Set the name of the role to add/remove to/from the user after they have been added/removed to/from the whitelist and if this feature is enabled
        whitelistedRoleAutoAdd = mainConfig.getBoolean("whitelisted-role-auto-add");
        whitelistedRoleAutoRemove = mainConfig.getBoolean("whitelisted-role-auto-remove");


        whitelistedRoleNames = new String[mainConfig.getList("whitelisted-roles").size()];
        for(int i = 0; i < whitelistedRoleNames.length; i++)
        {
            whitelistedRoleNames[i] = mainConfig.getList("whitelisted-roles").get(i).toString();
        }

        checkForMissingRole = mainConfig.getBoolean("un-whitelist-if-missing-role");
        checkAllRoles = mainConfig.getBoolean("check-all-roles");
        roleToCheck = mainConfig.getString("role-to-check-for");
        hideInfoCommandReplies = mainConfig.getBoolean("hide-info-command-replies");
    }

    private static void BuildStrings()
    {
        // build here instead of every time a message is received, as they do not change
        EmbedBuilder embedBuilderBotInfo = new EmbedBuilder();
        embedBuilderBotInfo.setTitle("Discord Whitelister for Spigot");
        embedBuilderBotInfo.addField("Version", VersionInfo.getVersion(), false);
        embedBuilderBotInfo.addField("Links", ("https://www.spigotmc.org/resources/discord-whitelister.69929/\nhttps://github.com/JoeShimell/DiscordWhitelisterSpigot"), false);
        embedBuilderBotInfo.addField("Commands", ("**Add:** !whitelist add minecraftUsername\n**Remove:** !whitelist remove minecraftUsername"), false);
        embedBuilderBotInfo.addField("Experiencing issues?", "If you encounter an issue, please report it here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues", false);
        embedBuilderBotInfo.setColor(infoColour);
        botInfo = embedBuilderBotInfo.build();

        addCommandInfo = CreateEmbeddedMessage("Whitelist Add Command",
                "!whitelist add minecraftUsername\n\nIf you encounter any issues, please report them here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues",
                EmbedMessageType.INFO).build();

        removeCommandInfo = CreateEmbeddedMessage("Whitelist Remove Command",
                "!whitelist remove minecraftUsername\n\nIf you encounter any issues, please report them here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues",
                EmbedMessageType.INFO).build();

        whoIsInfo = CreateEmbeddedMessage("Whitelist WhoIs Command",
                "!whitelist whois minecraftUsername\n\nIf you encounter any issues, please report them here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues",
                EmbedMessageType.INFO).build();
    }

    public static String getOnlineStatus()
    {
        try
        {
            return javaDiscordAPI.getStatus().name();
        }
        catch(NullPointerException ex)
        {
            return "OFFLINE";
        }
    }

    public static void SetPlayerCountStatus(int playerCount)
    {
        javaDiscordAPI.getPresence().setActivity(Activity.watching(playerCount + "/" + DiscordWhitelister.getMaximumAllowedPlayers() + " players."));
    }

    public enum EmbedMessageType { INFO, SUCCESS, FAILURE }
    private static Color infoColour = new Color(104, 109,224);
    private static Color successColour = new Color(46, 204, 113);
    private static Color failureColour = new Color(231, 76, 60);

    public static EmbedBuilder CreateEmbeddedMessage(String title, String message, EmbedMessageType messageType)
    {
        EmbedBuilder newMessageEmbed = new EmbedBuilder();
        newMessageEmbed.addField(title, message, false);

        if(messageType == EmbedMessageType.INFO)
            newMessageEmbed.setColor(infoColour);
        else if (messageType == EmbedMessageType.SUCCESS)
            newMessageEmbed.setColor(successColour);
        else if (messageType == EmbedMessageType.FAILURE)
            newMessageEmbed.setColor(failureColour);
        else
            newMessageEmbed.setColor(new Color(255,255,255));

        return newMessageEmbed;
    }

    public static EmbedBuilder AddWhitelistRemainingCount(EmbedBuilder embedBuilder, int timesWhitelisted)
    {
        if(!DiscordWhitelister.useCustomMessages)
        {
            embedBuilder.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
        }
        else
        {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining-title");
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining");
            customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - timesWhitelisted)));
            customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

            embedBuilder.addField(customTitle, customMessage, false);
        }

        return embedBuilder;
    }

    public static MessageEmbed CreateInsufficientPermsMessage(User messageAuthor)
    {
        MessageEmbed insufficientMessageEmbed;

        if(!DiscordWhitelister.useCustomMessages)
        {
            insufficientMessageEmbed = CreateEmbeddedMessage("Insufficient Permissions", (messageAuthor.getAsMention() + ", you do not have permission to use this command."), EmbedMessageType.FAILURE).build();
        }
        else
        {
            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-title");
            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions");
            customMessage = customMessage.replaceAll("\\{Sender}", messageAuthor.getAsMention()); // Only checking for {Sender}

            insufficientMessageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE).build();
        }

        return insufficientMessageEmbed;
    }

    // TODO can be placed in BuildStrings()
    public static MessageEmbed CreateInstructionalMessage()
    {
        MessageEmbed instructionalMessageEmbed;

        if(!DiscordWhitelister.useCustomMessages)
        {
            String addCommandExample = "!whitelist add";
            if(DiscordWhitelister.useCustomPrefixes)
                addCommandExample = DiscordWhitelister.customPrefixConfig.getFileConfiguration().getString("whitelist-add-prefix").trim();

            instructionalMessageEmbed = CreateEmbeddedMessage("How to Whitelist", ("Use `" + addCommandExample + " <minecraftUsername>` to whitelist yourself.\n" +
                    "In the case of whitelisting an incorrect name, please contact a staff member to clear it from the whitelist."), EmbedMessageType.INFO).build();
        }
        else
        {
            String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("instructional-message-title");
            String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("instructional-message");

            instructionalMessageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.INFO).build();
        }

        return instructionalMessageEmbed;
    }

    // returns true if the target string initially contains the prefix or is identical to the prefix
    public static boolean CheckForPrefix(String[] prefixToCheck, String[] targetString)
    {
        if(prefixToCheck == null || targetString == null)
            return false;

        if(targetString.length < prefixToCheck.length)
            return false;

        String[] tempCompareArray = new String[prefixToCheck.length];
        if(targetString.length > prefixToCheck.length)
            System.arraycopy(targetString, 0, tempCompareArray, 0, prefixToCheck.length);
        else
            tempCompareArray = targetString;

        boolean isIdentical = true;
        for(int i = 0; i < prefixToCheck.length; i++)
        {
            if (!prefixToCheck[i].equals(tempCompareArray[i]))
            {
                isIdentical = false;
                break;
            }
        }

        return isIdentical;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent)
    {
        if (messageReceivedEvent.isFromType(ChannelType.TEXT))
        {
            // Check if message should be handled
            if (!Arrays.asList(targetTextChannels).contains(messageReceivedEvent.getTextChannel().getId()))
                return;

            if(messageReceivedEvent.getAuthor().getIdLong() == javaDiscordAPI.getSelfUser().getIdLong())
                return;

            String messageContents = messageReceivedEvent.getMessage().getContentRaw();
            String[] splitMessage = messageContents.toLowerCase().trim().split(" ");
            String[] splitMessageCaseSensitive = messageContents.trim().split(" ");

            if(splitMessage.length <= 0)
                return;

            // TODO remove, use in command classes when complete
            AuthorPermissions authorPermissions = new AuthorPermissions(messageReceivedEvent);
            User author = messageReceivedEvent.getAuthor();
            TextChannel channel = messageReceivedEvent.getTextChannel();

            // determine which command to run
            if (splitMessage.length == 1 && CheckForPrefix(whitelistInfoPrefix, splitMessage))
            {
                if(hideInfoCommandReplies)
                    return;

                CommandInfo.ExecuteCommand(messageReceivedEvent);

                if(DiscordWhitelister.removeUnnecessaryMessages)
                    RemoveMessageAfterSeconds(messageReceivedEvent, DiscordWhitelister.removeMessageWaitTime);
                return;
            }
            //!whitelist add command:
            if(!DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length >= whitelistAddPrefix.length  && CheckForPrefix(whitelistAddPrefix, splitMessage)
                || DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length >= customWhitelistAddPrefixSplit.length && CheckForPrefix(customWhitelistAddPrefixSplit, splitMessage))
            {
                CommandAdd.ExecuteCommand(messageReceivedEvent, splitMessageCaseSensitive);

                if(DiscordWhitelister.removeUnnecessaryMessages)
                    RemoveMessageAfterSeconds(messageReceivedEvent, DiscordWhitelister.removeMessageWaitTime);
                return;
            }

            // Remove Command
            if (messageContents.toLowerCase().startsWith("!whitelist remove") && !DiscordWhitelister.getUseCustomPrefixes()
                    || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customWhitelistRemovePrefix))
            {
                if (authorPermissions.isUserCanAddRemove())
                {
                    messageContents = messageContents.toLowerCase();

                    String messageContentsAfterCommand = "";
                    if(!DiscordWhitelister.useCustomPrefixes)
                    {
                        if(messageContents.length() >  ("!whitelist remove".length() + 1))
                        {
                            messageContentsAfterCommand = messageContents.substring("!whitelist remove".length() + 1); // get everything after !whitelist remove[space]
                        }
                    }
                    else
                    {
                        if(messageContents.length() >  (customWhitelistRemovePrefix.length() + 1))
                        {
                            messageContentsAfterCommand = messageContents.substring(customWhitelistRemovePrefix.length() + 1); // get everything after whitelistRemovePrefix[space]
                        }
                    }

                    final String finalNameToRemove = messageContentsAfterCommand.replaceAll(" .*", ""); // The name is everything up to the first space

                    if (finalNameToRemove.isEmpty())
                    {
                        if(!hideInfoCommandReplies)
                            QueueAndRemoveAfterSeconds(channel, removeCommandInfo);

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                    else
                    {
                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to remove " + finalNameToRemove + " from the whitelist");

                        boolean notOnWhitelist = false;

                        if (WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayerEasyWhitelist(finalNameToRemove) || !WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayer(finalNameToRemove))
                        {
                            notOnWhitelist = true;

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                MessageEmbed messageEmbed = CreateEmbeddedMessage("This user is not on the whitelist",
                                        (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), EmbedMessageType.INFO).build();
                                QueueAndRemoveAfterSeconds(channel, messageEmbed);
                                TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                                // Return below
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist");
                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.INFO).build();
                                QueueAndRemoveAfterSeconds(channel, messageEmbed);
                                TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                            }
                        }

                        // not not on whitelist, nice
                        if (!notOnWhitelist) // aka on the whitelist
                        {
                            UnWhitelist(finalNameToRemove);
                            // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                            EmbedBuilder embedBuilderSuccess;

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                if(!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("set-removed-message-colour-to-red"))
                                    embedBuilderSuccess = CreateEmbeddedMessage((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), EmbedMessageType.SUCCESS);
                                else
                                    embedBuilderSuccess = CreateEmbeddedMessage((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), EmbedMessageType.FAILURE);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("remove-success-title");
                                customTitle = customTitle.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("remove-success");
                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                if(!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("set-removed-message-colour-to-red"))
                                    embedBuilderSuccess = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.SUCCESS);
                                else
                                    embedBuilderSuccess = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE);
                            }

                            if(DiscordWhitelister.showPlayerSkin)
                            {
                                String playerUUID = DiscordClient.minecraftUsernameToUUID(finalNameToRemove);

                                if(!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("use-crafatar-for-avatars"))
                                    embedBuilderSuccess.setThumbnail("https://minotar.net/armor/bust/" + playerUUID + "/100.png");
                                else
                                    embedBuilderSuccess.setThumbnail("https://crafatar.com/avatars/" + playerUUID + "?size=100&default=MHF_Steve&overlay.png");
                            }

                            EmbedBuilder embedBuilderFailure;

                            // No custom message needed
                            embedBuilderFailure = CreateEmbeddedMessage(("Failed to remove " + finalNameToRemove + " from the whitelist"), (author.getAsMention() + ", failed to remove `" + finalNameToRemove + "` from the whitelist. " +
                                    "This should never happen, you may have to remove the player manually and report the issue."), EmbedMessageType.FAILURE);

                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                            {
                                if(WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayerEasyWhitelist(finalNameToRemove)
                                        || !WhitelistedPlayers.usingEasyWhitelist && !WhitelistedPlayers.CheckForPlayer(finalNameToRemove))
                                {
                                    channel.sendMessage(embedBuilderSuccess.build()).queue();
                                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);

                                    if (whitelistedRoleAutoRemove) {
                                        List<String> whitelistRoles = new LinkedList<>();

                                        Collections.addAll(whitelistRoles, whitelistedRoleNames);

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
                                                            RemoveRolesFromUser(guild, targetDiscordId, whitelistRoles);
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
                                        ClearPlayerFromUserList(finalNameToRemove);
                                    }

                                    // if the name is not on the removed list
                                    if (!RemovedList.CheckStoreForPlayer(finalNameToRemove)) {
                                        RemovedList.getRemovedPlayers().set(finalNameToRemove, author.getId());
                                        RemovedList.SaveStore();
                                    }
                                } else {
                                    QueueAndRemoveAfterSeconds(channel, embedBuilderFailure.build());
                                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                                }

                                return null;
                            });
                            return;
                        }
                        return;
                    }

                }

                if (authorPermissions.isUserCanAdd() && !authorPermissions.isUserCanAddRemove())
                {
                    String higherPermRoles = DiscordWhitelister.mainConfig.getFileConfiguration().getList("add-remove-roles").toString();
                    higherPermRoles = higherPermRoles.replaceAll("\\[", "");
                    higherPermRoles = higherPermRoles.replaceAll("]", "");

                    EmbedBuilder embedBuilderInfo;

                    if(!DiscordWhitelister.useCustomMessages)
                    {
                        embedBuilderInfo = CreateEmbeddedMessage("Insufficient Permissions", (author.getAsMention() + ", you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: "
                                + higherPermRoles + "; or get the owner to move your role to 'add-remove-roles' in the config."), EmbedMessageType.INFO);
                    }
                    else
                    {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-remove-title");
                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-remove");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                        customMessage = customMessage.replaceAll("\\{AddRemoveRoles}", higherPermRoles);

                        embedBuilderInfo = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.INFO);
                    }

                    QueueAndRemoveAfterSeconds(channel, embedBuilderInfo.build());
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }

                // if the user doesn't have any allowed roles
                QueueAndRemoveAfterSeconds(channel, CreateInsufficientPermsMessage(author));
                //TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
            }

            // Clear Whitelists command
            if (messageContents.toLowerCase().startsWith("!clearname") && !DiscordWhitelister.getUseCustomPrefixes()
                    || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customClearNamePrefix)) {
                // !clearname <targetName>
                // TODO: !clearnames <@DiscordID>

                // Check permissions
                if (!authorPermissions.isUserCanUseClear()) {
                    QueueAndRemoveAfterSeconds(channel, CreateInsufficientPermsMessage(author));
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }  // Don't have permission

                if (messageContents.toLowerCase().trim().equals("!clearname") && !DiscordWhitelister.getUseCustomPrefixes()
                        || messageContents.toLowerCase().trim().equals(customClearNamePrefix) && DiscordWhitelister.getUseCustomPrefixes()) {
                    if (hideInfoCommandReplies) {
                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }

                    MessageEmbed messageEmbed;
                    if (!DiscordWhitelister.getUseCustomPrefixes()) {
                        messageEmbed = CreateEmbeddedMessage("Clear Name Command", "Usage: `!clearname <minecraftUsername>`\n", EmbedMessageType.INFO).build();
                    } else {
                        messageEmbed = CreateEmbeddedMessage("Clear Name Command", "Usage: `" + customClearNamePrefix + " <minecraftUsername>`\n", EmbedMessageType.INFO).build();
                    }
                    QueueAndRemoveAfterSeconds(channel, messageEmbed);

                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }

                // If command is not empty check for args
                // String[] splitMessage = messageContents.toLowerCase().trim().split(" "); // TODO

                int userNameIndex = 1;

                if (DiscordWhitelister.getUseCustomPrefixes()) {
                    String[] customPrefixCount = customClearNamePrefix.trim().split(" ");
                    userNameIndex = customPrefixCount.length; // Don't + 1 as index starts at 0, length doesn't
                }
                String NameToRemove = splitMessage[userNameIndex];
                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to clear " + NameToRemove + " from the whitelist");

                // Search for target name & linked ID
                boolean nameFound = false;
                String targetDiscordId = "";
                Set<String> keys = UserList.getUserList().getKeys(false);
                // Make sure the user list is not empty
                if (keys.size() > 0) {
                    for (String userid : keys) {
                        List<?> registeredUsers = UserList.getRegisteredUsers(userid);
                        if (registeredUsers.contains(NameToRemove)) {
                            nameFound = true;
                            targetDiscordId = userid;
                            if (registeredUsers.size() > 1) {
                                registeredUsers.remove(NameToRemove); // Clear name
                                // Set the updated list in the config
                                UserList.getUserList().set(userid, registeredUsers);
                            } else { // Remove entirely

                                UserList.getUserList().set(userid, null);
                            }
                            UserList.SaveStore();
                            if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("unwhitelist-and-clear-perms-on-name-clear")) {
                                // Remove name from the whitelist
                                UnWhitelist(splitMessage[userNameIndex]);
                            }
                            break;
                        }
                    }
                }
                if (nameFound) {
                    // Success message
                    if (DiscordWhitelister.useCustomMessages) {
                        String clearNameTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-success-title");
                        String clearNameMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-success-message");

                        clearNameMessage = clearNameMessage.replaceAll("\\{Sender}", author.getAsMention());
                        clearNameMessage = clearNameMessage.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);
                        clearNameMessage = clearNameMessage.replaceAll("\\{DiscordID}", "<@" + targetDiscordId + ">");

                        clearNameTitle = clearNameTitle.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);

                        MessageEmbed messageEmbed = CreateEmbeddedMessage(clearNameTitle, clearNameMessage, EmbedMessageType.SUCCESS).build();
                        QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    } else {
                        MessageEmbed messageEmbed = CreateEmbeddedMessage("Successfully Cleared Name", (author.getAsMention() + " successfully cleared username `" + splitMessage[userNameIndex] +
                                "` from <@" + targetDiscordId + ">'s whitelisted users."), EmbedMessageType.SUCCESS).build();
                        QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    }

                } else {
                    // Name not found
                    if (!DiscordWhitelister.useCustomMessages) {
                        MessageEmbed messageEmbed =
                                CreateEmbeddedMessage((splitMessage[userNameIndex] + " not Found"),
                                        (author.getAsMention() + ", could not find name " + splitMessage[userNameIndex] + " to clear in user list."), EmbedMessageType.FAILURE).build();
                        QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    } else {
                        String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-failure-title");
                        String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-name-failure-message");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                        customMessage = customMessage.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);
                        customTitle = customTitle.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);

                        MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE).build();
                        QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    }

                }
                TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                return;

            }
            // Clear whitelists for limited-whitelisters
            else if (messageContents.toLowerCase().startsWith("!whitelist clear") && !DiscordWhitelister.getUseCustomPrefixes()
                    || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customLimitedWhitelistClearPrefix)) {
                if (!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("allow-limited-whitelisters-to-unwhitelist-self"))
                    return;

                // just inform staff, can add custom messages later if really needed
                if (authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserIsBanned() || authorPermissions.isUserCanAdd() && !authorPermissions.isUserIsBanned()) {
                    MessageEmbed messageEmbed = CreateEmbeddedMessage("This Command is Only Available for Limited Whitelister Roles",
                            "If staff members need to clear a name from the whitelist please use `!clearname <mcName>`.", EmbedMessageType.INFO).build();
                    QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }

                if (authorPermissions.isUserHasLimitedAdd() && !authorPermissions.isUserIsBanned()) {
                    List<?> ls = UserList.getRegisteredUsers(author.getId());

                    // check for names whitelisted
                    if (ls != null) {
                        for (Object minecraftNameToRemove : ls) {
                            UnWhitelist(minecraftNameToRemove.toString());
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
                            String message = author.getAsMention() + " successfully removed the following users from the whitelist: \n";
                            for (Object minercaftName : ls) {
                                message += "- " + minercaftName.toString() + "\n";
                            }
                            message += "\n You now have **" + maxWhitelistAmount + " whitelist(s) remaining**.";

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(("Successfully Removed " + author.getName() + "'s Whitelisted Entries"),
                                    message, EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        } else {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-success-title");
                            customTitle = customTitle.replaceAll("\\{Sender}", author.getName());

                            String removedNames = "";
                            for (Object minercaftName : ls) {
                                removedNames += "- " + minercaftName.toString() + "\n";
                            }

                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-success-message");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{RemovedEntries}", removedNames);
                            customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.SUCCESS).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("whitelisted-role-auto-remove")) {
                            // Find all servers bot is in, remove whitelisted roles
                            for (int i = 0; i < javaDiscordAPI.getGuilds().size(); i++) {
                                // Remove the whitelisted role(s)
                                RemoveRolesFromUser(javaDiscordAPI.getGuilds().get(i), author.getId(), Arrays.asList(whitelistedRoleNames));
                            }
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    } else {
                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") triggered whitelist clear. " +
                                "Could not remove any whitelisted entries as they do not have any.");

                        // Log in Discord channel
                        if (!DiscordWhitelister.useCustomMessages) {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("No Entries to Remove",
                                    (author.getAsMention() + ", you do not have any whitelisted entries to remove."), EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        } else {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-failure-title");
                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-failure-message");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                }

                if (!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd() && !authorPermissions.isUserHasLimitedAdd() || authorPermissions.isUserIsBanned()) {
                    QueueAndRemoveAfterSeconds(channel, CreateInsufficientPermsMessage(author));
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }
            }
            else if (messageContents.toLowerCase().startsWith("!clearban") && !DiscordWhitelister.getUseCustomPrefixes()
                    || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customClearBanPrefix))
            {
                if(authorPermissions.isUserCanUseClear())
                {
                    // Check if empty command
                    if(messageContents.toLowerCase().trim().equals("!clearban") && !DiscordWhitelister.getUseCustomPrefixes()
                            || messageContents.toLowerCase().trim().equals(customClearBanPrefix) && DiscordWhitelister.getUseCustomPrefixes())
                    {
                        if(!hideInfoCommandReplies)
                        {
                            TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                            return;
                        }

                        // Send info message
                        if(!DiscordWhitelister.getUseCustomPrefixes())
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("Clear Ban Command", "Usage: `!clearban <minecraftUsername>`\n", EmbedMessageType.INFO).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("Clear Ban Command", "Usage: `" + customClearBanPrefix + " <minecraftUsername>`\n", EmbedMessageType.INFO).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }

                    // If command is not empty check for args
                    // String[] splitMessage = messageContents.toLowerCase().trim().split(" ");

                    int userNameIndex = 1;

                    if(DiscordWhitelister.getUseCustomPrefixes())
                    {
                        String[] customPrefixCount = customClearBanPrefix.trim().split(" ");
                        userNameIndex = customPrefixCount.length; // Don't + 1 as index starts at 0, length doesn't
                    }

                    String targetName = splitMessage[userNameIndex];

                    // Check both removed lists for target name
                    boolean nameFoundInLists = false;

                    // Remove name from removed list if found
                    if(RemovedList.CheckStoreForPlayer(targetName))
                    {
                        RemovedList.getRemovedPlayers().set(targetName, null);
                        RemovedList.SaveStore();

                        nameFoundInLists = true;
                    }

                    if(InGameRemovedList.CheckStoreForPlayer(targetName))
                    {
                        InGameRemovedList.RemoveUserFromStore(targetName);

                        nameFoundInLists = true;
                    }

                    if(nameFoundInLists)
                    {
                        EmbedBuilder clearBanSuccessEmbed;

                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage(("Successfully Cleared `" + targetName + "`"),
                                    (author.getAsMention() + " has successfully cleared `" + targetName + "` from the removed list(s)."), EmbedMessageType.SUCCESS).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-success-title");
                            String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-success-message");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", targetName);
                            customTitle = customTitle.replaceAll("\\{MinecraftUsername}", targetName);

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.INFO).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                    else
                    {
                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage(("Failed to Clear `" + targetName + "`"),
                                    (author.getAsMention() + ", `" + targetName + "` cannot be found in any of the removed lists!"), EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-failure-title");
                            String customMessage = DiscordWhitelister.customMessagesConfig.getFileConfiguration().getString("clear-ban-failure-message");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", targetName);
                            customTitle = customTitle.replaceAll("\\{MinecraftUsername}", targetName);

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                }
                else
                {
                    QueueAndRemoveAfterSeconds(channel, CreateInsufficientPermsMessage(author));
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }
            }

            if(!DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length >= whitelistWhoIsPrefix.length && CheckForPrefix(whitelistWhoIsPrefix, splitMessage)
                    || DiscordWhitelister.getUseCustomPrefixes() && splitMessage.length >= customWhoIsPrefix.length && CheckForPrefix(customWhoIsPrefix, splitMessage))
            {
                CommandWhoIs.ExecuteCommand(messageReceivedEvent, splitMessage);

                if(DiscordWhitelister.removeUnnecessaryMessages)
                    RemoveMessageAfterSeconds(messageReceivedEvent, DiscordWhitelister.removeMessageWaitTime);
                return;
            }

            // if no commands are executed, delete the message, if enabled
            if(DiscordWhitelister.removeUnnecessaryMessages)
            {
                RemoveMessageAfterSeconds(messageReceivedEvent, DiscordWhitelister.removeMessageWaitTime);
            }

            // Warn if enabled
            if(DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("show-warning-in-command-channel"))
            {
                if(!DiscordWhitelister.useCustomMessages)
                {
                    MessageEmbed messageEmbed = CreateEmbeddedMessage("This Channel is for Commands Only", (author.getAsMention() + ", this channel is for commands only, please use another channel."),
                            EmbedMessageType.FAILURE).build();
                    QueueAndRemoveAfterSeconds(channel, messageEmbed);
                }
                else
                {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("command-channel-title");

                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("command-channel-message");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                    MessageEmbed messageEmbed = DiscordClient.CreateEmbeddedMessage(customTitle, customMessage, DiscordClient.EmbedMessageType.FAILURE).build();
                    QueueAndRemoveAfterSeconds(channel, messageEmbed);
                }
            }
        }
    }

    @Override
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event)
    {
        if(!DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("un-whitelist-on-server-leave"))
            return;

        String discordUserToRemove = event.getMember().getId();
        DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Removing their whitelisted entries...");
        List<?> ls =  UserList.getRegisteredUsers(discordUserToRemove);

        if(ls != null)
        {
            for (Object minecraftNameToRemove : ls)
            {
                DiscordWhitelister.getPlugin().getLogger().info(minecraftNameToRemove.toString() + " left. Removing their whitelisted entries.");
                UnWhitelist(minecraftNameToRemove.toString());
            }

            try
            {
                UserList.resetRegisteredUsers(discordUserToRemove);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
            DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Successfully removed their whitelisted entries from the user list.");
        }
        else
        {
            DiscordWhitelister.getPlugin().getLogger().warning(discordUserToRemove + " left. Could not remove any whitelisted entries as they did not whitelist through this plugin.");
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent e)
    {
        CheckForRequiredRole(e);
    }

    private static void CheckForRequiredRole(GuildMemberRoleRemoveEvent e)
    {
        if(!checkForMissingRole)
            return;

        String disName = e.getMember().getEffectiveName();
        String disId = e.getMember().getId();
        String nameForLogger = disName + "(" + disId + ")";

        if(checkAllRoles)
        {
            List<Role> removedRoles = e.getRoles();
            boolean limitedRoleRemoved = false;

            // Check if removed roles contain a limited-add role
            for(Role role:removedRoles)
            {
                if(DiscordWhitelister.useIdForRoles)
                {
                    if(Arrays.asList(allowedToAddLimitedRoles).contains(role.getId()))
                    {
                        limitedRoleRemoved = true;
                        break;
                    }
                }
                else
                {
                    if(Arrays.asList(allowedToAddLimitedRoles).contains(role.getName()))
                    {
                        limitedRoleRemoved = true;
                        break;
                    }
                }
            }

            if(!limitedRoleRemoved)
                return;

            DiscordWhitelister.getPlugin().getLogger().info(nameForLogger + "'s limited role(s) has been removed. Checking for remaining roles...");
            boolean rolesRemaining= false;
            for(int i = 0; i < javaDiscordAPI.getGuilds().size(); i++)
            {
                Member member = javaDiscordAPI.getGuilds().get(i).getMemberById(disId);
                if(member != null)
                {
                    List<Role> roles = member.getRoles();
                    for(Role role:roles)
                    {
                        if(DiscordWhitelister.useIdForRoles)
                        {
                            if(Arrays.asList(combinedRoles).contains(role.getId()))
                            {
                                rolesRemaining = true;
                                break;
                            }
                        }
                        else
                        {
                            if(Arrays.asList(combinedRoles).contains(role.getName()))
                            {
                                rolesRemaining = true;
                                break;
                            }
                        }
                    }
                }
            }

            if(!rolesRemaining)
            {
                DiscordWhitelister.getPlugin().getLogger().info(nameForLogger + " has no roles remaining. Removing their whitelisted entries...");

                List<?> ls =  UserList.getRegisteredUsers(disId);
                if(ls != null)
                {
                    for (Object minecraftNameToRemove : ls)
                    {
                        UnWhitelist(minecraftNameToRemove.toString());
                    }

                    try
                    {
                        UserList.resetRegisteredUsers(disId);
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace();
                    }
                }
                else
                {
                    DiscordWhitelister.getPlugin().getLogger().warning(nameForLogger + " does not have any whitelisted entries doing nothing...");
                }
            }
            else
            {
                DiscordWhitelister.getPlugin().getLogger().info(nameForLogger + " has role(s) remaining. Doing nothing...");
            }
        }
        else
        {
            if(roleToCheck == null || roleToCheck.equals(""))
            {
                DiscordWhitelister.getPluginLogger().warning("'un-whitelist-if-missing-role' is enabled but " +
                        "'role-to-check-for' is null or empty, please double check the config");
                return;
            }

            for(Role r : e.getMember().getRoles())
            {
                // required role found, no need to proceed
                if(DiscordWhitelister.useIdForRoles)
                {
                    if(r.getId().equals(roleToCheck))
                        return;
                }
                else
                {
                    if(r.getName().equals(roleToCheck))
                        return;
                }
            }

            DiscordWhitelister.getPluginLogger().info(nameForLogger + " does not have the required " +
                    "role (" + roleToCheck + "). Attempting to remove their whitelisted entries...");

            List<?> regUsers = UserList.getRegisteredUsers(disId);
            if(regUsers != null)
            {
                if(regUsers.size() <= 0)
                {
                    DiscordWhitelister.getPluginLogger().info(nameForLogger + "'s entries are empty, doing nothing");
                    return;
                }

                for(Object mcName : regUsers)
                {
                    UnWhitelist(mcName.toString());
                }

                try
                {
                    UserList.resetRegisteredUsers(disId);
                }
                catch (IOException ex)
                {
                    DiscordWhitelister.getPluginLogger().severe("Failed to remove whitelisted users from " +
                            nameForLogger);
                    ex.printStackTrace();
                    return;
                }

                DiscordWhitelister.getPluginLogger().info("Successfully removed " + nameForLogger +
                        "'s whitelisted entries due to missing required role (" + roleToCheck + ")");
            }
            else
            {
                DiscordWhitelister.getPluginLogger().warning("Failed to remove whitelisted entries from " +
                        nameForLogger + " as they did not whitelist through this plugin");
            }
        }
    }

    public static void RequiredRoleStartupCheck() {
            if (!checkForMissingRole)
                return;

        // Don't attempt to remove roles if not connected
        if (javaDiscordAPI.getStatus() != JDA.Status.CONNECTED)
            return;

        if (checkAllRoles) {
            DiscordWhitelister.getPluginLogger().info("Checking Discord IDs for required roles...");

            Set<String> keys = UserList.getUserList().getKeys(false);
            // Make sure the user list is not empty
            if (keys.size() == 0) {
                return;
            }

            for (String userId : keys) {
                // Check all guilds
                boolean rolesRemaining = false;
                for (int i = 0; i < javaDiscordAPI.getGuilds().size(); i++) {
                    Member member = javaDiscordAPI.getGuilds().get(i).getMemberById(userId);

                    if (member != null) {
                        List<Role> roles = member.getRoles();
                        for (Role role : roles) {
                            if (DiscordWhitelister.useIdForRoles) {
                                if (Arrays.asList(combinedRoles).contains(role.getId())) {
                                    rolesRemaining = true;
                                    break;
                                }
                            } else {
                                if (Arrays.asList(combinedRoles).contains(role.getName())) {
                                    rolesRemaining = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!rolesRemaining) {
                    DiscordWhitelister.getPlugin().getLogger().info(userId + " has no roles remaining. Removing their whitelisted entries...");
                    List<?> registeredUsers = UserList.getRegisteredUsers(userId);
                    if (registeredUsers == null || registeredUsers.size() <= 0) {
                        DiscordWhitelister.getPluginLogger().info("User ID: " + userId + "has no whitelisted users, doing nothing...");
                    } else {
                        for (Object wUser : registeredUsers) {
                            if (wUser instanceof String) {
                                UnWhitelist((String) wUser);
                            }
                        }
                        // Clear entries in user-list
                        UserList.getUserList().set(userId, null);
                        UserList.SaveStore();
                        DiscordWhitelister.getPlugin().getLogger().info("Successfully removed " + userId + " whitelisted entries from the user list.");
                    }
                }
            }
        } else {
            if (roleToCheck == null || roleToCheck.equals("")) {
                DiscordWhitelister.getPluginLogger().warning("'un-whitelist-if-missing-role' is enabled but " +
                        "'role-to-check-for' is null or empty, please double check the config");
                return;
            }

            DiscordWhitelister.getPluginLogger().info("Checking Discord IDs for required role " + roleToCheck);

            Set<String> keys = UserList.getUserList().getKeys(false);
            // Make sure the user list is not empty
            if (keys.size() == 0) {
                return;
            }

            for (String userId : keys) {
                // Check all guilds
                boolean requiredRole = false;
                for (int i = 0; i < javaDiscordAPI.getGuilds().size(); i++) {
                    Member member = javaDiscordAPI.getGuilds().get(i).getMemberById(userId);
                    if (member != null) {
                        for (Role role : member.getRoles()) {
                            if (DiscordWhitelister.useIdForRoles) {
                                if (role.getId().equals(roleToCheck)) {
                                    requiredRole = true;
                                    break;
                                }
                            } else {
                                if (role.getName().equals(roleToCheck)) {
                                    requiredRole = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!requiredRole) {
                    List<?> registeredUsers = UserList.getRegisteredUsers(userId);
                    if (registeredUsers == null || registeredUsers.size() <= 0) {
                        DiscordWhitelister.getPluginLogger().info("User ID: " + userId + "has no whitelisted users, doing nothing...");
                    } else {
                        for (Object wUser : registeredUsers) {
                            if (wUser instanceof String) {
                                UnWhitelist((String) wUser);
                                DiscordWhitelister.getPluginLogger().info("Removed " + (String) wUser
                                        + " from the whitelist as Discord ID: " + userId + " due to missing required role (" + roleToCheck + ").");
                            }
                        }
                        UserList.getUserList().set(userId, null);
                        UserList.SaveStore();
                        DiscordWhitelister.getPlugin().getLogger().info("Successfully removed " + userId + " whitelisted entries from the user list.");

                    }
                }
            }
        }
    }

    public static void ServerLeaveStartupCheck() {
        if (DiscordWhitelister.mainConfig.getFileConfiguration().getBoolean("un-whitelist-on-server-leave")) {

            // Don't attempt to remove members if not connected
            if (javaDiscordAPI.getStatus() != JDA.Status.CONNECTED)
                return;

            DiscordWhitelister.getPluginLogger().info("Checking Discord IDs for leavers...");

            Set<String> keys = UserList.getUserList().getKeys(false);
            // Make sure the user list is not empty
            if (keys.size() == 0) {
                return;
            }

            for (String userId : keys) {
                // Check if the ID is in any guilds
                boolean inGuild = false;

                // Check all guilds
                for (int i = 0; i < javaDiscordAPI.getGuilds().size(); i++) {
                    if (javaDiscordAPI.getGuilds().get(i).getMemberById(userId) != null)
                        inGuild = true;
                }

                // un-whitelist associated minecraft usernames if not in any guilds
                if (!inGuild) {
                    List<?> registeredUsers = UserList.getRegisteredUsers(userId);
                    for (Object wUser : registeredUsers) {
                        if (wUser instanceof String) {
                            DiscordWhitelister.getPluginLogger().info("Removed " + (String) wUser
                                    + " from the whitelist as Discord ID: " + userId + " has left the server.");
                        }
                    }
                    UserList.getUserList().set(userId, null);
                    UserList.SaveStore();
                    DiscordWhitelister.getPlugin().getLogger().info("Discord ID: " + userId
                            + " left. Successfully removed their whitelisted entries from the user list.");
                }
            }
        }
    }

    // Find all occurrences of the target player and remove them
    private static void ClearPlayerFromUserList(String targetName) {
        // Just in-case
        targetName = targetName.toLowerCase();

        // Get a list of all IDs that contain targetName - shouldn't ever really happen
        List<String> idsContainingTargetName = new LinkedList<>();

        Set<String> keys = UserList.getUserList().getKeys(false);
        // Make sure the user list is not empty
        if (keys.size() == 0) {
            return;
        }
        // Search for name and Id linked to it
        for (String userId : keys) {
            List<?> registeredUsers = UserList.getRegisteredUsers(userId);
            for (Object wUser : registeredUsers) {
                if (wUser.equals(targetName)) {
                    // Found the target name, add ID to list
                    idsContainingTargetName.add(userId);
                }
            }
        }

        // Check if we found any IDs
        if (idsContainingTargetName.size() > 0) {
            DiscordWhitelister.getPluginLogger().info("Found " + idsContainingTargetName.size() + " occurrence(s) of " + targetName + " in the user list, removing...");

            for (String s : idsContainingTargetName) {
                // Get the IDs whitelisted users
                List<String> newWhitelistedUsers = UserList.getUserList().getStringList(s);

                if (newWhitelistedUsers.size() > 1) {
                    newWhitelistedUsers.remove(targetName);
                    UserList.getUserList().set(s, newWhitelistedUsers);
                } else {
                    // Double check the 1 whitelisted user == targetName
                    if (newWhitelistedUsers.get(0).equals(targetName))
                        UserList.getUserList().set(s, null);
                }

                UserList.SaveStore();
            }
        }
    }


    public static String minecraftUsernameToUUID(String minecraftUsername)
    {
        URL playerURL;
        String inputStream;
        BufferedReader bufferedReader;

        String playerUUID = null;

        try {
            playerURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + minecraftUsername);
            bufferedReader = new BufferedReader(new InputStreamReader(playerURL.openStream()));
            inputStream = bufferedReader.readLine();

            if (inputStream != null) {
                JSONObject inputStreamObject = (JSONObject) JSONValue.parseWithException(inputStream);
                playerUUID = inputStreamObject.get("id").toString();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return playerUUID;
    }

    public static void ExecuteServerCommand(String command)
    {
        DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), ()
                -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(
                DiscordWhitelister.getPlugin().getServer().getConsoleSender(), command));
    }

    private enum SenderType { CONSOLE, PLAYER, UNKNOWN }
    public static void CheckAndExecuteCommand(String configInput, String playerTargetName)
    {
        SenderType senderType;

        // Check command sender type
        if(configInput.startsWith("CONSOLE"))
            senderType = SenderType.CONSOLE;
        else if(configInput.startsWith("PLAYER"))
            senderType = SenderType.PLAYER;
        else
            senderType = SenderType.UNKNOWN;

        if(senderType.equals(SenderType.UNKNOWN))
        {
            DiscordWhitelister.getPluginLogger().warning("Unknown command sender type (should be one of the following: CONSOLE, PLAYER), offending line: " + configInput);
            return;
        }

        // Get command which is after the first :
        String commandToSend = configInput.substring(configInput.indexOf(":") + 1);
        // Set player name if %PLAYER% is used
        final String commandToSendFinal = commandToSend.replaceAll("%PLAYER%", playerTargetName);

        if(senderType.equals(SenderType.CONSOLE))
        {
            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(),
                    () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(), commandToSendFinal));
        }
        else
        {
            DiscordWhitelister.getPlugin().getServer().getPlayer(playerTargetName).performCommand(commandToSendFinal);
        }
    }

    // use this multiple times when checking all guilds
    // Accepts a single role in the form of a singleton list
    public static void AssignRolesToUser(Guild targetGuild, String targetUserId, List<String> targetRole)
    {
        // Check if the user is in the targetGuild
        if(targetGuild.getMemberById(targetUserId) == null)
        {
            DiscordWhitelister.getPluginLogger().warning("User cannot be found in Guild " + targetGuild.getName()
                    + "(" + targetGuild.getId() + ")" +". Will not attempt to assign role(s)");

            return;
        }

        // Locate target role(s)
        LinkedList<Role> rolesFound = new LinkedList<>();
        for(int i = 0; i < targetRole.size(); i++)
        {
            List<Role> tempFoundRoles;

            if(!DiscordWhitelister.useIdForRoles)
                tempFoundRoles = targetGuild.getRolesByName(targetRole.get(i), false);
            else
                tempFoundRoles = Collections.singletonList(targetGuild.getRoleById(targetRole.get(i)));

            if(tempFoundRoles.size() > 0)
            {
                rolesFound.addAll(tempFoundRoles);
            }
            else
            {
                String discordUserName = targetGuild.getMemberById(targetUserId).getEffectiveName();
                DiscordWhitelister.getPluginLogger().warning("Failed to assign role " + targetRole.get(i)
                        + " to user " + discordUserName + "(" + targetUserId + ")" + " as it could not be found in "
                        + targetGuild.getName() + "(" + targetGuild.getId() + ")");
            }
        }

        // Check if any roles were found
        if(rolesFound.size() > 0)
        {
            // Assign the roles
            for(int i = 0; i < rolesFound.size(); i++)
            {
                targetGuild.addRoleToMember(targetGuild.getMemberById(targetUserId), rolesFound.get(i)).queue();
            }
        }
    }

    public static void RemoveRolesFromUser(Guild targetGuild, String targetUserId, List<String> targetRole)
    {
        // Check if the user is in the targetGuild
        if(targetGuild.getMemberById(targetUserId) == null)
        {
            DiscordWhitelister.getPluginLogger().warning("User cannot be found in Guild " + targetGuild.getName()
                    + "(" + targetGuild.getId() + ")" +". Will not attempt to remove role(s)");

            return;
        }

        // Locate target role(s)
        LinkedList<Role> rolesFound = new LinkedList<>();
        for(int i = 0; i < targetRole.size(); i++)
        {
            List<Role> tempFoundRoles;

            if(!DiscordWhitelister.useIdForRoles)
                tempFoundRoles = targetGuild.getRolesByName(targetRole.get(i), false);
            else
                tempFoundRoles = Collections.singletonList(targetGuild.getRoleById(targetRole.get(i)));

            if(tempFoundRoles.size() > 0)
            {
                rolesFound.addAll(tempFoundRoles);
            }
            else
            {
                String discordUserName = targetGuild.getMemberById(targetUserId).getEffectiveName();
                DiscordWhitelister.getPluginLogger().warning("Failed to remove role " + targetRole.get(i)
                        + " from user " + discordUserName + "(" + targetUserId + ")" + " as it could not be found in "
                        + targetGuild.getName() + "(" + targetGuild.getId() + ")");
            }
        }

        // Check if any roles were found
        if(rolesFound.size() > 0)
        {
            // Remove the roles
            for(int i = 0; i < rolesFound.size(); i++)
            {
                targetGuild.removeRoleFromMember(targetGuild.getMemberById(targetUserId), rolesFound.get(i)).queue();
            }
        }
    }


    public static void RemoveMessageAfterSeconds(MessageReceivedEvent messageReceivedEvent, Integer timeToWait)
    {
        Thread removeTimerThread = new Thread(() ->
        {
            try
            {
                TimeUnit.SECONDS.sleep(timeToWait);
                messageReceivedEvent.getMessage().delete().queue();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });

        removeTimerThread.start();
    }

    public static void QueueAndRemoveAfterSeconds(TextChannel textChannel, MessageEmbed messageEmbed)
    {
        if(DiscordWhitelister.removeUnnecessaryMessages)
            textChannel.sendMessage(messageEmbed).queue(message -> message.delete().queueAfter(DiscordWhitelister.removeMessageWaitTime, TimeUnit.SECONDS));
        else
            textChannel.sendMessage(messageEmbed).queue();
    }

    public static void TempRemoveOriginalMessageAfterSeconds(MessageReceivedEvent messageReceivedEvent)
    {
        if(DiscordWhitelister.removeUnnecessaryMessages)
            RemoveMessageAfterSeconds(messageReceivedEvent, DiscordWhitelister.removeMessageWaitTime);
    }


    // TODO: improve, not go through console commands
    public static void AssignPerms(String targetPlayerName){
        // For ultra perms:
        if(DiscordWhitelister.useLuckPerms){
            for (String s : DiscordWhitelister.customPrefixConfig.getFileConfiguration().getStringList("perms-on-whitelist")) {
                DiscordClient.ExecuteServerCommand("lp user " + targetPlayerName + " permission set " + s);
            }
        }
        // For LuckPerms:
        if(DiscordWhitelister.useUltraPerms){
            for (String s : DiscordWhitelister.customPrefixConfig.getFileConfiguration().getStringList("perms-on-whitelist")) {
                DiscordClient.ExecuteServerCommand("upc addPlayerPermission " + targetPlayerName + " " + s);
            }
        }
    }

    public static void RemovePerms(String targetPlayerName){
        // For ultra perms:
        if(DiscordWhitelister.useLuckPerms){
            for (String s : DiscordWhitelister.customPrefixConfig.getFileConfiguration().getStringList("perms-on-whitelist")) {
                DiscordClient.ExecuteServerCommand("lp user " + targetPlayerName + " permission unset " + s);
            }
        }
        // For LuckPerms:
        if(DiscordWhitelister.useUltraPerms){
            for (String s : DiscordWhitelister.customPrefixConfig.getFileConfiguration().getStringList("perms-on-whitelist")) {
                DiscordClient.ExecuteServerCommand("upc removePlayerPermission " + targetPlayerName + " " + s);
            }
        }
    }

    // Remove player from whitelist
    public static void UnWhitelist(String minecraftNameToRemove) {
        if (WhitelistedPlayers.usingEasyWhitelist) {
            ExecuteServerCommand("easywl remove " + minecraftNameToRemove);
        } else {
            ExecuteServerCommand("whitelist remove " + minecraftNameToRemove);
        }
        // Clear permissions
        RemovePerms(minecraftNameToRemove);
    }
}
