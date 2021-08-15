package uk.jadestudios.sponge.tab;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.*;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.Set;
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
    private Server server;
    private Scoreboard scoreboard;



    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("[Tab] Activated!");
        if(Sponge.isServerAvailable()){
            this.server = Sponge.getServer();
        }

        Optional<Scoreboard> maybeScoreboard = this.server.getServerScoreboard();

        if(maybeScoreboard.isPresent()){
            logger.info("[Tab] ScoreBoard found");
            this.scoreboard = maybeScoreboard.get();
            Optional<Objective> maybeObjective = this.scoreboard.getObjective("Deaths");
            if(!maybeObjective.isPresent()){
                Objective deaths = Objective.builder().name("Deaths").criterion(Criteria.DEATHS).build();
                this.scoreboard.addObjective(deaths);
            }
        }else{
            this.scoreboard = null;
        }

        Task.Builder task = Task.builder();
        task.execute(() -> updatePingAll()).async().delay(5, TimeUnit.SECONDS).interval(5, TimeUnit.SECONDS).name("[Tab] Update Ping").submit(this);

        //TODO: Check and add Death deathcount objective to scoreboard
        //Assume scoreboard reference is immutable

        //TODO:Use StatisticsData instead.

        //TODO: Name is also changed


    }



    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event){
        logger.info("[Tab] Player Joined");
        if(this.scoreboard == null){return;}
        Player player = event.getTargetEntity();

        Optional<Objective> maybeObjective = this.scoreboard.getObjective("Deaths");
        if(maybeObjective.isPresent()){
            logger.info("[Tab] Found Objective");
            Objective deaths = maybeObjective.get();
            Text playerName = Text.builder(player.getName()).build();
            Text teamName = Text.builder("jstb."+ player.getName()).build();

            Optional<Score> maybeDeath = deaths.getScore(playerName);
            if(maybeDeath.isPresent()){
                logger.info("[Tab] Found Deaths");
                int deathScore = maybeDeath.get().getScore();
                Text prefix = LiteralText.builder("["+player.getConnection().getLatency()+"ms] ").build();
                Text suffix = LiteralText.builder(" - Deaths: "+ deathScore).build();

                Team playerTeam = Team.builder().prefix(prefix).suffix(suffix).name(teamName.toPlain()).build();
                //playerTeam.addMember(playerName);

                this.scoreboard.registerTeam(playerTeam);
                logger.info("[Tab] Team added");

            }
        }

        //TODO: Create new team where Prefix = PING and suffix = DeathCount
        //TODO: Add player to said team
        //TODO: Register team to scoreboard

        for (Player p: Sponge.getServer().getOnlinePlayers()) {
            p.setScoreboard((this.scoreboard));
        }

    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event){
        logger.info("[Tab] Player leave");
        if(this.scoreboard == null){return;}
        Player player = event.getTargetEntity();
        Text playerName = Text.builder(player.getName()).build();
        Text teamName = Text.builder("jstb."+ player.getName()).build();

        Optional<Team> maybeTeam = this.scoreboard.getTeam(teamName.toPlain());
        if(maybeTeam.isPresent()){
            Team team = maybeTeam.get();
            //team.removeMember(playerName);
            team.unregister();
            logger.info("[Tab] Team removed");
        }

        for (Player p: Sponge.getServer().getOnlinePlayers()) {
            p.setScoreboard((this.scoreboard));
        }


        //TODO: Unregister Team
    }

    @Listener
    public void onPlayerDeath(RespawnPlayerEvent event){
        logger.info("[Tab] Player died");
        if (!event.isDeath())return;

        if(this.scoreboard == null){return;}
        Player player = event.getTargetEntity();
        Text playerName = Text.builder(player.getName()).build();
        Text teamName = Text.builder("jstb."+ player.getName()).build();

        Optional<Objective> maybeObjective = this.scoreboard.getObjective("Deaths");
        if(maybeObjective.isPresent()){
            logger.info("[Tab] Found Objective");
            Objective deaths = maybeObjective.get();
            Optional<Score> maybeDeath = deaths.getScore(playerName);
            if(maybeDeath.isPresent()){
                logger.info("[Tab] Found Deaths");
                int deathScore = maybeDeath.get().getScore();
                Optional<Team> maybeTeam = this.scoreboard.getTeam(teamName.toPlain());
                if(maybeTeam.isPresent()){
                    Team team = maybeTeam.get();
                    Text suffix = LiteralText.builder(" - Deaths: "+ deathScore).build();
                    team.setSuffix(suffix);
                    logger.info("[Tab] Team updated");
                }
            }
        }

        for (Player p: Sponge.getServer().getOnlinePlayers()) {
            p.setScoreboard((this.scoreboard));
        }

        //TODO: Updates team suffix


    }

    public void updatePingAll(){
        logger.info("[Tab] Updating ping");
        Set<Team> teams = this.scoreboard.getTeams();
        //TODO: Change color of ping from green to red
        for (Team team: teams) {
            if(team.getName().contains("jstb.")){
                String playerName = team.getName().substring(5);
                Optional<Player> maybePlayer = Sponge.getServer().getPlayer(playerName);
                if(maybePlayer.isPresent()){
                    Player player = maybePlayer.get();
                    Text prefix = LiteralText.builder("["+player.getConnection().getLatency()+"ms] ").build();
                    team.setPrefix(prefix);
                    logger.info("[Tab] Ping updated");
                }
            }
        }

        for (Player p: Sponge.getServer().getOnlinePlayers()) {
            p.setScoreboard((this.scoreboard));
        }
    }

}
