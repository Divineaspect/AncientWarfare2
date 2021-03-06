package net.shadowmage.ancientwarfare.npc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.shadowmage.ancientwarfare.npc.item.AWNPCItems;
import net.shadowmage.ancientwarfare.npc.item.ItemNpcSpawner;
import net.shadowmage.ancientwarfare.npc.registry.FactionDefinition;
import net.shadowmage.ancientwarfare.npc.registry.FactionRegistry;

@SideOnly(Side.CLIENT)
public class NPCItemColors {
	private NPCItemColors() {}

	private static final int FACTION_TOP_COLOR = 0xEF5757;

	public static void init() {
		ItemColors itemColors = Minecraft.getMinecraft().getItemColors();

		itemColors.registerItemColorHandler(((stack, tintIndex) -> {
			if (tintIndex == 1 || tintIndex == 2) {
				String factionName = ItemNpcSpawner.getFaction(stack).orElse("");
				if (tintIndex == 2) {
					return FactionRegistry.getFactions().stream().anyMatch(f -> f.getName().equals(factionName)) ? FACTION_TOP_COLOR : -1;
				} else {
					return FactionRegistry.getFactions().stream().filter(f -> f.getName().equals(factionName))
							.map(FactionDefinition::getColor).findFirst().orElse(-1);
				}
			}

			return -1;

		}), AWNPCItems.npcSpawner);
	}
}
