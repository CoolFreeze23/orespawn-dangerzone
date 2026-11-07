package danger.orespawn.dangerzone.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** A monster that steers itself through the air: to a target at a set distance, or around its home. */
public abstract class DzFlyer extends Monster {
    @Nullable
    private BlockPos home;
    private Vec3 wander;

    protected DzFlyer(EntityType<? extends DzFlyer> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** Guard mode: it stays near this spot and won't chase far from it. */
    public void guardHome(BlockPos home) {
        this.home = home;
    }

    protected abstract double speed();

    protected abstract double search();

    protected abstract double holdOff();

    protected void attack(ServerLevel level, LivingEntity target, double distance) {
    }

    @Override
    public void tick() {
        super.tick();
        if (isNoAi() || isDeadOrDying() || !(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            target = level.getNearestPlayer(getX(), getY(), getZ(), search(), p -> !((Player) p).isCreative() && !p.isSpectator());
            setTarget(target);
        }
        if (target != null && this.home != null && !this.home.closerThan(target.blockPosition(), search() * 2.0)) {
            setTarget(null);
            target = null;
        }
        setAggressive(target != null);
        Vec3 want;
        if (target != null) {
            Vec3 aim = target.getEyePosition().subtract(position());
            want = target.getEyePosition().add(0.0, holdOff() * 0.4, 0.0).subtract(aim.normalize().scale(holdOff())).subtract(position());
            face(aim);
            attack(level, target, aim.length());
        } else {
            if (this.wander == null || this.random.nextInt(60) == 1) {
                Vec3 centre = this.home != null ? Vec3.atCenterOf(this.home).add(0.0, 8.0, 0.0) : position();
                this.wander = centre.add((this.random.nextDouble() - 0.5) * 32.0, (this.random.nextDouble() - 0.5) * 12.0, (this.random.nextDouble() - 0.5) * 32.0);
            }
            want = this.wander.subtract(position());
            face(want);
        }
        double length = want.length();
        double speed = target != null ? speed() : speed() * 0.5;
        setDeltaMovement(length < 0.5 ? Vec3.ZERO : want.scale(Math.min(speed, length) / length));
        this.hasImpulse = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.home != null) {
            tag.putLong("Home", this.home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) {
            this.home = BlockPos.of(tag.getLong("Home"));
        }
    }

    private void face(Vec3 direction) {
        if (direction.lengthSqr() > 0.01) {
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
