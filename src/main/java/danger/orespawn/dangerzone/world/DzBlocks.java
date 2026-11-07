package danger.orespawn.dangerzone.world;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The two blocks that do something. */
public final class DzBlocks {
    private DzBlocks() {
    }

    /** Lit in the Dragon's Lair, on the middle of nine Dragon's Eye Stones, it opens the way to Dargon. */
    public static class ExtremeTorch extends TorchBlock {
        public ExtremeTorch(Properties properties) {
            super(net.minecraft.core.particles.ParticleTypes.FLAME, properties);
        }

        public static boolean padReady(Level level, BlockPos pos) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!level.getBlockState(pos.offset(dx, -1, dz)).is(ModContent.DRAGONS_EYE_STONE.get())) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            super.onPlace(state, level, pos, old, moved);
            if (level instanceof ServerLevel server && level.dimension() == DzPlaces.DRAGONS_LAIR && padReady(level, pos)) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 1.0, 1.0, 1.0, 0.02);
                server.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 0.6f);
                DzPlaces.nextTick(() -> DzPlaces.startLair(server, pos));
            }
        }
    }

    /** Waits in the Kraken King's castle. It wakes him when its moment comes round, or as soon as it's broken. */
    public static class KingSpawner extends Block {
        public KingSpawner(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(5) == 0) {
                level.removeBlock(pos, false);
                wake(level, pos);
            }
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
            super.onRemove(state, level, pos, next, moved);
            if (level instanceof ServerLevel server && !state.is(next.getBlock()) && !next.is(Blocks.AIR)) {
                wake(server, pos);
            }
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
            if (level instanceof ServerLevel server) {
                wake(server, pos);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }

        /** An explosion is a kind of breaking too. */
        @Override
        public void wasExploded(Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
            if (level instanceof ServerLevel server) {
                wake(server, pos);
            }
        }

        public static void wake(ServerLevel level, BlockPos pos) {
            Mob king = ModContent.KRAKEN_KING.get().create(level);
            if (king != null) {
                king.moveTo(pos.getX() + 0.5, pos.getY() + 12.0, pos.getZ() + 0.5, 0.0f, 0.0f);
                king.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
                king.setPersistenceRequired();
                level.addFreshEntity(king);
            }
        }
    }
}
