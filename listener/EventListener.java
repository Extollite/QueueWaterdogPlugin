package pl.extollite.queuewaterdog.listener;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import pl.extollite.queuewaterdog.config.Config;
import pl.extollite.queuewaterdog.QueuePlugin;

/**
 * Events
 */
public class EventListener implements Listener {
    List<UUID> regular = new ArrayList<>();
    List<UUID> priority = new ArrayList<>();
    private final static ServerInfo queue = ProxyServer.getInstance().getServerInfo(Config.queueServer);
    public static boolean mainServerOnline = false;
    public static boolean queueServerOnline = false;
    private static final Random random = new Random();

    public static void CheckIfMainServerIsOnline() {
        try {
            DatagramSocket s = new DatagramSocket(ProxyServer.getInstance().getConfig().getServerInfo(Config.mainServer).getAddress().getPort(), ProxyServer.getInstance().getConfig().getServerInfo(Config.mainServer).getAddress().getAddress());
            s.close();
            // Port opened to bind so server is OFFLINE
            queueServerOnline = false;
        } catch (BindException e) {
            // Port bind so server is ONLINE
            mainServerOnline = true;
        } catch (SocketException e) {
            e.printStackTrace();
            mainServerOnline = false;
        }
    }

    public static void CheckIfQueueServerIsOnline() {
        try {
            DatagramSocket s = new DatagramSocket(ProxyServer.getInstance().getConfig().getServerInfo(Config.queueServer).getAddress().getPort(), ProxyServer.getInstance().getConfig().getServerInfo(Config.queueServer).getAddress().getAddress());
            s.close();
            // Port opened to bind so server is OFFLINE
            queueServerOnline = false;
        } catch (BindException e) {
            // Port bind so server is ONLINE
            queueServerOnline = true;
        } catch (SocketException e) {
            e.printStackTrace();
            queueServerOnline = false;
        }
    }

    @EventHandler
    public void onLogin(PostLoginEvent event) {
        if (mainServerOnline && queueServerOnline) {
            if (!Config.alwaysQueue && ProxyServer.getInstance().getOnlineCount() <= Config.mainServerSlots)
                return;
            if (event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
                // Send the priority player to the priority queue
                priority.add(event.getPlayer().getUniqueId());
            } else if (!event.getPlayer().hasPermission(Config.queueBypassPermission) && !event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
                // Send the player to the regular queue
                regular.add(event.getPlayer().getUniqueId());
            }
        } else {
            event.getPlayer().disconnect(TextComponent.fromLegacyText(Config.serverDownKickMessage.replace("&", "§")));
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        List<UUID> queueList = null;
        Map<UUID, String> queueMap = null;
        if (player.hasPermission(Config.queuePriorityPermission)) {
            queueList = priority;
            queueMap = QueuePlugin.priorityQueue;
        } else if (!event.getPlayer().hasPermission(Config.queueBypassPermission) && !event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
            queueList = regular;
            queueMap = QueuePlugin.regularQueue;
        }
        if (queueList == null || !queueList.contains(player.getUniqueId()))
            return;
        queueList.remove(player.getUniqueId());
        // Send the player to the queue and send a message.
        String originalTarget = event.getTarget().getName();
        event.setTarget(queue);
        player.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + Config.serverFullMessage.replace("&", "§")));
        // Store the data concerning the player's destination
        Map<UUID, String> finalQueueMap = queueMap;
        QueuePlugin.getInstance().getProxy().getScheduler().schedule(QueuePlugin.getInstance(), () -> finalQueueMap.put(player.getUniqueId(), originalTarget), 5, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        //if a play in queue logs off it removes them and their position so when they log again
        //they get sent to the back of the line and have to wait through the queue again yeah
        queuesRemovePlayer(event.getPlayer());
    }

    public static void moveQueue() {
        //checks if priority queue is empty if it is a non priority user always gets in
        //if it has people in it then it gives a chance for either a priority or non
        //priority user to get in when someone logs off the main server
        //gets a random number then if the number is less then or equal to the odds set in
        //this config.yml it will add a priority player if its anything above the odds then
        //a non priority player gets added to the main server
        if (Config.mainServerSlots <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.regularQueue.size() - QueuePlugin.priorityQueue.size())
            return;
        int chance = random.nextInt(100) + 1;
        Map<UUID, String> queue = QueuePlugin.regularQueue;
        if (chance <= Config.queuePriorityChance && !QueuePlugin.priorityQueue.isEmpty()) {
            queue = QueuePlugin.priorityQueue;
        } else if(queue.isEmpty())
            return;
        Entry<UUID, String> entry = queue.entrySet().iterator().next();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
        player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
        player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Config.joinMainServerMessage.replace("&", "§").replace("<server>", entry.getValue())));
        queue.remove(entry.getKey());
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.enableKickMessage) {
            event.setKickReason(Config.kickMessage.replace("&", "§"));
        }
        queuesRemovePlayer(event.getPlayer());
    }
    
    private void queuesRemovePlayer(ProxiedPlayer player){
        if(player == null || player.getServer() == null)
            return;
        if (player.getServer().getInfo().getName().equalsIgnoreCase(Config.queueServer)) {
            player.setReconnectServer(ProxyServer.getInstance()
                    .getServerInfo(QueuePlugin.priorityQueue.get(player.getUniqueId())));
            player.setReconnectServer(ProxyServer.getInstance()
                    .getServerInfo(QueuePlugin.regularQueue.get(player.getUniqueId())));
        }
        QueuePlugin.priorityQueue.remove(player.getUniqueId());
        QueuePlugin.regularQueue.remove(player.getUniqueId());
        priority.remove(player.getUniqueId());
        regular.remove(player.getUniqueId());
    }
}
