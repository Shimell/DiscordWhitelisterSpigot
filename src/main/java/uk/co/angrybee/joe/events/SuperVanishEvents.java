package uk.co.angrybee.joe.events;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import uk.co.angrybee.joe.DiscordWhitelister;

public class SuperVanishEvents implements Listener {

    @EventHandler
    public void onPlayerShowEvent(PlayerShowEvent event){
        VanishEvents.onPlayerShow(event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onPlayerHideEvent(PlayerHideEvent event){
        VanishEvents.onPlayerHide(event.getPlayer().getDisplayName());
    }

}
