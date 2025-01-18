package me.hmPlugin.assistPl.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.hmPlugin.assistPl.AssistPl;
import me.hmPlugin.assistPl.SubCommand;

public class HuntCommand implements SubCommand, Listener {
    private final AssistPl plugin;
    private boolean isGameActive = false;
    private boolean isGamePrepared = false;
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();
    private final Map<UUID, Integer> currentTargetIndex = new HashMap<>();

    public HuntCommand(AssistPl plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "prepare":
                return handlePrepare(sender);
            case "hunter":
                return handleHunterCommand(sender, args);
            case "runner":
                return handleRunnerCommand(sender, args);
            case "hunt":
                return handleHuntStart(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(Player sender) {
        sender.sendMessage("§6=== Manhunt Commands ===");
        sender.sendMessage("§f/hm hunt prepare §7- Prepare a new manhunt game");
        sender.sendMessage("§f/hm hunter <add/remove> <player> §7- Manage hunters");
        sender.sendMessage("§f/hm runner <add/remove> <player> §7- Manage runners");
        sender.sendMessage("§f/hm hunt <seconds> §7- Start the hunt with freeze duration");
    }

    private boolean handlePrepare(Player sender) {
        if (isGameActive) {
            sender.sendMessage("§cA game is already in progress!");
            return true;
        }

        isGamePrepared = true;
        hunters.clear();
        runners.clear();
        currentTargetIndex.clear();
        
        Bukkit.broadcastMessage("§6=== Manhunt Game Prepared ===");
        Bukkit.broadcastMessage("§fUse §7/hm hunter add <player>§f to add hunters");
        Bukkit.broadcastMessage("§fUse §7/hm runner add <player>§f to add runners");
        Bukkit.broadcastMessage("§fUse §7/hm hunt <seconds>§f to start the game");
        
        return true;
    }

    private boolean handleHunterCommand(Player sender, String[] args) {
        if (!isGamePrepared) {
            sender.sendMessage("§c Start manhunt preperation first with /hm hunt prepare");
            return true;
        }
        
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /hm hunter <add/remove> <player>");
            return true;
        }

        String action = args[2].toLowerCase();
        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        if (action.equals("add")) {
            if (runners.contains(target.getUniqueId())) {
                sender.sendMessage("§c You cannot run and hunt at the same time! Do you expect to just chase yourself?");
                return true;
            }
            hunters.add(target.getUniqueId());
            Bukkit.broadcastMessage("§6" + target.getName() + " §fhas been added as a hunter!");
        } else if (action.equals("remove")) {
            hunters.remove(target.getUniqueId());
            Bukkit.broadcastMessage("§6" + target.getName() + " §fhas been removed from hunters!");
        }

        return true;
    }

    private boolean handleRunnerCommand(Player sender, String[] args) {
        if (!isGamePrepared) {
            sender.sendMessage("§cPlease prepare the game first with /hm hunt prepare");
            return true;
        }
        
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /hm runner <add/remove> <player>");
            return true;
        }

        String action = args[2].toLowerCase();
        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        if (action.equals("add")) {
            if (hunters.contains(target.getUniqueId())) {
                sender.sendMessage("§c You cannot run and hunt at the same time! Do you expect to just chase yourself?");
                return true;
            }
            runners.add(target.getUniqueId());
            Bukkit.broadcastMessage("§6" + target.getName() + " §fhas been added as a runner!");
        } else if (action.equals("remove")) {
            runners.remove(target.getUniqueId());
            Bukkit.broadcastMessage("§6" + target.getName() + " §fhas been removed from runners!");
        }

        return true;
    }

