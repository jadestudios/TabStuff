package uk.jadestudios.sponge.tab;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scoreboard.*;


@Plugin(
        id = "tab",
        name = "TabStuff",
        description = "Does ping and deathcount in tab",
        url = "https://jadestudios.uk",
        authors = {
                "thenerdoflight"
        }
)
public class Tab {

    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        //TODO: Check and add Death deathcount objective to scoreboard
        //Assume scoreboard reference is immutable

    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event){
        //TODO: Create new team where Prefix = PING and suffix = DeathCount
        //TODO: Add player to said team
        //TODO: Register team to scoreboard

    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event){
        //TODO: Unregister Team
    }

    @Listener
    public void onPlayerDeath(RespawnPlayerEvent event){
        if (event.isDeath() != true)return;
        //TODO: Updates team suffix


    }

}
