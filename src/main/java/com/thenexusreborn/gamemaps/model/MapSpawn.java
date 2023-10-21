package com.thenexusreborn.gamemaps.model;

import me.firestar311.starlib.spigot.utils.Position;
import me.firestar311.starsql.api.annotations.table.TableName;
import org.bukkit.Location;

import java.util.Objects;

@TableName("sgmapspawns")
public class MapSpawn extends Position implements Comparable<MapSpawn> {
    private long id; 
    private long mapId;
    private int index = -1;
    
    public static MapSpawn fromLocation(int mapId, int index, Location location) {
        return new MapSpawn(mapId, index, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    
    private MapSpawn() {}
    
    public MapSpawn(long mapId, int index, int x, int y, int z) {
        super(x, y, z);
        this.mapId = mapId;
        this.index = index;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ')';
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getMapId() {
        return mapId;
    }
    
    public void setMapId(long mapId) {
        this.mapId = mapId;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MapSpawn mapSpawn = (MapSpawn) o;
        return mapId == mapSpawn.mapId && index == mapSpawn.index && x == mapSpawn.x && y == mapSpawn.y && z == mapSpawn.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(mapId, index, x, y, z);
    }
    
    @Override
    public int compareTo(MapSpawn o) {
        if (o.mapId != this.mapId) {
            return -1;
        }
        return Integer.compare(this.index, o.index);
    }
}
