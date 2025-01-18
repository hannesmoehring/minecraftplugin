package me.hmPlugin.assistPl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.hmPlugin.assistPl.commands.HelpCommand;
import me.hmPlugin.assistPl.commands.HuntCommand;
import me.hmPlugin.assistPl.commands.NearbyCommand;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final AssistPl plugin;
    private final Map<String, SubCommand> subCommands;

    public CommandManager(AssistPl plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("help", new HelpCommand());
        subCommands.put("nearby", new NearbyCommand(plugin));
        subCommands.put("hunt", new HuntCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cyou dont seem to be playing my man!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§6yerrr ! do /hm help ");
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            player.sendMessage("§cwhat the fuck is that supposed to be, do /hm help for a list of commands");
            return true;
        }

        // Check permissions
        if (subCommand.getPermission() != null && 
            !player.hasPermission(subCommand.getPermission())) {
            player.sendMessage("§cWomp womp, no perms!");
            return true;
        }

        // Execute subcommand
        return subCommand.execute(player, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partialCommand = args[0].toLowerCase();
            
            for (String subCommand : subCommands.keySet()) {
                if (subCommand.startsWith(partialCommand)) {
                    completions.add(subCommand);
                }
            }
            
            return completions;
        }
        
        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            
            if (subCommand != null) {
                return subCommand.getTabCompletions(sender, args);
            }
        }
        
        return new ArrayList<>();
    }
}
