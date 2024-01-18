package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import uk.co.angrybee.joe.AuthorPermissions;
import uk.co.angrybee.joe.DiscordClient;

public class CommandInfo
{
    public static void ExecuteCommand(SlashCommandInteractionEvent event)
    {
        AuthorPermissions authorPermissions = new AuthorPermissions(event);
        User author = event.getUser();

        if (authorPermissions.isUserCanUseCommand())
        {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.botInfo);
        }
        else
        {
            DiscordClient.ReplyAndRemoveAfterSeconds(event, DiscordClient.CreateInsufficientPermsMessage(author));
        }
        
    }
}
