package uk.co.angrybee.joe;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;

public class AuthorPermissions
{
    private boolean userCanAddRemove = false;
    private boolean userCanAdd = false;
    private boolean userHasLimitedAdd = false;
    private boolean userIsBanned = false;
    private boolean userCanUseClear = false;

    public AuthorPermissions(SlashCommandEvent event) {
        for (Role role : event.getMember().getRoles())
        {
            if(!DiscordWhitelister.useIdForRoles)
            {
                if (Arrays.stream(DiscordClient.allowedToAddRemoveRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userCanAddRemove = true;
                    break;
                }
            }
            else
            {
                if (Arrays.stream(DiscordClient.allowedToAddRemoveRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userCanAddRemove = true;
                    break;
                }
            }
        }

        for (Role role : event.getGuild().getMember(event.getUser()).getRoles())
        {
            if (!DiscordWhitelister.useIdForRoles)
            {
                if (Arrays.stream(DiscordClient.allowedToAddRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userCanAdd = true;
                    break;
                }
            }
            else
            {
                if (Arrays.stream(DiscordClient.allowedToAddRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userCanAdd = true;
                    break;
                }
            }
        }

        for (Role role : event.getGuild().getMember(event.getUser()).getRoles())
        {
            if(!DiscordWhitelister.useIdForRoles)
            {
                if (Arrays.stream(DiscordClient.allowedToAddLimitedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userHasLimitedAdd = true;
                    break;
                }
            }
            else
            {
                if (Arrays.stream(DiscordClient.allowedToAddLimitedRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userHasLimitedAdd = true;
                    break;
                }
            }
        }

        if(DiscordWhitelister.useOnBanEvents)
        {
            for(Role role : event.getGuild().getMember(event.getUser()).getRoles())
            {
                if(!DiscordWhitelister.useIdForRoles)
                {
                    if (Arrays.stream(DiscordWhitelister.bannedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                    {
                        userIsBanned = true;
                        break;
                    }
                }
                else
                {
                    if (Arrays.stream(DiscordWhitelister.bannedRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                    {
                        userIsBanned = true;
                        break;
                    }
                }
            }
        }

        for(Role role : event.getGuild().getMember(event.getUser()).getRoles())
        {
            if(!DiscordWhitelister.useIdForRoles)
            {
                if(Arrays.stream(DiscordClient.allowedToClearNamesRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userCanUseClear = true;
                    break;
                }
            }
            else
            {
                if(Arrays.stream(DiscordClient.allowedToClearNamesRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userCanUseClear = true;
                    break;
                }
            }
        }
    }

    public boolean isUserCanAddRemove() {
        return userCanAddRemove;
    }

    public boolean isUserCanAdd() {
        return userCanAdd;
    }

    public boolean isUserHasLimitedAdd() {
        return userHasLimitedAdd;
    }

    public boolean isUserCanUseCommand()
    {
        return userCanAdd || userCanAddRemove || userHasLimitedAdd;
    }

    public boolean isUserIsBanned() { return userIsBanned; }

    public boolean isUserCanUseClear() { return userCanUseClear; }

    public AuthorPermissions(MessageReceivedEvent event)
    {
        for (Role role : event.getGuild().getMember(event.getAuthor()).getRoles())
        {
            if(!DiscordWhitelister.useIdForRoles)
            {
                if (Arrays.stream(DiscordClient.allowedToAddRemoveRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userCanAddRemove = true;
                    break;
                }
            }
            else
            {
                if (Arrays.stream(DiscordClient.allowedToAddRemoveRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userCanAddRemove = true;
                    break;
                }
            }
        }

        for (Role role : event.getGuild().getMember(event.getAuthor()).getRoles())
        {
            if (!DiscordWhitelister.useIdForRoles)
            {
                if (Arrays.stream(DiscordClient.allowedToAddRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userCanAdd = true;
                    break;
                }
            }
            else
            {
                if (Arrays.stream(DiscordClient.allowedToAddRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userCanAdd = true;
                    break;
                }
            }
        }

        for (Role role : event.getGuild().getMember(event.getAuthor()).getRoles())
        {
            if(!DiscordWhitelister.useIdForRoles)
            {
                if (Arrays.stream(DiscordClient.allowedToAddLimitedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userHasLimitedAdd = true;
                    break;
                }
            }
            else
            {
                if (Arrays.stream(DiscordClient.allowedToAddLimitedRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userHasLimitedAdd = true;
                    break;
                }
            }
        }

        if(DiscordWhitelister.useOnBanEvents)
        {
            for(Role role : event.getGuild().getMember(event.getAuthor()).getRoles())
            {
                if(!DiscordWhitelister.useIdForRoles)
                {
                    if (Arrays.stream(DiscordWhitelister.bannedRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                    {
                        userIsBanned = true;
                        break;
                    }
                }
                else
                {
                    if (Arrays.stream(DiscordWhitelister.bannedRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                    {
                        userIsBanned = true;
                        break;
                    }
                }
            }
        }

        for(Role role : event.getGuild().getMember(event.getAuthor()).getRoles())
        {
            if(!DiscordWhitelister.useIdForRoles)
            {
                if(Arrays.stream(DiscordClient.allowedToClearNamesRoles).parallel().anyMatch(role.getName()::equalsIgnoreCase))
                {
                    userCanUseClear = true;
                    break;
                }
            }
            else
            {
                if(Arrays.stream(DiscordClient.allowedToClearNamesRoles).parallel().anyMatch(role.getId()::equalsIgnoreCase))
                {
                    userCanUseClear = true;
                    break;
                }
            }
        }
    }
}
