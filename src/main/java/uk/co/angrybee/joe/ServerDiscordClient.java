package uk.co.angrybee.joe;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

// handles Discord interaction
public class ServerDiscordClient extends ListenerAdapter
{
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
        // context
        User author = messageReceivedEvent.getAuthor();
        String messageContents = messageReceivedEvent.getMessage().getContentDisplay();
    }
}
