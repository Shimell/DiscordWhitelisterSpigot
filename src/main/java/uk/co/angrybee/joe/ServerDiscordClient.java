package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

// handles Discord interaction
public class ServerDiscordClient extends ListenerAdapter
{
    public static String[] allowedToUseRoles;

    public void InitializeClient(String clientToken)
    {
        try
        {
            JDA javaDiscordAPI = new JDABuilder(AccountType.BOT)
                    .setToken(clientToken)
                    .addEventListeners(new ServerDiscordClient())
                    .build();
            javaDiscordAPI.awaitReady();
        }
        catch(LoginException | InterruptedException e)
        {
            e.printStackTrace();
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

                boolean userHasPerms = false;

                for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                {
                    if(Arrays.stream(allowedToUseRoles).parallel().anyMatch(role.getName()::contains))
                    {
                        userHasPerms = true;
                    }
                }

                if(messageContents.toLowerCase().equals("!whitelist") && userHasPerms)
                {
                    channel.sendMessage("```Discord Whitelister Bot For Spigot" + System.lineSeparator() +
                            "Version: 1.0.2" + System.lineSeparator() + "Links:" + System.lineSeparator() +
                            "https://www.spigotmc.org/resources/discord-whitelister.69929/" + System.lineSeparator() + "https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot" + System.lineSeparator() +
                            "Commands:" + System.lineSeparator() + "Add:" + System.lineSeparator() +
                            "!whitelist add <MinecraftUsername> -- Usage: Adds a user to the whitelist" + System.lineSeparator() +
                            "Remove:" + System.lineSeparator() + "!whitelistremove <MinecraftUsername> -- Usage: Removes the target user from the whitelist" + System.lineSeparator() +
                            "If you encounter an issue, please report it here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues```").queue();
                }
                else if(messageContents.toLowerCase().equals("!whitelist") && !userHasPerms && !author.isBot())
                {
                    channel.sendMessage(author.getAsMention() + ", you do not have permission to use this command").queue();
                }

                if(messageContents.toLowerCase().contains("!whitelist add") && userHasPerms)
                {
                    String nameToWhitelist = messageContents;
                    nameToWhitelist = nameToWhitelist.toLowerCase();
                    nameToWhitelist = nameToWhitelist.substring(nameToWhitelist.indexOf("!whitelist add")+14); // get everything after !whitelist add
                    nameToWhitelist = nameToWhitelist.replaceAll(" ", "");

                    final String finalNameToWhitelist = nameToWhitelist;

                    if(finalNameToWhitelist.isEmpty())
                    {
                        channel.sendMessage(author.getAsMention() + ", ```Whitelist Command:" + System.lineSeparator() +
                                "!whitelist add <MinecraftUsername>" + System.lineSeparator() + "Usage: Adds a user to the whitelist" + System.lineSeparator() +
                                "If you encounter an issue, please report it here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues```").queue();
                    }
                    else
                    {
                        File whitelistJSON = (new File(".", "whitelist.json"));

                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to whitelist " + finalNameToWhitelist);

                        if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                        {
                            channel.sendMessage(author.getAsMention() + ", user is already on the whitelist!").queue();
                        }
                        else
                        {
                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                    "whitelist add " + finalNameToWhitelist));

                            // run through the server so that the check doesn't execute before the server has had a chance to run the whitelist command -- unsure if this is the best way of doing this, but it works
                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                            {
                                if(checkWhitelistJSON(whitelistJSON, finalNameToWhitelist))
                                {
                                    channel.sendMessage(author.getAsMention() + ", successfully added **" + finalNameToWhitelist + "** to the whitelist").queue();
                                }
                                else
                                {
                                    channel.sendMessage(author.getAsMention() + ", failed to add **" + finalNameToWhitelist + "** to the whitelist, this is most likely due to an invalid Minecraft username").queue();
                                }
                                return null;
                            });
                        }
                    }
                }
                else if(messageContents.toLowerCase().contains("!whitelist add") && !userHasPerms && !author.isBot())
                {
                    channel.sendMessage(author.getAsMention() + ", you do not have permission to use this command").queue();
                }

                if(messageContents.toLowerCase().contains("!whitelist remove") && userHasPerms)
                {
                    String nameToRemove = messageContents;
                    nameToRemove = nameToRemove.toLowerCase();
                    nameToRemove = nameToRemove.substring(nameToRemove.indexOf("!whitelist remove")+17); // get everything after !whitelist remove
                    nameToRemove = nameToRemove.replaceAll(" ", "");

                    final String finalNameToRemove = nameToRemove;

                    if(finalNameToRemove.isEmpty())
                    {
                        channel.sendMessage(author.getAsMention() + ", ```Whitelist Remove Command:" + System.lineSeparator() +
                                "!whitelist remove <MinecraftUsername>" + System.lineSeparator() + "Usage: Removes the target user from the whitelist" + System.lineSeparator() +
                                "If you encounter an issue, please report it here: https://github.com/JoeShimo/DiscordWhitelisterBot-Spigot/issues```").queue();
                    }
                    else
                    {
                        File whitelistJSON = (new File(".", "whitelist.json"));

                        DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ") attempted to remove " + finalNameToRemove + " from the whitelist");

                        if(!checkWhitelistJSON(whitelistJSON, finalNameToRemove))
                        {
                            channel.sendMessage(author.getAsMention() + ", user is not on the whitelist!").queue();
                        }
                        else
                        {
                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () -> DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                                    "whitelist remove " + finalNameToRemove));

                            DiscordWhitelister.getPlugin().getServer().getScheduler().callSyncMethod(DiscordWhitelister.getPlugin(), () ->
                            {
                                if(!checkWhitelistJSON(whitelistJSON, finalNameToRemove))
                                {
                                    channel.sendMessage(author.getAsMention() + ", successfully removed **" + finalNameToRemove + "** from the whitelist").queue();
                                }
                                else
                                {
                                    channel.sendMessage(author.getAsMention() + ", failed to remove **" + finalNameToRemove + "** from the whitelist, this should never really happen, you may have to remove the player manually and report the issue.").queue();
                                }
                                return null;
                            });
                        }
                    }
                }
                else if(messageContents.toLowerCase().contains("!whitelist remove") && !userHasPerms && !author.isBot())
                {
                    channel.sendMessage(author.getAsMention() + ", you do not have permission to use this command").queue();
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
