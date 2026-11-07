package danger.orespawn.dangerzone;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class DzConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue QUEEN_MAX_BROOD = BUILDER
            .comment("How many ants may stand near an Ant Queen before she stops calling more.")
            .defineInRange("antQueenMaxBrood", 16, 0, 128);
    public static final ModConfigSpec.IntValue PRIME_MAX_PACK = BUILDER
            .comment("How many Needlenoses may stand near a Nastysaurus Prime before it stops calling more.")
            .defineInRange("primeMaxPack", 6, 0, 64);
    public static final ModConfigSpec.DoubleValue TAPE_REPAIR_FRACTION = BUILDER
            .comment("How much of an item's durability one Duct Tape restores. 1.0 is DangerZone's full repair.")
            .defineInRange("ductTapeRepairFraction", 1.0, 0.01, 1.0);
    public static final ModConfigSpec.IntValue SHRINK_PLAYER_SECONDS = BUILDER
            .comment("How long the Shrink Ray shrinks a player, in seconds. Creatures stay shrunk.")
            .defineInRange("shrinkPlayerSeconds", 60, 1, 3600);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STAFF_DENYLIST = BUILDER
            .comment("Dimensions the Dimension Staff skips, such as \"minecraft:the_end\".")
            .defineListAllowEmpty("dimensionStaffDenylist", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DzConfig() {
    }
}
