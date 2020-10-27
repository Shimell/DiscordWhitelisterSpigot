package uk.co.angrybee.joe;

import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;

import java.util.List;

// Check for privileged intents on disconnect
public class CheckIntents extends ListenerAdapter
{
    @Override
    public void onShutdown(ShutdownEvent shutdownEvent)
    {
        CheckIntents(shutdownEvent.getCloseCode());
    }

    @Override
    public void onDisconnect(DisconnectEvent disconnectEvent)
    {
        CheckIntents(disconnectEvent.getCloseCode());
    }

    private void CheckIntents(CloseCode closeCode)
    {
        if(closeCode == null)
            return;

        if(closeCode == CloseCode.DISALLOWED_INTENTS)
        {
            DiscordClient.ShutdownClient();

            DiscordWhitelister.getPluginLogger().severe("[DiscordWhitelister] Cannot connect as this bot is not eligible to request the privileged intent 'GUILD_MEMBERS'");
            DiscordWhitelister.getPluginLogger().severe("[DiscordWhitelister] To correct this please go to your bot located at: https://discord.com/developers/applications and enable 'Server Members Intent'");
        }
    }
}
