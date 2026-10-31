package io.github.whiteviera.trashbin.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.registries.BuiltInRegistries;

/** Registry lookup keeps optional mods optional and avoids linking to their internals. */
public final class ExperienceFluidCompat {
    public record Output(Fluid fluid, int millibucketsPerPoint, int id) {}
    private static final ResourceLocation CEI = ResourceLocation.fromNamespaceAndPath("create_enchantment_industry", "experience");
    private static final ResourceLocation SOPHISTICATED = ResourceLocation.fromNamespaceAndPath("sophisticatedcore", "xp_still");

    public static Output preferred() {
        Fluid cei = BuiltInRegistries.FLUID.get(CEI);
        if (cei != null && cei != Fluids.EMPTY) return new Output(cei, 1, 1);
        Fluid sophisticated = BuiltInRegistries.FLUID.get(SOPHISTICATED);
        if (sophisticated != null && sophisticated != Fluids.EMPTY) return new Output(sophisticated, 20, 2);
        return new Output(Fluids.EMPTY, 1, 0);
    }
    private ExperienceFluidCompat() {}
}
