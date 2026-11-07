package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** The Shrink Ray's sparkling shot: straight, weightless, and gone when it touches anything. */
public class ShrinkBolt extends ThrowableProjectile {
    private static final int LIFETIME = 60;

    public ShrinkBolt(EntityType<? extends ShrinkBolt> type, Level level) {
        super(type, level);
    }

    public ShrinkBolt(Level level, LivingEntity shooter) {
        super(ModContent.SHRINK_BOLT.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            if (this.tickCount > LIFETIME) {
                discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
            shrink(target);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            discard();
        }
    }

    /** Shrinks what can be shrunk. Returns false for the bosses, which never are. */
    public static boolean shrink(LivingEntity target) {
        if (target.getType().is(ModContent.SHRINK_IMMUNE)) {
            return false;
        }
        int duration = target instanceof Player ? DzConfig.SHRINK_PLAYER_SECONDS.get() * 20 : MobEffectInstance.INFINITE_DURATION;
        return target.addEffect(new MobEffectInstance(ModContent.SHRUNK, duration, 0, false, false, true));
    }
}
