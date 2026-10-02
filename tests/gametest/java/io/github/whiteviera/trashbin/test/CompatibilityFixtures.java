package io.github.whiteviera.trashbin.test;

import io.github.whiteviera.trashbin.TrashBinMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Test-only registry fixtures. No third-party code or real third-party mod is loaded. */
@Mod.EventBusSubscriber(modid = TrashBinMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CompatibilityFixtures {
    @SubscribeEvent public static void construct(FMLConstructModEvent event) {
        String mode = System.getProperty("trashbin.test.compat", "none");
        if (mode.equals("cei") || mode.equals("both")) register("create_enchantment_industry", "experience");
        if (mode.equals("sophisticated") || mode.equals("both")) register("sophisticatedcore", "xp_still");
    }
    private static void register(String namespace, String name) {
        DeferredRegister<Fluid> registry = DeferredRegister.create(ForgeRegistries.FLUIDS, namespace);
        registry.register(name, VirtualTestFluid::new);
        registry.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
    private static final class VirtualTestFluid extends Fluid {
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
