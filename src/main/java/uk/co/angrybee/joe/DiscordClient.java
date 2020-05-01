package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// handles Discord interaction
public class DiscordClient extends ListenerAdapter {
    static String[] allowedToAddRemoveRoles;
    static String[] allowedToAddRoles;
    static String[] allowedToAddLimitedRoles;

    private static String[] targetTextChannels;

    private static MessageEmbed botInfo;
    private static MessageEmbed addCommandInfo;
    private static MessageEmbed removeCommandInfo;

    private static int maxWhitelistAmount;

    private static boolean limitedWhitelistEnabled;
    private static boolean usernameValidation;

    private static boolean whitelistedRoleAutoAdd;
    private static boolean whitelistedRoleAutoRemove;
    private static String whitelistedRoleName;

    private final char[] validCharacters = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h',
            'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_'};

    private static JDA javaDiscordAPI;

    public static int InitializeClient(String clientToken) {
        AssignVars();
        BuildStrings();

        try {
            javaDiscordAPI = new JDABuilder(AccountType.BOT)
                    .setToken(clientToken)
                    .addEventListeners(new DiscordClient())
                    .build();
            javaDiscordAPI.awaitReady();
            return 0;
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private static void AssignVars() {
        // assign vars here instead of every time a message is received, as they do not change
        targetTextChannels = new String[DiscordWhitelister.getWhitelisterBotConfig().getList("target-text-channels").size()];
        for (int i = 0; i < targetTextChannels.length; ++i) {
            targetTextChannels[i] = DiscordWhitelister.getWhitelisterBotConfig().getList("target-text-channels").get(i).toString();
        }

        maxWhitelistAmount = DiscordWhitelister.getWhitelisterBotConfig().getInt("max-whitelist-amount");
        limitedWhitelistEnabled = DiscordWhitelister.getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled");
        usernameValidation = DiscordWhitelister.getWhitelisterBotConfig().getBoolean("username-validation");

        // Set the name of the role to add/remove to/from the user after they have been added/removed to/from the whitelist and if this feature is enabled
        whitelistedRoleAutoAdd = DiscordWhitelister.getWhitelisterBotConfig().getBoolean("whitelisted-role-auto-add");
        whitelistedRoleAutoRemove = DiscordWhitelister.getWhitelisterBotConfig().getBoolean("whitelisted-role-auto-remove");
        whitelistedRoleName = DiscordWhitelister.getWhitelisterBotConfig().getString("whitelisted-role");
    }

    private static void BuildStrings() {
        // build here instead of every time a message is received, as they do not change
        EmbedBuilder embedBuilderBotInfo = new EmbedBuilder();
        embedBuilderBotInfo.setTitle("Discord Whitelister Bot for Spigot");
        embedBuilderBotInfo.addField("Version", VersionInfo.getVersion(), false);
        embedBuilderBotInfo.addField("Links", ("https://www.spigotmc.org/resources/discord-whitelister.69929/" + System.lineSeparator() + "https://github.com/JoeShimell/DiscordWhitelisterSpigot"), false);
        embedBuilderBotInfo.addField("Commands", ("**Add:** !whitelist add minecraftUsername" + System.lineSeparator() + "**Remove:** !whitelist remove minecraftUsername"), false);
        embedBuilderBotInfo.addField("Experiencing issues?", "If you encounter an issue, please report it here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues", false);
        embedBuilderBotInfo.setColor(new Color(104, 109, 224));
        botInfo = embedBuilderBotInfo.build();

        EmbedBuilder embedBuilderAddCommandInfo = new EmbedBuilder();
        embedBuilderAddCommandInfo.addField("Whitelist Add Command", ("!whitelist add minecraftUsername" + System.lineSeparator() + System.lineSeparator() +
                "If you encounter any issues, please report them here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues"), false);
        embedBuilderAddCommandInfo.setColor(new Color(104, 109, 224));
        addCommandInfo = embedBuilderAddCommandInfo.build();

        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
        embedBuilderInfo.addField("Whitelist Remove Command", ("!whitelist remove minecraftUsername" + System.lineSeparator() + System.lineSeparator() +
                "If you encounter any issues, please report them here: https://github.com/JoeShimell/DiscordWhitelisterSpigot/issues"), false);
        embedBuilderInfo.setColor(new Color(104, 109, 224));
        removeCommandInfo = embedBuilderInfo.build();
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

    @Override
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent) {
        if (messageReceivedEvent.isFromType(ChannelType.TEXT)) {
            // Check if message should be handled
            if (!Arrays.asList(targetTextChannels).contains(messageReceivedEvent.getTextChannel().getId())) {
                return;
            }

            if (messageReceivedEvent.getAuthor().isBot()) {
                return;
            }

            AuthorPermissions authorPermissions = new AuthorPermissions(messageReceivedEvent);
            User author = messageReceivedEvent.getAuthor();
            String messageContents = messageReceivedEvent.getMessage().getContentDisplay();
            TextChannel channel = messageReceivedEvent.getTextChannel();

            // select different whitelist commands
            if (messageContents.toLowerCase().equals("!whitelist"))
            {
                // info command
                if (authorPermissions.isUserCanUseCommand())
                {
                    channel.sendMessage(botInfo).queue();
                }
                else
                {
                    EmbedBuilder insufficientPermission = new EmbedBuilder();
                    insufficientPermission.setColor(new Color(231, 76, 60));

                    if(!DiscordWhitelister.useCustomMessages)
                    {
                        insufficientPermission.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                    }
                    else
                    {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-title");
                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention()); // Only checking for {Sender}

                        insufficientPermission.addField(customTitle, customMessage, false);
                    }

                    channel.sendMessage(insufficientPermission.build()).queue();
                }
            }

            // Add Command
            if (messageContents.toLowerCase().startsWith("!whitelist add"))
            {
                // Permission check
                if (!(authorPermissions.isUserCanAddRemove() || authorPermissions.isUserCanAdd() || limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd()))
                {
                    EmbedBuilder insufficientPermission = new EmbedBuilder();
                    insufficientPermission.setColor(new Color(231, 76, 60));

                    if(!DiscordWhitelister.useCustomMessages)
                    {
                        insufficientPermission.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                    }
                    else
                    {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-title");
                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention()); // Only checking for {Sender}

                        insufficientPermission.addField(customTitle, customMessage, false);
                    }

                    channel.sendMessage(insufficientPermission.build()).queue();
                    return;
                }

                /* if limited whitelist is enabled, check if the user is in the limited whitelister group and add the user to the list
                which records how many times the user has successfully used the whitelist command */
                if (limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd())
                {
                    if (DiscordWhitelister.getUserList().getString(author.getId()) == null)
                    {
                        DiscordWhitelister.getUserList().set(author.getId(), new ArrayList<String>());
                        try
                        {
                            DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                        }
                        catch (IOException e)
                        {
                            EmbedBuilder failure = new EmbedBuilder();
                            failure.addField("Internal Error", (author.getAsMention() + ", something went wrong while accessing config files. Please contact a staff member."), false);
                            failure.setColor(new Color(231, 76, 60));
                            channel.sendMessage(failure.build()).queue();
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                boolean usedAllWhitelists = false;
                try
                {
                    usedAllWhitelists = DiscordWhitelister.getRegisteredUsersCount(author.getId()) >= maxWhitelistAmount &&
                                    !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();
                }
                catch (NullPointerException exception)
                {
                    exception.printStackTrace();
                }

                if (authorPermissions.isUserCanAddRemove() || authorPermissions.isUserCanAdd() || limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd())
                {
                    messageContents = messageContents.toLowerCase();
                    String messageContentsAfterCommand = messageContents.substring("!whitelist add".length() + 1); // get everything after !whitelist add[space]
                    final String finalNameToAdd = messageContentsAfterCommand.replaceAll(" .*", ""); // The name is everything up to the first space

                    final char[] finalNameToWhitelistChar = finalNameToAdd.toCharArray();

                    int timesWhitelisted = 0;

                    boolean onlyHasLimitedAdd = limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd() &&
                            !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();

                    if (onlyHasLimitedAdd)
                    {
                        timesWhitelisted = DiscordWhitelister.getRegisteredUsersCount(author.getId());

                        // set to current max in case the max whitelist amount was changed
                        if (timesWhitelisted > maxWhitelistAmount)
                        {
                            timesWhitelisted = maxWhitelistAmount;
                        }
                    }

                    if (onlyHasLimitedAdd && usedAllWhitelists)
                    {
                        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                        embedBuilderInfo.setColor(new Color(104, 109, 224));

                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            embedBuilderInfo.addField("No Whitelists Remaining", (author.getAsMention() + ", unable to whitelist. You have **" + (maxWhitelistAmount - timesWhitelisted)
                                    + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining-title");
                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("no-whitelists-remaining");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - timesWhitelisted)));
                            customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                            embedBuilderInfo.addField(customTitle, customMessage, false);
                        }

                        channel.sendMessage(embedBuilderInfo.build()).queue();
                        return;
                    }

                    if (finalNameToAdd.isEmpty())
                    {
                        channel.sendMessage(addCommandInfo).queue();
                    }
                    else
                    {
                        if (usernameValidation)
                        {
                            // Invalid char check
                            for (int a = 0; a < finalNameToWhitelistChar.length; ++a)
                            {
                                if (new String(validCharacters).indexOf(finalNameToWhitelistChar[a]) == -1)
                                {
                                    EmbedBuilder embedBuilderInvalidChar = new EmbedBuilder();
                                    embedBuilderInvalidChar.setColor(new Color(231, 76, 60));

                                    if(!DiscordWhitelister.useCustomMessages)
                                    {
                                        embedBuilderInvalidChar.addField("Invalid Username", (author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**."), false);
                                    }
                                    else
                                    {
                                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning-title");
                                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-characters-warning");
                                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                                        embedBuilderInvalidChar.addField(customTitle, customMessage,false);
                                    }

                                    if (onlyHasLimitedAdd)
                                    {
                                        if(!DiscordWhitelister.useCustomMessages)
                                        {
                                            embedBuilderInvalidChar.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                        }
                                        else
                                        {
                                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining-title");
                                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining");
                                            customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - timesWhitelisted)));
                                            customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                                            embedBuilderInvalidChar.addField(customTitle, customMessage, false);
                                        }
                                    }

                                    channel.sendMessage(embedBuilderInvalidChar.build()).queue();
                                    return;
                                }
                            }

                            // Length check
                            if (finalNameToAdd.length() < 3 || finalNameToAdd.length() > 16)
                            {
                                EmbedBuilder embedBuilderLengthInvalid = new EmbedBuilder();
                                embedBuilderLengthInvalid.setColor(new Color(231, 76, 60));

                                if(!DiscordWhitelister.useCustomMessages)
                                {
                                    embedBuilderLengthInvalid.addField("Invalid Username", (author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**."), false);
                                }
                                else
                                {
                                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning-title");
                                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("invalid-length-warning");
                                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());

                                    embedBuilderLengthInvalid.addField(customTitle, customMessage,false);
                                }

                                if (onlyHasLimitedAdd)
                                {
                                    if(!DiscordWhitelister.useCustomMessages)
                                    {
                                        embedBuilderLengthInvalid.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                    }
                                    else
                                    {
                                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining-title");
                                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining");
                                        customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - timesWhitelisted)));
                                        customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                                        embedBuilderLengthInvalid.addField(customTitle, customMessage, false);
                                    }
                                }

                                channel.sendMessage(embedBuilderLengthInvalid.build()).queue();
                                return;
                            }
                        }

                        // EasyWhitelist username store
                        FileConfiguration tempFileConfiguration = new YamlConfiguration();
                        // Default Minecraft username store
                        File whitelistJSON = (new File(".", "whitelist.json"));

                        if (onlyHasLimitedAdd) {
                            DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to whitelist: " + finalNameToAdd + ", " + (maxWhitelistAmount - timesWhitelisted) + " whitelists remaining");
                        } else {
                            DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to whitelist: " + finalNameToAdd);
                        }

                        if (DiscordWhitelister.useEasyWhitelist)
                        {
                            try
                            {
                                tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                            }
                            catch (IOException | InvalidConfigurationException e)
                            {
                                EmbedBuilder failure = new EmbedBuilder();
                                failure.setColor(new Color(231, 76, 60));
                                failure.addField("Internal Error", (author.getAsMention() + ", something went wrong while accessing EasyWhitelist file. Please contact a staff member."), false);
                                channel.sendMessage(failure.build()).queue();
                                e.printStackTrace();
                                return;
                            }
                        }

                        boolean alreadyOnWhitelist = false;

                        if(DiscordWhitelister.useEasyWhitelist)
                        {
                            if (tempFileConfiguration.getStringList("whitelisted").contains(finalNameToAdd))
                            {
                                alreadyOnWhitelist = true;
                            }
                        }
                        else if (checkWhitelistJSON(whitelistJSON, finalNameToAdd))
                        {
                            alreadyOnWhitelist = true;
                        }

                        if(alreadyOnWhitelist)
                        {
                            EmbedBuilder embedBuilderAlreadyWhitelisted = new EmbedBuilder();
                            embedBuilderAlreadyWhitelisted.setColor(new Color(104, 109, 224));

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderAlreadyWhitelisted.addField("User already on the whitelist", (author.getAsMention() + ", cannot add user as `" + finalNameToAdd + "` is already on the whitelist!"), false);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("already-on-whitelist");
                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                                embedBuilderAlreadyWhitelisted.addField(customTitle, customMessage, false);
                            }

                            channel.sendMessage(embedBuilderAlreadyWhitelisted.build()).queue();
                            return;
                        }

                        if (DiscordWhitelister.getRemovedList().get(finalNameToAdd) != null) // If the user has been removed before
                        {
                            if (onlyHasLimitedAdd)
                            {
                                EmbedBuilder embedBuilderRemovedByStaff = new EmbedBuilder();
                                embedBuilderRemovedByStaff.setColor(new Color(231, 76, 60));

                                if(!DiscordWhitelister.useCustomMessages)
                                {
                                    embedBuilderRemovedByStaff.addField("This user was previously removed by a staff member", (author.getAsMention() + ", this user was previously removed by a staff member (<@" + DiscordWhitelister.getRemovedList().get(finalNameToAdd) + ">)."
                                            + System.lineSeparator() + "Please ask a user with higher permissions to add this user." + System.lineSeparator()), false);
                                    embedBuilderRemovedByStaff.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted)
                                            + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                }
                                else
                                {
                                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed-title");
                                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-was-removed");
                                    String customWhitelistsRemaining = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining");
                                    String staffMemberMention = "<@" + DiscordWhitelister.getRemovedList().get(finalNameToAdd) + ">";

                                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                    customMessage = customMessage.replaceAll("\\{StaffMember}", staffMemberMention);

                                    customWhitelistsRemaining = customWhitelistsRemaining.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - timesWhitelisted)));
                                    customWhitelistsRemaining = customWhitelistsRemaining.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                                    embedBuilderRemovedByStaff.addField(customTitle, customMessage + " " + customWhitelistsRemaining, false);
                                }

                                channel.sendMessage(embedBuilderRemovedByStaff.build()).queue();
                                return;
                            }
                            else // Remove from removed list
                            {
                                DiscordWhitelister.getRemovedList().set(finalNameToAdd, null);

                                try
                                {
                                    DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                }

                                DiscordWhitelister.getPlugin().getLogger().info(finalNameToAdd + " has been removed from the removed list by " + author.getName()
                                        + "(" + author.getId() + ")");
                            }
                        }

                        /* Do as much as possible off the main server thread.
                        convert username into UUID to avoid depreciation and rate limits (according to https://minotar.net/) */
                        String playerUUID = minecraftUsernameToUUID(finalNameToAdd);
                        final boolean invalidMinecraftName = playerUUID == null;

                        /* Configure success & failure messages here instead of on the main server thread -
                        this will run even if the message is never sent, but is a good trade off */
                        EmbedBuilder embedBuilderWhitelistSuccess = new EmbedBuilder();
                        embedBuilderWhitelistSuccess.setColor(new Color(46, 204, 113));
                        embedBuilderWhitelistSuccess.setThumbnail("https://minotar.net/bust/" + playerUUID + "/100.png");

                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            embedBuilderWhitelistSuccess.addField((finalNameToAdd + " is now whitelisted!"), (author.getAsMention() + " has added `" + finalNameToAdd + "` to the whitelist."), false);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success-title");
                            customTitle = customTitle.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-success");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                            embedBuilderWhitelistSuccess.addField(customTitle, customMessage, false);
                        }

                        if (onlyHasLimitedAdd)
                        {
                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderWhitelistSuccess.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - (timesWhitelisted + 1)) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining");
                                customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - (timesWhitelisted + 1))));
                                customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                                embedBuilderWhitelistSuccess.addField(customTitle, customMessage, false);
                            }
                        }

                        EmbedBuilder embedBuilderWhitelistFailure = new EmbedBuilder();
                        embedBuilderWhitelistFailure.setColor(new Color(231, 76, 60));

                        if(!DiscordWhitelister.useCustomMessages)
                        {
                            embedBuilderWhitelistFailure.addField("Failed to whitelist", (author.getAsMention() + ", failed to add `" + finalNameToAdd + "` to the whitelist. This is most likely due to an invalid Minecraft username."), false);
                        }
                        else
                        {
                            String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure-title");

                            String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelist-failure");
                            customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                            customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToAdd);

                            embedBuilderWhitelistFailure.addField(customTitle, customMessage, false);
                        }

                        if (onlyHasLimitedAdd)
                        {
                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderWhitelistFailure.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("whitelists-remaining");
                                customMessage = customMessage.replaceAll("\\{RemainingWhitelists}", String.valueOf((maxWhitelistAmount - (timesWhitelisted + 1))));
                                customMessage = customMessage.replaceAll("\\{MaxWhitelistAmount}", String.valueOf(maxWhitelistAmount));

                                embedBuilderWhitelistFailure.addField(customTitle, customMessage, false);
                            }
                        }

                        int tempTimesWhitelisted = timesWhitelisted;

                        if (onlyHasLimitedAdd) {
                            if (tempTimesWhitelisted < maxWhitelistAmount) {
                                tempTimesWhitelisted = timesWhitelisted + 1;
                            }
                        }

                        final int finalTimesWhitelisted = tempTimesWhitelisted;
                        final int successfulTimesWhitelisted = maxWhitelistAmount - finalTimesWhitelisted;
                        final int failedTimesWhitelisted = maxWhitelistAmount - timesWhitelisted;

                        if (!DiscordWhitelister.useEasyWhitelist) {
                            if (authorPermissions.isUserCanUseCommand()) {
                                executeServerCommand("whitelist add " + finalNameToAdd);
                            }
                        }

                        if (DiscordWhitelister.useEasyWhitelist) {
                            if (!invalidMinecraftName) // have to do this else the easy whitelist plugin will add the name regardless of whether it is valid on not
                            {
                                if (authorPermissions.isUserCanUseCommand()) {
                                    executeServerCommand("easywl add " + finalNameToAdd);
                                }
                            }

                            // run through the server so that the check doesn't execute before the server has had a chance to run the whitelist command -- unsure if this is the best way of doing this, but it works
                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                            {
                                try {
                                    tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                } catch (IOException | InvalidConfigurationException e) {
                                    e.printStackTrace();
                                }

                                if (!invalidMinecraftName && tempFileConfiguration.getStringList("whitelisted").contains(finalNameToAdd)) {
                                    channel.sendMessage(embedBuilderWhitelistSuccess.build()).queue();

                                    // Add role to user when they have been added to the whitelist if need be
                                    if(whitelistedRoleAutoAdd) {
                                        Role whitelistRole = null;
                                        try {
                                            whitelistRole = javaDiscordAPI.getRolesByName(whitelistedRoleName, false).get(0);
                                            Member member = messageReceivedEvent.getMember();
                                            messageReceivedEvent.getGuild().addRoleToMember(member, whitelistRole).queue();
                                        } catch (Exception e) {
                                            DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name " + whitelistedRoleName + " to " + author.getName() + ", check that it has the correct name in the config and the bot has the Manage Roles permission");
                                        }
                                    }

                                    if (onlyHasLimitedAdd) {

                                        DiscordWhitelister.addRegisteredUser(author.getId(), finalNameToAdd);
                                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") successfully added " + finalNameToAdd
                                                + " to the whitelist, " + successfulTimesWhitelisted + " whitelists remaining.");
                                    }
                                } else {
                                    channel.sendMessage(embedBuilderWhitelistFailure.build()).queue();
                                }
                                return null;
                            });
                        } else {
                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                            {
                                if (checkWhitelistJSON(whitelistJSON, finalNameToAdd)) {
                                    channel.sendMessage(embedBuilderWhitelistSuccess.build()).queue();

                                    // Add role to user when they have been added to the whitelist if need be
                                    if(whitelistedRoleAutoAdd) {
                                        Role whitelistRole = null;
                                        try {
                                            whitelistRole = javaDiscordAPI.getRolesByName(whitelistedRoleName, false).get(0);
                                            Member member = messageReceivedEvent.getMember();
                                            messageReceivedEvent.getGuild().addRoleToMember(member, whitelistRole).queue();
                                        } catch (Exception e) {
                                            DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name " + whitelistedRoleName + " to " + author.getName() + ", check that it has the correct name in the config and the bot has the Manage Roles permission");
                                        }
                                    }

                                    if (onlyHasLimitedAdd) {

                                        DiscordWhitelister.addRegisteredUser(author.getId(), finalNameToAdd);

                                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") successfully added " + finalNameToAdd
                                                + " to the whitelist, " + failedTimesWhitelisted + " whitelists remaining.");
                                    }
                                } else {
                                    channel.sendMessage(embedBuilderWhitelistFailure.build()).queue();
                                }
                                return null;
                            });
                        }

                    }
                }
            }

            if (messageContents.toLowerCase().startsWith("!whitelist remove")) {
                if (authorPermissions.isUserCanAddRemove()) {
                    messageContents = messageContents.toLowerCase();
                    String messageContentsAfterCommand = messageContents.substring("!whitelist remove".length() + 1); // get everything after !whitelist add[space]
                    final String finalNameToRemove = messageContentsAfterCommand.replaceAll(" .*", ""); // The name is everything up to the first space

                    if (finalNameToRemove.isEmpty()) {
                        channel.sendMessage(removeCommandInfo).queue();
                        return;
                    } else {
                        // easy whitelist
                        FileConfiguration tempFileConfiguration = new YamlConfiguration();
                        // default whitelist
                        File whitelistJSON = (new File(".", "whitelist.json"));

                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "(" + author.getId() + ") attempted to remove " + finalNameToRemove + " from the whitelist");

                        if (DiscordWhitelister.useEasyWhitelist) {
                            try {
                                tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                            } catch (IOException | InvalidConfigurationException e) {
                                e.printStackTrace();
                            }
                        }

                        boolean notOnWhitelist = false;

                        if (DiscordWhitelister.useEasyWhitelist)
                        {
                            if (!tempFileConfiguration.getStringList("whitelisted").contains(finalNameToRemove))
                            {
                                notOnWhitelist = true;

                                EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                embedBuilderInfo.setColor(new Color(104, 109, 224));

                                if(!DiscordWhitelister.useCustomMessages)
                                {
                                    embedBuilderInfo.addField("This user is not on the whitelist", (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), false);
                                }
                                else
                                {
                                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist-title");
                                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist");
                                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                    customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                    embedBuilderInfo.addField(customTitle, customMessage, false);
                                }

                                channel.sendMessage(embedBuilderInfo.build()).queue();
                            }
                        }

                        if (!DiscordWhitelister.useEasyWhitelist && !checkWhitelistJSON(whitelistJSON, finalNameToRemove))
                        {
                            notOnWhitelist = true;

                            EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                            embedBuilderInfo.setColor(new Color(104, 109, 224));

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderInfo.addField("This user is not on the whitelist", (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), false);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist-title");
                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("user-not-on-whitelist");
                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                embedBuilderInfo.addField(customTitle, customMessage, false);
                            }

                            channel.sendMessage(embedBuilderInfo.build()).queue();
                        }

                        if (!notOnWhitelist)
                        {
                            if (DiscordWhitelister.useEasyWhitelist)
                            {
                                try
                                {
                                    tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                }
                                catch (IOException | InvalidConfigurationException e)
                                {
                                    e.printStackTrace();
                                }

                                executeServerCommand("easywl remove " + finalNameToRemove);
                            }
                            else
                            {
                                executeServerCommand("whitelist remove " + finalNameToRemove);
                            }

                            // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                            EmbedBuilder embedBuilderSuccess = new EmbedBuilder();
                            embedBuilderSuccess.setColor(new Color(46, 204, 113));

                            if(!DiscordWhitelister.useCustomMessages)
                            {
                                embedBuilderSuccess.addField((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), false);
                            }
                            else
                            {
                                String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("remove-success-title");
                                customTitle = customTitle.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("remove-success");
                                customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                                customMessage = customMessage.replaceAll("\\{MinecraftUsername}", finalNameToRemove);

                                embedBuilderSuccess.addField(customTitle, customMessage, false);
                            }

                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                            embedBuilderFailure.setColor(new Color(231, 76, 60));

                            // No custom message needed
                            embedBuilderFailure.addField(("Failed to remove " + finalNameToRemove + " from the whitelist"), (author.getAsMention() + ", failed to remove `" + finalNameToRemove + "` from the whitelist. " +
                                    "This should never happen, you may have to remove the player manually and report the issue."), false);


                            if (DiscordWhitelister.useEasyWhitelist) {
                                DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                                {
                                    try {
                                        tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                    } catch (IOException | InvalidConfigurationException e) {
                                        e.printStackTrace();
                                    }

                                    if (!tempFileConfiguration.getStringList("whitelisted").contains(finalNameToRemove)) {
                                        channel.sendMessage(embedBuilderSuccess.build()).queue();

                                        // Remove role from user when they have been removed from the whitelist if need be
                                        if(whitelistedRoleAutoRemove) {
                                            Role whitelistRole = null;
                                            try {
                                                whitelistRole = javaDiscordAPI.getRolesByName(whitelistedRoleName, false).get(0);
                                                Member member = messageReceivedEvent.getMember();
                                                messageReceivedEvent.getGuild().removeRoleFromMember(member, whitelistRole).queue();
                                            } catch (Exception e) {
                                                DiscordWhitelister.getPlugin().getLogger().severe("Could not remove role with name " + whitelistedRoleName + " from " + author.getName() + ", check that it has the correct name in the config and the bot has the Manage Roles permission");
                                            }
                                        }

                                        // if the name is not on the list
                                        if (DiscordWhitelister.getRemovedList().get(finalNameToRemove) == null) {
                                            DiscordWhitelister.getRemovedList().set(finalNameToRemove, author.getId());
                                            DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                        }
                                    } else {
                                        channel.sendMessage(embedBuilderFailure.build()).queue();
                                    }
                                    return null;
                                });
                            } else {
                                DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                                {
                                    if (!checkWhitelistJSON(whitelistJSON, finalNameToRemove)) {
                                        channel.sendMessage(embedBuilderSuccess.build()).queue();

                                        // Remove role from user when they have been removed from the whitelist if need be
                                        if(whitelistedRoleAutoRemove) {
                                            Role whitelistRole = null;
                                            try {
                                                whitelistRole = javaDiscordAPI.getRolesByName(whitelistedRoleName, false).get(0);
                                                Member member = messageReceivedEvent.getMember();
                                                messageReceivedEvent.getGuild().removeRoleFromMember(member, whitelistRole).queue();
                                            } catch (Exception e) {
                                                DiscordWhitelister.getPlugin().getLogger().severe("Could not remove role with name " + whitelistedRoleName + " from " + author.getName() + ", check that it has the correct name in the config and the bot has the Manage Roles permission");
                                            }
                                        }

                                        // if the name is not on the list
                                        if (DiscordWhitelister.getRemovedList().get(finalNameToRemove) == null) {
                                            DiscordWhitelister.getRemovedList().set(finalNameToRemove, author.getId());
                                            DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                        }
                                    } else {
                                        channel.sendMessage(embedBuilderFailure.build()).queue();
                                    }
                                    return null;
                                });
                            }
                            return;
                        }
                        return;
                    }
                }

                if (authorPermissions.isUserCanAdd() && !authorPermissions.isUserCanAddRemove())
                {
                    String higherPermRoles = DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles").toString();
                    higherPermRoles = higherPermRoles.replaceAll("\\[", "");
                    higherPermRoles = higherPermRoles.replaceAll("]", "");

                    EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                    embedBuilderInfo.setColor(new Color(104, 109, 224));

                    if(!DiscordWhitelister.useCustomMessages)
                    {
                        embedBuilderInfo.addField("Insufficient Permissions", (author.getAsMention() + ", you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: "
                                + higherPermRoles + "; or get the owner to move your role to 'add-remove-roles' in the config."), false);
                    }
                    else
                    {
                        String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-remove-title");
                        String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-remove");
                        customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention());
                        customMessage = customMessage.replaceAll("\\{AddRemoveRoles}", higherPermRoles);

                        embedBuilderInfo.addField(customTitle, customMessage, false);
                    }

                    channel.sendMessage(embedBuilderInfo.build()).queue();
                    return;
                }

                // if the user doesn't have any allowed roles
                EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                embedBuilderFailure.setColor(new Color(231, 76, 60));

                if(!DiscordWhitelister.useCustomMessages)
                {
                    embedBuilderFailure.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                }
                else
                {
                    String customTitle = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions-title");
                    String customMessage = DiscordWhitelister.getCustomMessagesConfig().getString("insufficient-permissions");
                    customMessage = customMessage.replaceAll("\\{Sender}", author.getAsMention()); // Only checking for {Sender}

                    embedBuilderFailure.addField(customTitle, customMessage, false);
                }

                channel.sendMessage(embedBuilderFailure.build()).queue();
            }
        }
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        String discordUserToRemove = event.getMember().getId();
        DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Removing their whitelisted entries...");
        List<?> ls =  DiscordWhitelister.getRegisteredUsers(discordUserToRemove);

        if(ls != null) {

            for (Object minecraftNameToRemove : ls) {
                DiscordWhitelister.getPlugin().getLogger().info(minecraftNameToRemove.toString() + " left. Removing their whitelisted entries.");
                if (DiscordWhitelister.useEasyWhitelist) {
                    executeServerCommand("easywl remove " + minecraftNameToRemove.toString());
                } else {
                    executeServerCommand("whitelist remove " + minecraftNameToRemove.toString());
                }
            }
            try {
                DiscordWhitelister.resetRegisteredUsers(discordUserToRemove);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Successfully removed their whitelisted entries.");

        }
        else {
            DiscordWhitelister.getPlugin().getLogger().warning(discordUserToRemove + " left. Could not removed their whitelisted entries as they did not whitelist through this plugin.");
        }
    }

    private boolean checkWhitelistJSON(File whitelistFile, String minecraftUsername) {
        boolean correctUsername = false;

        try {
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonArray = (JSONArray) jsonParser.parse(new FileReader(whitelistFile));

            for (Object object : jsonArray) {
                JSONObject player = (JSONObject) object;

                String userName = (String) player.get("name");
                userName = userName.toLowerCase();

                if (userName.equals(minecraftUsername)) {
                    correctUsername = true;
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return correctUsername;
    }

    private String minecraftUsernameToUUID(String minecraftUsername) {
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

    private void executeServerCommand(String command) {
        DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), ()
                -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(
                DiscordWhitelister.getPlugin().getServer().getConsoleSender(), command));
    }
}
