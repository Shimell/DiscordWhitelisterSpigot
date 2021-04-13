package uk.co.angrybee.joe.events;

import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EssentialsVanishEvents implements Listener {

    @EventHandler
    public void onVanishStatusChangeEvent(VanishStatusChangeEvent event){
        // If value is true (player just vanished)
        if(event.getValue()) {
            VanishEvents.onPlayerHide(event.getAffected().getDisplayName());
        }
        // If value is false (player just un-vanished)
        else {
            VanishEvents.onPlayerShow(event.getAffected().getDisplayName());
        }
    }

}
