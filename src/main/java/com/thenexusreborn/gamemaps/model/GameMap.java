package com.thenexusreborn.gamemaps.model;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.stardevllc.starlib.helper.FileHelper;
import com.stardevllc.starmclib.Position;
import com.thenexusreborn.api.NexusReborn;
import com.thenexusreborn.api.player.NexusPlayer;
import com.thenexusreborn.api.sql.annotations.column.*;
import com.thenexusreborn.api.sql.objects.codecs.StringSetCodec;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GameMap {
    protected long id;
    protected String url;
    protected String name;
    protected String prefix;

    @ColumnType("varchar(1000)")
    @ColumnCodec(StringSetCodec.class)
    protected Set<String> creators = new HashSet<>();
    protected boolean active;
    
    //This is the center of the spawns for the arena, where spectators will spawn in
    //And where players will look at when teleported to a game spawn
    protected Position spawnCenter;
    
    protected Position arenaMinimum;
    protected Position arenaMaximum;
    protected Position arenaCenter;

    @ColumnIgnored
    protected UUID uniqueId;
    @ColumnIgnored
    protected World world;

    @ColumnIgnored
    protected Path downloadedZip, unzippedFolder, worldFolder;
    @ColumnIgnored
    protected boolean editing;
    @ColumnIgnored
    protected int votes;
    @ColumnIgnored
    protected CuboidRegion arenaRegion;
    
    protected GameMap() {
    }

    public boolean isValid() {
        if (this.url == null || this.url.isEmpty()) {
            return false;
        }
        
        if (this.name == null || this.name.isEmpty()) {
            return false;
        }
        
        if (this.creators.isEmpty()) {
            return false;
        }
        
        if (this.spawnCenter == null) {
            return false;
        }
        
        return this.arenaMinimum != null && this.arenaMaximum != null && this.arenaCenter != null;
    }

    public GameMap(String fileName, String name) {
        this.url = fileName;
        this.name = name;
    }

    public Position getArenaCenter() {
        return arenaCenter;
    }

    public void setArenaCenter(Position arenaCenter) {
        this.arenaCenter = arenaCenter;
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

    public Location getCenterLocation() {
        if (this.world != null) {
            if (this.spawnCenter != null) {
                return getSpawnCenter().toLocation(this.world);
            }
        }

        return null;
    }
    
    public void removeFromServer(JavaPlugin plugin) {
        try {
            uniqueId = null;

            if (this.world != null) {
                for (Player player : world.getPlayers()) {
                    NexusPlayer nexusPlayer = NexusReborn.getPlayerManager().getNexusPlayer(player.getUniqueId());
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

    public void addCreator(String creator) {
        this.creators.add(creator);
    }

    public void addCreators(String... creators) {
        this.creators.addAll(Arrays.asList(creators));
    }

    public void removeCreator(String creator) {
        this.creators.remove(creator);
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
                this.prefix = prefix != null ? prefix : "";
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

    @Override
    public String toString() {
        return "GameMap{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", creators=" + creators +
                ", active=" + active +
                ", uniqueId=" + uniqueId +
                ", world=" + world +
                ", downloadedZip=" + downloadedZip +
                ", unzippedFolder=" + unzippedFolder +
                ", worldFolder=" + worldFolder +
                ", editing=" + editing +
                ", votes=" + votes +
                '}';
    }

    public static GameMap loadFromYaml(FileConfiguration config) {
        GameMap gameMap = new GameMap();
        gameMap.setId(config.getLong("id"));
        gameMap.setUrl(config.getString("url"));
        gameMap.setName(config.getString("name"));
        gameMap.setPrefix(config.getString("prefix"));
        
        gameMap.setCreators(config.getStringList("creators"));
        
        gameMap.setSpawnCenter((Position) config.get("spawncenter"));
        
        gameMap.setArenaMinimum((Position) config.get("arena.min"));
        gameMap.setArenaMaximum((Position) config.get("arena.max"));
        gameMap.setArenaCenter((Position) config.get("arena.center"));
        
        gameMap.setActive(config.getBoolean("active"));

        return gameMap;
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
        
        config.set("creators", new ArrayList<>(this.creators));

        config.set("spawncenter", this.spawnCenter);
        
        config.set("arena.min", this.arenaMinimum);
        config.set("arena.max", this.arenaMaximum);
        config.set("arena.center", this.arenaCenter);
    }
    
    public void copyFrom(GameMap other) {
        this.url = other.url;
        this.name = other.name;
        this.prefix = other.prefix;

        this.creators.clear();
        this.creators.addAll(other.creators);
        
        this.active = other.active;
        this.spawnCenter = other.spawnCenter == null ? null : other.spawnCenter.clone();

        this.arenaMinimum = other.arenaMinimum == null ? null : other.arenaMinimum.clone();
        this.arenaMaximum = other.arenaMaximum == null ? null : other.arenaMaximum.clone();
        this.arenaCenter = other.arenaCenter == null ? null : other.arenaCenter.clone();
    }
}