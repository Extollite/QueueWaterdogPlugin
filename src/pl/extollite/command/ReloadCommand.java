package pl.extollite.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import pl.extollite.config.Config;
import pl.extollite.QueuePlugin;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("qw");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args[0].equalsIgnoreCase("help"))  {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "QueueWaterdog");
            sender.sendMessage(ChatColor.GOLD + "/qw help");
            sender.sendMessage(ChatColor.GOLD + "/qw reload");
            sender.sendMessage(ChatColor.GOLD + "/qw version");
            sender.sendMessage(ChatColor.GOLD + "/qw stats");
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            return;
        }
        if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "QueueWaterdog");
            sender.sendMessage(ChatColor.GOLD + "Version "+QueuePlugin.getInstance().getDescription().getVersion()+" by");
            sender.sendMessage(ChatColor.GOLD + QueuePlugin.getInstance().getDescription().getAuthor());
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            return;
        }
        if (args[0].equalsIgnoreCase("stats")) {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "Queue stats");
            sender.sendMessage(ChatColor.GOLD + "Priority: " + ChatColor.BOLD + QueuePlugin.priorityQueue.size());
            sender.sendMessage(ChatColor.GOLD + "Regular: " + ChatColor.BOLD + QueuePlugin.regularQueue.size());
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            return;
        }
        if (sender.hasPermission(Config.adminPermission)) {
            if (args[0].equalsIgnoreCase("reload")) {
                QueuePlugin.getInstance().processConfig();
                sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
                sender.sendMessage(ChatColor.GOLD + "QueueWaterdog");
                sender.sendMessage(ChatColor.GREEN + "Config reloaded");
                sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
                return;
            }
            } else {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "QueueWaterdog");
            sender.sendMessage(ChatColor.RED + "You do not");
            sender.sendMessage(ChatColor.RED + "have permission");
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            }
    }
}