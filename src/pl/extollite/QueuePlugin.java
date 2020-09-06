package pl.extollite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import pl.extollite.command.ReloadCommand;
import pl.extollite.config.Config;
import pl.extollite.listener.EventListener;
import pl.extollite.listener.QueryEventListener;

/**
 * QueuePlugin
 */
public class QueuePlugin extends Plugin {
    public static LinkedHashMap<UUID, String> regularQueue = new LinkedHashMap<>();
    public static LinkedHashMap<UUID, String> priorityQueue = new LinkedHashMap<>();
    private Configuration config;
    private static QueuePlugin instance;

    public static QueuePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        processConfig();
        instance = this;
        this.getLogger().info("Based on https://github.com/Leeeunderscore/LeeesBungeeQueue");
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
        getProxy().getPluginManager().registerListener(this, new EventListener());
        getProxy().getPluginManager().registerListener(this, new QueryEventListener());

        runQueueTasks();
    }


    public void processConfig() {
        try {
            loadConfig();
        } catch (IOException e) {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath());
                    loadConfig();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }

    }

    private void loadConfig() throws IOException {
        config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new File(getDataFolder(), "config.yml"));
        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);
                it.set(Config.class, config.get(it.getName()));
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    private void runQueueTasks() {
        //sends the position message and updates tab on an interval for non priority players and priority players in chat
        getProxy().getScheduler().schedule(this, () -> sendPosition(regularQueue), 10000, 10000, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> sendPosition(priorityQueue), 10000, 10000, TimeUnit.MILLISECONDS);

        //moves the queue when someone logs off the main server on an interval set in the config.yml
        try {
            getProxy().getScheduler().schedule(this, EventListener::moveQueue, Config.queueMoveDelay, Config.queueMoveDelay, TimeUnit.MILLISECONDS);
        } catch (NoSuchElementException error) {
        }

        //checks if servers are online
        try {
            getProxy().getScheduler().schedule(this, EventListener::CheckIfMainServerIsOnline, 5, 5, TimeUnit.SECONDS);
        } catch (NoSuchElementException error) {
            //ignore
        }

        try {
            getProxy().getScheduler().schedule(this, EventListener::CheckIfQueueServerIsOnline, 5, 5, TimeUnit.SECONDS);
        } catch (NoSuchElementException error) {
            //ignore
        }
    }

    private void sendPosition(Map<UUID, String> queue){
        int i = 0;
        for (Entry<UUID, String> entry : queue.entrySet()) {
            try {
                i++;

                ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                if (player == null) {
                    queue.remove(entry.getKey());
                    continue;
                }
                player.sendMessage((Config.positionMessageOnHotBar ? ChatMessageType.ACTION_BAR : ChatMessageType.CHAT),
                        TextComponent.fromLegacyText(Config.queuePosition.replace("&", "§")
                                .replace("<position>", i + "").replace("<total>",
                                        queue.size() + "").replace("<server>",
                                        entry.getValue())));

            } catch (Exception e) {
                queue.remove(entry.getKey());
                //TODO: handle exception
            }
        }
    }
}
