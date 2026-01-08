package com.thenexusreborn.gamemaps;

import com.thenexusreborn.gamemaps.model.GameMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class MapManager<M extends GameMap> {
    
    public static final Function<String, String> normalizeFunction = rawName -> rawName.toLowerCase().replace(" ", "_").replace("'", "");
    
    protected final JavaPlugin plugin;
    protected final List<M> gameMaps = new ArrayList<>();
    
    protected boolean editMode;
    protected M mapBeingEdited;
    protected boolean viewingWorldBorder;
    protected String borderViewOption;
    
    public MapManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public abstract void loadMaps();
    
    public abstract void saveMaps();
    
    public abstract void saveMap(M gameMap);
    
    public abstract M loadMap(String name);
    
    public abstract void deleteMap(M map);
    
    public void deleteMap(String name) {
        M map = getMap(name);
        if (map != null) {
            deleteMap(map);
        }
    }
    
    public M getMap(String mapName) {
        for (M gameMap : this.gameMaps) {
            if (gameMap.getName().equalsIgnoreCase(mapName)) {
                return gameMap;
            }
            
            if (normalizeFunction.apply(gameMap.getName()).equals(normalizeFunction.apply(mapName))) {
                return gameMap;
            }
            
            if (gameMap.getUrl().equalsIgnoreCase(mapName)) {
                return gameMap;
            }
        }
        return null;
    }
    
    public void addMap(M gameMap) {
        this.gameMaps.add(gameMap);
    }
    
    public List<M> getMaps() {
        return this.gameMaps;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public M getMapBeingEdited() {
        return mapBeingEdited;
    }
    
    public boolean isViewingWorldBorder() {
        return viewingWorldBorder;
    }
    
    public String getBorderViewOption() {
        return borderViewOption;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public void setMapBeingEdited(M mapBeingEdited) {
        this.mapBeingEdited = mapBeingEdited;
    }
    
    public void setViewingWorldBorder(boolean viewingWorldBorder) {
        this.viewingWorldBorder = viewingWorldBorder;
    }
    
    public void setBorderViewOption(String borderViewOption) {
        this.borderViewOption = borderViewOption;
    }
}
