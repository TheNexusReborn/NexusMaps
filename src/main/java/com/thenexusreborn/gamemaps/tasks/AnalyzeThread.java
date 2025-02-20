package com.thenexusreborn.gamemaps.tasks;

import com.stardevllc.colors.StarColors;
import com.stardevllc.starcore.utils.Cuboid;
import com.stardevllc.starcore.utils.Position;
import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.NexusPlayer;
import com.thenexusreborn.gamemaps.model.SGMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class AnalyzeThread extends BukkitRunnable {

    private JavaPlugin plugin;
    private SGMap gameMap;
    private Player player;
    private Cuboid cuboid;

    private NexusPlayer nexusPlayer;

    private int totalBlocks, chests, enchantTables, workbenches, furnaces;
    private int totalProcessed;

    private final DecimalFormat format = new DecimalFormat("#,###,###,###");

    private int x, z;
    
    private Map<Material, Integer> materialCounts = new HashMap<>();

    public AnalyzeThread(JavaPlugin plugin, SGMap map, Player player) {
        this.plugin = plugin;
        this.gameMap = map;
        this.player = player;
        Position center = gameMap.getCenter();
        int borderDistance = gameMap.getBorderDistance();
        
        int halfBorderDistance = borderDistance / 2;
        
        Location min = new Location(gameMap.getWorld(), center.getX() - halfBorderDistance, 0, center.getZ() - halfBorderDistance);
        Location max = new Location(gameMap.getWorld(), center.getX() + halfBorderDistance, 256, center.getZ() + halfBorderDistance);

        this.cuboid = new Cuboid(min, max);

        this.x = cuboid.getXMin();
        this.z = cuboid.getZMin();
        nexusPlayer = NexusAPI.getApi().getPlayerManager().getNexusPlayer(this.player.getUniqueId());
    }

    public void run() {
        for (int i = 0; i < 100; i++) {
            for (int y = 0; y < 256; y++) {
                Block block = gameMap.getWorld().getBlockAt(x, y, z);
                nexusPlayer.setActionBar(() -> "X: " + this.x + " Z: " + this.z + " P: " + format.format(totalProcessed));
                Material type = block.getType();
                
                if (this.materialCounts.containsKey(type)) {
                    this.materialCounts.put(type, this.materialCounts.get(type) + 1);
                } else {
                    this.materialCounts.put(type, 1);
                }
                
                if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                    incrementChests();
                } else if (type == Material.ENCHANTMENT_TABLE) {
                    incrementEnchantTables();
                } else if (type == Material.WORKBENCH) {
                    incrementWorkbenches();
                } else if (type == Material.FURNACE || type == Material.BURNING_FURNACE) {
                    incrementFurnaces();
                }

                if (block.getType() != Material.AIR) {
                    incrementTotalBlocks();
                }
                
                totalProcessed++;
            }

            if (x >= cuboid.getXMax() && z >= cuboid.getZMax()) {
                player.sendMessage(StarColors.color("&eAnalysis Complete. Use /sgmap analysis to view results."));
//                this.materialCounts.forEach((material, count) -> System.out.println(material + ": " + count));
                cancel();
                return;
            } else if (x < cuboid.getXMax()) {
                x++;
            } else {
                z++;
                x = cuboid.getXMin();
            }
        }
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
    }
}
