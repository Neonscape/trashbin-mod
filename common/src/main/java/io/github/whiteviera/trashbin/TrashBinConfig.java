package io.github.whiteviera.trashbin;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TrashBinConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue XP_CAPACITY;
    public static final ForgeConfigSpec.IntValue ITEMS_PER_XP;
    public static final ForgeConfigSpec.IntValue FLUID_MB_PER_XP;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Trash Bin: experience points, not experience levels.",
                "Automatic overflow is discarded. Manual clear overflow becomes orbs.").push("recycling");
        XP_CAPACITY = b.comment("Maximum experience points stored in each bin.")
                .defineInRange("experienceCapacity", 999999, 1, 1000000000);
        ITEMS_PER_XP = b.comment("Number of items converted to one XP point. Remainders persist.")
                .defineInRange("itemsPerExperience", 64, 1, Integer.MAX_VALUE);
        FLUID_MB_PER_XP = b.comment("mB of any input fluid converted to one XP point. Remainders persist.",
                "This also applies to incoming XP fluids; it never uses the output conversion rate.")
                .defineInRange("fluidMillibucketsPerExperience", 512000, 1, Integer.MAX_VALUE);
        b.pop(); SPEC = b.build();
    }
    private TrashBinConfig() {}
}
