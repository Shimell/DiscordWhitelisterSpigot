package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
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
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

// handles Discord interaction
public class ServerDiscordClient extends ListenerAdapter
{
    public static String[] allowedToAddRemoveRoles;
    public static String[] allowedToAddRoles;
    public static String[] allowedToAddLimitedRoles;

    final char[] validCharacters = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h',
    'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_'};

    public int InitializeClient(String clientToken)
    {
        try
        {
            JDA javaDiscordAPI = new JDABuilder(AccountType.BOT)
                    .setToken(clientToken)
                    .addEventListeners(new ServerDiscordClient())
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

    @Override
    public void onMessageReceived(MessageReceivedEvent messageReceivedEvent)
    {
        if(messageReceivedEvent.isFromType(ChannelType.TEXT))
        {
            boolean correctChannel = false;

            for(int channel = 0; channel < DiscordWhitelister.getWhitelisterBotConfig().getList("target-text-channels").size(); ++channel)
            {
                if(messageReceivedEvent.getTextChannel().getId().equals(DiscordWhitelister.getWhitelisterBotConfig().getList("target-text-channels").get(channel)))
                {
                    correctChannel = true;
                }
            }

            if(correctChannel && !messageReceivedEvent.getAuthor().isBot())
            {
                // message context
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

                // limited add check
                if(DiscordWhitelister.getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled"))
                {
                    // check if user is in a limited add role
                    for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                    {
                        if(Arrays.stream(allowedToAddLimitedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                        {
                            userHasLimitedAdd = true;
                        }
                    }

                    // check if user is already in the list if in limited add role
                    if(userHasLimitedAdd)
                    {
                        // create entry if user is not on list
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
                    if(userCanAddRemove || userCanAdd)
                    {
                        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                        embedBuilderInfo.setTitle("Discord Whitelister Bot for Spigot");
                        embedBuilderInfo.addField("Version", "1.0.9", false);
                        embedBuilderInfo.addField("Links", ("https://www.spigotmc.org/resources/discord-whitelister.69929/" + System.lineSeparator()
                                + "https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot"), false);
                        embedBuilderInfo.addField("Commands", ("**Add:** !whitelist add minecraftUsername" + System.lineSeparator()
                                + "**Remove:** !whitelist remove minecraftUsername"), false);
                        embedBuilderInfo.addField("Experiencing issues?", "If you encounter an issue, please report it here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues", false);
                        embedBuilderInfo.setColor(new Color(104, 109, 224));
                        channel.sendMessage(embedBuilderInfo.build()).queue();
                    }
                }

                if(messageContents.toLowerCase().equals("!whitelist") && !author.isBot())
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

                if(messageContents.toLowerCase().contains("!whitelist add"))
                {
                    boolean usedAllWhitelists = false;

                    if(DiscordWhitelister.getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled") && userHasLimitedAdd && !userCanAddRemove && !userCanAdd)
                    {
                        if(DiscordWhitelister.getUserList().getString(author.getId()) != null
                                && Integer.parseInt(DiscordWhitelister.getUserList().getString(author.getId())) >= DiscordWhitelister.getWhitelisterBotConfig().getInt("max-whitelist-amount"))
                        {
                            usedAllWhitelists = true;
                        }
                    }

                    if(userCanAddRemove || userCanAdd || userHasLimitedAdd)
                    {
                        String nameToWhitelist = messageContents;
                        nameToWhitelist = nameToWhitelist.toLowerCase();
                        nameToWhitelist = nameToWhitelist.substring(nameToWhitelist.indexOf("!whitelist add")+14); // get everything after !whitelist add
                        nameToWhitelist = nameToWhitelist.replaceAll(" ", "");

                        final String finalNameToWhitelist = nameToWhitelist;

                        final char[] finalNameToWhitelistChar = finalNameToWhitelist.toCharArray();

                        if(userCanAddRemove || userCanAdd)
                        {
                            if(finalNameToWhitelist.isEmpty())
                            {
                                EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                embedBuilderInfo.addField("Whitelist Add Command", ("!whitelist add minecraftUsername" + System.lineSeparator() + System.lineSeparator() +
                                        "If you encounter any issues, please report them here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues"), false);
                                embedBuilderInfo.setColor(new Color(104, 109, 224));
                                channel.sendMessage(embedBuilderInfo.build()).queue();
                            }
                            else
                            {
                                if(DiscordWhitelister.getWhitelisterBotConfig().getBoolean("username-validation"))
                                {
                                    // invalid char check
                                    for(int a = 0; a < finalNameToWhitelistChar.length; ++a)
                                    {
                                        if(new String(validCharacters).indexOf(finalNameToWhitelistChar[a]) == -1)
                                        {
                                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                            embedBuilderFailure.addField("Invalid username", (author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**."), false);
                                            embedBuilderFailure.setColor(new Color(231, 76, 60));
                                            channel.sendMessage(embedBuilderFailure.build()).queue();
                                            return;
                                        }
                                    }

                                    // length check
                                    if(finalNameToWhitelist.length() < 3 || finalNameToWhitelist.length() > 16)
                                    {
                                        EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                        embedBuilderFailure.addField("Invalid username", (author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**."), false);
                                        embedBuilderFailure.setColor(new Color(231, 76, 60));
                                        channel.sendMessage(embedBuilderFailure.build()).queue();
                                        return;
                                    }
                                }

                                // easy whitelist
                                FileConfiguration tempFileConfiguration = new YamlConfiguration();
                                // default whitelist
                                File whitelistJSON = (new File(".", "whitelist.json"));

                                DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to whitelist: " + finalNameToWhitelist);

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

                                boolean onWhitelist = false;

                                if(DiscordWhitelister.useEasyWhitelist)
                                {
                                    if(Objects.requireNonNull(tempFileConfiguration.getStringList("whitelisted")).contains(finalNameToWhitelist))
                                    {
                                        onWhitelist = true;

                                        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                        embedBuilderInfo.addField("This user is already on the whitelist", (author.getAsMention() + ", `" + finalNameToWhitelist + "` is already on the whitelist."), false);
                                        embedBuilderInfo.setColor(new Color(104, 109, 224));
                                        channel.sendMessage(embedBuilderInfo.build()).queue();
                                    }
                                }
                                else if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                                {
                                    onWhitelist = true;

                                    EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                    embedBuilderInfo.addField("This user is already on the whitelist", (author.getAsMention() + ", `" + finalNameToWhitelist + "` is already on the whitelist."), false);
                                    embedBuilderInfo.setColor(new Color(104, 109, 224));
                                    channel.sendMessage(embedBuilderInfo.build()).queue();
                                }

                                if(!onWhitelist)
                                {
                                    // remove from removed list
                                    if(DiscordWhitelister.getRemovedList().get(finalNameToWhitelist) != null)
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

                                    if(!DiscordWhitelister.useEasyWhitelist)
                                    {
                                        DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                                "whitelist add " + finalNameToWhitelist));
                                    }

                                    /* Do as much as possible off the main thread.
                                    convert username into UUID to avoid depreciation and rate limits (according to https://minotar.net/) */
                                    URL playerURL = null;
                                    String playerURLString = null;
                                    String playerUUID = null;

                                    boolean invalidMinecraftName = false;

                                    try
                                    {
                                        playerURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + finalNameToWhitelist);
                                    }
                                    catch (MalformedURLException e)
                                    {
                                        e.printStackTrace();
                                    }

                                    BufferedReader bufferedReader = null;
                                    try
                                    {
                                        bufferedReader = new BufferedReader(new InputStreamReader(playerURL.openStream()));
                                        playerURLString = bufferedReader.readLine();
                                    }
                                    catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }

                                    if(playerURLString == null)
                                    {
                                        invalidMinecraftName = true;
                                    }

                                    try
                                    {
                                        if(!invalidMinecraftName)
                                        {
                                            JSONObject playerUUIDObject = (JSONObject) JSONValue.parseWithException(playerURLString);
                                            playerUUID = playerUUIDObject.get("id").toString();
                                        }
                                    }
                                    catch (ParseException e)
                                    {
                                        e.printStackTrace();
                                    }

                                    if(invalidMinecraftName)
                                    {
                                        playerUUID = "";
                                    }

                                    String finalPlayerURLString = playerURLString;

                                    // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                                    EmbedBuilder embedBuilderSuccess = new EmbedBuilder();
                                    embedBuilderSuccess.addField((finalNameToWhitelist + " is now whitelisted!"), (author.getAsMention() + " has added `" + finalNameToWhitelist + "` to the whitelist."), false);
                                    embedBuilderSuccess.setColor(new Color(46, 204, 113));
                                    embedBuilderSuccess.setThumbnail("https://minotar.net/bust/" + playerUUID + "/100.png");

                                    EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                    embedBuilderFailure.addField("Failed to Whitelist", (author.getAsMention() + ", failed to add `" + finalNameToWhitelist + "` to the whitelist. This is most likely due to an invalid Minecraft username."), false);
                                    embedBuilderFailure.setColor(new Color(231, 76, 60));

                                    if(DiscordWhitelister.useEasyWhitelist)
                                    {
                                        if(finalPlayerURLString != null) // have to do this else the easy whitelist plugin will add the name regardless of whether it is valid on not
                                        {
                                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                                    "easywl add " + finalNameToWhitelist));
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

                                            if(finalPlayerURLString != null && Objects.requireNonNull(tempFileConfiguration.getStringList("whitelisted")).contains(finalNameToWhitelist))
                                            {

                                                channel.sendMessage(embedBuilderSuccess.build()).queue();
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
                                            if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                                            {
                                                channel.sendMessage(embedBuilderSuccess.build()).queue();
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

                        if(DiscordWhitelister.getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled") && userHasLimitedAdd && !usedAllWhitelists && !userCanAddRemove && !userCanAdd)
                        {
                            if(DiscordWhitelister.getUserList().getString(author.getId()) != null)
                            {
                                int whitelistLimit = DiscordWhitelister.getWhitelisterBotConfig().getInt("max-whitelist-amount");
                                int timesWhitelisted = Integer.parseInt(DiscordWhitelister.getUserList().getString(author.getId()));

                                if(finalNameToWhitelist.isEmpty())
                                {
                                    EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                    embedBuilderInfo.addField("Whitelist Add Command", ("!whitelist add minecraftUsername" + System.lineSeparator() + System.lineSeparator() +
                                            "If you encounter any issues, please report them here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues" + System.lineSeparator()), false);
                                    embedBuilderInfo.addField("Whitelists Remaining", ("You have **" + (whitelistLimit - timesWhitelisted)
                                            + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                    embedBuilderInfo.setColor(new Color(104, 109, 224));
                                    channel.sendMessage(embedBuilderInfo.build()).queue();
                                }
                                else
                                {
                                    if(DiscordWhitelister.getWhitelisterBotConfig().getBoolean("username-validation"))
                                    {
                                        // invalid char check
                                        for(int a = 0; a < finalNameToWhitelistChar.length; ++a)
                                        {
                                            if(new String(validCharacters).indexOf(finalNameToWhitelistChar[a]) == -1)
                                            {
                                                EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                                embedBuilderFailure.addField("Invalid username", (author.getAsMention() + ", the username you have specified contains invalid characters. **Only letters, numbers and underscores are allowed**."
                                                        + System.lineSeparator()), false);
                                                embedBuilderFailure.addField("Whitelists Remaining", ("You have **" + (whitelistLimit - timesWhitelisted)
                                                        + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                                embedBuilderFailure.setColor(new Color(231, 76, 60));
                                                channel.sendMessage(embedBuilderFailure.build()).queue();
                                                return;
                                            }
                                        }

                                        // length check
                                        if(finalNameToWhitelist.length() < 3 || finalNameToWhitelist.length() > 16)
                                        {
                                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                            embedBuilderFailure.addField("Invalid username", (author.getAsMention() + ", the username you have specified either contains too few or too many characters. **Usernames can only consist of 3-16 characters**."
                                                    + System.lineSeparator()), false);
                                            embedBuilderFailure.addField("Whitelists Remaining", ("You have **" + (whitelistLimit - timesWhitelisted)
                                                    + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                            embedBuilderFailure.setColor(new Color(231, 76, 60));
                                            channel.sendMessage(embedBuilderFailure.build()).queue();
                                            return;
                                        }
                                    }

                                    // easy whitelist
                                    FileConfiguration tempFileConfiguration = new YamlConfiguration();
                                    // default whitelist
                                    File whitelistJSON = (new File(".", "whitelist.json"));

                                    DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to whitelist: " + finalNameToWhitelist
                                            + ", " + (whitelistLimit - timesWhitelisted) + " whitelists remaining");

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

                                    boolean onWhitelist = false;

                                    if(DiscordWhitelister.useEasyWhitelist)
                                    {
                                        if(Objects.requireNonNull(tempFileConfiguration.getStringList("whitelisted")).contains(finalNameToWhitelist))
                                        {
                                            onWhitelist = true;

                                            EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                            embedBuilderInfo.addField("This user is already on the whitelist", (author.getAsMention() + ", `" + finalNameToWhitelist + "` is already on the whitelist."), false);
                                            embedBuilderInfo.setColor(new Color(104, 109, 224));
                                            channel.sendMessage(embedBuilderInfo.build()).queue();
                                        }
                                    }
                                    else if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                                    {
                                        onWhitelist = true;

                                        EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                                        embedBuilderInfo.addField("This user is already on the whitelist", (author.getAsMention() + ", cannot add user as `" + finalNameToWhitelist + "` is already on the whitelist!"), false);
                                        embedBuilderInfo.setColor(new Color(104, 109, 224));
                                        channel.sendMessage(embedBuilderInfo.build()).queue();
                                    }

                                    if(!onWhitelist)
                                    {
                                        if(DiscordWhitelister.getRemovedList().get(finalNameToWhitelist) != null)
                                        {
                                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                            embedBuilderFailure.addField("This user was previously removed by a staff member",
                                                    (author.getAsMention() + ", this user was previously removed by a staff member (<@" + DiscordWhitelister.getRemovedList().get(finalNameToWhitelist) + ">)."
                                                            + System.lineSeparator() + "Please ask a user with higher permissions to add this user." + System.lineSeparator()), false);
                                            embedBuilderFailure.addField("Whitelists Remaining", ("You have **" + (whitelistLimit - timesWhitelisted)
                                                    + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                            embedBuilderFailure.setColor(new Color(231, 76, 60));
                                            channel.sendMessage(embedBuilderFailure.build()).queue();
                                        }
                                        else
                                        {
                                            if(!DiscordWhitelister.useEasyWhitelist)
                                            {
                                                DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                                        "whitelist add " + finalNameToWhitelist));
                                            }

                                            /* Do as much as possible off the main thread.
                                            convert username into UUID to avoid depreciation and rate limits (according to https://minotar.net/) */
                                            URL playerURL = null;
                                            String playerURLString = null;
                                            String playerUUID = null;

                                            boolean invalidMinecraftName = false;

                                            try
                                            {
                                                playerURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + finalNameToWhitelist);
                                            }
                                            catch (MalformedURLException e)
                                            {
                                                e.printStackTrace();
                                            }

                                            BufferedReader bufferedReader = null;
                                            try
                                            {
                                                bufferedReader = new BufferedReader(new InputStreamReader(playerURL.openStream()));
                                                playerURLString = bufferedReader.readLine();
                                            }
                                            catch (IOException e)
                                            {
                                                e.printStackTrace();
                                            }

                                            if(playerURLString == null)
                                            {
                                                invalidMinecraftName = true;
                                            }

                                            try
                                            {
                                                if(!invalidMinecraftName)
                                                {
                                                    JSONObject playerUUIDObject = (JSONObject) JSONValue.parseWithException(playerURLString);
                                                    playerUUID = playerUUIDObject.get("id").toString();
                                                }
                                            }
                                            catch (ParseException e)
                                            {
                                                e.printStackTrace();
                                            }

                                            if(invalidMinecraftName)
                                            {
                                                playerUUID = "";
                                            }

                                            String finalPlayerURLString = playerURLString;

                                            // Configure message here instead of on the main thread - this means this will run even if the message is never sent, but is a good trade off (I think)
                                            EmbedBuilder embedBuilderSuccess = new EmbedBuilder();
                                            embedBuilderSuccess.addField((finalNameToWhitelist + " is now whitelisted!"), (author.getAsMention() + " has added `" + finalNameToWhitelist + "` to the whitelist."), false);
                                            embedBuilderSuccess.addField("Whitelists Remaining", ("You have **" + (whitelistLimit - (timesWhitelisted + 1))
                                                    + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                            embedBuilderSuccess.setColor(new Color(46, 204, 113));
                                            embedBuilderSuccess.setThumbnail("https://minotar.net/bust/" + playerUUID + "/100.png");

                                            EmbedBuilder embedBuilderFailure = new EmbedBuilder();
                                            embedBuilderFailure.addField("Failed to Whitelist", (author.getAsMention() + ", failed to add `" + finalNameToWhitelist + "` to the whitelist. This is most likely due to an invalid Minecraft username."), false);
                                            embedBuilderFailure.addField("Whitelists Remaining", ("You have **" + (whitelistLimit - timesWhitelisted)
                                                    + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getString("max-whitelist-amount") + "** whitelists remaining."), false);
                                            embedBuilderFailure.setColor(new Color(231, 76, 60));

                                            int tempFinal = timesWhitelisted;

                                            if(tempFinal < 3)
                                            {
                                                tempFinal = timesWhitelisted + 1;
                                            }

                                            int finalTimesWhitelistedInc = tempFinal;

                                            int successfulFinalTimesWhitelisted = whitelistLimit - finalTimesWhitelistedInc;
                                            int failedFinalTimesWhitelisted = whitelistLimit - timesWhitelisted;

                                            if(DiscordWhitelister.useEasyWhitelist)
                                            {
                                                if(finalPlayerURLString != null) // have to do this else the easy whitelist plugin will add the name regardless of whether it is valid on not
                                                {
                                                    DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                                            "easywl add " + finalNameToWhitelist));
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

                                                    if(finalPlayerURLString != null && Objects.requireNonNull(tempFileConfiguration.getStringList("whitelisted")).contains(finalNameToWhitelist))
                                                    {
                                                        //DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") successfully added " + finalNameToWhitelist);

                                                        channel.sendMessage(embedBuilderSuccess.build()).queue();

                                                        try
                                                        {
                                                            DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                                                        }
                                                        catch (IOException e)
                                                        {
                                                            e.printStackTrace();
                                                        }

                                                        DiscordWhitelister.getUserList().set(author.getId(), finalTimesWhitelistedInc);

                                                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") successfully added " + finalNameToWhitelist
                                                                + " to the whitelist, " + successfulFinalTimesWhitelisted + " whitelists remaining.");
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
                                                    if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                                                    {
                                                       channel.sendMessage(embedBuilderSuccess.build()).queue();

                                                        DiscordWhitelister.getUserList().set(author.getId(), finalTimesWhitelistedInc);

                                                        try
                                                        {
                                                            DiscordWhitelister.getUserList().save(DiscordWhitelister.getUserListFile().getPath());
                                                        }
                                                        catch (IOException e)
                                                        {
                                                            e.printStackTrace();
                                                        }

                                                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") successfully added " + finalNameToWhitelist
                                                                + " to the whitelist, " + successfulFinalTimesWhitelisted + " whitelists remaining.");
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
                            }
                        }
                        else if(userHasLimitedAdd && usedAllWhitelists)
                        {
                            EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                            embedBuilderInfo.addField("No Whitelists Remaining", (author.getAsMention() + ", unable to whitelist. You have **" + (DiscordWhitelister.getWhitelisterBotConfig().getInt("max-whitelist-amount") - Integer.parseInt(DiscordWhitelister.getUserList().getString(author.getId())))
                                    + " out of " + DiscordWhitelister.getWhitelisterBotConfig().getInt("max-whitelist-amount") + "** whitelists remaining."), false);
                            embedBuilderInfo.setColor(new Color(104, 109, 224));
                            channel.sendMessage(embedBuilderInfo.build()).queue();
                        }
                    }

                    if (messageContents.toLowerCase().contains("!whitelist add") && !author.isBot())
                    {
                        boolean hasPerms = false;
                        if(userCanAddRemove || userCanAdd || DiscordWhitelister.getWhitelisterBotConfig().getBoolean("limited-whitelist-enabled") && userHasLimitedAdd)
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
                            EmbedBuilder embedBuilderInfo = new EmbedBuilder();
                            embedBuilderInfo.addField("Whitelist Remove Command", ("!whitelist remove minecraftUsername" + System.lineSeparator() + System.lineSeparator() +
                                    "If you encounter any issues, please report them here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues"), false);
                            embedBuilderInfo.setColor(new Color(104, 109, 224));
                            channel.sendMessage(embedBuilderInfo.build()).queue();
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
//                        channel.sendMessage(author.getAsMention() +
//                                ", you only have permission to add people to the whitelist. To remove people from the whitelist you must be moved to the following roles: "
//                                + DiscordWhitelister.getWhitelisterBotConfig().getList("add-remove-roles").toString() + "; or get the owner to move your role to 'add-remove-roles' in the config.").queue();

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

                String name = (String)player.get("name");
                name = name.toLowerCase();

                if(name.equals(minecraftUsername))
                {
                    correctUsername = true;
                }
            }
        }
        catch(IOException | ParseException e)
        {
            e.printStackTrace();
        }

        if(correctUsername)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
