package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.LaserBall;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A small attack craft: it flies at whatever it finds and fires a laser every half a second, trailing fire. */
public class UfoFighter extends Monster implements DzAnimated {
    private static final double SEARCH = 128.0;
    private float angle;
    private int reload;

    public UfoFighter(EntityType<? extends UfoFighter> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.FOLLOW_RANGE, SEARCH);
    }

    /**
     * Launched from 200 blocks up, well past the distance at which a creature far from every player is cleared away: it
     * would be gone the moment it was made, and the mothership would launch another, and another. A minute to come down.
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.tickCount > 1200;
    }

    @Override
    public void tick() {
        super.tick();
        if (isNoAi() || isDeadOrDying() || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (this.reload > 0) {
            this.reload--;
        }
        level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0.3, 0.1, 0.3, 0.0);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            target = level.getNearestPlayer(getX(), getY(), getZ(), SEARCH, p -> !((Player) p).isCreative() && !p.isSpectator());
            setTarget(target);
        }
        setAggressive(target != null);
        if (target == null) {
            setDeltaMovement(getDeltaMovement().scale(0.9));
            return;
        }
        Vec3 aim = target.getEyePosition().subtract(position());
        double distance = aim.length();
        // Closes in and holds about twelve blocks off, a little above.
        Vec3 want = target.getEyePosition().add(0.0, 6.0, 0.0).subtract(aim.normalize().scale(12.0)).subtract(position());
        double speed = Math.min(0.7, want.length());
        setDeltaMovement(want.length() < 0.5 ? Vec3.ZERO : want.normalize().scale(speed));
        this.hasImpulse = true;
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        if (distance < 32.0 && this.reload == 0 && hasLineOfSight(target)) {
            fire(level, aim);
        }
    }

    /** One laser shot along the aim. Public for the tests. */
    public void fire(ServerLevel level, Vec3 aim) {
        this.reload = 10;
        LaserBall shot = new LaserBall(level, this);
        shot.setPos(getX(), getY() - 0.5, getZ());
        shot.shoot(aim.x, aim.y, aim.z, 0.75f, 0.5f);
        level.addFreshEntity(shot);
        playSound(ModContent.SND_RAY.get(), 1.0f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.3f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public float getNewangle() {
        this.angle = (this.angle + 11.1f) % 360.0f;
        return (float) Math.toRadians(this.angle);
    }
}
