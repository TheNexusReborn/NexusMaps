package com.thenexusreborn.gamemaps;

import com.stardevllc.starcore.color.ColorHandler;
import com.stardevllc.starcore.utils.Position;
import com.thenexusreborn.gamemaps.model.MapSpawn;
import com.thenexusreborn.gamemaps.model.SGMap;
import com.thenexusreborn.gamemaps.tasks.AnalyzeThread;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    
    public SGMapCommand(JavaPlugin plugin, MapManager mapManager) {
        this.plugin = plugin;
        this.mapManager = mapManager;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(args.length > 0)) {
            sender.sendMessage(ColorHandler.getInstance().color("You must provide a sub command."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorHandler.getInstance().color("Only players can use that command."));
            return true;
        }

        String mapSubCommand = args[0].toLowerCase();

        if (!mapManager.isEditMode()) {
            sender.sendMessage(ColorHandler.getInstance().color("You can only use that command when map editing mode is active."));
            return true;
        }

        if (mapSubCommand.equals("create") || mapSubCommand.equals("c")) {
            if (!(args.length > 2)) {
                sender.sendMessage(ColorHandler.getInstance().color("Usage: /" + label + " create <url> <name>"));
                return true;
            }

            String url = args[1];
            String mapName = getMapNameFromCommand(args, 2);
            if (mapManager.getMap(mapName) != null) {
                sender.sendMessage(ColorHandler.getInstance().color("A map with that name already exists."));
                return true;
            }

            SGMap gameMap = new SGMap(url, mapName);
            mapManager.addMap(gameMap);
            sender.sendMessage(ColorHandler.getInstance().color("Created a map with the name " + gameMap.getName() + "."));
            new BukkitRunnable() {
                @Override
                public void run() {
                    mapManager.saveMap(gameMap);
                    sender.sendMessage(ColorHandler.getInstance().color("The map has been saved to the database."));
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

            if (gameMap == null) {
                player.sendMessage(ColorHandler.getInstance().color("Could not find a valid map."));
                return true;
            }

            switch (mapSubCommand) {
                case "download", "dl" -> {
                    SGMap finalGameMap = gameMap;
                    mapManager.setMapBeingEdited(finalGameMap);
                    player.sendMessage(ColorHandler.getInstance().color("Please wait, downloading the map " + finalGameMap.getName() + "."));
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        boolean successful = finalGameMap.download(plugin);
                        if (successful) {
                            player.sendMessage(ColorHandler.getInstance().color("Downloaded the map " + finalGameMap.getName() + "."));
                        } else {
                            player.sendMessage(ColorHandler.getInstance().color("Failed to download the map " + finalGameMap.getName()));
                        }
                    });
                    return true;
                }
                case "load", "l" -> {
                    gameMap.unzip(plugin);
                    gameMap.copyFolder(plugin, "", false);
                    gameMap.load(plugin);
                    if (gameMap.getWorld() != null) {
                        sender.sendMessage(ColorHandler.getInstance().color("Successfully loaded the map " + gameMap.getName() + "."));
                    } else {
                        sender.sendMessage(ColorHandler.getInstance().color("Could not load the map " + gameMap.getName() + ". Please report as a bug."));
                    }
                    return true;
                }
                case "teleport", "tp" -> {
                    Location spawn;
                    if (gameMap.getWorld() == null) {
                        sender.sendMessage(ColorHandler.getInstance().color("That map is not loaded. Please load before teleporting."));
                        return true;
                    }
                    if (gameMap.getCenter() != null) {
                        spawn = gameMap.getCenter().toLocation(gameMap.getWorld());
                    } else {
                        spawn = gameMap.getWorld().getSpawnLocation();
                    }
                    player.teleport(spawn);
                    player.sendMessage(ColorHandler.getInstance().color("Teleported to the map " + gameMap.getName()));
                    return true;
                }
                case "save", "s" -> {
                    mapManager.saveMap(gameMap);
                    player.sendMessage(ColorHandler.getInstance().color("Saved the settings for the map " + gameMap.getName()));
                }
                case "removefromserver", "rfs" -> {
                    mapManager.setMapBeingEdited(null);
                    gameMap.removeFromServer(plugin);
                    player.sendMessage(ColorHandler.getInstance().color("Removed the map " + gameMap.getName() + " from the server."));
                    return true;
                }
                case "delete" -> player.sendMessage(ColorHandler.getInstance().color("This command is not yet implemented."));
                case "addspawn", "as" -> {
                    Location location = player.getLocation();
                    int position = gameMap.addSpawn(new MapSpawn(0, -1, location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                    sender.sendMessage(ColorHandler.getInstance().color("You added a spawn with index &b" + (position + 1) + " &eto the map &b" + gameMap.getName()));
                }
                case "setspawn", "sp" -> {
                    int argIndex;
                    if (mapFromArgument) {
                        argIndex = 3;
                    } else {
                        argIndex = 2;
                    }

                    if (!(args.length > argIndex)) {
                        sender.sendMessage(ColorHandler.getInstance().color("You must provide an index for the spawn."));
                        return true;
                    }

                    int position;
                    try {
                        position = Integer.parseInt(args[argIndex]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ColorHandler.getInstance().color("You provided an invalid number for the spawn index."));
                        return true;
                    }

                    Location location = player.getLocation();
                    gameMap.setSpawn(position, new MapSpawn(gameMap.getId(), position, location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                    sender.sendMessage(ColorHandler.getInstance().color("You set the spawn at position &b" + position + " &eto your location in the map &b" + gameMap.getName()));
                }
                case "setcenter", "sc" -> {
                    Location location = player.getPlayer().getLocation();
                    gameMap.setCenter(new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                    player.sendMessage(ColorHandler.getInstance().color("You set the center of the map &b" + gameMap.getName() + " &eto your current location."));
                }
                case "setborderradius", "sbr" -> {
                    int argIndex;
                    if (mapFromArgument) {
                        argIndex = 3;
                    } else {
                        argIndex = 2;
                    }

                    if (!(args.length > argIndex)) {
                        sender.sendMessage(ColorHandler.getInstance().color("You must provide a radius."));
                        return true;
                    }

                    int radius;
                    try {
                        radius = Integer.parseInt(args[argIndex]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ColorHandler.getInstance().color("You provided an invalid number for the radius."));
                        return true;
                    }

                    gameMap.setBorderDistance(radius);
                    if (mapManager.isViewingWorldBorder()) {
                        gameMap.applyWorldBoarder(mapManager.getBorderViewOption());
                    }
                    sender.sendMessage(ColorHandler.getInstance().color("You set the border radius on map &b" + gameMap.getName() + " &eto &b" + radius));
                }
                case "setdeathmatchborderradius", "sdmbr" -> {
                    int argIndex;
                    if (mapFromArgument) {
                        argIndex = 3;
                    } else {
                        argIndex = 2;
                    }

                    if (!(args.length > argIndex)) {
                        sender.sendMessage(ColorHandler.getInstance().color("You must provide a radius."));
                        return true;
                    }

                    int radius;
                    try {
                        radius = Integer.parseInt(args[argIndex]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ColorHandler.getInstance().color("You provided an invalid number for the radius."));
                        return true;
                    }

                    gameMap.setDeathmatchBorderDistance(radius);
                    if (mapManager.isViewingWorldBorder()) {
                        gameMap.applyWorldBoarder(mapManager.getBorderViewOption());
                    }
                    sender.sendMessage(ColorHandler.getInstance().color("You set the deathmatch border radius on map &b" + gameMap.getName() + " &eto &b" + radius));
                }
                case "creators", "cs" -> {
                    int argIndex;
                    if (mapFromArgument) {
                        argIndex = 3;
                    } else {
                        argIndex = 2;
                    }

                    if (!(args.length > argIndex)) {
                        sender.sendMessage(ColorHandler.getInstance().color("You must provide the creators."));
                        return true;
                    }

                    StringBuilder cb = new StringBuilder();
                    for (int i = argIndex; i < args.length; i++) {
                        cb.append(args[i]).append(" ");
                    }

                    String[] creators = cb.toString().trim().split(",");
                    if (creators.length == 0) {
                        sender.sendMessage(ColorHandler.getInstance().color("You must separate the creators with commas."));
                        return true;
                    }

                    for (String creator : creators) {
                        gameMap.addCreator(creator);
                        sender.sendMessage(ColorHandler.getInstance().color("You added " + creator + " as a creator on map " + gameMap.getName()));
                    }
                }
                case "setactive", "sa" -> {
                    int argIndex;
                    if (mapFromArgument) {
                        argIndex = 3;
                    } else {
                        argIndex = 2;
                    }

                    if (!(args.length > argIndex)) {
                        sender.sendMessage(ColorHandler.getInstance().color("You must provide a true or false value."));
                        return true;
                    }

                    boolean value = Boolean.parseBoolean(args[argIndex]);
                    gameMap.setActive(value);
                    if (value && !gameMap.isActive()) {
                        sender.sendMessage(ColorHandler.getInstance().color("Failed to set the map to an active status, there are required elements missing."));
                    } else {
                        sender.sendMessage(ColorHandler.getInstance().color("You set the status of the map to " + value));
                    }
                }
                case "setswagshack", "sss" -> {
                    Location location = player.getPlayer().getLocation();
                    gameMap.setSwagShack(new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
                    player.sendMessage(ColorHandler.getInstance().color("You set the swag shack of the map &b" + gameMap.getName() + " &eto your current location."));
                }
                case "viewborder", "vb" -> {
                    if (!(args.length > 2)) {
                        player.sendMessage(ColorHandler.getInstance().color("You must say if it is for the game or deathmatch."));
                        return true;
                    }
                    
                    String viewOption = args[1].toLowerCase();
                    if (!(viewOption.equals("game") || viewOption.equals("deathmatch"))) {
                        player.sendMessage(ColorHandler.getInstance().color("You provided an invalid type."));
                        return true;
                    }
                    
                    gameMap.applyWorldBoarder(viewOption);
                    mapManager.setBorderViewOption(viewOption);
                    mapManager.setViewingWorldBorder(true);
                    player.sendMessage(ColorHandler.getInstance().color("You are now viewing the world border as " + args[1].toLowerCase()));
                    return true;
                }
                case "disableworldborder", "dwb" -> {
                    if (mapManager.isViewingWorldBorder()) {
                        mapManager.setViewingWorldBorder(false);
                        mapManager.setBorderViewOption("");
                        gameMap.disableWorldBorder();
                        player.sendMessage(ColorHandler.getInstance().color("You disabled the world border preview."));
                    } else {
                        player.sendMessage(ColorHandler.getInstance().color("The world border is not being previewed."));
                    }
                    return true;
                }
                case "analyze" -> {
                    gameMap.setChests(0);
                    gameMap.setEnchantTables(0);
                    gameMap.setWorkbenches(0);
                    gameMap.setTotalBlocks(0);
                    gameMap.setFurnaces(0);
                    player.sendMessage(ColorHandler.getInstance().color("Performing map analysis on " + gameMap.getName()));
                    Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new AnalyzeThread(plugin, gameMap, player), 1L);
                    return true;
                }
                case "analysis" -> {
                    player.sendMessage(ColorHandler.getInstance().color("Map analysis results for &b" + gameMap.getName()));
                    player.sendMessage(ColorHandler.getInstance().color("Total Blocks: &b" + gameMap.getTotalBlocks()));
                    player.sendMessage(ColorHandler.getInstance().color("Total Chests: &b" + gameMap.getChests()));
                    player.sendMessage(ColorHandler.getInstance().color("Total Workbenches: &b" + gameMap.getWorkbenches()));
                    player.sendMessage(ColorHandler.getInstance().color("Total Enchantment Tables: &b" + gameMap.getEnchantTables()));
                    player.sendMessage(ColorHandler.getInstance().color("Total Furnaces: &b" + gameMap.getFurnaces()));
                    return true;
                }
                case "downloadloadteleport", "dlltp" -> {
                    SGMap finalGameMap = gameMap;
                    mapManager.setMapBeingEdited(finalGameMap);
                    player.sendMessage(ColorHandler.getInstance().color("Please wait, setting up the map, then teleporting you to " + finalGameMap.getName() + "."));
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (!finalGameMap.download(plugin)) {
                            player.sendMessage(ColorHandler.getInstance().color("Failed to download the map " + finalGameMap.getName()));
                            return;
                        }

                        finalGameMap.unzip(plugin);
                        finalGameMap.copyFolder(plugin, "", false);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!finalGameMap.load(plugin)) {
                                sender.sendMessage(ColorHandler.getInstance().color("Could not load the map " + finalGameMap.getName() + ". Please report as a bug."));
                                return;
                            }

                            Location spawn;
                            if (finalGameMap.getCenter() != null) {
                                spawn = finalGameMap.getCenter().toLocation(finalGameMap.getWorld());
                            } else {
                                spawn = finalGameMap.getWorld().getSpawnLocation();
                            }
                            player.teleport(spawn);
                            player.sendMessage(ColorHandler.getInstance().color("Successfully setup and teleported you to the map " + finalGameMap.getName()));
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
                    sender.sendMessage(ColorHandler.getInstance().color("The map has been saved to the database."));
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
