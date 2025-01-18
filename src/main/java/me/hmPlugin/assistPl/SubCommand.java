package me.hmPlugin.assistPl;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface SubCommand {
    boolean execute(Player player, String[] args);
    String getPermission();
    List<String> getTabCompletions(CommandSender sender, String[] args);
}