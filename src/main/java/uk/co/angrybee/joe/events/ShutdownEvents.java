package uk.co.angrybee.joe.events;

import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;
import uk.co.angrybee.joe.DiscordWhitelister;

public class ShutdownEvents extends ListenerAdapter
{
    @Override
    public void onShutdown(ShutdownEvent shutdownEvent)
    {
        CheckIntents(shutdownEvent.getCloseCode());
    }

    // Check for the 'SERVER MEMBERS INTENT' and inform users if not enabled
    private void CheckIntents(CloseCode closeCode)
    {
        if(closeCode == null)
            return;

        if(closeCode == CloseCode.DISALLOWED_INTENTS)
        {
            DiscordWhitelister.getPluginLogger().severe("\u001B[31m" + "Cannot connect as this bot is not eligible to request the privileged intent 'GUILD_MEMBERS'" + "\u001B[0m");
            DiscordWhitelister.getPluginLogger().severe( "\u001B[31m" + "To fix this, please enable 'SERVER MEMBERS INTENT' located " +
                    "at https://discord.com/developers/applications -> the application you're using to run this bot -> the button called 'bot' on the left" + "\u001B[0m");
        }
    }
}
