package uk.jadestudios.sponge.tab;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.*;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


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
        logger.info("[Tab] Activated! 1.0.16");

        Optional<Scoreboard> maybeScoreboard = Sponge.getServer().getServerScoreboard();

        if (maybeScoreboard.isPresent()) {
            logger.info("[Tab] ScoreBoard found");
            Scoreboard scoreboard = maybeScoreboard.get();
            Optional<Objective> maybeObjective = scoreboard.getObjective("Deaths");
            if (!maybeObjective.isPresent()) {
                Objective deaths = Objective.builder().name("Deaths").criterion(Criteria.DEATHS).build();
                scoreboard.addObjective(deaths);
            }
        }
    }


    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        logger.info("[Tab] Player Joined");

        if(Sponge.getServer().getOnlinePlayers().size() <= 1){
            //Creates a new task when at least one person is on
            Task.Builder task = Task.builder();
            task.execute(() -> updateAll()).async().delay(5, TimeUnit.SECONDS).interval(5, TimeUnit.SECONDS).name("[Tab]UpdatePing").submit(this);
        }

        //Adds everyone to player TabList
        updateAll();
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        logger.info("[Tab] Player leave");
        Player player = event.getTargetEntity();
        if(Sponge.getServer().getOnlinePlayers().size() <= 1){
            //Finds the updateAll task and kills it when no one is on
            for (Task t: Sponge.getScheduler().getScheduledTasks(this)) {
                if(t.getName().equals("[Tab]UpdatePing")){
                    t.cancel();
                }
            }
        }
        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            p.getTabList().removeEntry(player.getUniqueId());
        }

        //Cuz when player leaves, it leaves the tablist
    }

    @Listener
    public void onPlayerDeath(RespawnPlayerEvent event) {
        logger.info("[Tab] Player died");
        if (!event.isDeath()) {
            return;
        }

        //Only when player dies when tab might not have anything
        updateAll();
    }


    //Private Methods below this point ==================================================
    //

    /**
     * Updates everyone's tablist for everyone
     */
    private void updateAll() {
        logger.info("[Tab] Updating Everything");

        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            for (Player s : Sponge.getServer().getOnlinePlayers()) {
                propagatePlayer(p,s);
                propagatePlayer(s,p);
            }
            propagatePlayer(p,p);
            //TODO: ADD Header Footer for each p here
        }
    }

    /**
     * Propagates the info of thisPlayer to thatPlayer
     * info in this case is the new display name
     * @param thisPlayer Sponge Entity Player
     * @param thatPlayer Sponge Entity Player
     */
    private void propagatePlayer(Player thisPlayer, Player thatPlayer) {
        Optional<TabListEntry> maybeEntry = thatPlayer.getTabList().getEntry(thisPlayer.getUniqueId());
        if (maybeEntry.isPresent()) {
            TabListEntry entry = maybeEntry.get();
            entry.setDisplayName(Text.join(getPing(thisPlayer), Text.of(thisPlayer.getName()), getDeaths(thisPlayer)));
            logger.info("Added all to me");
        } else {
            TabListEntry entry = createTabListEntry(thisPlayer, thatPlayer);
            entry.setDisplayName(Text.join(getPing(thisPlayer), Text.of(thisPlayer.getName()), getDeaths(thisPlayer)));
            thatPlayer.getTabList().addEntry(entry);
            logger.info("Added all to me 2");
        }
    }

    /**
     * Creates a tabListEntry
     * This method does not add the entry to the tabList
     * Nor does it set the displayName
     *
     * @param tabListPlayer Player to create a TabListEntry of
     * @param tabListOwner  Player to apply the TabListEntry to
     * @return TabListEntry of tabListPlayer with .list() set to tabListOwner
     */
    private TabListEntry createTabListEntry(Player tabListPlayer, Player tabListOwner) {
        TabListEntry entry = TabListEntry.builder()
                .latency(tabListPlayer.getConnection().getLatency())
                .profile(tabListPlayer.getProfile())
                .list(tabListOwner.getTabList())
                .gameMode(tabListPlayer.gameMode().get())
                .build();
        return entry;
    }

    /**
     * Gets ping of player as a Sponge.Text
     *
     * @param player Sponge Entity Player
     * @return Sponge API Text: "[123ms]" where alpha-numeric portion is colored
     */
    private Text getPing(Player player) {
        int ping = player.getConnection().getLatency();
        Text prefix = Text.of("[");
        Text suffix = Text.of("]");
        Text center;

        if (ping < 100) {
            center = Text.builder(player.getConnection().getLatency() + "ms").color(TextColors.DARK_GREEN).build();
        } else if (ping < 150) {
            center = Text.builder(player.getConnection().getLatency() + "ms").color(TextColors.YELLOW).build();
        } else {
            center = Text.builder(player.getConnection().getLatency() + "ms").color(TextColors.DARK_RED).build();
        }

        return Text.join(prefix,center,suffix);
    }


    /**
     * Gets DeathCount from ScoreBoard as a Sponge.Text
     *
     * @param player Sponge Entity Player
     * @return Sponge API Text: " -- Deaths: [DEATHCOUNT]"
     */
    private Text getDeaths(Player player) {
        Optional<Scoreboard> maybeScoreboard = Sponge.getServer().getServerScoreboard();
        Optional<Objective> maybeObjective = maybeScoreboard.get().getObjective("Deaths");
        return Text.of(" -- Deaths: " + getObjectiveScore(maybeObjective, Text.of(player.getName())));
    }

    /**
     * Gets the score from an Optional objective
     *
     * @param maybeObjective Optional<Objective>
     * @param playerName     Sponge API Text
     * @return Objective score of player (playerName) else it returns 0 on fail
     */
    private int getObjectiveScore(Optional<Objective> maybeObjective, Text playerName) {
        int score = 0;
        if (maybeObjective.isPresent()) {
            logger.info("[Tab] Found Objective");
            Objective objective = maybeObjective.get();
            Optional<Score> maybeScore = objective.getScore(playerName);
            if (maybeScore.isPresent()) {
                logger.info("[Tab] Found Score");
                score = maybeScore.get().getScore();
            }
        }
        return score;
    }
}
