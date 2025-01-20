package me.hmPlugin.assistPl.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import me.hmPlugin.assistPl.AssistPl;
import me.hmPlugin.assistPl.SubCommand;

public class HuntCommand implements SubCommand, Listener {
    private final AssistPl plugin;
    private boolean isGameActive = false;
    private boolean isGamePrepared = false;
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> runners = new HashSet<>();
    private final Map<UUID, Integer> currentTargetIndex = new HashMap<>();
    private final int fadeIn = 10;   
    private final int stay = 60;     
    private final int fadeOut = 20; 
    private final Set<UUID> deadRunners = new HashSet<>();
    private BukkitTask checkGameEndTask = null;

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
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§6§l Manhunt", "§fa hunt is being prepared by " + sender.getDisplayName(), fadeIn, stay, fadeOut);
        }

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
        deadRunners.clear(); 
        
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                // Start Effects like blindness and slowness
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, freezeTime * 20, 100));
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, freezeTime * 20, 100));
                
                // Hunter tracking compass
                ItemStack compass = new ItemStack(Material.COMPASS);
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setDisplayName("§6Runner Tracker");
                meta.setLodestone(hunter.getLocation());
                meta.setLodestoneTracked(false);
                // Add invisible Curse of Vanishing
                meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                
                // Mark compass with PersistentDataContainer
                NamespacedKey key = new NamespacedKey(getPermission(), "tracker_compass");
                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
                
                compass.setItemMeta(meta);
                hunter.getInventory().addItem(compass);
                
                currentTargetIndex.put(hunterId, 0); // Compass is pointing to the first runner
            }
        }

        Bukkit.broadcastMessage("§6=== Manhunt Has Begun! ===");
        Bukkit.broadcastMessage("§fHunters are frozen for §6" + freezeTime + " §fseconds!");
        Bukkit.broadcastMessage("§fRunners, start running!");

        String title = "§4§lThe Hunt Begins!";
        String subtitle_runner = "§cRun bitch run!";
        String subtitle_hunter = "§c You go get 'em man!";
         

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (runners.contains(player.getUniqueId())) {
                player.sendTitle(title, subtitle_runner, fadeIn, stay, fadeOut);
            } else {
                player.sendTitle(title, subtitle_hunter, fadeIn, stay, fadeOut);
            }
            

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§6=== Hunters have been unleashed! ===");
        }, freezeTime * 20L);

        checkGameEndTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
        for (UUID runnerId : runners) {
            Player runner = Bukkit.getPlayer(runnerId);
            if (runner != null && runner.getWorld().getEnvironment() == World.Environment.THE_END) {
                if (runner.getWorld().getEnderDragonBattle() != null && 
                    runner.getWorld().getEnderDragonBattle().getEndPortalLocation() != null) {
                    endGame(true); 
                    }
                }
            }
        }, 20L, 20L);
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
                    hunter.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            String.format("§6Tracking §f%s §6- §f%.1f §6blocks", 
                            target.getName(), distance)));  
                    
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

    private void endGame(boolean runnersWon) {
        if (!isGameActive) return;
        
        isGameActive = false;
        
        if (checkGameEndTask != null) {
            checkGameEndTask.cancel();
            checkGameEndTask = null;
        }
        
        Bukkit.broadcastMessage("§6=== Manhunt Has Ended! ===");
        if (runnersWon) {
            Bukkit.broadcastMessage("§a§lRunners have won!");
        } else {
            Bukkit.broadcastMessage("§c§lHunters have won!");
        }
        
        Bukkit.broadcastMessage("§6=== Final Stats ===");
        Bukkit.broadcastMessage("§fSurviving Runners: §6" + 
            (runners.size() - deadRunners.size()) + "/" + runners.size());
        
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                ItemStack[] contents = hunter.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item != null && item.getType() == Material.COMPASS) {
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                            item.getItemMeta().getDisplayName().equals("§6Runner Tracker")) {
                            hunter.getInventory().setItem(i, null);
                        }
                    }
                }
            }
        }
        
        for (UUID runnerId : runners) {
            Player runner = Bukkit.getPlayer(runnerId);
            if (runner != null) {
                runner.setGameMode(GameMode.SURVIVAL);
            }
        }
        
        hunters.clear();
        runners.clear();
        deadRunners.clear();
        currentTargetIndex.clear();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isGameActive) return;
        
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        
        if (runners.contains(playerId)) {
            // Prevent tracker compass from dropping
            Iterator<ItemStack> drops = event.getDrops().iterator();
            NamespacedKey key = new NamespacedKey(getPermission(), "tracker_compass");
            while (drops.hasNext()) {
                ItemStack item = drops.next();
                if (item != null && item.hasItemMeta()) {
                    if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) {
                        drops.remove();
                    }
                }
            }
            
            deadRunners.add(playerId);
            
            Bukkit.broadcastMessage("§c" + player.getName() + " §6has been eliminated!");
            
            player.setGameMode(GameMode.SPECTATOR);
            if (deadRunners.size() == runners.size()) {
                endGame(false); 
            }
        }
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        // Prevent tracker compass dropping
        ItemStack item = event.getItemDrop().getItemStack();
        NamespacedKey key = new NamespacedKey(getPermission(), "tracker_compass");
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) {
            event.setCancelled(true);
        }
    }
}
