package uk.co.angrybee.joe;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;
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
import javax.xml.soap.Text;
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
        // assign vars here instead of every time a message is received, as they do not change
        targetTextChannels = new String[MainConfig.getMainConfig().getList("target-text-channels").size()];
        for (int i = 0; i < targetTextChannels.length; ++i)
        {
            targetTextChannels[i] = MainConfig.getMainConfig().getList("target-text-channels").get(i).toString();
        }

        maxWhitelistAmount = MainConfig.getMainConfig().getInt("max-whitelist-amount");
        limitedWhitelistEnabled = MainConfig.getMainConfig().getBoolean("limited-whitelist-enabled");
        usernameValidation = MainConfig.getMainConfig().getBoolean("username-validation");

        // Set the name of the role to add/remove to/from the user after they have been added/removed to/from the whitelist and if this feature is enabled
        whitelistedRoleAutoAdd = MainConfig.getMainConfig().getBoolean("whitelisted-role-auto-add");
        whitelistedRoleAutoRemove = MainConfig.getMainConfig().getBoolean("whitelisted-role-auto-remove");


        whitelistedRoleNames = new String[MainConfig.getMainConfig().getList("whitelisted-roles").size()];
        for(int i = 0; i < whitelistedRoleNames.length; i++)
        {
            whitelistedRoleNames[i] = MainConfig.getMainConfig().getList("whitelisted-roles").get(i).toString();
        }
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
                addCommandExample = CustomPrefixConfig.getCustomPrefixesConfig().getString("whitelist-add-prefix").trim();

            instructionalMessageEmbed = CreateEmbeddedMessage("How to Whitelist", ("Use `" + addCommandExample + " <minecraftUsername>` to whitelist yourself.\n" +
                    "In the case of whitelisting an incorrect name, please contact a staff member to clear it from the whitelist."), EmbedMessageType.INFO).build();
        }
        else
        {
            String customTitle = CustomMessagesConfig.getCustomMessagesConfig().getString("instructional-message-title");
            String customMessage = CustomMessagesConfig.getCustomMessagesConfig().getString("instructional-message");

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
                if(MainConfig.getMainConfig().getBoolean("hide-info-command-replies"))
                    return;

                CommandInfo.ExecuteCommand(messageReceivedEvent);

                if(DiscordWhitelister.removeUnnecessaryMessages)
                    RemoveMessageAfterSeconds(messageReceivedEvent, DiscordWhitelister.removeMessageWaitTime);
                return;
            }

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
                        if(!MainConfig.getMainConfig().getBoolean("hide-info-command-replies"))
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
                            if (WhitelistedPlayers.usingEasyWhitelist)
                                ExecuteServerCommand("easywl remove " + finalNameToRemove);
                            else
                                ExecuteServerCommand("whitelist remove " + finalNameToRemove);

                            // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                            EmbedBuilder embedBuilderSuccess;

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                if(!MainConfig.getMainConfig().getBoolean("set-removed-message-colour-to-red"))
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

                                if(!MainConfig.getMainConfig().getBoolean("set-removed-message-colour-to-red"))
                                    embedBuilderSuccess = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.SUCCESS);
                                else
                                    embedBuilderSuccess = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE);
                            }

                            if(DiscordWhitelister.showPlayerSkin)
                            {
                                String playerUUID = DiscordClient.minecraftUsernameToUUID(finalNameToRemove);

                                if(!MainConfig.getMainConfig().getBoolean("use-crafatar-for-avatars"))
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

                                    if(DiscordWhitelister.useUltraPerms)
                                    {
                                        RemovePermsFromUser(finalNameToRemove, PermissionsConfig.getPermissionsConfig().getStringList("perms-on-whitelist"));
                                    }

                                    if(whitelistedRoleAutoRemove)
                                    {
                                        List<String> whitelistRoles = new LinkedList<>();

                                        for (String whitelistedRoleName : whitelistedRoleNames)
                                        {
                                            whitelistRoles.add(whitelistedRoleName);
                                        }

                                        // Find the Discord Id linked to the removed name
                                        Boolean idFound = false;
                                        String targetDiscordId = "";
                                        List<String> targetWhitelistedPlayers = Collections.emptyList();

                                        Yaml userYaml = new Yaml();
                                        InputStream inputStream = new FileInputStream(UserList.getUserListFile());
                                        Map<String, List<String>> userListObject = userYaml.load(inputStream);

                                        for (Map.Entry<String, List<String>> entry : userListObject.entrySet())
                                        {
                                            for (int i = 0; i < entry.getValue().size(); i++)
                                            {
                                                if (entry.getValue().get(i).equals(finalNameToRemove))
                                                {
                                                    // Found the ban target, assign the corresponding Discord id
                                                    targetDiscordId = entry.getKey();
                                                    targetWhitelistedPlayers = entry.getValue();
                                                    idFound = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if(idFound)
                                        {
                                            boolean namesRemainingAfterRemoval = false;

                                            if((targetWhitelistedPlayers.size() - 1) > 0)
                                            {
                                                namesRemainingAfterRemoval = true;
                                                DiscordWhitelister.getPluginLogger().info("The Discord ID (" + targetDiscordId + ") linked to " + finalNameToRemove + " contains "
                                                        + (targetWhitelistedPlayers.size() - 1) + " more whitelisted user(s), not removing whitelisted roles...");
                                            }

                                            // Find all servers bot is in, remove whitelisted roles
                                            if(!whitelistRoles.isEmpty() && !namesRemainingAfterRemoval)
                                            {
                                                for (int i = 0; i < DiscordClient.javaDiscordAPI.getGuilds().size(); i++)
                                                {
                                                    // Remove the whitelisted role(s)
                                                    RemoveRolesFromUser(javaDiscordAPI.getGuilds().get(i), targetDiscordId, whitelistRoles);
                                                    DiscordWhitelister.getPluginLogger().info("Successfully removed whitelisted roles from "
                                                            + targetDiscordId + "(" + finalNameToRemove + ") in guild: " + javaDiscordAPI.getGuilds().get(i).getName());
                                                }
                                            }
                                            else if (whitelistRoles.isEmpty())
                                            {
                                                DiscordWhitelister.getPluginLogger().warning("Cannot remove any whitelisted roles from: " + targetDiscordId + "(" + finalNameToRemove + ") as there are none specified in the config");
                                            }
                                        }
                                        else
                                        {
                                            DiscordWhitelister.getPluginLogger().warning("Could not find any Discord id linked to Minecraft name: " + finalNameToRemove + ", therefore cannot remove any roles");
                                        }

                                        ClearPlayerFromUserList(finalNameToRemove);
                                    }

                                    // if the name is not on the removed list
                                    if (!RemovedList.CheckStoreForPlayer(finalNameToRemove))
                                    {
                                        RemovedList.getRemovedPlayers().set(finalNameToRemove, author.getId());
                                        RemovedList.SaveStore();
                                    }
                                }
                                else
                                {
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
                    String higherPermRoles = MainConfig.getMainConfig().getList("add-remove-roles").toString();
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
            if(messageContents.toLowerCase().startsWith("!clearname") && !DiscordWhitelister.getUseCustomPrefixes()
                || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customClearNamePrefix))
            {
                // !clearname <targetName>
                // TODO: !clearnames <@DiscordID>

                // Check permissions
                if(authorPermissions.isUserCanUseClear())
                {
                    if(messageContents.toLowerCase().trim().equals("!clearname") && !DiscordWhitelister.getUseCustomPrefixes()
                        || messageContents.toLowerCase().trim().equals(customClearNamePrefix) && DiscordWhitelister.getUseCustomPrefixes())
                    {
                        if(!MainConfig.getMainConfig().getBoolean("hide-info-command-replies"))
                        {
                            TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                            return;
                        }

                        if(!DiscordWhitelister.getUseCustomPrefixes())
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("Clear Name Command", "Usage: `!clearname <minecraftUsername>`\n", EmbedMessageType.INFO).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("Clear Name Command", "Usage: `" + customClearNamePrefix + " <minecraftUsername>`\n", EmbedMessageType.INFO).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }

                    // If command is not empty check for args
                    // String[] splitMessage = messageContents.toLowerCase().trim().split(" "); // TODO

                    int userNameIndex = 1;

                    if(DiscordWhitelister.getUseCustomPrefixes())
                    {
                        String[] customPrefixCount = customClearNamePrefix.trim().split(" ");
                        userNameIndex = customPrefixCount.length; // Don't + 1 as index starts at 0, length doesn't
                    }

                    // Search for target name & linked ID
                    Boolean nameFound = false;
                    String targetDiscordId = "";
                    List<String> targetWhitelistedPlayers = Collections.emptyList();
                    int nameToClearIndex = 0;

                    Yaml userYaml = new Yaml();

                    try
                    {
                        InputStream inputStream = new FileInputStream(UserList.getUserListFile());

                        // Check if input stream is empty
                        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
                        int b = pushbackInputStream.read();

                        // Make sure the user list is not empty
                        if(b != -1)
                        {
                            pushbackInputStream.unread(b);
                            Map<String, List<String>> userListObject = userYaml.load(pushbackInputStream);

                            // Search for name and Id linked to it
                            for(Map.Entry<String, List<String>> entry : userListObject.entrySet())
                            {
                                for(int i = 0; i < entry.getValue().size(); i++)
                                {
                                    if(entry.getValue().get(i).equals(splitMessage[userNameIndex])) // Target name
                                    {
                                        // Found the target name
                                        targetDiscordId = entry.getKey();
                                        targetWhitelistedPlayers = entry.getValue();
                                        nameToClearIndex = i;
                                        nameFound = true;
                                    }
                                }
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    if(nameFound)
                    {
                        List<String> updatedTargetWhitelistedPlayers = targetWhitelistedPlayers;

                        // Check count - clear id entirely if only 1 entry
                        if(updatedTargetWhitelistedPlayers.size() > 1)
                        {
                            updatedTargetWhitelistedPlayers.remove(nameToClearIndex); // Clear name

                            // Set the updated list in the config
                            UserList.getUserList().set(targetDiscordId, updatedTargetWhitelistedPlayers);
                        }
                        else // Remove entirely
                        {
                            UserList.getUserList().set(targetDiscordId, null);
                        }

                        UserList.SaveStore();

                        // Un-whitelist and remove perms if enabled
                        if(MainConfig.getMainConfig().getBoolean("unwhitelist-and-clear-perms-on-name-clear"))
                        {
                            // Remove name from the whitelist
                            if(!WhitelistedPlayers.usingEasyWhitelist)
                            {
                                DiscordClient.ExecuteServerCommand("whitelist remove " + splitMessage[userNameIndex]);
                            }
                            else
                            {
                                DiscordClient.ExecuteServerCommand("easywl remove " + splitMessage[userNameIndex]);
                            }

                            // Clear permissions
                            if(DiscordWhitelister.useUltraPerms)
                                DiscordClient.RemovePermsFromUser(splitMessage[userNameIndex], PermissionsConfig.getPermissionsConfig().getStringList("perms-on-whitelist"));
                        }

                        // Success message
                        if(DiscordWhitelister.useCustomMessages)
                        {
                            String clearNameTitle = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-name-success-title");
                            String clearNameMessage = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-name-success-message");

                            clearNameMessage = clearNameMessage.replaceAll("\\{Sender}", author.getAsMention());
                            clearNameMessage = clearNameMessage.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);
                            clearNameMessage = clearNameMessage.replaceAll("\\{DiscordID}", "<@" + targetDiscordId + ">");

                            clearNameTitle = clearNameTitle.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(clearNameTitle, clearNameMessage, EmbedMessageType.SUCCESS).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("Successfully Cleared Name", (author.getAsMention() + " successfully cleared username `" + splitMessage[userNameIndex] +
                                    "` from <@" + targetDiscordId + ">'s whitelisted users."), EmbedMessageType.SUCCESS).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                    else
                    {
                        // Name not found
                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            MessageEmbed messageEmbed =
                                    CreateEmbeddedMessage((splitMessage[userNameIndex] + " not Found"),
                                            (author.getAsMention() + ", could not find name " + splitMessage[userNameIndex] + " to clear in user list."), EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            String customTitle = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-name-failure-title");
                            String customMessage = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-name-failure-message");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);
                            customTitle = customTitle.replaceAll("\\{MinecraftUsername}", splitMessage[userNameIndex]);

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle, customMessage, EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                }
                else // Don't have permission
                {
                    QueueAndRemoveAfterSeconds(channel, CreateInsufficientPermsMessage(author));
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }
            }

            // Clear whitelists for limited-whitelisters
            if(messageContents.toLowerCase().startsWith("!whitelist clear") && !DiscordWhitelister.getUseCustomPrefixes()
                    || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customLimitedWhitelistClearPrefix))
            {
                if(!MainConfig.getMainConfig().getBoolean("allow-limited-whitelisters-to-unwhitelist-self"))
                    return;

                // just inform staff, can add custom messages later if really needed
                if(authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserIsBanned() || authorPermissions.isUserCanAdd() && !authorPermissions.isUserIsBanned())
                {
                    MessageEmbed messageEmbed = CreateEmbeddedMessage("This Command is Only Available for Limited Whitelister Roles",
                            "If staff members need to clear a name from the whitelist please use `!clearname <mcName>`.", EmbedMessageType.INFO).build();
                    QueueAndRemoveAfterSeconds(channel, messageEmbed);
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }

                if(authorPermissions.isUserHasLimitedAdd() && !authorPermissions.isUserIsBanned())
                {
                    List<?> ls =  UserList.getRegisteredUsers(author.getId());

                    // check for names whitelisted
                    if(ls != null)
                    {
                        for (Object minecraftNameToRemove : ls)
                        {
                            if (WhitelistedPlayers.usingEasyWhitelist)
                            {
                                ExecuteServerCommand("easywl remove " + minecraftNameToRemove.toString());
                            }
                            else
                            {
                                ExecuteServerCommand("whitelist remove " + minecraftNameToRemove.toString());
                            }
                        }

                        try
                        {
                            UserList.resetRegisteredUsers(author.getId());
                        }
                        catch (IOException e)
                        {
                            DiscordWhitelister.getPluginLogger().severe("Failed to remove" + author.getId() + "'s entries.");
                            e.printStackTrace();
                            return;
                        }

                        DiscordWhitelister.getPlugin().getLogger().info( author.getName() + "(" + author.getId() + ") triggered whitelist clear. " +
                                "Successfully removed their whitelisted entries from the user list.");

                        // Log in Discord channel
                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            String message = author.getAsMention() + " successfully removed the following users from the whitelist: \n";
                            for (Object minercaftName : ls)
                            {
                                message += "- " + minercaftName.toString() + "\n";
                            }
                            message += "\n You now have **" + maxWhitelistAmount + " whitelist(s) remaining**.";

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(("Successfully Removed " + author.getName() + "'s Whitelisted Entries"),
                                    message, EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-success-title");
                            customTitle = customTitle.replaceAll("\\{Sender}", author.getName());

                            String removedNames = "";
                            for (Object minercaftName : ls)
                            {
                                removedNames += "- " + minercaftName.toString() + "\n";
                            }

                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-clear-success-message");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{RemovedEntries}", removedNames);
                            customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                            MessageEmbed messageEmbed = CreateEmbeddedMessage(customTitle,customMessage,EmbedMessageType.SUCCESS).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }

                        if(MainConfig.getMainConfig().getBoolean("whitelisted-role-auto-remove"))
                        {
                            // Find all servers bot is in, remove whitelisted roles
                            for(int i = 0; i < javaDiscordAPI.getGuilds().size(); i++)
                            {
                                // Remove the whitelisted role(s)
                                RemoveRolesFromUser(javaDiscordAPI.getGuilds().get(i), author.getId(), Arrays.asList(whitelistedRoleNames));
                            }
                        }

                        TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                        return;
                    }
                    else
                    {
                        DiscordWhitelister.getPlugin().getLogger().info( author.getName() + "(" + author.getId() + ") triggered whitelist clear. " +
                                "Could not remove any whitelisted entries as they do not have any.");

                        // Log in Discord channel
                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            MessageEmbed messageEmbed = CreateEmbeddedMessage("No Entries to Remove",
                                    (author.getAsMention() + ", you do not have any whitelisted entries to remove."), EmbedMessageType.FAILURE).build();
                            QueueAndRemoveAfterSeconds(channel, messageEmbed);
                        }
                        else
                        {
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

                if(!authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd() && !authorPermissions.isUserHasLimitedAdd() || authorPermissions.isUserIsBanned())
                {
                    QueueAndRemoveAfterSeconds(channel, CreateInsufficientPermsMessage(author));
                    TempRemoveOriginalMessageAfterSeconds(messageReceivedEvent);
                    return;
                }
            }

            if(messageContents.toLowerCase().startsWith("!clearban") && !DiscordWhitelister.getUseCustomPrefixes()
                || DiscordWhitelister.getUseCustomPrefixes() && messageContents.toLowerCase().startsWith(customClearBanPrefix))
            {
                if(authorPermissions.isUserCanUseClear())
                {
                    // Check if empty command
                    if(messageContents.toLowerCase().trim().equals("!clearban") && !DiscordWhitelister.getUseCustomPrefixes()
                            || messageContents.toLowerCase().trim().equals(customClearBanPrefix) && DiscordWhitelister.getUseCustomPrefixes())
                    {
                        if(!MainConfig.getMainConfig().getBoolean("hide-info-command-replies"))
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
                            String customTitle = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-ban-success-title");
                            String customMessage = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-ban-success-message");
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
                            String customTitle = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-ban-failure-title");
                            String customMessage = CustomMessagesConfig.getCustomMessagesConfig().getString("clear-ban-failure-message");
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
            if(MainConfig.getMainConfig().getBoolean("show-warning-in-command-channel"))
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
        if(!MainConfig.getMainConfig().getBoolean("un-whitelist-on-server-leave"))
            return;

        String discordUserToRemove = event.getMember().getId();
        DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Removing their whitelisted entries...");
        List<?> ls =  UserList.getRegisteredUsers(discordUserToRemove);

        if(ls != null)
        {
            for (Object minecraftNameToRemove : ls)
            {
                DiscordWhitelister.getPlugin().getLogger().info(minecraftNameToRemove.toString() + " left. Removing their whitelisted entries.");
                if (WhitelistedPlayers.usingEasyWhitelist)
                {
                    ExecuteServerCommand("easywl remove " + minecraftNameToRemove.toString());
                } else {
                    ExecuteServerCommand("whitelist remove " + minecraftNameToRemove.toString());
                }
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

    public static void StartUpMemberCheck() throws IOException
    {
        if(!MainConfig.getMainConfig().getBoolean("un-whitelist-on-server-leave"))
            return;

        // Don't attempt to remove members if not connected
        if(javaDiscordAPI.getStatus() != JDA.Status.CONNECTED)
            return;

        DiscordWhitelister.getPluginLogger().info("Checking Discord IDs for leavers...");

        Yaml idYaml = new Yaml();
        UserList.SaveStore();
        InputStream inputStream = new FileInputStream(UserList.getUserListFile());

        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
        int b = pushbackInputStream.read();

        if(b == -1)
            return;
        else
            pushbackInputStream.unread(b);

        Map<String, List<String>> userObject = idYaml.load(pushbackInputStream);

        for(Map.Entry<String, List<String>> entry : userObject.entrySet())
        {
            // Check if the ID is in any guilds
            boolean inGuild = false;

            // Check all guilds
            for(int i = 0; i < javaDiscordAPI.getGuilds().size(); i++)
            {
                if(javaDiscordAPI.getGuilds().get(i).getMemberById(entry.getKey()) != null)
                    inGuild = true;
            }

            // un-whitelist associated minecraft usernames if not in any guilds
            if(!inGuild)
            {
                for(int i = 0; i < entry.getValue().size(); i++)
                {
                    // un-whitelist
                    if(!WhitelistedPlayers.usingEasyWhitelist)
                    {
                        DiscordClient.ExecuteServerCommand("whitelist remove " + entry.getValue().get(i));
                    }
                    else
                    {
                        DiscordClient.ExecuteServerCommand("easywl remove " + entry.getValue().get(i));
                    }

                    DiscordWhitelister.getPluginLogger().info("Removed " + entry.getValue().get(i)
                            + " from the whitelist as Discord ID: " + entry.getKey() + " has left the server.");
                }

                // Clear entries in user-list
                if(userObject.get(entry.getKey()) != null)
                {
                    UserList.getUserList().set(entry.getKey(), null);

                    UserList.SaveStore();

                    DiscordWhitelister.getPlugin().getLogger().info("Discord ID: " + entry.getKey()
                            + " left. Successfully removed their whitelisted entries from the user list.");
                }
            }
        }
    }

    // Find all occurrences of the target player and remove them
    private static void ClearPlayerFromUserList(String targetName)
    {
        // Just in-case
        targetName = targetName.toLowerCase();

        // Get a list of all IDs that contain targetName - shouldn't ever really happen
        List<String> idsContainingTargetName = new LinkedList<>();

        Yaml userYaml = new Yaml();

        try
        {
            InputStream inputStream = new FileInputStream(UserList.getUserListFile());

            // Check if input stream is empty
            PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
            int b = pushbackInputStream.read();

            // Make sure the user list is not empty
            if(b != -1)
            {
                pushbackInputStream.unread(b);

                Map<String, List<String>> userListObject = userYaml.load(pushbackInputStream);

                // Search for name and Id linked to it
                for(Map.Entry<String, List<String>> entry : userListObject.entrySet())
                {
                    for(int i = 0; i < entry.getValue().size(); i++)
                    {
                        if(entry.getValue().get(i).equals(targetName))
                        {
                            // Found the target name, add ID to list
                            idsContainingTargetName.add(entry.getKey());
                        }
                    }
                }

                // Check if we found any IDs
                if(idsContainingTargetName.size() > 0)
                {
                    DiscordWhitelister.getPluginLogger().info("Found " + idsContainingTargetName.size() + " occurrence(s) of " + targetName + " in the user list, removing...");

                    for(int i = 0; i < idsContainingTargetName.size(); i++)
                    {
                        // Get the IDs whitelisted users
                        List<String> newWhitelistedUsers = UserList.getUserList().getStringList(idsContainingTargetName.get(i));

                        if(newWhitelistedUsers.size() > 1)
                        {
                            newWhitelistedUsers.remove(targetName);
                            UserList.getUserList().set(idsContainingTargetName.get(i), newWhitelistedUsers);
                        }
                        else
                        {
                            // Double check the 1 whitelisted user == targetName
                            if(newWhitelistedUsers.get(0).equals(targetName))
                                UserList.getUserList().set(idsContainingTargetName.get(i), null);
                        }

                        UserList.SaveStore();
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
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
    // For ultra perms
    public static void AssignPermsToUser(String targetPlayerName, List<String> permsToAssign)
    {
        for(int i = 0; i < permsToAssign.size(); i++)
        {
            DiscordClient.ExecuteServerCommand("upc addPlayerPermission " + targetPlayerName + " " + permsToAssign.get(i));
        }
    }

    public static void RemovePermsFromUser(String targetPlayerName, List<String> permsToRemove)
    {
        for(int i = 0; i < permsToRemove.size(); i++)
        {
            DiscordClient.ExecuteServerCommand("upc removePlayerPermission " + targetPlayerName + " " + permsToRemove.get(i));
        }
    }
}