    private boolean handleHuntStart(Player sender, String[] args) {
        if (!isGamePrepared) {
            sender.sendMessage("§cPlease prepare the game first with /hm hunt prepare");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage("§cUsage: /hm hunt <seconds>");
            return true;
        }

        if (hunters.isEmpty() || runners.isEmpty()) {
            sender.sendMessage("§cYou need at least one hunter and one runner!");
            return true;
        }

        int freezeTime;
        try {
            freezeTime = Integer.parseInt(args[2]);
            if (freezeTime < 1) {
                sender.sendMessage("§cFreeze time must be at least 1 second!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cFreeze time must be a number!");
            return true;
        }

        startHunt(freezeTime);
        return true;
    }

    private void startHunt(int freezeTime) {
        isGameActive = true;
        isGamePrepared = false;

        // Give hunters their tracking compasses
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                // Apply effects
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, freezeTime * 20, 100));
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, freezeTime * 20, 100));
                
                // Give compass
                ItemStack compass = new ItemStack(Material.COMPASS);
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setDisplayName("§6Runner Tracker");
                meta.setLodestone(hunter.getLocation());
                meta.setLodestoneTracked(false);
                compass.setItemMeta(meta);
                
                hunter.getInventory().addItem(compass);
                currentTargetIndex.put(hunterId, 0);
            }
        }

        Bukkit.broadcastMessage("§6=== Manhunt Has Begun! ===");
        Bukkit.broadcastMessage("§fHunters are frozen for §6" + freezeTime + " §fseconds!");
        Bukkit.broadcastMessage("§fRunners, start running!");

        String title = "§4§lThe Hunt Begins!";
        String subtitle = "§cRun while you can...";
        int fadeIn = 10;   // 10 ticks (0.5 sec)
        int stay = 60;     // 60 ticks (3 sec)
        int fadeOut = 20;  // 20 ticks (1 sec)

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§6=== Hunters have been unleashed! ===");
        }, freezeTime * 20L);
    }

    @EventHandler
    public void onCompassInteract(PlayerInteractEvent event) {
        if (!isGameActive) return;
        if (!hunters.contains(event.getPlayer().getUniqueId())) return;
        if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) return;

        Player hunter = event.getPlayer();
        List<UUID> runnerList = new ArrayList<>(runners);
        
        if (runnerList.isEmpty()) {
            hunter.sendMessage("§cNo runners to track!");
            return;
        }

        int currentIndex = currentTargetIndex.getOrDefault(hunter.getUniqueId(), 0);

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            currentIndex = (currentIndex + 1) % runnerList.size();
            currentTargetIndex.put(hunter.getUniqueId(), currentIndex);
            Player target = Bukkit.getPlayer(runnerList.get(currentIndex));
            if (target != null) {
                hunter.sendMessage("§6Now tracking: §f" + target.getName());
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player target = Bukkit.getPlayer(runnerList.get(currentIndex));
            if (target != null) {
                if (target.getWorld() == hunter.getWorld()) {
                    double distance = target.getLocation().distance(hunter.getLocation());
                    hunter.sendMessage(String.format("§6Distance to §f%s§6: §f%.1f §6blocks", 
                        target.getName(), distance));
                    
                    CompassMeta meta = (CompassMeta) event.getItem().getItemMeta();
                    meta.setLodestone(target.getLocation());
                    event.getItem().setItemMeta(meta);
                } else {
                    hunter.sendMessage("§cTarget is in a different world!");
                }
            }
        }
    }

    @Override
    public String getPermission() {
        return "assistpl.hunt";
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            List<String> subCommands = Arrays.asList("prepare", "hunter", "runner", "hunt");
            for (String cmd : subCommands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("hunter") || args[1].equalsIgnoreCase("runner")) {
                String partial = args[2].toLowerCase();
                List<String> actions = Arrays.asList("add", "remove");
                for (String action : actions) {
                    if (action.startsWith(partial)) {
                        completions.add(action);
                    }
                }
            } else if (args[1].equalsIgnoreCase("hunt")) {
                completions.addAll(Arrays.asList("30", "60", "90", "120"));
            }
        } else if (args.length == 4) {
            if ((args[1].equalsIgnoreCase("hunter") || args[1].equalsIgnoreCase("runner")) &&
                (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                String partial = args[3].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}