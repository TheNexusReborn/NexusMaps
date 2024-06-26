package com.thenexusreborn.gamemaps.tasks;

import com.stardevllc.starcore.color.ColorHandler;
import com.stardevllc.starcore.utils.Cuboid;
import com.stardevllc.starcore.utils.Position;
import com.thenexusreborn.gamemaps.model.SGMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalyzeThread implements Runnable {

    private JavaPlugin plugin;
    private SGMap gameMap;
    private Player player;
    private Cuboid cuboid;

    private AtomicInteger totalBlocks = new AtomicInteger(), chests = new AtomicInteger(), enchantTables = new AtomicInteger(), workbenches = new AtomicInteger(), furnaces = new AtomicInteger();

    public AnalyzeThread(JavaPlugin plugin, SGMap map, Player player) {
        this.plugin = plugin;
        this.gameMap = map;
        this.player = player;
        Position center = gameMap.getCenter();
        int borderDistance = gameMap.getBorderDistance();
        Location min = new Location(gameMap.getWorld(), center.getX() - borderDistance, 0, center.getZ() - borderDistance);
        Location max = new Location(gameMap.getWorld(), center.getX() + borderDistance, 256, center.getZ() + borderDistance);
        this.cuboid = new Cuboid(min, max);
    }

    public void run() {
        List<Position> blocks = new ArrayList<>();
        
        for (int x = cuboid.getXMin(); x <= cuboid.getXMax(); x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = cuboid.getZMin(); z <= cuboid.getZMax(); z++) {
                    blocks.add(new Position(x, y, z));
                    if (blocks.size() == 50) {
                        Bukkit.getServer().getScheduler().runTaskLater(plugin, new BlockAnalyzeThread(this, new ArrayList<>(blocks)), 1L);
                        blocks.clear();
                    }
                }
            }
        }

        Bukkit.getServer().getScheduler().runTask(plugin, new BlockAnalyzeThread(this, new ArrayList<>(blocks)));
        player.sendMessage(ColorHandler.getInstance().color("Analysis Complete. Use /sg map analysis to view results."));
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public SGMap getGameMap() {
        return gameMap;
    }

    public Cuboid getCuboid() {
        return cuboid;
    }

    public void incrementTotalBlocks() {
        this.totalBlocks.getAndIncrement();
        updateGameMapValues();
    }

    public void incrementChests() {
        this.chests.getAndIncrement();
        updateGameMapValues();
    }

    public void incrementEnchantTables() {
        this.enchantTables.getAndIncrement();
        updateGameMapValues();
    }

    public void incrementWorkbenches() {
        this.workbenches.getAndIncrement();
        updateGameMapValues();
    }

    public void incrementFurnaces() {
        this.furnaces.getAndIncrement();
        updateGameMapValues();
    }

    public void setValues(int totalBlocks, int chests, int enchantTables, int workbenches, int furnaces) {
        this.totalBlocks.getAndAdd(totalBlocks);
        this.chests.getAndAdd(chests);
        this.enchantTables.getAndAdd(enchantTables);
        this.workbenches.getAndAdd(workbenches);
        this.furnaces.getAndAdd(furnaces);
    }
    
    public void updateGameMapValues() {
        this.gameMap.setTotalBlocks(this.totalBlocks.get());
        this.gameMap.setChests(this.chests.get());
        this.gameMap.setEnchantTables(this.enchantTables.get());
        this.gameMap.setWorkbenches(this.workbenches.get());
        this.gameMap.setFurnaces(this.furnaces.get());
    }
}
