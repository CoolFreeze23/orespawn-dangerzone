package danger.orespawn.dangerzone.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** The Kraken King's guards, thick around his castle. They drift in close and whip you with their tentacles. */
public class SquidGuard extends DzFlyer implements DzAnimated {
    private int strike;

    public SquidGuard(EntityType<? extends SquidGuard> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DzFlyer.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ATTACK_DAMAGE, 22.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected double speed() {
        return 0.4;
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
        if (--this.strike <= 0 && distance < 4.0) {
            this.strike = 20;
            doHurtTarget(target);
        }
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
