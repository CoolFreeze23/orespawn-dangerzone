package danger.orespawn.dangerzone.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** Placing a boss's helpers, the way DangerZone's creatures do: on the first free footing in a short column. */
public final class Summons {
    private Summons() {
    }

    @Nullable
    public static BlockPos footing(Level level, double x, double y, double z, int reach) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -reach; dy <= reach; dy++) {
            pos.set(x, y + dy, z);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return pos.immutable();
            }
        }
        return null;
    }

    public static int countNear(Entity center, double radius, Predicate<Entity> which) {
        AABB box = center.getBoundingBox().inflate(radius);
        return center.level().getEntities(center, box, which).size();
    }

    /** Spawns one helper at a footing near the point, aimed at the target. Returns false when nothing could stand there. */
    public static boolean spawnAt(ServerLevel level, EntityType<?> type, double x, double y, double z, @Nullable LivingEntity target) {
        BlockPos foot = footing(level, x, y, z, 5);
        if (foot == null) {
            return false;
        }
        Entity entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        if (!level.noCollision(entity)) {
            entity.discard();
            return false;
        }
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(foot), MobSpawnType.MOB_SUMMONED, null);
            if (target != null) {
                mob.setTarget(target);
            }
        }
        return level.addFreshEntity(entity);
    }
}
