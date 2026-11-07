package danger.orespawn.dangerzone.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** For creatures too wide for any path: straight at the target, or to a nearby spot when there is none. */
public class MarchGoal extends Goal {
    private final Mob mob;
    private final double keepOff;
    private Vec3 wander;
    private int wanderTime;

    public MarchGoal(Mob mob, double keepOff) {
        this.mob = mob;
        this.keepOff = keepOff;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (this.mob.distanceToSqr(target) > Math.pow(this.mob.getBbWidth() / 2.0 + this.keepOff, 2.0)) {
                this.mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
            }
            return;
        }
        if (this.wander == null || --this.wanderTime <= 0) {
            this.wanderTime = 80 + this.mob.getRandom().nextInt(120);
            this.wander = this.mob.position().add((this.mob.getRandom().nextDouble() - 0.5) * 32.0, 0.0, (this.mob.getRandom().nextDouble() - 0.5) * 32.0);
        }
        if (this.mob.getRandom().nextInt(3) != 0) {
            this.mob.getMoveControl().setWantedPosition(this.wander.x, this.wander.y, this.wander.z, 0.6);
        }
    }
}
