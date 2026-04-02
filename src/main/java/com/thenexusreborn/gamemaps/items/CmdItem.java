package com.thenexusreborn.gamemaps.items;

import com.stardevllc.registry.PluginKey;
import com.stardevllc.smaterial.SMaterial;
import com.stardevllc.staritems.ItemBuilders;
import com.stardevllc.staritems.StarItems;
import com.stardevllc.staritems.model.CustomItem;
import com.thenexusreborn.gamemaps.NexusGameMaps;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CmdItem extends CustomItem {
    private static final NexusGameMaps plugin = NexusGameMaps.getPlugin(NexusGameMaps.class);
    
    public CmdItem(String name, SMaterial material, String command) {
        super(plugin, PluginKey.of(plugin, "map" + name.toLowerCase().replace(" ", "") + "item"), "map" + name.toLowerCase().replace(" ", "") + "item", ItemBuilders.of(material).displayName("&e&l" + name)
                .addLoreLine("&7This item runs the command &e/" + command));

        StarItems.getItemRegistry().register(getKey(), this);
        
        addEventHandler(PlayerInteractEvent.class, e -> {
            if (!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                return;
            }
            
            e.setCancelled(true);
            
            e.getPlayer().performCommand(command);
        });
    }
}
