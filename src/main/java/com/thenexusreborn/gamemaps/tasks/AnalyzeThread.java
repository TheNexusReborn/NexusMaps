package com.thenexusreborn.gamemaps.tasks;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.stardevllc.starcore.utils.Position;
import com.thenexusreborn.gamemaps.model.SGMap;
import com.thenexusreborn.nexuscore.util.MsgType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnalyzeThread extends BukkitRunnable {

    private JavaPlugin plugin;
    private SGMap gameMap;
    private Player player;

    private int totalBlocks, chests, enchantTables, workbenches, furnaces;
    private Set<Position> echestLocs = new HashSet<>();

    private Map<Material, Integer> materialCounts = new HashMap<>();
    
    private CuboidRegion region;

    public AnalyzeThread(JavaPlugin plugin, SGMap map, Player player) {
        this.plugin = plugin;
        this.gameMap = map;
        this.player = player;
        
        region = map.getArenaRegion();
    }

    public void run() {
        for (BlockVector vector : region) {
            Block block = gameMap.getWorld().getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            
            Material type = block.getType();
            
            if (this.materialCounts.containsKey(type)) {
                this.materialCounts.put(type, this.materialCounts.get(type) + 1);
            } else {
                this.materialCounts.put(type, 1);
            }
            
            if (type == Material.ENDER_CHEST) {
                this.echestLocs.add(Position.fromLocation(block.getLocation()));
            }
            
            if (type != Material.AIR) {
                totalBlocks++;
            }
        }
        
        this.chests = this.materialCounts.getOrDefault(Material.CHEST, 0) + this.materialCounts.getOrDefault(Material.TRAPPED_CHEST, 0);
        this.enchantTables = this.materialCounts.getOrDefault(Material.ENCHANTMENT_TABLE, 0);
        this.workbenches = this.materialCounts.getOrDefault(Material.WORKBENCH, 0);
        this.furnaces = this.materialCounts.getOrDefault(Material.FURNACE, 0) + this.materialCounts.getOrDefault(Material.BURNING_FURNACE, 0);
        
        updateGameMapValues();
        
        MsgType.INFO.send(player, "Map analysis complete for %v.", gameMap.getName());
        MsgType.INFO.send(player, " Use %v to view results", "/sgmap analysis");
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public SGMap getGameMap() {
        return gameMap;
    }

    public void incrementTotalBlocks() {
        this.totalBlocks++;
        updateGameMapValues();
    }

    public void incrementChests() {
        this.chests++;
        updateGameMapValues();
    }

    public void incrementEnchantTables() {
        this.enchantTables++;
        updateGameMapValues();
    }

    public void incrementWorkbenches() {
        this.workbenches++;
        updateGameMapValues();
    }

    public void incrementFurnaces() {
        this.furnaces++;
        updateGameMapValues();
    }

    public void setValues(int totalBlocks, int chests, int enchantTables, int workbenches, int furnaces) {
        this.totalBlocks = totalBlocks;
        this.chests = chests;
        this.enchantTables = enchantTables;
        this.workbenches = workbenches;
        this.furnaces = furnaces;
        updateGameMapValues();
    }

    public void updateGameMapValues() {
        this.gameMap.setTotalBlocks(this.totalBlocks);
        this.gameMap.setChests(this.chests);
        this.gameMap.setEnchantTables(this.enchantTables);
        this.gameMap.setWorkbenches(this.workbenches);
        this.gameMap.setFurnaces(this.furnaces);
        this.gameMap.setEnderChests(this.echestLocs);
    }
}
