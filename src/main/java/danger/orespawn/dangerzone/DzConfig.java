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

    public static final ModConfigSpec.IntValue FLAG_WAVE_SECONDS = BUILDER
            .comment("Seconds between Tower Defense Flag waves.")
            .defineInRange("flagWaveSeconds", 30, 5, 600);
    public static final ModConfigSpec.BooleanValue FLAG_CLEARS_BLOCKS = BUILDER
            .comment("Whether a flag keeps the blocks around it clear, as in DangerZone. Also needs mobGriefing. Containers are left alone.")
            .define("flagClearsBlocks", true);
    public static final ModConfigSpec.DoubleValue TROPHY_CHANCE = BUILDER
            .comment("Chance a creature drops its trophy.")
            .defineInRange("trophyChance", 0.05, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue BOSS_TROPHY_CHANCE = BUILDER
            .comment("Chance a boss drops its trophy.")
            .defineInRange("bossTrophyChance", 0.2, 0.0, 1.0);
    public static final ModConfigSpec.IntValue NOPE_MAX_BABIES = BUILDER
            .comment("How many Baby Nopes may stand near a Nope before it stops spawning more.")
            .defineInRange("nopeMaxBabies", 8, 0, 64);
    public static final ModConfigSpec.IntValue SWARM_AVERAGE_MINUTES = BUILDER
            .comment("Average minutes between swarms. 0 turns them off.")
            .defineInRange("swarmAverageMinutes", 50, 0, 100000);
    public static final ModConfigSpec.IntValue SWARM_SIZE = BUILDER
            .comment("Creatures in a swarm. DangerZone sends 100 to 199, which is hard on a Minecraft server.")
            .defineInRange("swarmSize", 60, 1, 300);
    public static final ModConfigSpec.IntValue VOLCANO_MAX_FLYING = BUILDER
            .comment("How many thrown blocks a volcano allows in the air around it at once.")
            .defineInRange("volcanoMaxFlying", 40, 1, 500);
    public static final ModConfigSpec.IntValue UFO_MAX_CRAFT = BUILDER
            .comment("How many Fighters and Carriers may be near a UFO before it stops launching more.")
            .defineInRange("ufoMaxCraft", 6, 0, 64);
    public static final ModConfigSpec.IntValue UFO_MAX_MARTIANS = BUILDER
            .comment("How many Martians may stand near a landing Carrier before it lets out fewer.")
            .defineInRange("ufoMaxMartians", 30, 0, 200);

    public static final ModConfigSpec.IntValue BUILDER_BLOCKS_PER_TICK = BUILDER
            .comment("How many blocks a castle or the Dragon's Lair places per tick. Lower is kinder to the server and slower.")
            .defineInRange("builderBlocksPerTick", 15000, 500, 500000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DzConfig() {
    }
}
