package com.thenexusreborn.gamemaps.items;

import com.stardevllc.itembuilder.ItemBuilder;
import com.stardevllc.itembuilder.XMaterial;
import com.stardevllc.staritems.StarItems;
import com.stardevllc.staritems.model.CustomItem;
import com.stardevllc.staritems.model.types.PlayerEvent;
import com.thenexusreborn.gamemaps.NexusGameMaps;
import org.bukkit.event.block.Action;

public class CmdItem extends CustomItem {
    public CmdItem(String name, XMaterial material, String command) {
        super(NexusGameMaps.getPlugin(NexusGameMaps.class), "map" + name.toLowerCase().replace(" ", "") + "item", ItemBuilder.of(material).displayName("&e&l" + name)
                .addLoreLine("&7This item runs the command &e/" + command));

        StarItems.getPlugin(StarItems.class).getItemRegistry().register(this);
        
        addEventHandler(PlayerEvent.INTERACT, e -> {
            if (!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                return;
            }
            
            e.setCancelled(true);
            
            e.getPlayer().performCommand(command);
        });
    }
}
