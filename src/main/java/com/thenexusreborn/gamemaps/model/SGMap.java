package com.thenexusreborn.gamemaps.model;

import com.stardevllc.starmclib.Cuboid;
import com.stardevllc.starmclib.Position;
import com.stardevllc.starmclib.ServerProperties;
import com.thenexusreborn.api.sql.annotations.column.ColumnCodec;
import com.thenexusreborn.api.sql.annotations.column.ColumnIgnored;
import com.thenexusreborn.api.sql.annotations.column.ColumnType;
import com.thenexusreborn.api.sql.annotations.table.TableHandler;
import com.thenexusreborn.api.sql.annotations.table.TableName;
import com.thenexusreborn.api.sql.objects.codecs.StringSetCodec;
import com.thenexusreborn.gamemaps.FileHelper;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@TableName(value = "sgmaps")
@TableHandler(GameMapObjectHandler.class)
public class SGMap {
    private long id;
    private String url;
    private String name;
    
    private Position center;
    @ColumnIgnored
    private Set<MapSpawn> spawns = new LinkedHashSet<>();
    private int borderDistance, deathmatchBorderDistance;
    @ColumnType("varchar(1000)")
    @ColumnCodec(StringSetCodec.class)
    private Set<String> creators = new HashSet<>();
    private boolean active;
    @ColumnIgnored
    private Map<UUID, MapRating> ratings = new HashMap<>();
    private Position swagShack;
    
    //Analytics - Done via command /sg map analyze
    private int chests, enchantTables, workbenches, furnaces, totalBlocks;
    
    @ColumnIgnored
    private UUID uniqueId;
    @ColumnIgnored
    private World world;
    
    @ColumnIgnored
    private Path downloadedZip, unzippedFolder, worldFolder;
    @ColumnIgnored
    private boolean editing;
    @ColumnIgnored
    private int votes;
    @ColumnIgnored
    private Cuboid deathmatchArea;
    
    private SGMap() {
    }
    
    public boolean isSetup() {
        if (this.center == null) {
            System.out.println("Center for map " + getName() + " is null");
            return false;
        }
        
        if (this.spawns.size() != 24) {
            System.out.println("Spawn size for map " + getName() + " is " + this.spawns.size());
            return false;
        }
        
        if (this.borderDistance == 0) {
            System.out.println("Border for map " + getName() + " is 0");
            return false;
        }
        
        if (this.deathmatchBorderDistance == 0) {
            System.out.println("Deathmatch Border Distance for map " + getName() + " is 0");
            return false;
        }
        
        if (creators.isEmpty()) {
            System.out.println("Creators for map " + getName() + " is empty");
            return false;
        }

        return true;
    }
    
    public SGMap(String fileName, String name) {
        this.url = fileName;
        this.name = name;
    }
    
    public Location getCenterLocation() {
        if (this.world != null) {
            return getCenter().toLocation(this.world);
        }
        
        return null;
    }
    
    public void recalculateSpawns() {
        if (spawns.isEmpty()) {
            return;
        }
        
        List<MapSpawn> spawns = new LinkedList<>(this.spawns);
        Collections.sort(spawns);
        
        for (int i = 0; i < spawns.size(); i++) {
            MapSpawn spawn = spawns.get(i);
            if (spawn != null) {
                spawn.setIndex(i);
            }
        }
    }
    
    public int getNextIndex() {
        if (this.spawns.isEmpty()) {
            return 0;
        }
        
        int lastIndex = 0;
        for (MapSpawn spawn : this.spawns) {
            if (spawn.getIndex() > lastIndex) {
                lastIndex = spawn.getIndex();
            }
        }
        
        return lastIndex + 1;
    }
    
