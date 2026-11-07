package danger.orespawn.dangerzone.world;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.entity.ElementDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/** Dragon's Lair decoration: the fossils that hold the Eye Stones, and the Element Dragons' pillars. */
public final class DzFeatures {
    private DzFeatures() {
    }

    /** A cross of Fossilized Bone with a Dragon's Eye Stone at one tip. */
    public static class FossilCross extends Feature<NoneFeatureConfiguration> {
        public FossilCross() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            RandomSource random = context.random();
            int arm = 2 + random.nextInt(2);
            for (int i = -arm; i <= arm; i++) {
                level.setBlock(origin.offset(i, 0, 0), ModContent.FOSSIL_BONE.get().defaultBlockState(), 2);
                level.setBlock(origin.offset(0, 0, i), ModContent.FOSSIL_BONE.get().defaultBlockState(), 2);
            }
            int tip = random.nextBoolean() ? arm : -arm;
            BlockPos eye = random.nextBoolean() ? origin.offset(tip, 0, 0) : origin.offset(0, 0, tip);
            level.setBlock(eye, ModContent.DRAGONS_EYE_STONE.get().defaultBlockState(), 2);
            return true;
        }
    }

    /** A twenty-block pillar with a chest on top, and an Element Dragon guarding it. */
    public static class ElementPillar extends Feature<NoneFeatureConfiguration> {
        private static final ResourceKey<LootTable> CHEST = ResourceKey.create(Registries.LOOT_TABLE, OreSpawnDangerZone.id("chests/elementstuff"));
        private static final int WIDTH = 12;
        private static final int HEIGHT = 20;

        public ElementPillar() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos ground = context.origin();
            RandomSource random = context.random();
            for (int y = 0; y < HEIGHT; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        level.setBlock(ground.offset(dx, y, dz), Blocks.ANDESITE.defaultBlockState(), 2);
                    }
                }
            }
            BlockPos top = ground.above(HEIGHT);
            for (int dx = 0; dx < WIDTH; dx++) {
                for (int dz = 0; dz < WIDTH; dz++) {
                    BlockPos pos = top.offset(dx - WIDTH / 2, 0, dz - WIDTH / 2);
                    level.setBlock(pos, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                    if ((dx == 0 || dx == WIDTH - 1) && (dz == 0 || dz == WIDTH - 1)) {
                        level.setBlock(pos.above(), Blocks.SHROOMLIGHT.defaultBlockState(), 2);
                    }
                }
            }
            BlockPos chest = top.above();
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(CHEST, random.nextLong());
            }
            ElementDragon dragon = ModContent.ELEMENT_DRAGON.get().create(level.getLevel());
            if (dragon != null) {
                dragon.moveTo(top.getX() + 0.5, top.getY() + 4.0, top.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
                dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.STRUCTURE, null);
                dragon.setElement(random.nextInt(ElementDragon.ELEMENTS));
                dragon.guardHome(top);
                dragon.setPersistenceRequired();
                level.addFreshEntity(dragon);
            }
            return true;
        }
    }
}
