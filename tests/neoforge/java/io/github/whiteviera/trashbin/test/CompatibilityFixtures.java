package io.github.whiteviera.trashbin.test;

import io.github.whiteviera.trashbin.TrashBinMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Test-only IDs exercise optional lookup without loading third-party mods. */
@EventBusSubscriber(modid = TrashBinMod.ID)
public final class CompatibilityFixtures {
    @SubscribeEvent public static void register(RegisterEvent event) {
        String mode = System.getProperty("trashbin.test.compat", "none");
        event.register(Registries.FLUID, registry -> {
            if (mode.equals("cei") || mode.equals("both"))
                registry.register(ResourceLocation.fromNamespaceAndPath("create_enchantment_industry", "experience"), new VirtualTestFluid());
            if (mode.equals("sophisticated") || mode.equals("both"))
                registry.register(ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "xp_still"), new VirtualTestFluid());
        });
    }
    private static final class VirtualTestFluid extends Fluid {
        @Override public FluidType getFluidType() { return NeoForgeMod.WATER_TYPE.value(); }
        @Override public Item getBucket() { return Items.AIR; }
        @Override protected boolean canBeReplacedWith(FluidState s, BlockGetter l, BlockPos p, Fluid f, Direction d) { return false; }
        @Override protected Vec3 getFlow(BlockGetter l, BlockPos p, FluidState s) { return Vec3.ZERO; }
        @Override public int getTickDelay(LevelReader l) { return 5; }
        @Override protected float getExplosionResistance() { return 100; }
        @Override public float getHeight(FluidState s, BlockGetter l, BlockPos p) { return 0; }
        @Override public float getOwnHeight(FluidState s) { return 0; }
        @Override protected BlockState createLegacyBlock(FluidState s) { return Blocks.AIR.defaultBlockState(); }
        @Override public boolean isSource(FluidState s) { return true; }
        @Override public int getAmount(FluidState s) { return 0; }
        @Override public VoxelShape getShape(FluidState s, BlockGetter l, BlockPos p) { return Shapes.empty(); }
    }
    private CompatibilityFixtures() {}
}
