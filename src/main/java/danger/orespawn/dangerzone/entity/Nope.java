package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.Acid;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * NOPE. A nine-block bug that spits acid, sometimes spawning a Baby Nope with each shot and sometimes leaping at you.
 * DangerZone's 1200 health and 85 bite are kept in proportion to the port's numbers.
 */
public class Nope extends Monster {
    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private int spitCooldown;

    public Nope(EntityType<? extends Nope> type, Level level) {
        super(type, level);
        this.xpReward = 1600;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ATTACK_DAMAGE, 54.0)
                .add(Attributes.ARMOR, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 3.0);
    }

    /** A monster's own check turns down anywhere bright, whatever its spawn placement says. This one doesn't mind the sun. */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MarchGoal(this, 6.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, BabyNope.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof BabyNope ? null : target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        setAggressive(target != null);
        if (this.spitCooldown > 0) {
            this.spitCooldown--;
        }
        if (target == null || !target.isAlive() || !(level() instanceof ServerLevel level)) {
            return;
        }
        double reach = 8.0 + getBbWidth() / 2.0 + target.getBbWidth() / 2.0;
        double distance = distanceToSqr(target);
        if (this.spitCooldown == 0 && distance <= 32.0 * 32.0 && hasLineOfSight(target)) {
            this.spitCooldown = 30;
            spit(level, target);
            if (distance <= reach * reach) {
                playSound(ModContent.SND_QUEEN_ATTACK.get(), 2.0f, 1.0f);
                doHurtTarget(target);
            }
        }
    }

    /** One shot, with its chances of a baby and a leap. Public for the tests. */
    public void spit(ServerLevel level, LivingEntity victim) {
        Acid acid = new Acid(level, this);
        Vec3 aim = victim.getEyePosition().subtract(getX(), getY(0.6), getZ());
        Vec3 flat = aim.multiply(1.0, 0.0, 1.0).normalize().scale(getBbWidth() / 2.0);
        acid.setPos(getX() + flat.x, getY(0.6), getZ() + flat.z);
        acid.shoot(aim.x, aim.y, aim.z, 1.5f, 2.0f);
        level.addFreshEntity(acid);
        playSound(SoundEvents.ARROW_SHOOT, 0.5f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.3f);
        if (this.random.nextInt(3) != 1) {
            return;
        }
        callBaby(level, victim);
        if (this.random.nextInt(8) == 0 && onGround()) {
            Vec3 leap = victim.position().subtract(position()).normalize().scale(1.5);
            setDeltaMovement(leap.x, 0.6, leap.z);
            this.hasImpulse = true;
        }
    }

    /** Calls one Baby Nope toward the victim, up to the configured limit. */
    public boolean callBaby(ServerLevel level, LivingEntity victim) {
        if (Summons.countNear(this, 32.0, e -> e instanceof BabyNope) >= DzConfig.NOPE_MAX_BABIES.get()) {
            return false;
        }
        double x = (getX() + victim.getX()) / 2.0 + (this.random.nextFloat() - this.random.nextFloat()) * 5.0;
        double y = (getY() + victim.getY()) / 2.0;
        double z = (getZ() + victim.getZ()) / 2.0 + (this.random.nextFloat() - this.random.nextFloat()) * 5.0;
        return Summons.spawnAt(level, ModContent.BABY_NOPE.get(), x, y, z, victim);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_NOPE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_QUEEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_QUEEN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0f;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }
}
