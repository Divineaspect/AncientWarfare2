package net.shadowmage.ancientwarfare.structure.template.plugin.default_plugins.block_rules;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.shadowmage.ancientwarfare.core.util.WorldTools;
import net.shadowmage.ancientwarfare.structure.api.IStructureBuilder;
import net.shadowmage.ancientwarfare.structure.block.BlockDataManager;

public class TemplateRuleBlockLogic extends TemplateRuleVanillaBlocks {

	public NBTTagCompound tag = new NBTTagCompound();

	public TemplateRuleBlockLogic(World world, BlockPos pos, Block block, int meta, int turns) {
		super(world, pos, block, meta, turns);
		WorldTools.getTile(world, pos).ifPresent(t -> {
			t.writeToNBT(tag);
			tag.removeTag("x");
			tag.removeTag("y");
			tag.removeTag("z");
		});
	}

	public TemplateRuleBlockLogic() {
	}

	@Override
	public void handlePlacement(World world, int turns, BlockPos pos, IStructureBuilder builder) {
		super.handlePlacement(world, turns, pos, builder);
		int localMeta = BlockDataManager.INSTANCE.getRotatedMeta(block, this.meta, turns);
		world.setBlockState(pos, block.getStateFromMeta(localMeta), 3);
		WorldTools.getTile(world, pos).ifPresent(t -> {
			//TODO look into changing this so that the whole TE doesn't need reloading from custom NBT
			tag.setString("id", block.getRegistryName().toString());
			tag.setInteger("x", pos.getX());
			tag.setInteger("y", pos.getY());
			tag.setInteger("z", pos.getZ());
			t.readFromNBT(tag);
		});
	}

	@Override
	public boolean shouldReuseRule(World world, Block block, int meta, int turns, BlockPos pos) {
		return false;
	}

	@Override
	public void writeRuleData(NBTTagCompound tag) {
		super.writeRuleData(tag);
		tag.setTag("teData", this.tag);
	}

	@Override
	public void parseRuleData(NBTTagCompound tag) {
		super.parseRuleData(tag);
		this.tag = tag.getCompoundTag("teData");
	}
}
