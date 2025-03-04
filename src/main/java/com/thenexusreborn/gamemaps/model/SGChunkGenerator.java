package com.thenexusreborn.gamemaps.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class SGChunkGenerator extends ChunkGenerator {
    
    private SGMap sgMap;

    public SGChunkGenerator(SGMap sgMap) {
        this.sgMap = sgMap;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return this.sgMap.getSpawnCenter().toLocation(world);
    }
}
