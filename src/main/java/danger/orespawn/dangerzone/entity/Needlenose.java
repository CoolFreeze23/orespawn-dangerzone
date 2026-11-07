package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.Nastysaurus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** The pack hunter of the Nastysaurus Prime. It takes up whatever fight a Prime nearby is having. */
public class Needlenose extends Monster {
    public Needlenose(EntityType<? extends Needlenose> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, NastysaurusPrime.class, Needlenose.class, Nastysaurus.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof Nastysaurus || target instanceof Needlenose ? null : target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() == null && this.tickCount % 15 == 0) {
            this.level().getEntitiesOfClass(NastysaurusPrime.class, getBoundingBox().inflate(64.0),
                            prime -> prime.getTarget() != null)
                    .stream().findFirst().ifPresent(prime -> setTarget(prime.getTarget()));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_NEEDLENOSE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_NEEDLENOSE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_NEEDLENOSE_DEATH.get();
    }
}
