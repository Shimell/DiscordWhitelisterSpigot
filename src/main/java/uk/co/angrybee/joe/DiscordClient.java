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
            if (messageContents.toLowerCase().equals("!whitelist")) {
                // info command
                if (authorPermissions.isUserCanUseCommand()) {
                    channel.sendMessage(botInfo).queue();
                } else {
                    EmbedBuilder insufficientPermission = new EmbedBuilder();
                    insufficientPermission.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                    insufficientPermission.setColor(new Color(231, 76, 60));
                    channel.sendMessage(insufficientPermission.build()).queue();
                }
            }

            if (messageContents.toLowerCase().contains("!whitelist add")) {
                // add command

                // permissions
                if (!(authorPermissions.isUserCanAddRemove() || authorPermissions.isUserCanAdd() || limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd())) {
                    EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                    embedBuilderFailure.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                    embedBuilderFailure.setColor(new Color(231, 76, 60));
                    channel.sendMessage(embedBuilderFailure.build()).queue();
                    return;
                }

                /* if limited whitelist is enabled, check if the user is in the limited whitelister group and add the user to the list
                which records how many times the user has successfully used the whitelist command */
                if (limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd()) {
                    if (DiscordWhitelister.getUserList().getString(author.getId()) == null) {
                        DiscordWhitelister.getUserList().set(author.getId(), new ArrayList<String>());
                        try {
                            DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                        } catch (IOException e) {
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
                try {
                    usedAllWhitelists =
                            DiscordWhitelister.getRegisteredUsersCount(author.getId()) >= maxWhitelistAmount &&
                                    !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();
                } catch (NullPointerException exception) {
                    exception.printStackTrace();
                }

                if (authorPermissions.isUserCanAddRemove() || authorPermissions.isUserCanAdd() || limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd()) {
                    String nameToWhitelist = messageContents.toLowerCase();
                    nameToWhitelist = nameToWhitelist.substring(nameToWhitelist.indexOf("!whitelist add") + 14); // get everything after !whitelist add
                    nameToWhitelist = nameToWhitelist.replaceAll(" ", "");

                    final String finalNameToAdd = nameToWhitelist;
                    final char[] finalNameToWhitelistChar = finalNameToAdd.toCharArray();

                    int timesWhitelisted = 0;

                    boolean onlyHasLimitedAdd = limitedWhitelistEnabled && authorPermissions.isUserHasLimitedAdd() &&
                            !authorPermissions.isUserCanAddRemove() && !authorPermissions.isUserCanAdd();

                    if (onlyHasLimitedAdd) {
                        timesWhitelisted = DiscordWhitelister.getRegisteredUsersCount(author.getId());

                        // set to current max in case the max whitelist amount was changed
                        if (timesWhitelisted > maxWhitelistAmount) {
                            timesWhitelisted = maxWhitelistAmount;
                        }
                    }

                    if (onlyHasLimitedAdd && usedAllWhitelists) {
                        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                        embedBuilderInfo.addField("No Whitelists Remaining", (author.getAsMention() + ", unable to whitelist. You have **" + (maxWhitelistAmount - timesWhitelisted)
                                + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                        embedBuilderInfo.setColor(new Color(104, 109, 224));
                        channel.sendMessage(embedBuilderInfo.build()).queue();
                        return;
                    }

                    if (finalNameToAdd.isEmpty()) {
                        channel.sendMessage(addCommandInfo).queue();
                    } else {
                        if (usernameValidation) {
                            // Invalid char check
                            for (int a = 0; a < finalNameToWhitelistChar.length; ++a) {
                                if (new String(validCharacters).indexOf(finalNameToWhitelistChar[a]) == -1) {
                                    EmbedBuilder embedBuilderInvalidChar = new EmbedBuilder();
                                    embedBuilderInvalidChar.setColor(new Color(231, 76, 60));
                                    embedBuilderInvalidChar.addField("Invalid username", (author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**."), false);
                                    if (onlyHasLimitedAdd) {
                                        embedBuilderInvalidChar.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                    }
                                    channel.sendMessage(embedBuilderInvalidChar.build()).queue();
                                    return;
                                }
                            }

                            // Length check
                            if (finalNameToAdd.length() < 3 || finalNameToAdd.length() > 16) {
                                EmbedBuilder embedBuilderLengthInvalid = new EmbedBuilder();
                                embedBuilderLengthInvalid.setColor(new Color(231, 76, 60));
                                embedBuilderLengthInvalid.addField("Invalid username", (author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**."), false);
                                if (onlyHasLimitedAdd) {
                                    embedBuilderLengthInvalid.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
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

                        if (DiscordWhitelister.useEasyWhitelist) {
                            try {
                                tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                            } catch (IOException | InvalidConfigurationException e) {
                                EmbedBuilder failure = new EmbedBuilder();
                                failure.setColor(new Color(231, 76, 60));
                                failure.addField("Internal Error", (author.getAsMention() + ", something went wrong while accessing EasyWhitelist file. Please contact a staff member."), false);
                                channel.sendMessage(failure.build()).queue();
                                e.printStackTrace();
                                return;
                            }
                        }

                        if (DiscordWhitelister.useEasyWhitelist || checkWhitelistJSON(whitelistJSON, finalNameToAdd)) {
                            if (tempFileConfiguration.getStringList("whitelisted").contains(finalNameToAdd)) {

                                EmbedBuilder embedBuilderAlreadyWhitelisted = new EmbedBuilder();
                                embedBuilderAlreadyWhitelisted.addField("This user is already on the whitelist", (author.getAsMention() + ", cannot add user as `" + finalNameToAdd + "` is already on the whitelist!"), false);
                                embedBuilderAlreadyWhitelisted.setColor(new Color(104, 109, 224));
                                channel.sendMessage(embedBuilderAlreadyWhitelisted.build()).queue();
                                return;
                            }
                        }

                        if (DiscordWhitelister.getRemovedList().get(finalNameToAdd) != null) // If the user has been removed before
                        {
                            if (onlyHasLimitedAdd) {
                                EmbedBuilder embedBuilderRemovedByStaff = new EmbedBuilder();
                                embedBuilderRemovedByStaff.setColor(new Color(231, 76, 60));
                                embedBuilderRemovedByStaff.addField("This user was previously removed by a staff member", (author.getAsMention() + ", this user was previously removed by a staff member (<@" + DiscordWhitelister.getRemovedList().get(finalNameToAdd) + ">)."
                                        + System.lineSeparator() + "Please ask a user with higher permissions to add this user." + System.lineSeparator()), false);
                                embedBuilderRemovedByStaff.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted)
                                        + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                channel.sendMessage(embedBuilderRemovedByStaff.build()).queue();
                                return;
                            } else // Remove from removed list
                            {
                                DiscordWhitelister.getRemovedList().set(finalNameToAdd, null);

                                try {
                                    DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                } catch (IOException e) {
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
                        embedBuilderWhitelistSuccess.addField((finalNameToAdd + " is now whitelisted!"), (author.getAsMention() + " has added `" + finalNameToAdd + "` to the whitelist."), false);
                        if (onlyHasLimitedAdd) {
                            embedBuilderWhitelistSuccess.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - (timesWhitelisted + 1)) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                        }
                        embedBuilderWhitelistSuccess.setColor(new Color(46, 204, 113));
                        embedBuilderWhitelistSuccess.setThumbnail("https://minotar.net/bust/" + playerUUID + "/100.png");

                        EmbedBuilder embedBuilderWhitelistFailure = new EmbedBuilder();
                        embedBuilderWhitelistFailure.setColor(new Color(231, 76, 60));
                        embedBuilderWhitelistFailure.addField("Failed to Whitelist", (author.getAsMention() + ", failed to add `" + finalNameToAdd + "` to the whitelist. This is most likely due to an invalid Minecraft username."), false);
                        if (onlyHasLimitedAdd) {
                            embedBuilderWhitelistFailure.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
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
                                            DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name " + whitelistedRoleName + " to " + author.getName() + ", check that it has the correct name in the config");
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
                                            DiscordWhitelister.getPlugin().getLogger().severe("Could not add role with name " + whitelistedRoleName + " to " + author.getName() + ", check that it has the correct name in the config");
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

            if (messageContents.toLowerCase().contains("!whitelist remove")) {
                if (authorPermissions.isUserCanAddRemove()) {
                    String nameToRemove = messageContents.toLowerCase();
                    nameToRemove = nameToRemove.substring(nameToRemove.indexOf("!whitelist remove") + 17); // get everything after !whitelist remove
                    nameToRemove = nameToRemove.replaceAll(" ", "");

                    final String finalNameToRemove = nameToRemove;

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

                        if (DiscordWhitelister.useEasyWhitelist) {
                            if (!tempFileConfiguration.getStringList("whitelisted").contains(finalNameToRemove)) {
                                notOnWhitelist = true;

                                EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                embedBuilderInfo.addField("This user is not on the whitelist", (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), false);
                                embedBuilderInfo.setColor(new Color(104, 109, 224));
                                channel.sendMessage(embedBuilderInfo.build()).queue();
                            }
                        }

                        if (!DiscordWhitelister.useEasyWhitelist && !checkWhitelistJSON(whitelistJSON, finalNameToRemove)) {
                            notOnWhitelist = true;

                            EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                            embedBuilderInfo.addField("This user is not on the whitelist", (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), false);
                            embedBuilderInfo.setColor(new Color(104, 109, 224));
                            channel.sendMessage(embedBuilderInfo.build()).queue();
                        }

                        if (!notOnWhitelist) {
                            if (DiscordWhitelister.useEasyWhitelist) {
                                try {
                                    tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                } catch (IOException | InvalidConfigurationException e) {
                                    e.printStackTrace();
                                }

                                executeServerCommand("easywl remove " + finalNameToRemove);
                            } else {
                                executeServerCommand("whitelist remove " + finalNameToRemove);
                            }

                            // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                            EmbedBuilder embedBuilderSuccess = new EmbedBuilder();
                            embedBuilderSuccess.addField((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), false);
                            embedBuilderSuccess.setColor(new Color(46, 204, 113));

                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                            embedBuilderFailure.addField(("Failed to remove " + finalNameToRemove + " from the whitelist"), (author.getAsMention() + ", failed to remove `" + finalNameToRemove + "` from the whitelist. " +
                                    "This should never happen, you may have to remove the player manually and report the issue."), false);
                            embedBuilderFailure.setColor(new Color(231, 76, 60));

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
                                                DiscordWhitelister.getPlugin().getLogger().severe("Could not remove role with name " + whitelistedRoleName + " from " + author.getName() + ", check that it has the correct name in the config");
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
                                                DiscordWhitelister.getPlugin().getLogger().severe("Could not remove role with name " + whitelistedRoleName + " from " + author.getName() + ", check that it has the correct name in the config");
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

                if (authorPermissions.isUserCanAdd() && !authorPermissions.isUserCanAddRemove()) {
                    String higherPermRoles = DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles").toString();
                    higherPermRoles = higherPermRoles.replaceAll("\\[", "");
                    higherPermRoles = higherPermRoles.replaceAll("]", "");

                    EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                    embedBuilderInfo.addField("Insufficient Permissions", (author.getAsMention() + ", you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: "
                            + higherPermRoles + "; or get the owner to move your role to 'add-remove-roles' in the config."), false);
                    embedBuilderInfo.setColor(new Color(104, 109, 224));
                    channel.sendMessage(embedBuilderInfo.build()).queue();
                    return;
                }
                // if the user doesn't have any allowed roles
                EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                embedBuilderFailure.setColor(new Color(231, 76, 60));
                embedBuilderFailure.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                channel.sendMessage(embedBuilderFailure.build()).queue();
                return;
            }
        }
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        String discordUserToRemove = event.getMember().getId();
        DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Removing his whitelisted entries.");
        List<?> ls =  DiscordWhitelister.getRegisteredUsers(discordUserToRemove);
        for (Object minecraftNameToRemove: ls) {
            DiscordWhitelister.getPlugin().getLogger().info(minecraftNameToRemove.toString() + " left. Removing his whitelisted entries.");
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
        DiscordWhitelister.getPlugin().getLogger().info(discordUserToRemove + " left. Successfully removed his whitelisted entries.");
        return;
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
