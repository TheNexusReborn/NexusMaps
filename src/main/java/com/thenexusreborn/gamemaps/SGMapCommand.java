package com.thenexusreborn.gamemaps;

import com.stardevllc.colors.StarColors;
import com.stardevllc.starcore.utils.Position;
import com.thenexusreborn.gamemaps.model.MapSpawn;
import com.thenexusreborn.gamemaps.model.SGMap;
import com.thenexusreborn.gamemaps.tasks.AnalyzeThread;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("DuplicatedCode")
public class SGMapCommand implements CommandExecutor {
    private JavaPlugin plugin;
    private MapManager mapManager;
    
    private static final String URL_BASE = "https://assets.thenexusreborn.com/survivalgames/maps/";
    
    public SGMapCommand(JavaPlugin plugin, MapManager mapManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(args.length > 0)) {
            sender.sendMessage(StarColors.color("&cYou must provide a sub command."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(StarColors.color("&cOnly players can use that command."));
            return true;
        }

        String mapSubCommand = args[0].toLowerCase();

        if (!mapManager.isEditMode()) {
            sender.sendMessage(StarColors.color("&cYou can only use that command when map editing mode is active."));
            return true;
        }

        if (mapSubCommand.equals("create") || mapSubCommand.equals("c")) {
            if (!(args.length > 2)) {
                sender.sendMessage(StarColors.color("&cUsage: /" + label + " create <url> <name>"));
                return true;
            }

            String url = URL_BASE + args[1];
            String mapName = getMapNameFromCommand(args, 2);
            if (mapManager.getMap(mapName) != null) {
                sender.sendMessage(StarColors.color("&cA map with that name already exists."));
                return true;
            }

            SGMap gameMap = new SGMap(url, mapName);
            mapManager.addMap(gameMap);
            sender.sendMessage(StarColors.color("&eCreated a map with the name &b" + gameMap.getName() + "&e."));
            new BukkitRunnable() {
                @Override
                public void run() {
                    mapManager.saveMap(gameMap);
                    sender.sendMessage(StarColors.color("&7&oThe map has been saved to the database."));
                }
            }.runTaskAsynchronously(plugin);
        } else {
            SGMap gameMap = null;
            boolean mapFromArgument = false;
            for (SGMap map : mapManager.getMaps()) {
                if (map.getWorld() != null) {
                    if (map.getWorld().getName().equalsIgnoreCase(player.getWorld().getName())) {
                        gameMap = map;
                        break;
                    }
                }
            }

            if (args.length > 1 && gameMap == null) {
                gameMap = mapManager.getMap(args[1]);
                mapFromArgument = true;
            }

            int argIndex;
            if (mapFromArgument) {
                argIndex = 2;
            } else {
                argIndex = 1;
            }

            if (gameMap == null) {
                player.sendMessage(StarColors.color("&cCould not find a valid map."));
                return true;
            }

            switch (mapSubCommand) {
                case "download", "dl" -> {
                    SGMap finalGameMap = gameMap;
                    mapManager.setMapBeingEdited(finalGameMap);
                    player.sendMessage(StarColors.color("&7&oPlease wait, downloading the map " + finalGameMap.getName() + "."));
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        boolean successful = finalGameMap.download(plugin);
                        if (successful) {
                            player.sendMessage(StarColors.color("&eDownloaded the map &b" + finalGameMap.getName() + "."));
                        } else {
                            player.sendMessage(StarColors.color("&cFailed to download the map " + finalGameMap.getName()));
                        }
                    });
                    return true;
                }
                case "load", "l" -> {
                    gameMap.unzip(plugin);
                    gameMap.copyFolder(plugin, "", false);
                    gameMap.load(plugin);
                    if (gameMap.getWorld() != null) {
                        if (gameMap.getCenterLocation() != null) {
                            gameMap.getCenterLocation().getBlock().setType(Material.BEDROCK);
                        }
                        
                        if (gameMap.getSwagShack() != null) {
                            gameMap.getSwagShack().toBlockLocation(gameMap.getWorld()).getBlock().setType(Material.BEDROCK);
                        }
                        
                        if (!gameMap.getSpawns().isEmpty()) {
                            for (MapSpawn mapSpawn : gameMap.getSpawns()) {
                                mapSpawn.toBlockLocation(gameMap.getWorld()).getBlock().setType(Material.BEDROCK);
                            }
                        }
                        
                        sender.sendMessage(StarColors.color("&eSuccessfully loaded the map &b" + gameMap.getName() + "."));
                    } else {
                        sender.sendMessage(StarColors.color("&cCould not load the map " + gameMap.getName() + ". Please report as a bug."));
                    }
                    return true;
                }
                case "teleport", "tp" -> {
                    Location spawn;
                    if (gameMap.getWorld() == null) {
                        sender.sendMessage(StarColors.color("&cThat map is not loaded. Please load before teleporting."));
                        return true;
                    }
                    if (gameMap.getCenter() != null) {
                        spawn = gameMap.getCenter().toLocation(gameMap.getWorld());
                    } else {
                        spawn = gameMap.getWorld().getSpawnLocation();
                    }
                    player.teleport(spawn);
                    player.sendMessage(StarColors.color("&eTeleported to the map " + gameMap.getName()));
                    return true;
                }
                case "save", "s" -> {
                    mapManager.saveMap(gameMap);
                    player.sendMessage(StarColors.color("&eSaved the settings for the map &b" + gameMap.getName()));
                }
                case "removefromserver", "rfs" -> {
                    mapManager.setMapBeingEdited(null);
                    gameMap.removeFromServer(plugin);
                    player.sendMessage(StarColors.color("&eRemoved the map &b" + gameMap.getName() + " &efrom the server."));
                    return true;
                }
                case "delete" -> player.sendMessage(StarColors.color("&cThis command is not yet implemented."));
                case "addspawn", "as" -> {
                    Location location = player.getLocation();
                    MapSpawn spawn = new MapSpawn(0, -1, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    int position = gameMap.addSpawn(spawn);
                    Location blockLocation = spawn.toBlockLocation(gameMap.getWorld());
                    player.teleport(location.clone().add(0, 1, 0));
                    blockLocation.getBlock().setType(Material.BEDROCK);
                    sender.sendMessage(StarColors.color("&eYou added a spawn with index &b" + (position + 1) + " &eto the map &b" + gameMap.getName()));
                }
                case "setspawn", "sp" -> {
                    if (!(args.length > argIndex)) {
                        sender.sendMessage(StarColors.color("&cYou must provide an index for the spawn."));
                        return true;
                    }

                    int position;
                    try {
                        position = Integer.parseInt(args[argIndex]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(StarColors.color("&cYou provided an invalid number for the spawn index."));
                        return true;
                    }

                    MapSpawn existingSpawn = gameMap.getSpawns().get(position);
                    if (existingSpawn != null) {
                        existingSpawn.toBlockLocation(gameMap.getWorld()).add(0, 1, 0).getBlock().setType(Material.AIR);
                    }

                    Location location = player.getLocation();
                    MapSpawn mapSpawn = new MapSpawn(gameMap.getId(), position, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    gameMap.setSpawn(position, mapSpawn);
                    Location blockLocation = mapSpawn.toBlockLocation(gameMap.getWorld());
                    player.teleport(location.clone().add(0, 1, 0));
                    blockLocation.getBlock().setType(Material.AIR);
                    sender.sendMessage(StarColors.color("&eYou set the spawn at position &b" + position + " &eto your location in the map &b" + gameMap.getName()));
                }
                case "setcenter", "sc" -> {
                    Location centerLocation = gameMap.getCenterLocation();
                    if (centerLocation != null) {
                        centerLocation.getBlock().setType(Material.AIR);
                    }

                    Location location = player.getLocation();
                    gameMap.setCenter(new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                    player.teleport(location.clone().add(0, 1, 0));
                    location.getBlock().setType(Material.BEDROCK);
                    player.sendMessage(StarColors.color("&eYou set the center of the map &b" + gameMap.getName() + " &eto your current location."));
                }
                case "setborderradius", "sbr" -> {
                    if (!(args.length > argIndex)) {
                        sender.sendMessage(StarColors.color("&cYou must provide a radius."));
                        return true;
                    }

                    int radius;
                    try {
                        radius = Integer.parseInt(args[argIndex]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(StarColors.color("&cYou provided an invalid number for the radius."));
                        return true;
                    }

                    gameMap.setBorderDistance(radius);
                    if (mapManager.isViewingWorldBorder()) {
                        gameMap.applyWorldBoarder(mapManager.getBorderViewOption());
                    }
                    sender.sendMessage(StarColors.color("&eYou set the border radius on map &b" + gameMap.getName() + " &eto &b" + radius));
                }
                case "setdeathmatchborderradius", "sdmbr" -> {
                    if (!(args.length > argIndex)) {
                        sender.sendMessage(StarColors.color("&cYou must provide a radius."));
                        return true;
                    }

                    int radius;
                    try {
                        radius = Integer.parseInt(args[argIndex]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(StarColors.color("&cYou provided an invalid number for the radius."));
                        return true;
                    }

                    gameMap.setDeathmatchBorderDistance(radius);
                    if (mapManager.isViewingWorldBorder()) {
                        gameMap.applyWorldBoarder(mapManager.getBorderViewOption());
                    }
                    sender.sendMessage(StarColors.color("&eYou set the deathmatch border radius on map &b" + gameMap.getName() + " &eto &b" + radius));
                }
                case "creators", "cs" -> {
                    if (!(args.length > argIndex)) {
                        sender.sendMessage(StarColors.color("&cYou must provide the creators."));
                        return true;
                    }

                    StringBuilder cb = new StringBuilder();
                    for (int i = argIndex; i < args.length; i++) {
                        cb.append(args[i]).append(" ");
                    }

                    String[] creators = cb.toString().trim().split(",");
                    if (creators.length == 0) {
                        sender.sendMessage(StarColors.color("&cYou must separate the creators with commas."));
                        return true;
                    }

                    for (String creator : creators) {
                        gameMap.addCreator(creator);
                        sender.sendMessage(StarColors.color("&eYou added &b" + creator + " &eas a creator on map &b" + gameMap.getName()));
                    }
                }
                case "setactive", "sa" -> {
                    if (!(args.length > argIndex)) {
                        sender.sendMessage(StarColors.color("&cYou must provide a true or false value."));
                        return true;
                    }

                    boolean value = Boolean.parseBoolean(args[argIndex]);
                    gameMap.setActive(value);
                    if (value && !gameMap.isActive()) {
                        sender.sendMessage(StarColors.color("&cFailed to set the map to an active status, there are required elements missing."));
                    } else {
                        sender.sendMessage(StarColors.color("&eYou set the status of the map to " + value));
                    }
                }
                case "setswagshack", "sss" -> {
                    if (gameMap.getSwagShack() != null) {
                        gameMap.getSwagShack().toBlockLocation(gameMap.getWorld()).getBlock().setType(Material.AIR);
                    }
                    
                    Location location = player.getPlayer().getLocation();
                    gameMap.setSwagShack(new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                    player.teleport(location.clone().add(0, 1, 0));
                    location.getBlock().setType(Material.BEDROCK);
                    player.sendMessage(StarColors.color("&eYou set the swag shack of the map &b" + gameMap.getName() + " &eto your current location."));
                }
                case "viewborder", "vb" -> {
                    if (!(args.length > 1)) {
                        player.sendMessage(StarColors.color("&cYou must say if it is for the game or deathmatch."));
                        return true;
                    }
                    
                    String viewOption = args[1].toLowerCase();
                    if (!(viewOption.equals("game") || viewOption.equals("deathmatch"))) {
                        player.sendMessage(StarColors.color("&cYou provided an invalid type."));
                        return true;
                    }
                    
                    gameMap.applyWorldBoarder(viewOption);
                    mapManager.setBorderViewOption(viewOption);
                    mapManager.setViewingWorldBorder(true);
                    player.sendMessage(StarColors.color("&eYou are now viewing the world border as " + args[1].toLowerCase()));
                    return true;
                }
                case "disableworldborder", "dwb" -> {
                    if (mapManager.isViewingWorldBorder()) {
                        mapManager.setViewingWorldBorder(false);
                        mapManager.setBorderViewOption("");
                        gameMap.disableWorldBorder();
                        player.sendMessage(StarColors.color("&eYou disabled the world border preview."));
                    } else {
                        player.sendMessage(StarColors.color("&cThe world border is not being previewed."));
                    }
                    return true;
                }
                case "analyze" -> {
                    gameMap.setChests(0);
                    gameMap.setEnchantTables(0);
                    gameMap.setWorkbenches(0);
                    gameMap.setTotalBlocks(0);
                    gameMap.setFurnaces(0);
                    player.sendMessage(StarColors.color("&ePerforming map analysis on " + gameMap.getName()));
                    Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new AnalyzeThread(plugin, gameMap, player), 1L);
                    return true;
                }
                case "analysis" -> {
                    player.sendMessage(StarColors.color("&eMap analysis results for &b" + gameMap.getName()));
                    player.sendMessage(StarColors.color("&eTotal Blocks: &b" + gameMap.getTotalBlocks()));
                    player.sendMessage(StarColors.color("&eTotal Chests: &b" + gameMap.getChests()));
                    player.sendMessage(StarColors.color("&eTotal Workbenches: &b" + gameMap.getWorkbenches()));
                    player.sendMessage(StarColors.color("&eTotal Enchantment Tables: &b" + gameMap.getEnchantTables()));
                    player.sendMessage(StarColors.color("&eTotal Furnaces: &b" + gameMap.getFurnaces()));
                    return true;
                }
                case "downloadloadteleport", "dlltp" -> {
                    SGMap finalGameMap = gameMap;
                    mapManager.setMapBeingEdited(finalGameMap);
                    player.sendMessage(StarColors.color("&ePlease wait, setting up the map, then teleporting you to &b" + finalGameMap.getName() + "&e."));
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (!finalGameMap.download(plugin)) {
                            player.sendMessage(StarColors.color("&cFailed to download the map " + finalGameMap.getName()));
                            return;
                        }

                        finalGameMap.unzip(plugin);
                        finalGameMap.copyFolder(plugin, "", false);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!finalGameMap.load(plugin)) {
                                sender.sendMessage(StarColors.color("&cCould not load the map " + finalGameMap.getName() + ". Please report as a bug."));
                                return;
                            }

                            Location spawn;
                            if (finalGameMap.getCenter() != null) {
                                spawn = finalGameMap.getCenter().toLocation(finalGameMap.getWorld());
                            } else {
                                spawn = finalGameMap.getWorld().getSpawnLocation();
                            }

                            if (finalGameMap.getCenterLocation() != null) {
                                finalGameMap.getCenterLocation().getBlock().setType(Material.BEDROCK);
                            }

                            if (finalGameMap.getSwagShack() != null) {
                                finalGameMap.getSwagShack().toBlockLocation(finalGameMap.getWorld()).getBlock().setType(Material.BEDROCK);
                            }

                            if (!finalGameMap.getSpawns().isEmpty()) {
                                for (MapSpawn mapSpawn : finalGameMap.getSpawns()) {
                                    mapSpawn.toBlockLocation(finalGameMap.getWorld()).getBlock().setType(Material.BEDROCK);
                                }
                            }
                            
                            player.teleport(spawn);
                            player.sendMessage(StarColors.color("&eSuccessfully setup and teleported you to the map " + finalGameMap.getName()));
                        });
                    });
                    return true;
                }
                default -> {
                    return true;
                }
            }
            SGMap finalGameMap = gameMap;
            new BukkitRunnable() {
                @Override
                public void run() {
                    mapManager.saveMap(finalGameMap);
                    sender.sendMessage(StarColors.color("&7&oThe map has been saved to the database."));
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }
        return true;
    }

    public static String getMapNameFromCommand(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        return sb.toString().trim();
    }
}
