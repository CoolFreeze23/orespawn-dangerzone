package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.Boyfriend;
import danger.orespawn.entity.Girlfriend;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * A daylight flyer that seeks out hurt players, Girlfriends and Boyfriends and heals them: DangerZone's mantis with a
 * negative bite, 15 of a 50-health player's life per sting, so 6 of a Minecraft player's 20, on a two-second beat.
 */
public class NurseBug extends PathfinderMob {
    private static final double SEARCH = 48.0;
    private static final double REACH = 4.0;

    public NurseBug(EntityType<? extends NurseBug> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 87.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, SEARCH);
    }

    public static boolean checkNurseSpawnRules(EntityType<NurseBug> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getLevel().isDay() && pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new TendGoal());
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.level().isNight() || super.removeWhenFarAway(distance);
    }

    /** Heals the patient once. Public for the tests. */
    public void sting(LivingEntity patient) {
        patient.heal(6.0f);
        patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true));
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getEyeY() + 0.3, patient.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
        }
    }

    public static boolean needsNurse(LivingEntity entity) {
        return entity.isAlive() && entity.getHealth() < entity.getMaxHealth()
                && (entity instanceof Player player && !player.isSpectator() || entity instanceof Girlfriend || entity instanceof Boyfriend);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_NURSE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_NURSE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orespawn", "alo_death"));
    }

    private final class TendGoal extends Goal {
        @Nullable
        private LivingEntity patient;
        private int beat;

        TendGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // canUse only runs on every other tick, offset by the entity's id: this window catches both parities.
            if (tickCount % 10 > 1) {
                return false;
            }
            List<LivingEntity> hurt = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SEARCH), NurseBug::needsNurse);
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : hurt) {
                double distance = distanceToSqr(candidate);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
            this.patient = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.patient != null && needsNurse(this.patient) && distanceToSqr(this.patient) < SEARCH * SEARCH * 2.0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            setAggressive(true);
            this.beat = 0;
        }

        @Override
        public void stop() {
            setAggressive(false);
            this.patient = null;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(this.patient, 30.0f, 30.0f);
            double reach = REACH + this.patient.getBbWidth() / 2.0 + getBbWidth() / 2.0;
            if (distanceToSqr(this.patient) > reach * reach) {
                getNavigation().moveTo(this.patient.getX(), this.patient.getY() + 2.0, this.patient.getZ(), 1.2);
                return;
            }
            getNavigation().stop();
            if (++this.beat >= 40) {
                this.beat = 0;
                sting(this.patient);
            }
        }
    }
}
