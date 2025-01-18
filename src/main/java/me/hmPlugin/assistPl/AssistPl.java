package me.hmPlugin.assistPl;

import org.bukkit.plugin.java.JavaPlugin;

public class AssistPl extends JavaPlugin {
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        commandManager = new CommandManager(this);
        
        //getCommand("testHM").setExecutor(subComManager);
        getCommand("hm").setExecutor(commandManager);
        getCommand("hm").setTabCompleter(commandManager);
        
        getLogger().info("AssistPl has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AssistPl has been disabled!");
    }
}