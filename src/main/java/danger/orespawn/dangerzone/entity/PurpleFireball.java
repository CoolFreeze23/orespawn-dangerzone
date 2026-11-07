package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Dargon's breath: it grows as it flies, goes through whatever it hits, and sets it alight. Dragons shrug it off. */
public class PurpleFireball extends ThrowableProjectile {
    private static final int LIFETIME = 30;

    public PurpleFireball(EntityType<? extends PurpleFireball> type, Level level) {
        super(type, level);
    }

    public PurpleFireball(Level level, LivingEntity shooter) {
        super(ModContent.PURPLE_FIREBALL.get(), shooter, level);
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
        if (level() instanceof ServerLevel level) {
            double spread = 0.1 + this.tickCount * 0.05;
            level.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 4, spread, spread, spread, 0.0);
            level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 1, spread, spread, spread, 0.0);
            if (this.tickCount > LIFETIME) {
                discard();
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof Dargon) && !(target instanceof ElementDragon) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level() instanceof ServerLevel && result.getEntity() instanceof LivingEntity target) {
            // DangerZone's 100 over the fireball's scale: closer is worse.
            float damage = 30.0f / (1.0f + this.tickCount * 0.1f);
            target.invulnerableTime = 0;
            target.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity owner ? owner : null), damage);
            target.igniteForSeconds(4.0f);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        // Straight on through entities; only the ground stops it.
        if (result.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) result);
        } else if (result instanceof BlockHitResult && !level().isClientSide) {
            discard();
        }
    }
}
