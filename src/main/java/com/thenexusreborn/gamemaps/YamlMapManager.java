package com.thenexusreborn.gamemaps;

import com.thenexusreborn.gamemaps.model.SGMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class YamlMapManager extends MapManager {
    
    private File configsFolder;
    private long lastId;
    
    public YamlMapManager(JavaPlugin plugin) {
        super(plugin);
        configsFolder = new File(plugin.getDataFolder(), "mapconfigs");
        if (!configsFolder.exists()) {
            configsFolder.mkdirs();
        }
    }

    @Override
    public void loadMaps() {
        
    }

    @Override
    public void saveMaps() {

    }

    @Override
    public void saveMap(SGMap gameMap) {
        File configFile = new File(configsFolder, normalizeFunction.apply(gameMap.getName()) + File.separator + ".yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        gameMap.saveToYaml(config);

        try {
            config.save(configFile);
        } catch (IOException e) {
        }
    }

    @Override
    public SGMap loadMap(String name) {
        String mapName = normalizeFunction.apply(name);
        File configFile = new File(configsFolder, mapName + File.separator + ".yml");
        if (!configFile.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        return SGMap.loadFromYaml(config);
    }
}
