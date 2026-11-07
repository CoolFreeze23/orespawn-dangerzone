package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** What Element Dragons throw and elemental staffs fire. It does its element's thing where it lands. */
public class ElementShot extends ThrowableProjectile {
    public static final int EARTH = 0;
    public static final int WIND = 1;
    public static final int FIRE = 2;
    public static final int WATER = 3;
    public static final int LIGHTNING = 4;
    public static final int PLANT = 5;
    private static final ParticleOptions[] TRAILS = {ParticleTypes.MYCELIUM, ParticleTypes.CLOUD, ParticleTypes.FLAME,
            ParticleTypes.SPLASH, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.COMPOSTER};
    private static final float[] DAMAGE = {8.0f, 6.0f, 12.0f, 8.0f, 6.0f, 6.0f};

    private int element;

    public ElementShot(EntityType<? extends ElementShot> type, Level level) {
        super(type, level);
    }

    public ElementShot(Level level, LivingEntity shooter, int element) {
        super(ModContent.ELEMENT_SHOT.get(), shooter, level);
        this.element = element;
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
            level.sendParticles(TRAILS[this.element], getX(), getY(), getZ(), 2, 0.1, 0.1, 0.1, 0.0);
            if (this.tickCount > 100) {
                discard();
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !(target instanceof ElementDragon) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity target) {
            target.hurt(damageSources().thrown(this, getOwner()), DAMAGE[this.element]);
            switch (this.element) {
                case EARTH -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                case FIRE -> target.igniteForSeconds(5.0f);
                case WATER -> target.clearFire();
                case PLANT -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                default -> {
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            land(level, result.getLocation());
            discard();
        }
    }

    private void land(ServerLevel level, Vec3 at) {
        boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        BlockPos pos = BlockPos.containing(at);
        switch (this.element) {
            case WIND -> {
                level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(6.0), e -> e != getOwner() && !(e instanceof ElementDragon))) {
                    Vec3 away = near.position().subtract(at);
                    double strength = (6.0 - away.length()) * 0.4;
                    if (strength > 0.0) {
                        near.push(away.normalize().x * strength, 0.4 + strength * 0.2, away.normalize().z * strength);
                        near.hurtMarked = true;
                    }
                }
            }
            case FIRE -> level.explode(this, at.x, at.y, at.z, 1.5f, griefing, Level.ExplosionInteraction.MOB);
            case LIGHTNING -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(at.x, at.y, at.z);
                    level.addFreshEntity(bolt);
                }
            }
            case EARTH -> {
                if (griefing && level.getBlockState(pos).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
            case PLANT -> {
                if (griefing && level.getBlockState(pos).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.AZALEA_LEAVES.defaultBlockState());
                }
            }
            default -> level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 30, 0.6, 0.6, 0.6, 0.1);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Element", this.element);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.element = Math.floorMod(tag.getInt("Element"), TRAILS.length);
    }
}
