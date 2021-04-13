package uk.co.angrybee.joe.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.kitteh.vanish.event.VanishStatusChangeEvent;

public class VanishNoPacketEvents implements Listener {

    @EventHandler
    public void onVanishStatusChangeEvent(VanishStatusChangeEvent event){
        // If value is true (player just vanished)
        if(event.isVanishing()) {
            VanishEvents.onPlayerHide(event.getPlayer().getDisplayName());
        }
        // If value is false (player just un-vanished)
        else {
            VanishEvents.onPlayerShow(event.getPlayer().getDisplayName());
        }
    }

}
