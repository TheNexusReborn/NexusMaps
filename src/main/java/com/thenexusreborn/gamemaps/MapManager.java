package com.thenexusreborn.gamemaps;

import com.thenexusreborn.gamemaps.model.SGMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class MapManager {
    
    public static final Function<String, String> normalizeFunction = rawName -> rawName.toLowerCase().replace(" ", "_").replace("'", "");
    
    protected final JavaPlugin plugin;
    protected final List<SGMap> gameMaps = new ArrayList<>();

    protected boolean editMode;
    protected SGMap mapBeingEdited;
    protected boolean viewingWorldBorder;
    protected String borderViewOption;

    public MapManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract void loadMaps();
    public abstract void saveMaps();
    public abstract void saveMap(SGMap gameMap);
    public abstract SGMap loadMap(String name);

    public SGMap getMap(String mapName) {
        for (SGMap gameMap : this.gameMaps) {
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

    public void addMap(SGMap gameMap) {
        this.gameMaps.add(gameMap);
    }

    public List<SGMap> getMaps() {
        return this.gameMaps;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public SGMap getMapBeingEdited() {
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

    public void setMapBeingEdited(SGMap mapBeingEdited) {
        this.mapBeingEdited = mapBeingEdited;
    }

    public void setViewingWorldBorder(boolean viewingWorldBorder) {
        this.viewingWorldBorder = viewingWorldBorder;
    }

    public void setBorderViewOption(String borderViewOption) {
        this.borderViewOption = borderViewOption;
    }
}
