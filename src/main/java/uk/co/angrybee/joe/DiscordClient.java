package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
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

import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

// handles Discord interaction
public class DiscordClient extends ListenerAdapter
{
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

    private final char[] validCharacters = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h',
    'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_'};

    public static int InitializeClient(String clientToken)
    {
        AssignVars();
        BuildStrings();

        try
        {
            JDA javaDiscordAPI = new JDABuilder(AccountType.BOT)
                    .setToken(clientToken)
                    .addEventListeners(new DiscordClient())
                    .build();
            javaDiscordAPI.awaitReady();
            return 0;
        }
        catch(LoginException | InterruptedException e)
        {
            e.printStackTrace();
            return 1;
        }
    }

    private static void AssignVars()
    {
        // assign vars here instead of every time a message is received, as they do not change
        targetTextChannels = new String[DiscordWhitelister.getWhitelisterBotConfig().getList("target-text-channels").size()];
        for(int i = 0; i < targetTextChannels.length; ++i)
        {
            targetTextChannels[i] = DiscordWhitelister.getWhitelisterBotConfig().getList("target-text-channels").get(i).toString();
        }

        maxWhitelistAmount = DiscordWhitelister.getWhitelisterBotConfig().getInt("max-whitelist-amount");
        limitedWhitelistEnabled = DiscordWhitelister.getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled");
        usernameValidation = DiscordWhitelister.getWhitelisterBotConfig().getBoolean("username-validation");
    }

    private static void BuildStrings()
    {
        // build here instead of every time a message is received, as they do not change
        EmbedBuilder embedBuilderBotInfo = new EmbedBuilder();
        embedBuilderBotInfo.setTitle("Discord Whitelister Bot for Spigot");
        embedBuilderBotInfo.addField("Version", "1.1.0", false);
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

    @Override
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent)
    {
        if(messageReceivedEvent.isFromType(ChannelType.TEXT))
        {
            boolean correctChannel = false;

            for (String targetTextChannel : targetTextChannels)
            {
                if (messageReceivedEvent.getTextChannel().getId().equals(targetTextChannel))
                {
                    correctChannel = true;
                }
            }

            if(!correctChannel)
            {
                return;
            }

            if(correctChannel && !messageReceivedEvent.getAuthor().isBot())
            {
                User author = messageReceivedEvent.getAuthor();
                String messageContents = messageReceivedEvent.getMessage().getContentDisplay();
                TextChannel channel = messageReceivedEvent.getTextChannel();

                boolean userCanAddRemove = false;
                boolean userCanAdd = false;
                boolean userHasLimitedAdd = false;

                for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                {
                    if(Arrays.stream(allowedToAddRemoveRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                    {
                        userCanAddRemove = true;
                    }
                }

                for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                {
                    if(Arrays.stream(allowedToAddRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                    {
                        userCanAdd = true;
                    }
                }

                /* if limited whitelist is enabled, check if the user is in the limited whitelister group and add the user to the list
                which records how many times the user has successfully used the whitelist command */
                if(limitedWhitelistEnabled)
                {
                    for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                    {
                        if(Arrays.stream(allowedToAddLimitedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                        {
                            userHasLimitedAdd = true;
                        }
                    }

                    if(userHasLimitedAdd)
                    {
                        if(DiscordWhitelister.getUserList().getString(author.getId()) == null)
                        {
                            DiscordWhitelister.getUserList().set(author.getId(), 0);

                            try
                            {
                                DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if(messageContents.toLowerCase().equals("!whitelist") && userCanAddRemove || messageContents.toLowerCase().equals("!whitelist") && userCanAdd)
                {
                    channel.sendMessage(botInfo).queue();
                }

                if(messageContents.toLowerCase().equals("!whitelist") && !userCanAddRemove && !userCanAdd && !author.isBot())
                {
                    EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                    embedBuilderFailure.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                    embedBuilderFailure.setColor(new Color(231, 76, 60));
                    channel.sendMessage(embedBuilderFailure.build()).queue();
                }

                if(messageContents.toLowerCase().contains("!whitelist add"))
                {
                    boolean usedAllWhitelists = false;

                    if(limitedWhitelistEnabled && userHasLimitedAdd && !userCanAddRemove && !userCanAdd)
                    {
                        if(DiscordWhitelister.getUserList().getString(author.getId()) != null
                                && Integer.parseInt(DiscordWhitelister.getUserList().getString(author.getId())) >= maxWhitelistAmount)
                        {
                            usedAllWhitelists = true;
                        }
                    }

                    if(userCanAddRemove || userCanAdd || limitedWhitelistEnabled && userHasLimitedAdd)
                    {
                        String nameToWhitelist = messageContents;
                        nameToWhitelist = nameToWhitelist.toLowerCase();
                        nameToWhitelist = nameToWhitelist.substring(nameToWhitelist.indexOf("!whitelist add")+14); // get everything after !whitelist add
                        nameToWhitelist = nameToWhitelist.replaceAll(" ", "");

                        final String finalNameToWhitelist = nameToWhitelist;
                        final char[] finalNameToWhitelistChar = finalNameToWhitelist.toCharArray();
                        
                        int timesWhitelisted = 0;

                        boolean onlyHasLimitedAdd = limitedWhitelistEnabled && userHasLimitedAdd && !userCanAddRemove && !userCanAdd;
                        boolean onWhitelist = false;

                        if(onlyHasLimitedAdd)
                        {
                            timesWhitelisted = Integer.parseInt(DiscordWhitelister.getUserList().getString(author.getId()));

                            // set to current max in case the max whitelist amount was changed
                            if(timesWhitelisted > maxWhitelistAmount)
                            {
                                timesWhitelisted = maxWhitelistAmount;
                            }
                        }

                        if(onlyHasLimitedAdd && usedAllWhitelists)
                        {
                            EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                            embedBuilderInfo.addField("No Whitelists Remaining", (author.getAsMention() + ", unable to whitelist. You have **" + (maxWhitelistAmount - timesWhitelisted)
                                    + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                            embedBuilderInfo.setColor(new Color(104, 109, 224));
                            channel.sendMessage(embedBuilderInfo.build()).queue();
                            return;
                        }

                        if(finalNameToWhitelist.isEmpty())
                        {
                            channel.sendMessage(addCommandInfo).queue();
                        }
                        else
                        {
                            if(usernameValidation)
                            {
                                // Invalid char check
                                for(int a = 0; a < finalNameToWhitelistChar.length; ++a)
                                {
                                    if(new String(validCharacters).indexOf(finalNameToWhitelistChar[a]) == -1)
                                    {
                                        EmbedBuilder embedBuilderInvalidChar = new EmbedBuilder();
                                        embedBuilderInvalidChar.addField("Invalid username", (author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**."), false);
                                        if(onlyHasLimitedAdd)
                                        {
                                            embedBuilderInvalidChar.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                        }
                                        embedBuilderInvalidChar.setColor(new Color(231, 76, 60));
                                        channel.sendMessage(embedBuilderInvalidChar.build()).queue();
                                        return;
                                    }
                                }

                                // Length check
                                if(finalNameToWhitelist.length() < 3 || finalNameToWhitelist.length() > 16)
                                {
                                    EmbedBuilder embedBuilderLengthInvalid = new EmbedBuilder();
                                    embedBuilderLengthInvalid.addField("Invalid username", (author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**."), false);
                                    if(onlyHasLimitedAdd)
                                    {
                                        embedBuilderLengthInvalid.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                    }
                                    embedBuilderLengthInvalid.setColor(new Color(231, 76, 60));
                                    channel.sendMessage(embedBuilderLengthInvalid.build()).queue();
                                    return;
                                }
                            }

                            // EasyWhitelist username store
                            FileConfiguration tempFileConfiguration = new YamlConfiguration();
                            // Default Minecraft username store
                            File whitelistJSON = (new File(".", "whitelist.json"));

                            if(onlyHasLimitedAdd)
                            {
                                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to whitelist: " + finalNameToWhitelist + ", " + (maxWhitelistAmount - timesWhitelisted) + " whitelists remaining");
                            }
                            else
                            {
                                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to whitelist: " + finalNameToWhitelist);
                            }

                            if(DiscordWhitelister.useEasyWhitelist)
                            {
                                try
                                {
                                    tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                }
                                catch(IOException | InvalidConfigurationException e)
                                {
                                    e.printStackTrace();
                                }
                            }

                            if(DiscordWhitelister.useEasyWhitelist || checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                            {
                                if(tempFileConfiguration.getStringList("whitelisted").contains(finalNameToWhitelist))
                                {
                                    onWhitelist = true;

                                    EmbedBuilder embedBuilderAlreadyWhitelisted = new EmbedBuilder();
                                    embedBuilderAlreadyWhitelisted.addField("This user is already on the whitelist", (author.getAsMention() + ", cannot add user as `" + finalNameToWhitelist + "` is already on the whitelist!"), false);
                                    embedBuilderAlreadyWhitelisted.setColor(new Color(104, 109, 224));
                                    channel.sendMessage(embedBuilderAlreadyWhitelisted.build()).queue();
                                }
                            }

                            if(!onWhitelist)
                            {
                                if(DiscordWhitelister.getRemovedList().get(finalNameToWhitelist) != null) // If the user has been removed before
                                {
                                    if(onlyHasLimitedAdd)
                                    {
                                        EmbedBuilder embedBuilderRemovedByStaff = new EmbedBuilder();
                                        embedBuilderRemovedByStaff.addField("This user was previously removed by a staff member", (author.getAsMention() + ", this user was previously removed by a staff member (<@" + DiscordWhitelister.getRemovedList().get(finalNameToWhitelist) + ">)."
                                                        + System.lineSeparator() + "Please ask a user with higher permissions to add this user." + System.lineSeparator()), false);
                                        embedBuilderRemovedByStaff.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted)
                                                + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                        embedBuilderRemovedByStaff.setColor(new Color(231, 76, 60));
                                        channel.sendMessage(embedBuilderRemovedByStaff.build()).queue();
                                        return;
                                    }
                                    else // Remove from removed list
                                    {
                                        DiscordWhitelister.getRemovedList().set(finalNameToWhitelist, null);

                                        try
                                        {
                                            DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                        }
                                        catch (IOException e)
                                        {
                                            e.printStackTrace();
                                        }

                                        DiscordWhitelister.getPlugin().getLogger().info(finalNameToWhitelist + " has been removed from the removed list by " + author.getName()
                                                + "(" + author.getId() + ")");
                                    }
                                }

                                /* Do as much as possible off the main server thread.
                                convert username into UUID to avoid depreciation and rate limits (according to https://minotar.net/) */
                                String playerUUID = minecraftUsernameToUUID(finalNameToWhitelist);
                                final boolean invalidMinecraftName = playerUUID == null;

                                /* Configure success & failure messages here instead of on the main server thread -
                                this will run even if the message is never sent, but is a good trade off */
                                EmbedBuilder embedBuilderWhitelistSuccess = new EmbedBuilder();
                                embedBuilderWhitelistSuccess.addField((finalNameToWhitelist + " is now whitelisted!"), (author.getAsMention() + " has added `" + finalNameToWhitelist + "` to the whitelist."), false);
                                if(onlyHasLimitedAdd)
                                {
                                    embedBuilderWhitelistSuccess.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - (timesWhitelisted + 1)) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                }
                                embedBuilderWhitelistSuccess.setColor(new Color(46, 204, 113));
                                embedBuilderWhitelistSuccess.setThumbnail("https://minotar.net/bust/" + playerUUID + "/100.png");

                                EmbedBuilder embedBuilderWhitelistFailure = new EmbedBuilder();
                                embedBuilderWhitelistFailure.addField("Failed to Whitelist", (author.getAsMention() + ", failed to add `" + finalNameToWhitelist + "` to the whitelist. This is most likely due to an invalid Minecraft username."), false);
                                if(onlyHasLimitedAdd)
                                {
                                    embedBuilderWhitelistFailure.addField("Whitelists Remaining", ("You have **" + (maxWhitelistAmount - timesWhitelisted) + " out of " + maxWhitelistAmount + "** whitelists remaining."), false);
                                }
                                embedBuilderWhitelistFailure.setColor(new Color(231, 76, 60));

                                int tempTimesWhitelisted = timesWhitelisted;

                                if(onlyHasLimitedAdd)
                                {
                                        if(tempTimesWhitelisted < maxWhitelistAmount)
                                        {
                                            tempTimesWhitelisted = timesWhitelisted + 1;
                                        }
                                }

                                final int finalTimesWhitelisted = tempTimesWhitelisted;
                                final int successfulTimesWhitelisted = maxWhitelistAmount - finalTimesWhitelisted;
                                final int failedTimesWhitelisted = maxWhitelistAmount - timesWhitelisted;

                                if(!DiscordWhitelister.useEasyWhitelist)
                                {
                                    if(userCanAddRemove || userCanAdd || onlyHasLimitedAdd && !usedAllWhitelists)
                                    {
                                        DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                                "whitelist add " + finalNameToWhitelist));
                                    }
                                }

                                if(DiscordWhitelister.useEasyWhitelist)
                                {
                                    if(!invalidMinecraftName) // have to do this else the easy whitelist plugin will add the name regardless of whether it is valid on not
                                    {
                                        if(userCanAddRemove || userCanAdd || onlyHasLimitedAdd && !usedAllWhitelists)
                                        {
                                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                                    "easywl add " + finalNameToWhitelist));
                                        }
                                    }

                                    // run through the server so that the check doesn't execute before the server has had a chance to run the whitelist command -- unsure if this is the best way of doing this, but it works
                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                                    {
                                        try
                                        {
                                            tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                        }
                                        catch(IOException | InvalidConfigurationException e)
                                        {
                                            e.printStackTrace();
                                        }

                                        if(!invalidMinecraftName && tempFileConfiguration.getStringList("whitelisted").contains(finalNameToWhitelist))
                                        {
                                            channel.sendMessage(embedBuilderWhitelistSuccess.build()).queue();

                                            if(onlyHasLimitedAdd)
                                            {
                                                DiscordWhitelister.getUserList().set(author.getId(), finalTimesWhitelisted);

                                                try
                                                {
                                                    DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                                                }
                                                catch (IOException e)
                                                {
                                                    e.printStackTrace();
                                                }

                                                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") successfully added " + finalNameToWhitelist
                                                        + " to the whitelist, " + successfulTimesWhitelisted + " whitelists remaining.");
                                            }
                                        }
                                        else
                                        {
                                            channel.sendMessage(embedBuilderWhitelistFailure.build()).queue();
                                        }
                                        return null;
                                    });
                                }
                                else
                                {
                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                                    {
                                        if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                                        {
                                            channel.sendMessage(embedBuilderWhitelistSuccess.build()).queue();

                                            if(onlyHasLimitedAdd)
                                            {
                                                DiscordWhitelister.getUserList().set(author.getId(), finalTimesWhitelisted);

                                                try
                                                {
                                                    DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                                                }
                                                catch (IOException e)
                                                {
                                                    e.printStackTrace();
                                                }

                                                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") successfully added " + finalNameToWhitelist
                                                        + " to the whitelist, " + failedTimesWhitelisted + " whitelists remaining.");
                                            }
                                        }
                                        else
                                        {
                                            channel.sendMessage(embedBuilderWhitelistFailure.build()).queue();
                                        }
                                        return null;
                                    });
                                }
                            }
                        }
                    }

                    if (messageContents.toLowerCase().contains("!whitelist add") && !author.isBot())
                    {
                        boolean hasPerms = false;
                        if(userCanAddRemove || userCanAdd || limitedWhitelistEnabled && userHasLimitedAdd)
                        {
                            hasPerms = true;
                        }

                        // if the user doesn't have any allowed roles
                        if(!hasPerms)
                        {
                            //channel.sendMessage(author.getAsMention() + ", you do not have permission to use this command").queue();

                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                            embedBuilderFailure.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                            embedBuilderFailure.setColor(new Color(231, 76, 60));
                            channel.sendMessage(embedBuilderFailure.build()).queue();
                        }
                    }
                }

                if(messageContents.toLowerCase().contains("!whitelist remove"))
                {
                    if(userCanAddRemove)
                    {
                        String nameToRemove = messageContents;
                        nameToRemove = nameToRemove.toLowerCase();
                        nameToRemove = nameToRemove.substring(nameToRemove.indexOf("!whitelist remove")+17); // get everything after !whitelist remove
                        nameToRemove = nameToRemove.replaceAll(" ", "");

                        final String finalNameToRemove = nameToRemove;

                        if(finalNameToRemove.isEmpty())
                        {
                            channel.sendMessage(removeCommandInfo).queue();
                        }
                        else
                        {
                            // easy whitelist
                            FileConfiguration tempFileConfiguration = new YamlConfiguration();
                            // default whitelist
                            File whitelistJSON = (new File(".", "whitelist.json"));

                            DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to remove " + finalNameToRemove + " from the whitelist");

                            if(DiscordWhitelister.useEasyWhitelist)
                            {
                                try
                                {
                                    tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                }
                                catch(IOException | InvalidConfigurationException e)
                                {
                                    e.printStackTrace();
                                }
                            }

                            boolean notOnWhitelist = false;

                            if(DiscordWhitelister.useEasyWhitelist)
                            {
                                if(!tempFileConfiguration.getStringList("whitelisted").contains(finalNameToRemove))
                                {
                                    notOnWhitelist = true;

                                    EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                    embedBuilderInfo.addField("This user is not on the whitelist", (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), false);
                                    embedBuilderInfo.setColor(new Color(104, 109, 224));
                                    channel.sendMessage(embedBuilderInfo.build()).queue();
                                }
                            }

                            if(!DiscordWhitelister.useEasyWhitelist && !checkWhitelistJSON(whitelistJSON, finalNameToRemove))
                            {
                                notOnWhitelist = true;

                                EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                embedBuilderInfo.addField("This user is not on the whitelist", (author.getAsMention() + ", cannot remove user as `" + finalNameToRemove + "` is not on the whitelist!"), false);
                                embedBuilderInfo.setColor(new Color(104, 109, 224));
                                channel.sendMessage(embedBuilderInfo.build()).queue();
                            }

                            if(!notOnWhitelist)
                            {
                                if(DiscordWhitelister.useEasyWhitelist)
                                {
                                    try
                                    {
                                        tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                    }
                                    catch(IOException | InvalidConfigurationException e)
                                    {
                                        e.printStackTrace();
                                    }

                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                            "easywl remove " + finalNameToRemove));
                                }
                                else
                                {
                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                            "whitelist remove " + finalNameToRemove));
                                }

                                // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                                EmbedBuilder embedBuilderSuccess = new EmbedBuilder();
                                embedBuilderSuccess.addField((finalNameToRemove + " has been removed"), (author.getAsMention() + " has removed `" + finalNameToRemove + "` from the whitelist."), false);
                                embedBuilderSuccess.setColor(new Color(46, 204, 113));

                                EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                embedBuilderFailure.addField(("Failed to remove " + finalNameToRemove + " from the whitelist"), (author.getAsMention() + ", failed to remove `" + finalNameToRemove + "` from the whitelist. " +
                                        "This should never happen, you may have to remove the player manually and report the issue."), false);
                                embedBuilderFailure.setColor(new Color(231, 76, 60));

                                if(DiscordWhitelister.useEasyWhitelist)
                                {
                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                                    {
                                        try
                                        {
                                            tempFileConfiguration.load(new File(DiscordWhitelister.easyWhitelist.getDataFolder(), "config.yml"));
                                        }
                                        catch(IOException | InvalidConfigurationException e)
                                        {
                                            e.printStackTrace();
                                        }

                                        if(!tempFileConfiguration.getStringList("whitelisted").contains(finalNameToRemove))
                                        {
                                            channel.sendMessage(embedBuilderSuccess.build()).queue();

                                            // if the name is not on the list
                                            if(DiscordWhitelister.getRemovedList().get(finalNameToRemove) == null)
                                            {
                                                DiscordWhitelister.getRemovedList().set(finalNameToRemove, author.getId());
                                                DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                            }
                                        }
                                        else
                                        {
                                            channel.sendMessage(embedBuilderFailure.build()).queue();
                                        }
                                        return null;
                                    });
                                }
                                else
                                {
                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                                    {
                                        if(!checkWhitelistJSON(whitelistJSON, finalNameToRemove))
                                        {
                                            channel.sendMessage(embedBuilderSuccess.build()).queue();

                                            // if the name is not on the list
                                            if(DiscordWhitelister.getRemovedList().get(finalNameToRemove) == null)
                                            {
                                                DiscordWhitelister.getRemovedList().set(finalNameToRemove, author.getId());
                                                DiscordWhitelister.getRemovedList().save(DiscordWhitelister.getRemovedListFile().getPath());
                                            }
                                        }
                                        else
                                        {
                                            channel.sendMessage(embedBuilderFailure.build()).queue();
                                        }
                                        return null;
                                    });
                                }

                            }
                        }
                    }

                    if(userCanAdd && !userCanAddRemove)
                    {
                        String higherPermRoles = DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles").toString();
                        higherPermRoles = higherPermRoles.replaceAll("\\[", "");
                        higherPermRoles = higherPermRoles.replaceAll("]", "");

                        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                        embedBuilderInfo.addField("Insufficient Permissions", (author.getAsMention() +  ", you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: "
                                + higherPermRoles + "; or get the owner to move your role to 'add-remove-roles' in the config."), false);
                        embedBuilderInfo.setColor(new Color(104, 109, 224));
                        channel.sendMessage(embedBuilderInfo.build()).queue();
                    }

                    if(messageContents.toLowerCase().contains("!whitelist remove") && !author.isBot())
                    {
                        boolean hasPerms = false;
                        if(userCanAddRemove || userCanAdd)
                        {
                            hasPerms = true;
                        }

                        // if the user doesn't have any allowed roles
                        if(!hasPerms)
                        {
                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                            embedBuilderFailure.addField("Insufficient Permissions", (author.getAsMention() + ", you do not have permission to use this command."), false);
                            embedBuilderFailure.setColor(new Color(231, 76, 60));
                            channel.sendMessage(embedBuilderFailure.build()).queue();
                        }
                    }
                }
            }
        }
    }

    private boolean checkWhitelistJSON(File whitelistFile, String minecraftUsername)
    {
        boolean correctUsername = false;

        try
        {
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonArray = (JSONArray)jsonParser.parse(new FileReader(whitelistFile));

            for(Object object : jsonArray)
            {
                JSONObject player = (JSONObject) object;

                String userName = (String)player.get("name");
                userName = userName.toLowerCase();

                if(userName.equals(minecraftUsername))
                {
                    correctUsername = true;
                }
            }
        }
        catch(IOException | ParseException e)
        {
            e.printStackTrace();
        }

        return correctUsername;
    }

    private String minecraftUsernameToUUID(String minecraftUsername)
    {
        URL playerURL;
        String inputStream;
        BufferedReader bufferedReader;

        String playerUUID = null;

        try
        {
            playerURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + minecraftUsername);
            bufferedReader = new BufferedReader(new InputStreamReader(playerURL.openStream()));
            inputStream = bufferedReader.readLine();

            if(inputStream != null)
            {
                JSONObject inputStreamObject = (JSONObject)JSONValue.parseWithException(inputStream);
                playerUUID = inputStreamObject.get("id").toString();
            }
        }
        catch (IOException | ParseException e)
        {
            e.printStackTrace();
        }

        return playerUUID;
    }
}
