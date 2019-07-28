package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

// handles Discord interaction
public class ServerDiscordClient extends ListenerAdapter
{
    public List<String> allowedRoles;
    public List<String> channelIds;

    public void InitializeClient(String clientToken)
    {
        try
        {
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken(clientToken)
                    .build();
        }
        catch(LoginException e)
        {
            e.printStackTrace();
        }
    }

    public void onMessageRecieved(MessageReceivedEvent messageReceivedEvent)
    {
        if(messageReceivedEvent.isFromType(ChannelType.TEXT))
        {
            boolean correctChannel = false;

            for(int channel = 0; channel < channelIds.size(); ++channel)
            {
                if(messageReceivedEvent.getTextChannel().getId().equals(channelIds.get(channel)))
                {
                    correctChannel = true;
                }
            }

            if(correctChannel)
            {
                // message context
                User author = messageReceivedEvent.getAuthor();
                String messageContents = messageReceivedEvent.getMessage().getContentDisplay();

                boolean userHasPerms = false;

                for(Role role : messageReceivedEvent.getGuild().getMember(messageReceivedEvent.getAuthor()).getRoles())
                {
                    if(Arrays.stream((String[])allowedRoles.toArray()).parallel().anyMatch(role.getName()::contains))
                    {
                        userHasPerms = true;
                    }
                }

                if(messageContents.contains("!whitelist") && userHasPerms)
                {
                    String nameToWhitelist = messageContents;
                    nameToWhitelist = nameToWhitelist.replaceAll("!whitelist", "");
                    nameToWhitelist = nameToWhitelist.replaceAll(" ", "");

                    // TODO: execute whitelist command on server, read whitelist.json and check if user was successfully added
                }
            }
        }
    }
}
