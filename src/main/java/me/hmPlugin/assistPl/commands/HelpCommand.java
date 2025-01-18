package me.hmPlugin.assistPl.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.hmPlugin.assistPl.SubCommand;

public class HelpCommand implements SubCommand {
    @Override
    public boolean execute(Player player, String[] args) {
        player.sendMessage("§6=== AssistPl Help ===");
        player.sendMessage("§f/hm help §7- Display this help message");
        player.sendMessage("§f/hm nearby start/stop ...");
        return true;
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}