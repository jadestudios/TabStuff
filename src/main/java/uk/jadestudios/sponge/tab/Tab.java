package uk.jadestudios.sponge.tab;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
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

import java.nio.file.Path;
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
    @DefaultConfig(sharedRoot = true)
    private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager; //since only 1 HOCON

    @Inject
    private Logger logger;

    private CommentedConfigurationNode configNode;
    private String header;
    private String footer;
    private boolean showDeaths;
    private boolean showPing;
    private int maxNameLength = 0;


    @Listener
    public void preInit(GamePreInitializationEvent event) {
        try {
            //Creates a config file and/or loads it - stored in config\tab.conf
            if (!defaultConfig.toFile().exists()) {
                defaultConfig.toFile().createNewFile();
                this.configNode = configManager.load();
                this.configNode.getNode("setHeader").setComment("TabList Header").setValue("SOME HEADER");
                this.configNode.getNode("setFooter").setComment("TabStuff Config" + System.lineSeparator() + " Here are some very basic configs for this plugin" + System.lineSeparator() + "TabList Footer").setValue("SOME FOOTER");
                configManager.save(this.configNode);
                this.configNode.getNode("showDeaths").setValue(true).setComment("If both are off, this plugin is turned off.");
                this.configNode.getNode("showPing").setValue(true);
                configManager.save(this.configNode);
                logger.info("[Tab] Config created");
            }
            this.configNode = configManager.load();
            this.header = this.configNode.getNode("setHeader").getValue(TypeToken.of(String.class));
            this.footer = this.configNode.getNode("setFooter").getValue(TypeToken.of(String.class));
            this.showDeaths = this.configNode.getNode("showDeaths").getBoolean();
            this.showPing = this.configNode.getNode("showPing").getBoolean();

            logger.info("[Tab] Config loaded");
        } catch (Exception e) {
            logger.info("[Tab] Failed to create config");
        }
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("[Tab] Activated! 1.0.0");

        Optional<Scoreboard> maybeScoreboard = Sponge.getServer().getServerScoreboard();

        //Sets up scorebaord to have deaht count
        if (maybeScoreboard.isPresent()) {
            //   logger.info("[Tab] ScoreBoard found");
            Scoreboard scoreboard = maybeScoreboard.get();
            Optional<Objective> maybeObjective = scoreboard.getObjective("Deaths");
            if (!maybeObjective.isPresent()) {
                Objective deaths = Objective.builder().name("Deaths").criterion(Criteria.DEATHS).build();
                scoreboard.addObjective(deaths);
            }
        }

        if (!(this.showDeaths || this.showPing)) {
            //Removes any object references since this plugin is off.
            logger.info("[Tab] Shutting Down");
            this.configNode = null;
            this.header = null;
            this.footer = null;
        }


    }


    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        //logger.info("[Tab] Player Joined");
        if (!(this.showDeaths || this.showPing)) {
            return;
        }

        if(event.getTargetEntity().getName().length() > this.maxNameLength){
            this.maxNameLength = event.getTargetEntity().getName().length();

        }

        if(this.showPing) {
            if (Sponge.getServer().getOnlinePlayers().size() <= 1) {
                //Creates a new task when at least one person is on
                Task.Builder task = Task.builder();
                task.execute(() -> updateAll()).async().delay(5, TimeUnit.SECONDS).interval(5, TimeUnit.SECONDS).name("[Tab]UpdatePing").submit(this);
            }
        }

        //Adds everyone to player TabList
        updateAll();
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        //logger.info("[Tab] Player leave");
        if (!(this.showDeaths || this.showPing)) {
            return;
        }
        Player player = event.getTargetEntity();
        if (Sponge.getServer().getOnlinePlayers().size() <= 1) {
            //Finds the updateAll task and kills it when no one is on
            for (Task t : Sponge.getScheduler().getScheduledTasks(this)) {
                if (t.getName().equals("[Tab]UpdatePing")) {
                    t.cancel();
                }
            }
        }
        this.maxNameLength = 0;
        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            //Removes the player from every tabList
            if(p.getName().length() > this.maxNameLength){
                this.maxNameLength = p.getName().length();
            }
        }
    }

    @Listener
    public void onPlayerDeath(RespawnPlayerEvent event) {
        //logger.info("[Tab] Player died");
        if (!(this.showDeaths || this.showPing)) {
            return;
        }
        if (!event.isDeath()) {
            return;
        }
        updateAll();        //Only when player dies when tab might not have anything

    }


    //Private Methods below this point ==================================================
    //

    /**
     * Updates everyone's tablist for everyone
     */
    private void updateAll() {
        //logger.info("[Tab] Updating Everything");

        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            for (Player s : Sponge.getServer().getOnlinePlayers()) {
                propagatePlayer(p, s);
                propagatePlayer(s, p);
            }
            propagatePlayer(p, p);
            if (!(p.getTabList().getHeader().isPresent() & p.getTabList().getFooter().isPresent())) {
                p.getTabList().setFooter(Text.of(this.footer));
                p.getTabList().setHeader(Text.of(this.header));
            }
        }
    }

    /**
     * Propagates the info of thisPlayer to thatPlayer
     * info in this case is the new display name
     *
     * @param thisPlayer Sponge Entity Player
     * @param thatPlayer Sponge Entity Player
     */
    private void propagatePlayer(Player thisPlayer, Player thatPlayer) {

        Text printText = Text.of("");

        if (this.showPing) {
            printText = Text.join(printText, getPing(thisPlayer));
        }

        printText = Text.join(printText, getName(thisPlayer));

        if (this.showDeaths) {
            printText = Text.join(printText, getDeaths(thisPlayer));
        }

        Optional<TabListEntry> maybeEntry = thatPlayer.getTabList().getEntry(thisPlayer.getUniqueId());
        if (maybeEntry.isPresent()) {
            TabListEntry entry = maybeEntry.get();
            entry.setDisplayName(printText);
            //logger.info("Added all to me");
        } else {
            TabListEntry entry = createTabListEntry(thisPlayer, thatPlayer);
            entry.setDisplayName(printText);
            thatPlayer.getTabList().addEntry(entry);
            //logger.info("Added all to me 2");
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
     * Gets the name padded out with spaces
     * It somewhat works, but Minecraft uses proportional fonts
     * @param player Sponge Player Entity
     * @return Sponge.Text
     */
    private Text getName(Player player){
        String printThis = player.getName();

        for (int i = player.getName().length(); i < this.maxNameLength; i++) {
            printThis = printThis + " ";
        }
        return Text.of(printThis);
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
        Text suffix = Text.of("] ");
        Text center;

        if (ping < 100) {
            center = Text.builder(player.getConnection().getLatency() + "ms").color(TextColors.DARK_GREEN).build();
        } else if (ping < 150) {
            center = Text.builder(player.getConnection().getLatency() + "ms").color(TextColors.YELLOW).build();
        } else {
            center = Text.builder(player.getConnection().getLatency() + "ms").color(TextColors.DARK_RED).build();
        }

        return Text.join(prefix, center, suffix);
    }


    /**
     * Gets DeathCount from ScoreBoard as a Sponge.Text
     *
     * @param player Sponge Entity Player
     * @return Sponge API Text: " -- Deaths: [DEATHCOUNT]"
     */
    private Text getDeaths(Player player) {
        Optional<Scoreboard> maybeScoreboard = Sponge.getServer().getServerScoreboard();
        if (maybeScoreboard.isPresent()) {
            Optional<Objective> maybeObjective = maybeScoreboard.get().getObjective("Deaths");
            return Text.of(" -- Deaths: " + getObjectiveScore(maybeObjective, Text.of(player.getName())));
        }

        return Text.of(" -- Deaths: N/A");
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
            //logger.info("[Tab] Found Objective");
            Objective objective = maybeObjective.get();
            Optional<Score> maybeScore = objective.getScore(playerName);
            if (maybeScore.isPresent()) {
                //logger.info("[Tab] Found Score");
                score = maybeScore.get().getScore();
            }
        }
        return score;
    }
}
