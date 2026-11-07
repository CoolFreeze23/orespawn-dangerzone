package danger.orespawn.dangerzone.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Where an entity can stand after a jump: on something solid, in free air, out of any fluid. */
final class SafeSpots {
    private SafeSpots() {
    }

    /** The nearest place to stand in the column at x, z, searching out from y. */
    @Nullable
    static BlockPos standNear(Level level, Entity entity, int x, int y, int z) {
        int floor = level.getMinBuildHeight() + 1;
        int ceiling = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 2;
        int start = Math.max(floor, Math.min(ceiling, y));
        for (int d = 0; d <= ceiling - floor; d++) {
            for (int candidate : new int[]{start - d, start + d}) {
                if (candidate >= floor && candidate <= ceiling) {
                    BlockPos pos = new BlockPos(x, candidate, z);
                    if (canStand(level, entity, pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    static boolean canStand(Level level, Entity entity, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        Vec3 feet = Vec3.atBottomCenterOf(pos);
        AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(feet);
        if (!level.noCollision(entity, box)) {
            return false;
        }
        return !level.containsAnyLiquid(box);
    }
}
