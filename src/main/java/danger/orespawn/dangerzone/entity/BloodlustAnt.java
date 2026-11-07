package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** The Ant Queen's soldier. It takes up her fight and never hers against it. */
public class BloodlustAnt extends Monster {
    private static final double EXTRA_REACH = 2.0;

    public BloodlustAnt(EntityType<? extends BloodlustAnt> type, Level level) {
        super(type, level);
        this.xpReward = 24;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** Pathfinding stops something this wide a stride short of its target, where a plain reach never gets there. */
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(EXTRA_REACH, 0.0, EXTRA_REACH);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, AntQueen.class, BloodlustAnt.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof AntQueen || target instanceof BloodlustAnt ? null : target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() == null && this.tickCount % 15 == 0) {
            this.level().getEntitiesOfClass(AntQueen.class, getBoundingBox().inflate(64.0), queen -> queen.getTarget() != null)
                    .stream().findFirst().ifPresent(queen -> setTarget(queen.getTarget()));
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            playSound(ModContent.SND_BLOODLUST_ATTACK.get(), 1.0f, 1.0f);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_BLOODLUST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_BLOODLUST_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_BLOODLUST_AMBIENT.get();
    }
}
