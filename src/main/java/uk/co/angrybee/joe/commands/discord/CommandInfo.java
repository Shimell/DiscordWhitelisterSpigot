package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;

public class CommandInfo
{
    public static void ExecuteCommand(MessageReceivedEvent messageReceivedEvent)
    {
        AuthorPermissions authorPermissions = new AuthorPermissions(messageReceivedEvent);
        User author = messageReceivedEvent.getAuthor();
        TextChannel channel = messageReceivedEvent.getTextChannel();

        if (authorPermissions.isUserCanUseCommand())
        {
            channel.sendMessage(DiscordClient.botInfo).queue();
        }
        else
        {
            channel.sendMessage(DiscordClient.CreateInsufficientPermsMessage(author)).queue();
        }
    }
}
