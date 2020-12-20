package uk.co.angrybee.joe.commands.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import uk.co.angrybee.joe.AuthorPermissions;

// Used to pass information used by commands
public class MessageContext
{
    public String[] splitMessage;
    public AuthorPermissions authorPermissions;
    public User author;
    public Member member;
    public TextChannel channel;
}
