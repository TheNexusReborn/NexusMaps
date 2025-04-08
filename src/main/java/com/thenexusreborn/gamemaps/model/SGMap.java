package com.thenexusreborn.gamemaps.model;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.stardevllc.helper.FileHelper;
import com.stardevllc.starcore.utils.Position;
import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.NexusPlayer;
import com.thenexusreborn.api.sql.annotations.column.ColumnCodec;
import com.thenexusreborn.api.sql.annotations.column.ColumnIgnored;
import com.thenexusreborn.api.sql.annotations.column.ColumnType;
import com.thenexusreborn.api.sql.annotations.table.TableHandler;
import com.thenexusreborn.api.sql.annotations.table.TableName;
import com.thenexusreborn.api.sql.objects.codecs.StringSetCodec;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
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
    private String prefix;

    @ColumnIgnored
    private Set<MapSpawn> spawns = new LinkedHashSet<>();
    @ColumnType("varchar(1000)")
    @ColumnCodec(StringSetCodec.class)
    private Set<String> creators = new HashSet<>();
    private boolean active;
    @ColumnIgnored
    private Map<UUID, MapRating> ratings = new HashMap<>();
    private Position swagShack;
    
    private Set<Position> enderChestLocations = new HashSet<>();

    //This is the center of the spawns for the arena, where spectators will spawn in
    //And where players will look at when teleported to a game spawn
    private Position spawnCenter;

    private Position arenaMinimum;
    private Position arenaMaximum;
    private Position arenaCenter;

    //This is the length of the sides of the world border for the arena
    private int arenaBorderLength;

    private Position deathmatchMinimum;
    private Position deathmatchMaximum;
    private Position deathmatchCenter;

    //This is the length of the sides of the world border
    private int deathmatchBorderLength;

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
    private CuboidRegion deathmatchRegion, arenaRegion;

    private SGMap() {
    }

    public boolean isValid() {
        if (this.url == null || this.url.isEmpty()) {
            return false;
        }
        
        if (this.name == null || this.name.isEmpty()) {
            return false;
        }
        
        if (this.spawns.size() < 2) {
            return false;
        }
        
        if (this.creators.isEmpty()) {
            return false;
        }
        
        if (this.spawnCenter == null) {
            return false;
        }
        
        if (this.arenaMinimum == null || this.arenaMaximum == null || this.arenaCenter == null || this.arenaBorderLength == 0) {
            return false;
        }
        
        return !(this.deathmatchMinimum == null || this.deathmatchMaximum == null || this.deathmatchCenter == null || this.deathmatchBorderLength == 0);
    }

    public SGMap(String fileName, String name) {
        this.url = fileName;
        this.name = name;
    }

    public Position getArenaCenter() {
        return arenaCenter;
    }

    public void setArenaCenter(Position arenaCenter) {
        this.arenaCenter = arenaCenter;
    }

    public Position getDeathmatchCenter() {
        return deathmatchCenter;
    }

    public void setDeathmatchCenter(Position deathmatchCenter) {
        this.deathmatchCenter = deathmatchCenter;
    }

    public int getArenaBorderLength() {
        return arenaBorderLength;
    }

    public void setArenaBorderLength(int arenaBorderLength) {
        this.arenaBorderLength = arenaBorderLength;
    }

    public int getDeathmatchBorderLength() {
        return deathmatchBorderLength;
    }

    public void setDeathmatchBorderLength(int deathmatchBorderLength) {
        this.deathmatchBorderLength = deathmatchBorderLength;
    }

    public Position getSpawnCenter() {
        return spawnCenter;
    }

    public void setSpawnCenter(Position spawnCenter) {
        this.spawnCenter = spawnCenter;
    }

    public Position getArenaMinimum() {
        return arenaMinimum;
    }

    public void setArenaMinimum(Position arenaMinimum) {
        this.arenaMinimum = arenaMinimum;
    }

    public Position getArenaMaximum() {
        return arenaMaximum;
    }

    public void setArenaMaximum(Position arenaMaximum) {
        this.arenaMaximum = arenaMaximum;
    }

    public Position getDeathmatchMinimum() {
        return deathmatchMinimum;
    }

    public void setDeathmatchMinimum(Position deathmatchMinimum) {
        this.deathmatchMinimum = deathmatchMinimum;
    }

    public Position getDeathmatchMaximum() {
        return deathmatchMaximum;
    }

    public void setDeathmatchMaximum(Position deathmathMaximum) {
        this.deathmatchMaximum = deathmathMaximum;
    }

    public Location getCenterLocation() {
        if (this.world != null) {
            if (this.spawnCenter != null) {
                return getSpawnCenter().toLocation(this.world);
            }
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

    public Set<Position> getEnderChestLocations() {
        return enderChestLocations;
    }
    
    public void addEnderChestLocation(Position position) {
        this.enderChestLocations.add(position);
    }
    
    public void addEnderChestLocation(Location location) {
        addEnderChestLocation(Position.fromLocation(location));
    }

    public void removeFromServer(JavaPlugin plugin) {
        try {
            uniqueId = null;
//            if (downloadedZip != null) {
//                Files.deleteIfExists(downloadedZip);
//                downloadedZip = null;
//            }

            if (this.world != null) {
                for (Player player : world.getPlayers()) {
                    NexusPlayer nexusPlayer = NexusAPI.getApi().getPlayerManager().getNexusPlayer(player.getUniqueId());
                    if (nexusPlayer != null) {
                        nexusPlayer.getServer().teleportToSpawn(player.getUniqueId());
                    }
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

            String fileName = getName().toLowerCase().replace("'", "").replace(" ", "_") + ".zip";

            Path existing = FileSystems.getDefault().getPath(downloadFolder.toString(), fileName);
            if (Files.exists(existing)) {
                System.out.println("Found existing zip for " + name);
                this.downloadedZip = existing;
                return true;
            }

            downloadedZip = FileHelper.downloadFile(url, downloadFolder, fileName, true);
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

    public List<MapSpawn> getSpawns() {
        return new LinkedList<>(spawns);
    }

    public void clearSpawns() {
        this.spawns.clear();
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

    public boolean copyFolder(JavaPlugin plugin, String prefix, boolean randomizeName) {
        try {
            if (this.unzippedFolder != null) {
                String worldName;
                if (randomizeName) {
                    uniqueId = UUID.randomUUID();
                    worldName = uniqueId.toString();
                } else {
                    worldName = this.name;
                }
                this.prefix = (prefix != null) ? prefix : "";
                this.worldFolder = FileHelper.subPath(Bukkit.getServer().getWorldContainer().toPath(), this.prefix + worldName);
                FileHelper.createDirectoryIfNotExists(worldFolder);
                FileHelper.copyFolder(this.unzippedFolder, worldFolder);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean load(JavaPlugin plugin) {
        try {
            if (this.worldFolder != null) {
                this.world = Bukkit.createWorld(new WorldCreator(this.prefix + this.name));
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
            this.active = isValid();
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

    public CuboidRegion getArenaRegion() {
        if (this.arenaRegion != null) {
            return this.arenaRegion;
        }

        if (world == null) {
            return null;
        }

        Vector min = new Vector(this.arenaMinimum.getX(), this.arenaMinimum.getY(), this.arenaMinimum.getZ());
        Vector max = new Vector(this.arenaMaximum.getX(), this.arenaMaximum.getY(), this.arenaMaximum.getZ());

        com.sk89q.worldedit.world.World bukkitWorld = BukkitUtil.getLocalWorld(this.world);

        this.arenaRegion = new CuboidRegion(bukkitWorld, min, max);
        return arenaRegion;
    }

    public CuboidRegion getDeathmatchRegion() {
        if (this.deathmatchRegion != null) {
            return this.deathmatchRegion;
        }

        if (world == null) {
            return null;
        }

        Vector min = new Vector(this.deathmatchMinimum.getX(), this.deathmatchMinimum.getY(), this.deathmatchMinimum.getZ());
        Vector max = new Vector(this.deathmatchMaximum.getX(), this.deathmatchMaximum.getY(), this.deathmatchMaximum.getZ());

        com.sk89q.worldedit.world.World bukkitWorld = new BukkitWorld(this.world);

        this.deathmatchRegion = new CuboidRegion(bukkitWorld, min, max);
        return deathmatchRegion;
    }

    @Override
    public String toString() {
        return "GameMap{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", spawns=" + spawns +
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
                ", deathmatchArea=" + deathmatchRegion +
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
        if (viewOption.equalsIgnoreCase("deathmatch")) {
            worldBorder.setCenter(this.deathmatchCenter.toLocation(world));
            worldBorder.setSize(this.deathmatchBorderLength);
            if (seconds != 0) {
                worldBorder.setSize(10, seconds);
            }
        } else if (viewOption.equalsIgnoreCase("game")) {
            worldBorder.setCenter(this.arenaCenter.toLocation(world));
            worldBorder.setSize(this.arenaBorderLength);
        }
    }

    public void applyWorldBoarder(String viewOption) {
        applyWorldBoarder(viewOption, 0);
    }

    public static SGMap loadFromYaml(FileConfiguration config) {
        SGMap sgMap = new SGMap();
        sgMap.setId(config.getLong("id"));
        sgMap.setUrl(config.getString("url"));
        sgMap.setName(config.getString("name"));
        sgMap.setPrefix(config.getString("prefix"));

        ConfigurationSection spawnsSection = config.getConfigurationSection("spawns");
        if (spawnsSection != null) {
            for (String key : spawnsSection.getKeys(false)) {
                MapSpawn mapSpawn = (MapSpawn) spawnsSection.get(key);
                sgMap.setSpawn(mapSpawn.getIndex(), mapSpawn);
            }
        }
        
        sgMap.recalculateSpawns();
        
        sgMap.setCreators(config.getStringList("creators"));

        ConfigurationSection ratingsSection = config.getConfigurationSection("ratings");
        if (ratingsSection != null) {
            for (String key : ratingsSection.getKeys(false)) {
                sgMap.addRating((MapRating) ratingsSection.get(key));
            }
        }
        
        ConfigurationSection echestsSection = config.getConfigurationSection("enderchests");
        if (echestsSection != null) {
            for (String key : echestsSection.getKeys(false)) {
                sgMap.addEnderChestLocation((Position) echestsSection.get(key));
            }
        }
        
        sgMap.setSwagShack((Position) config.get("swagshack"));
        sgMap.setSpawnCenter((Position) config.get("spawncenter"));
        
        sgMap.setArenaMinimum((Position) config.get("arena.min"));
        sgMap.setArenaMaximum((Position) config.get("arena.max"));
        sgMap.setArenaCenter((Position) config.get("arena.center"));
        sgMap.setArenaBorderLength(config.getInt("arena.borderlength"));
        
        sgMap.setDeathmatchMinimum((Position) config.get("deathmatch.min"));
        sgMap.setDeathmatchMaximum((Position) config.get("deathmatch.max"));
        sgMap.setDeathmatchCenter((Position) config.get("deathmatch.center"));
        sgMap.setDeathmatchBorderLength(config.getInt("deathmatch.borderlength"));
        
        sgMap.setChests(config.getInt("stats.chests"));
        sgMap.setEnchantTables(config.getInt("stats.enchanttables"));
        sgMap.setWorkbenches(config.getInt("stats.workbenches"));
        sgMap.setFurnaces(config.getInt("stats.furnaces"));
        sgMap.setTotalBlocks(config.getInt("stats.totalblocks"));
        sgMap.setActive(config.getBoolean("active"));

        return sgMap;
    }
    
    public void addRating(MapRating rating) {
        this.ratings.put(rating.getPlayer(), rating);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setCreators(Collection<String> creators) {
        this.creators.clear();
        this.creators.addAll(creators);
    }

    public void saveToYaml(FileConfiguration config) {
        config.set("id", this.id);
        config.set("url", this.url);
        config.set("name", this.name);
        config.set("prefix", this.prefix);
        config.set("active", this.active);
        
        recalculateSpawns();
        
        for (MapSpawn spawn : this.spawns) {
            config.set("spawns." + spawn.getIndex(), spawn);
        }
        
        config.set("creators", new ArrayList<>(this.creators));

        for (MapRating rating : this.ratings.values()) {
            config.set("ratings." + rating.getPlayer().toString(), rating);
        }

        int echestIndex = 0;
        for (Position echestLoc : this.enderChestLocations) {
            config.set("enderchests." + echestIndex, echestLoc);
            echestIndex++;
        }
        
        config.set("swagshack", this.swagShack);
        config.set("spawncenter", this.spawnCenter);
        
        config.set("arena.min", this.arenaMinimum);
        config.set("arena.max", this.arenaMaximum);
        config.set("arena.center", this.arenaCenter);
        config.set("arena.borderlength", this.arenaBorderLength);
        
        config.set("deathmatch.min", this.deathmatchMinimum);
        config.set("deathmatch.max", this.deathmatchMaximum);
        config.set("deathmatch.center", this.deathmatchCenter);
        config.set("deathmatch.borderlength", this.deathmatchBorderLength);
        
        config.set("stats.chests", this.chests);
        config.set("stats.enchanttables", this.enchantTables);
        config.set("stats.workbenches", this.workbenches);
        config.set("stats.furnaces", this.furnaces);
        config.set("stats.totalblocks", this.totalBlocks);
    }
    
    public void copyFrom(SGMap other) {
        this.url = other.url;
        this.name = other.name;
        this.prefix = other.prefix;

        this.spawns.clear();
        
        for (MapSpawn spawn : other.spawns) {
            addSpawn(spawn.clone());
        }

        for (Position enderChestLocation : other.enderChestLocations) {
            this.addEnderChestLocation(enderChestLocation.clone());
        }
        
        this.creators.clear();
        this.creators.addAll(other.creators);
        
        this.active = other.active;
        this.swagShack = other.swagShack == null ? null : other.swagShack.clone();
        this.spawnCenter = other.spawnCenter == null ? null : other.spawnCenter.clone();

        this.arenaMinimum = other.arenaMinimum == null ? null : other.arenaMinimum.clone();
        this.arenaMaximum = other.arenaMaximum == null ? null : other.arenaMaximum.clone();
        this.arenaCenter = other.arenaCenter == null ? null : other.arenaCenter.clone();
        this.arenaBorderLength = other.arenaBorderLength;

        this.deathmatchMinimum = other.deathmatchMinimum == null ? null : other.deathmatchMinimum.clone();
        this.deathmatchMaximum = other.deathmatchMaximum == null ? null : other.deathmatchMaximum.clone();
        this.deathmatchCenter = other.deathmatchCenter == null ? null : other.deathmatchCenter.clone();
        this.deathmatchBorderLength = other.deathmatchBorderLength;
        
        this.chests = other.chests;
        this.enchantTables = other.enchantTables;
        this.workbenches = other.workbenches;
        this.furnaces = other.furnaces;
        this.totalBlocks = other.totalBlocks;
    }

    public void setEnderChests(Set<Position> echestLocs) {
        this.enderChestLocations.clear();
        this.enderChestLocations.addAll(echestLocs);
    }
}
