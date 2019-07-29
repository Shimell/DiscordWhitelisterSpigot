package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

// handles Discord interaction
public class ServerDiscordClient extends ListenerAdapter
{
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
                MessageChannel channel = messageReceivedEvent.getTextChannel();

                boolean userHasPerms = false;

                // TODO: fix cast exception
                for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                {
                    if(Arrays.stream((String[])DiscordWhitelister.getWhitelisterBotConfig().getList("allowed-to-use-roles").toArray()).parallel().anyMatch(role.getName()::contains))
                    {
                        userHasPerms = true;
                    }
                }

                if(messageContents.contains("!whitelist") && userHasPerms)
                {
                    String nameToWhitelist = messageContents;
                    nameToWhitelist = nameToWhitelist.substring(nameToWhitelist.indexOf("!whitelist")+1); // get everything after !whitelist
                    nameToWhitelist = nameToWhitelist.replaceAll(" ", "");
                    nameToWhitelist = nameToWhitelist.toLowerCase();

                    DiscordWhitelister.getPlugin().getLogger().info(author.getName() + "("  + author.getId() + ", " + author.getMutualGuilds() + ") attempted to whitelist " + nameToWhitelist);

                    DiscordWhitelister.getPlugin().getServer().dispatchCommand(DiscordWhitelister.getPlugin().getServer().getConsoleSender(),
                            "whitelist add " + nameToWhitelist);

                    File whitelistJSON = new File(DiscordWhitelister.getPlugin().getDataFolder(), "whitelist.json");

                    org.json.simple.parser.JSONParser jsonParser = new org.json.simple.parser.JSONParser();

                    boolean successfullyAdded = false;

                    try
                    {
                        JSONArray jsonArray = (JSONArray)jsonParser.parse(new FileReader(whitelistJSON));

                        for(Object object : jsonArray)
                        {
                            JSONObject player = (JSONObject) object;

                            String name = (String)player.get("name");

                            if(name.equals(nameToWhitelist))
                            {
                                successfullyAdded = true;
                            }
                        }
                    }
                    catch(IOException | ParseException e)
                    {
                        e.printStackTrace();
                    }

                    if(successfullyAdded)
                    {
                        channel.sendMessage("Successfully added **" + nameToWhitelist + "** to the whitelist").queue();
                    }
                    else
                    {
                        channel.sendMessage("Failed to add **" + nameToWhitelist + "** to the whitelist, this is most likely due to an invalid minecraft username").queue();
                    }
                }
                else if(messageContents.contains("!whitelist") && !userHasPerms && !messageReceivedEvent.getAuthor().isBot())
                {
                    channel.sendMessage(author.getAsMention() + ", you do not have permission to use this command").queue();
                }
            }
        }
    }
}