    public void removeFromServer(JavaPlugin plugin) {
        try {
            uniqueId = null;
            if (downloadedZip != null) {
                Files.deleteIfExists(downloadedZip);
                downloadedZip = null;
            }
            
            if (this.world != null) {
                for (Player player : world.getPlayers()) {
                    player.teleport(Bukkit.getWorld(ServerProperties.getLevelName()).getSpawnLocation());
                }
                
                boolean success = Bukkit.unloadWorld(world, false);
                if (!success) {
                    plugin.getLogger().severe("Failed to unload world for map " + this.name);
                }
                world = null;
            }
            
            if (Files.exists(worldFolder)) {
                FileHelper.deleteDirectory(worldFolder);
                worldFolder = null;
            }
            
            if (Files.exists(unzippedFolder)) {
                FileHelper.deleteDirectory(unzippedFolder);
                unzippedFolder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean download(JavaPlugin plugin) {
        if (downloadedZip == null || !Files.exists(downloadedZip)) {
            Path downloadFolder = FileHelper.subPath(plugin.getDataFolder().toPath(), "mapdownloads");
            FileHelper.createDirectoryIfNotExists(downloadFolder);
            downloadedZip = FileHelper.downloadFile(url, downloadFolder, getName().toLowerCase().replace("'", "").replace(" ", "_") + ".zip", true);
        }
        return downloadedZip != null && Files.exists(downloadedZip);
    }
    
    public void addCreator(String creator) {
        this.creators.add(creator);
    }
    
    public void addCreators(String... creators) {
        this.creators.addAll(Arrays.asList(creators));
    }
    
    public void removeCreator(String creator) {
        this.creators.remove(creator);
    }
    
    public void setSpawns(Collection<MapSpawn> spawns) {
        this.spawns.clear();
        this.spawns.addAll(spawns);
        this.spawns.forEach(spawn -> spawn.setMapId(this.id));
    }
    
    public int addSpawn(MapSpawn spawn) {
        if (spawn.getIndex() == -1) {
            int index = getNextIndex();
            spawn.setIndex(index);
        }
        this.spawns.add(spawn);
        spawn.setMapId(this.getId());
        return spawn.getIndex();
    }
    
    public void setSpawn(int index, MapSpawn spawn) {
        spawn.setIndex(index);
        spawn.setMapId(this.getId());
        this.spawns.removeIf(s -> s.getIndex() == index);
        this.spawns.add(spawn);
    }
    
    public void removeSpawn(int index) {
        this.spawns.removeIf(spawn -> spawn.getIndex() == index);
        recalculateSpawns();
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getName() {
        if (this.name.contains("''")) {
            this.name = name.replace("''", "'");
        }
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Position getCenter() {
        return center;
    }
    
    public void setCenter(Position center) {
        this.center = center;
    }
    
    public List<MapSpawn> getSpawns() {
        return new LinkedList<>(spawns);
    }
    
    public int getBorderDistance() {
        return borderDistance;
    }
    
    public void setBorderDistance(int borderDistance) {
        this.borderDistance = borderDistance;
    }
    
    public int getDeathmatchBorderDistance() {
        return this.deathmatchBorderDistance;
    }
    
    public void setDeathmatchBorderDistance(int deathmatchBorderDistance) {
        this.deathmatchBorderDistance = deathmatchBorderDistance;
    }
    
    public Set<String> getCreators() {
        return creators;
    }
    
    public UUID getUniqueId() {
        return uniqueId;
    }
    
    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }
    
    public World getWorld() {
        return world;
    }
    
    public void setWorld(World world) {
        this.world = world;
    }
    
    public Path getDownloadedZip() {
        return downloadedZip;
    }
    
    public void setDownloadedZip(Path downloadedZip) {
        this.downloadedZip = downloadedZip;
    }
    
    public boolean unzip(JavaPlugin plugin) {
        Path unzippedMapsFolder = FileHelper.subPath(plugin.getDataFolder().toPath(), "unzippedmaps");
        unzippedFolder = FileHelper.subPath(unzippedMapsFolder, this.name);
        FileHelper.createDirectoryIfNotExists(unzippedFolder);
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(Files.newInputStream(this.downloadedZip.toFile().toPath()));
            ZipEntry zipEntry = zis.getNextEntry();
            
            while (zipEntry != null) {
                Path newFile = newFile(unzippedFolder.toFile(), zipEntry);
                if (zipEntry.isDirectory()) {
                    FileHelper.createDirectoryIfNotExists(newFile);
                } else {
                    // fix for Windows-created archives
                    Path parent = newFile.getParent();
                    if (!Files.isDirectory(parent)) {
                        FileHelper.createDirectoryIfNotExists(parent);
                        if (Files.notExists(parent)) {
                            throw new IOException("Failed to create directory " + parent);
                        }
                    }
                    
                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile.toFile());
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            unzippedFolder = null;
            return false;
        }
        return true;
    }
    
    public Path getUnzippedFolder() {
        return unzippedFolder;
    }
    
    private Path newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        Path path = FileHelper.subPath(destinationDir.toPath(), zipEntry.getName());
        
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = path.toFile().getCanonicalPath();
        
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        
        return path;
    }
    
    public boolean copyFolder(JavaPlugin plugin, boolean randomizeName) {
        try {
            if (this.unzippedFolder != null) {
                String worldName;
                if (randomizeName) {
                    uniqueId = UUID.randomUUID();
                    worldName = uniqueId.toString();
                } else {
                    worldName = this.name;
                }
                this.worldFolder = FileHelper.subPath(Bukkit.getServer().getWorldContainer().toPath(), worldName);
                FileHelper.createDirectoryIfNotExists(worldFolder);
                FileHelper.copyFolder(this.unzippedFolder, worldFolder);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    public boolean load(JavaPlugin plugin) {
        try {
            if (this.worldFolder != null) {
                this.world = Bukkit.createWorld(new WorldCreator(this.name));
                return this.world != null;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    
    public void setEditing(boolean editing) {
        this.editing = editing;
    }
    
    public boolean isEditing() {
        return editing;
    }
    
    public int getVotes() {
        return votes;
    }
    
    public void setVotes(int votes) {
        this.votes = votes;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        if (active) {
            this.active = isSetup();
        } else {
            this.active = false;
        }
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
        this.spawns.forEach(spawn -> spawn.setMapId(id));
    }
    
    public void setDeathmatchArea(Cuboid deathmatchArea) {
        this.deathmatchArea = deathmatchArea;
    }
    
    public Cuboid getDeathmatchArea() {
        return deathmatchArea;
    }

    @Override
    public String toString() {
        return "GameMap{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", center=" + center +
                ", spawns=" + spawns +
                ", borderDistance=" + borderDistance +
                ", deathmatchBorderDistance=" + deathmatchBorderDistance +
                ", creators=" + creators +
                ", active=" + active +
                ", ratings=" + ratings +
                ", swagShack=" + swagShack +
                ", uniqueId=" + uniqueId +
                ", world=" + world +
                ", downloadedZip=" + downloadedZip +
                ", unzippedFolder=" + unzippedFolder +
                ", worldFolder=" + worldFolder +
                ", editing=" + editing +
                ", votes=" + votes +
                ", deathmatchArea=" + deathmatchArea +
                '}';
    }

    public void setRatings(List<MapRating> ratings) {
        ratings.forEach(rating -> this.ratings.put(rating.getPlayer(), rating));
    }
    
    public Map<UUID, MapRating> getRatings() {
        return ratings;
    }
    
    public Position getSwagShack() {
        return swagShack;
    }
    
    public void setSwagShack(Position swagShack) {
        this.swagShack = swagShack;
    }

    public void disableWorldBorder() {
        World world = getWorld();
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.reset();
    }

    public int getChests() {
        return chests;
    }

    public void setChests(int chests) {
        this.chests = chests;
    }

    public int getEnchantTables() {
        return enchantTables;
    }

    public void setEnchantTables(int enchantTables) {
        this.enchantTables = enchantTables;
    }

    public int getWorkbenches() {
        return workbenches;
    }

    public void setWorkbenches(int workbenches) {
        this.workbenches = workbenches;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }

    public int getFurnaces() {
        return furnaces;
    }

    public void setFurnaces(int furnaces) {
        this.furnaces = furnaces;
    }

    public void applyWorldBoarder(String viewOption, int seconds) {
        World world = getWorld();
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(this.getCenter().toLocation(world));
        if (viewOption.equalsIgnoreCase("deathmatch")) {
            worldBorder.setSize(this.deathmatchBorderDistance);
            if (seconds != 0) {
                worldBorder.setSize(10, seconds);
            }
        } else if (viewOption.equalsIgnoreCase("game")) {
            worldBorder.setSize(this.borderDistance);
        }
    }

    public void applyWorldBoarder(String viewOption) {
        applyWorldBoarder(viewOption, 0);
    }
    
    public static SGMap loadFromYaml(FileConfiguration config) {
        long id = config.getLong("id");
        String url = config.getString("url");
        String name = config.getString("name");
        SGMap map = new SGMap(url, name);
        map.setId(id);
        map.setBorderDistance(config.getInt("borderDistance"));
        map.setDeathmatchBorderDistance(config.getInt("deathmatchBorderDistance"));
        if (config.contains("center")) {
            Position center = new Position(config.getInt("center.x"), config.getInt("center.y"), config.getInt("center.z"));
            map.setCenter(center);
        }
        
        if (config.contains("swagshack")) {
            Position swagShack = new Position(config.getInt("swagshack.x"), config.getInt("swagshack.y"), config.getInt("swagshack.z"));
            map.setSwagShack(swagShack);
        }
        
        if (config.contains("creators")) {
            map.setCreators(config.getStringList("creators"));
        }
        
        if (config.contains("ratings")) {
            ConfigurationSection ratingsSection = config.getConfigurationSection("ratings");
            if (ratingsSection != null) {
                for (String key : ratingsSection.getKeys(false)) {
                    UUID player = UUID.fromString(ratingsSection.getString(key + ".player"));
                    int value = ratingsSection.getInt(key + ".value");
                    long timestamp = ratingsSection.getLong(key + ".timestamp");
                    map.getRatings().put(player, new MapRating(map.getName(), player, value, timestamp));
                }
            }
        }
        
        if (config.contains("spawns")) {
            ConfigurationSection spawnsSection = config.getConfigurationSection("spawns");
            if (spawnsSection != null) {
                for (String key : spawnsSection.getKeys(false)) {
                    int index = Integer.parseInt(key);
                    int x = spawnsSection.getInt(key + ".x");
                    int y = spawnsSection.getInt(key + ".y");
                    int z = spawnsSection.getInt(key + ".z");
                    map.addSpawn(new MapSpawn(map.getId(), index, x, y, z));
                }
            }
        }

        map.setActive(config.getBoolean("active"));
        
        return map;
    }

    public void setCreators(Collection<String> creators) {
        this.creators.clear();
        this.creators.addAll(creators);
    }

    public void saveToYaml(FileConfiguration config) {
        config.set("id", getId());
        config.set("url", getUrl());
        config.set("name", getName());
        config.set("active", isActive());
        config.set("borderDistance", getBorderDistance());
        config.set("deathmatchBorderDistance", getDeathmatchBorderDistance());
        if (getCenter() != null) {
            config.set("center.x", getCenter().getX());
            config.set("center.y", getCenter().getY());
            config.set("center.z", getCenter().getZ());
        }

        if (getSwagShack() != null) {
            config.set("swagshack.x", getSwagShack().getX());
            config.set("swagshack.y", getSwagShack().getY());
            config.set("swagshack.z", getSwagShack().getZ());
        }

        if (!getCreators().isEmpty()) {
            config.set("creators", new ArrayList<>(getCreators()));
        }

        if (!getRatings().isEmpty()) {
            int rid = 0;
            for (MapRating rating : getRatings().values()) {
                String path = "ratings." + rid + ".";
                config.set(path + "player", rating.getPlayer().toString());
                config.set(path + "value", rating.getRating());
                config.set(path + "timestamp", rating.getTimestamp());
                rid++;
            }
        }

        if (!getSpawns().isEmpty()) {
            for (MapSpawn spawn : getSpawns()) {
                String path = "spawns." + spawn.getIndex() + ".";
                config.set(path + "x", spawn.getX());
                config.set(path + "y", spawn.getY());
                config.set(path + "z", spawn.getZ());
            }
        }
    }
}
