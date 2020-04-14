package uk.co.angrybee.joe;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;

public class AuthorPermissions {
    private boolean userCanAddRemove = false;
    private boolean userCanAdd = false;
    private boolean userHasLimitedAdd = false;

    public boolean isUserCanAddRemove() {
        return userCanAddRemove;
    }

    public boolean isUserCanAdd() {
        return userCanAdd;
    }

    public boolean isUserHasLimitedAdd() {
        return userHasLimitedAdd;
    }

    public boolean isUserCanUseCommand() {
        return userCanAdd || userCanAddRemove || userHasLimitedAdd;
    }

    public AuthorPermissions(MessageReceivedEvent event) {
        for (Role role : event.getGuild().getMember(event.getAuthor()).getRoles()) {
            if (Arrays.stream(DiscordClient.allowedToAddRemoveRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase)) {
                userCanAddRemove = true;
                break;
            }
        }

        for (Role role : event.getGuild().getMember(event.getAuthor()).getRoles()) {
            if (Arrays.stream(DiscordClient.allowedToAddRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase)) {
                userCanAdd = true;
                break;
            }
        }

        for (Role role : event.getGuild().getMember(event.getAuthor()).getRoles()) {
            if (Arrays.stream(DiscordClient.allowedToAddLimitedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase)) {
                userHasLimitedAdd = true;
                break;
            }
        }
    }
}
