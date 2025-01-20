package me.hmPlugin.assistPl.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.hmPlugin.assistPl.AssistPl;
import me.hmPlugin.assistPl.SubCommand;

public class NearbyCommand implements SubCommand {
    private final AssistPl plugin;
    private final Map<String, BukkitTask> activeTasks = new HashMap<>();

    public NearbyCommand(AssistPl plugin) {
        this.plugin = plugin;
    }
    private boolean sameWorld = true;

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(Player sender) {
        sender.sendMessage("§cUsage:");
        sender.sendMessage("§c/hm nearby start <player1> <player2> <interval>");
        sender.sendMessage("§c/hm nearby stop <player1> <player2>");
    }

    private boolean handleStart(Player sender, String[] args) {
        if (args.length != 5) {
            sender.sendMessage("§cUsage: /hm nearby start <player1> <player2> <interval>");
            return true;
        }

        String player1Name = args[2];
        String player2Name = args[3];
        
        int interval;
        try {
            interval = Integer.parseInt(args[4]);
            if (interval < 1) {
                sender.sendMessage("§cInterval must be at least 1 second!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInterval must be a number!");
            return true;
        }

        Player player1 = Bukkit.getPlayer(player1Name);
        Player player2 = Bukkit.getPlayer(player2Name);

        if (player1 == null || player2 == null) {
            sender.sendMessage("§cOne or both players are not online!");
            return true;
        }

        String taskId = player1Name + "_" + player2Name;

        if (activeTasks.containsKey(taskId)) {
            sender.sendMessage("§cAlready tracking these players! Use /hm nearby stop to stop tracking first.");
            return true;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player1.isOnline() || !player2.isOnline()) {
                activeTasks.get(taskId).cancel();
                activeTasks.remove(taskId);
                Bukkit.broadcastMessage("§cDistance tracking stopped: One or both players went offline!");
                return;
            }

            if (player1.getWorld() == player2.getWorld()) {
                if (!sameWorld) {
                    Bukkit.broadcastMessage("§6 Players have returned to the same world again!");
                    sameWorld = true;
                }
                double distance = player1.getLocation().distance(player2.getLocation());
                if (distance > 100) {
                    Bukkit.broadcastMessage(String.format("§2 Distance between §f%s§2 and §f%s§2: §f%.2f §2 blocks", 
                    player1Name, player2Name, distance));
                } else { 
                    Bukkit.broadcastMessage(String.format("§c Distance between §f%s§c and §f%s§c: §f%.2f §c blocks", 
                    player1Name, player2Name, distance));} 
                }
                else {
                if (sameWorld) {
                    Bukkit.broadcastMessage("§dPlayers are in different worlds!");
                    sameWorld = false;
                }
            }
        }, 0L, interval * 20L);

        activeTasks.put(taskId, task);
        sender.sendMessage("§6 Put ankle monitors onto " + player1Name + " and " + player2Name);
        return true;
    }

    private boolean handleStop(Player sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /hm nearby stop <player1> <player2>");
            return true;
        }

        String player1Name = args[2];
        String player2Name = args[3];
        String taskId = player1Name + "_" + player2Name;

        if (!activeTasks.containsKey(taskId)) {
            sender.sendMessage("§c no active ankle monitor for these good guys, or girls, we dont care");
            return true;
        }

        activeTasks.get(taskId).cancel();
        activeTasks.remove(taskId);
        sender.sendMessage("§6 removed ankle monitor for " + player1Name + " and " + player2Name);
        return true;
    }

    @Override
    public String getPermission() {
        return "assistpl.nearby";
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            // start stop thing
            String partial = args[1].toLowerCase();
            List<String> subCommands = Arrays.asList("start", "stop");
            for (String cmd : subCommands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3 || args.length == 4) {
            String partialName = args[args.length - 1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 5 && args[1].equalsIgnoreCase("start")) {
            String partial = args[4].toLowerCase();
            List<String> intervals = Arrays.asList("5", "10", "30", "60");
            for (String interval : intervals) {
                if (interval.startsWith(partial)) {
                    completions.add(interval);
                }
            }
        }
        
        return completions;
    }
}