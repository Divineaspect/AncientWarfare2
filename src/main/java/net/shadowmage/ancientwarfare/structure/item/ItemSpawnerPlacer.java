package net.shadowmage.ancientwarfare.structure.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.shadowmage.ancientwarfare.core.util.EntityTools;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.npc.entity.faction.NpcFaction;
import net.shadowmage.ancientwarfare.structure.block.AWStructuresBlocks;
import net.shadowmage.ancientwarfare.structure.tile.SpawnerSettings;
import net.shadowmage.ancientwarfare.structure.tile.TileAdvancedSpawner;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemSpawnerPlacer extends ItemBaseStructure {
	private static final String SPAWNER_DATA_TAG = "spawnerData";

	ItemSpawnerPlacer(String name) {
		super(name);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(I18n.format("guistrings.selected_mob") + ":");
		//noinspection ConstantConditions
		if (stack.hasTagCompound() && stack.getTagCompound().hasKey(SPAWNER_DATA_TAG)) {
			SpawnerSettings settings = new SpawnerSettings();
			settings.readFromNBT(stack.getTagCompound().getCompoundTag(SPAWNER_DATA_TAG));
			tooltip.add(I18n.format(settings.getSpawnGroups().get(0).getEntitiesToSpawn().get(0).getEntityName()));
		} else {
			tooltip.add(I18n.format("guistrings.no_selection"));
		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.world.isRemote) {
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		if (stack.isEmpty()) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
		RayTraceResult traceResult = rayTrace(player.world, player, false);
		//noinspection ConstantConditions
		if (traceResult != null && traceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
			//noinspection ConstantConditions
			if (stack.hasTagCompound() && stack.getTagCompound().hasKey(SPAWNER_DATA_TAG)) {
				BlockPos placePos = traceResult.getBlockPos().offset(traceResult.sideHit);
				if (player.world.setBlockState(placePos, AWStructuresBlocks.advancedSpawner.getDefaultState())) {
					WorldTools.getTile(player.world, placePos, TileAdvancedSpawner.class)
							.ifPresent(t -> {
								SpawnerSettings settings = new SpawnerSettings();
								settings.readFromNBT(stack.getTagCompound().getCompoundTag(SPAWNER_DATA_TAG));
								t.setSettings(settings);
							});
				}
			} else {
				player.sendMessage(new TextComponentTranslation("guistrings.spawner.nodata"));
			}
		} else {
			player.sendMessage(new TextComponentTranslation("guistrings.spawner.noblock"));
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@SubscribeEvent
	public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		EntityPlayer player = event.getEntityPlayer();
		ItemStack spawnerPlacer = EntityTools.getItemFromEitherHand(player, ItemSpawnerPlacer.class);
		if (!spawnerPlacer.isEmpty()) {
			event.setCanceled(true);
			event.setCancellationResult(EnumActionResult.SUCCESS);

			if (player.world.isRemote) {
				return;
			}

			Entity entity = event.getTarget();

			//noinspection ConstantConditions
			ResourceLocation registryName = EntityRegistry.getEntry(entity.getClass()).getRegistryName();
			SpawnerSettings.EntitySpawnSettings spawnSettings = new SpawnerSettings.EntitySpawnSettings();
			//noinspection ConstantConditions
			spawnSettings.setEntityToSpawn(registryName);
			spawnSettings.setSpawnLimitTotal(1);
			spawnSettings.setSpawnCountMin(1);
			spawnSettings.setSpawnCountMax(1);
			spawnSettings.setCustomSpawnTag(getCustomSpawnTag(entity));
			spawnSettings.toggleForce();
			SpawnerSettings.EntitySpawnGroup group = new SpawnerSettings.EntitySpawnGroup();
			group.addSpawnSetting(spawnSettings);
			SpawnerSettings settings = new SpawnerSettings();
			settings.addSpawnGroup(group);
			settings.setSpawnDelay(0);
			settings.setMaxDelay(0);
			settings.setSpawnRange(0);
			settings.setPlayerRange(16);

			spawnerPlacer.setTagInfo(SPAWNER_DATA_TAG, settings.writeToNBT(new NBTTagCompound()));
			entity.setDead();

			event.getEntityPlayer().sendMessage(new TextComponentTranslation("guistrings.spawner.entity_set", entity.getName()));
		}
	}

	private NBTTagCompound getCustomSpawnTag(Entity entity) {
		NBTTagCompound entityTag = new NBTTagCompound();
		entity.writeToNBT(entityTag);

		NBTTagCompound ret = new NBTTagCompound();

		for (Map.Entry<Class, Set<String>> entry : ENTITY_TAGS.entrySet()) {
			if (entry.getKey().isInstance(entity)) {
				for (String tag : entry.getValue()) {
					ret.setTag(tag, entityTag.getTag(tag));
				}
			}
		}
		return ret;
	}

	private static final Map<Class, Set<String>> ENTITY_TAGS = new ImmutableMap.Builder<Class, Set<String>>()
			.put(NpcFaction.class, Collections.singleton("factionName"))
			.put(EntityVillager.class, ImmutableSet.of("Offers", "Profession", "ProfessionName", "Career", "CareerLevel"))
			.put(EntityHorse.class, Collections.singleton("Variant"))
			.put(EntityLiving.class, ImmutableSet.of("HandItems", "HandDropChances", "ArmorItems", "ArmorDropChances"))
			.build();
}
