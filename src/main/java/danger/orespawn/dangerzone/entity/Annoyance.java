package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Exactly what it says: a small, fast flyer of the Dragon's Lair that won't leave you alone. */
public class Annoyance extends DzFlyer implements DzAnimated {
    public Annoyance(EntityType<? extends Annoyance> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DzFlyer.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected double speed() {
        return 0.38;
    }

    @Override
    protected double search() {
        return 32.0;
    }

    @Override
    protected double holdOff() {
        return 1.0;
    }

    @Override
    protected void attack(ServerLevel level, LivingEntity target, double distance) {
        if (distance < 3.0 && this.random.nextInt(8) == 1) {
            doHurtTarget(target);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_ANNOYANCE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_ANNOYANCE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_ANNOYANCE_DEATH.get();
    }

    @Override
    public boolean dzAttacking() {
        return isAggressive();
    }

    @Override
    public float getNewangle() {
        return 0.0f;
    }
}
