package com.thenexusreborn.gamemaps.model;

import com.stardevllc.starmclib.Position;
import com.thenexusreborn.api.sql.annotations.table.TableName;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

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
        return "(" + id + "," + index + "," + mapId + ") (" + x + "," + y + "," + z + ')';
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

    public Location toGameLocation(World world, Location mapCenter) {
        return new Location(world, x + 0.5, y + 2, z + 0.5, getAngle(new Vector(x, y, z), mapCenter.toVector()), 0);
    }

    private static float getAngle(Vector point1, Vector point2) {
        double dx = point2.getX() - point1.getX();
        double dz = point2.getZ() - point1.getZ();
        float angle = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        if (angle < 0) {
            angle += 360.0F;
        }
        return angle;
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
