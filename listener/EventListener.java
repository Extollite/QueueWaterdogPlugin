package pl.extollite.queuewaterdog.listener;

import java.net.*;
import java.util.*;
import java.util.Map.Entry;

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
    ServerInfo queue = ProxyServer.getInstance().getServerInfo(Config.queueServer);
    public static boolean mainServerOnline = false;
    public static boolean queueServerOnline = false;

    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (!ple.getConnection().getName().matches(Config.regex)) {
            ple.setCancelReason(ChatColor.GOLD + "[QW] Invalid username please use: " + Config.regex);
            ple.setCancelled(true);
        }
    }

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
            if (!Config.alwaysQueue) {
                if (ProxyServer.getInstance().getOnlineCount() <= Config.mainServerSlots)
                    return;
                if (event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
                    // Send the priority player to the priority queue
                    priority.add(event.getPlayer().getUniqueId());
                }
                else if (!event.getPlayer().hasPermission(Config.queueBypassPermission) && !event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
                    // Send the player to the regular queue
                    regular.add(event.getPlayer().getUniqueId());
                }
            } else {
                if (event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
                    // Send the priority player to the priority queue
                    priority.add(event.getPlayer().getUniqueId());
                }
                else if (!event.getPlayer().hasPermission(Config.queueBypassPermission) && !event.getPlayer().hasPermission(Config.queuePriorityPermission)) {
                    // Send the player to the regular queue
                    regular.add(event.getPlayer().getUniqueId());
                }
            }
        } else {
            event.getPlayer().disconnect(Config.serverDownKickMessage.replace("&", "§"));
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (player.hasPermission(Config.queuePriorityPermission)) {
            if (!priority.contains(player.getUniqueId()))
                return;
            priority.remove(player.getUniqueId());
            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();
            e.setTarget(queue);
            player.sendMessage(ChatColor.GOLD + Config.serverFullMessage.replace("&", "§"));
            // Store the data concerning the player's destination
            QueuePlugin.priorityQueue.put(player.getUniqueId(), originalTarget);
        } else if (!e.getPlayer().hasPermission(Config.queueBypassPermission) && !e.getPlayer().hasPermission(Config.queuePriorityPermission)) {
            if (!regular.contains(player.getUniqueId()))
                return;
            regular.remove(player.getUniqueId());
            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();
            e.setTarget(queue);
            player.sendMessage(ChatColor.GOLD + Config.serverFullMessage.replace("&", "§"));
            // Store the data concerning the player's destination
            QueuePlugin.regularQueue.put(player.getUniqueId(), originalTarget);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        //if a play in queue logs off it removes them and their position so when they log again
        //they get sent to the back of the line and have to wait through the queue again yeah
        try {
            if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(Config.queueServer)) {
                e.getPlayer().setReconnectServer(ProxyServer.getInstance()
                        .getServerInfo(QueuePlugin.priorityQueue.get(e.getPlayer().getUniqueId())));
                e.getPlayer().setReconnectServer(ProxyServer.getInstance()
                        .getServerInfo(QueuePlugin.regularQueue.get(e.getPlayer().getUniqueId())));
            }
            QueuePlugin.priorityQueue.remove(e.getPlayer().getUniqueId());
            QueuePlugin.regularQueue.remove(e.getPlayer().getUniqueId());
            priority.remove(e.getPlayer().getUniqueId());
            regular.remove(e.getPlayer().getUniqueId());
        } catch (NullPointerException error) {
        }
    }

    public static void moveQueue() {
        //checks if priority queue is empty if it is a non priority user always gets in
        //if it has people in it then it gives a chance for either a priority or non
        //priority user to get in when someone logs off the main server
        //gets a random number then if the number is less then or equal to the odds set in
        //this pl.extollite.queuewaterdog.config.yml it will add a priority player if its anything above the odds then
        //a non priority player gets added to the main server
        //Random rn = new Random();
        //for (int i = 0; i < 100; i++) {
        //int answer = rn.nextInt(10) + 1;
        if (!QueuePlugin.priorityQueue.isEmpty()) {
            if (Config.mainServerSlots <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.regularQueue.size() - QueuePlugin.priorityQueue.size())
                return;
            if (QueuePlugin.priorityQueue.isEmpty())
                return;
            Entry<UUID, String> entry = QueuePlugin.priorityQueue.entrySet().iterator().next();
            ProxiedPlayer player2 = ProxyServer.getInstance().getPlayer(entry.getKey());
            player2.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
            player2.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Config.joinMainServerMessage.replace("&", "§").replace("<server>", entry.getValue())));
            QueuePlugin.priorityQueue.remove(entry.getKey());
        } else {
            if (Config.mainServerSlots <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.regularQueue.size() - QueuePlugin.priorityQueue.size())
                return;
            if (QueuePlugin.regularQueue.isEmpty())
                return;
            Entry<UUID, String> entry = QueuePlugin.regularQueue.entrySet().iterator().next();
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
            player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
            player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Config.joinMainServerMessage.replace("&", "§").replace("<server>", entry.getValue())));
            QueuePlugin.regularQueue.remove(entry.getKey());
        }
    }
    //}

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.enableKickMessage) {
            event.setKickReason(Config.kickMessage.replace("&", "§"));
        }
    }
}
