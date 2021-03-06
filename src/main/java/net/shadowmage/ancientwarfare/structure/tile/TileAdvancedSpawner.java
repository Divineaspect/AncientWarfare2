package net.shadowmage.ancientwarfare.structure.tile;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.shadowmage.ancientwarfare.core.tile.TileUpdatable;
import net.shadowmage.ancientwarfare.core.util.BlockTools;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

public class TileAdvancedSpawner extends TileUpdatable implements ITickable {

	private SpawnerSettings settings = new SpawnerSettings();

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		settings.setWorld(world, pos);
	}

	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);
		settings.setPos(posIn);
	}

	@Override
	public void update() {
		if (!hasWorld() || world.isRemote) {
			return;
		}
		if (!settings.hasWorld()) {
			settings.setWorld(world, pos);
		}
		settings.onUpdate();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		NBTTagCompound ntag = new NBTTagCompound();
		settings.writeToNBT(ntag);
		tag.setTag("spawnerSettings", ntag);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		settings.readFromNBT(tag.getCompoundTag("spawnerSettings"));
	}

	@Override
	protected void writeUpdateNBT(NBTTagCompound tag) {
		super.writeUpdateNBT(tag);
		settings.writeToNBT(tag);
	}

	@Override
	protected void handleUpdateNBT(NBTTagCompound tag) {
		super.handleUpdateNBT(tag);
		settings.readFromNBT(tag);
		world.markBlockRangeForRenderUpdate(pos, pos);
		BlockTools.notifyBlockUpdate(this);
	}

	public SpawnerSettings getSettings() {
		return settings;
	}

	public void setSettings(SpawnerSettings settings) {
		this.settings = settings;
		BlockTools.notifyBlockUpdate(this);
	}

	public float getBlockHardness() {
		return settings.blockHardness;
	}

	public void onBlockBroken() {
		if (world.isRemote) {
			return;
		}
		int xp = settings.getXpToDrop();
		while (xp > 0) {
			int j = EntityXPOrb.getXPSplit(xp);
			xp -= j;
			this.world.spawnEntity(new EntityXPOrb(this.world, this.pos.getX() + 0.5d, this.pos.getY(), this.pos.getZ() + 0.5d, j));
		}
		InventoryTools.dropItemsInWorld(world, settings.getInventory(), pos);
	}
}
