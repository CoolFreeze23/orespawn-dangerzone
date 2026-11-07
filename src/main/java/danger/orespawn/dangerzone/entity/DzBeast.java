package danger.orespawn.dangerzone.entity;

import danger.orespawn.entity.LaserBall;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * DangerZone's plain hostile creatures, which its engine runs from a few numbers each. So do these: health, damage and
 * sizes are the original's, its defence is read back to armour as the Tower Defense Flag's ladder reads it the other
 * way, and its movement speeds are brought to this game's scale. The lizards throw things as well.
 */
public class DzBeast extends Monster implements DzAnimated {
    public enum Shot { NONE, LAVA, LASER, LIGHTNING }

    /** byDay is DangerZone's daytime spawner, which doesn't need the dark. voice names its sound events. */
    public record Spec(String name, float width, float height, double health, double damage, double armor, double speed, double follow,
                       double reach, int xp, boolean byDay, boolean fireproof, Shot shot, boolean venom, String voice, boolean ambient) {
        Spec(String name, float width, float height, double health, double damage, double armor, double speed, double follow, double reach, int xp, boolean byDay, boolean ambient) {
            this(name, width, height, health, damage, armor, speed, follow, reach, xp, byDay, false, Shot.NONE, false, name, ambient);
        }

        static Spec lizard(String name, Shot shot) {
            return new Spec(name, 1.5f, 1.1f, 50.0, 10.0, 10.0, 0.23, 32.0, 3.5, 55, true, true, shot, false, "lizard", false);
        }
    }

    public static final Spec FLATTOP = new Spec("flattop", 2.28f, 0.95f, 125.0, 13.0, 14.0, 0.28, 32.0, 2.25, 409, true, true);
    public static final Spec HYENA = new Spec("hyena", 0.78f, 1.23f, 25.0, 3.0, 16.0, 0.28, 24.0, 1.5, 49, true, false);
    public static final Spec LAVA_LIZARD = Spec.lizard("lava_lizard", Shot.LAVA);
    public static final Spec LAZER_LIZARD = Spec.lizard("lazer_lizard", Shot.LASER);
    public static final Spec LIGHTNING_LIZARD = Spec.lizard("lightning_lizard", Shot.LIGHTNING);
    public static final Spec RAGING_BULL = new Spec("raging_bull", 1.33f, 3.33f, 150.0, 12.0, 14.0, 0.26, 20.0, 3.5, 199, true, false);
    public static final Spec RATTLESNAKE = new Spec("rattlesnake", 0.58f, 0.33f, 15.0, 3.0, 12.0, 0.23, 16.0, 1.5, 49, true, false, Shot.NONE, true, "rattlesnake", true);
    public static final Spec ROLLY_OUCHY = new Spec("rolly_ouchy", 0.85f, 1.9f, 20.0, 5.0, 10.0, 0.26, 9.0, 2.25, 29, true, true);
    public static final Spec SKINNY_THING = new Spec("skinny_thing", 0.85f, 2.9f, 25.0, 4.0, 0.0, 0.19, 16.0, 2.5, 29, false, true);
    public static final Spec V = new Spec("v", 1.25f, 1.9f, 40.0, 5.0, 10.0, 0.26, 22.0, 2.5, 59, false, true);
    public static final Spec WARTHOG = new Spec("warthog", 0.98f, 0.98f, 35.0, 5.0, 16.0, 0.28, 24.0, 1.5, 69, true, true);
    public static final Spec WOOLLY_MAMMOTH = new Spec("woolly_mammoth", 5.0f, 8.5f, 650.0, 50.0, 12.0, 0.2, 24.0, 4.5, 960, true, true);

    public static final List<Spec> ALL = List.of(FLATTOP, HYENA, LAVA_LIZARD, LAZER_LIZARD, LIGHTNING_LIZARD, RAGING_BULL, RATTLESNAKE,
            ROLLY_OUCHY, SKINNY_THING, V, WARTHOG, WOOLLY_MAMMOTH);

    private final Spec spec;
    private float spin;
    private float headSpin;

    public DzBeast(EntityType<? extends DzBeast> type, Level level, Spec spec) {
        super(type, level);
        this.spec = spec;
        this.xpReward = spec.xp();
    }

    public static AttributeSupplier.Builder attributes(Spec spec) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, spec.health())
                .add(Attributes.ATTACK_DAMAGE, spec.damage())
                .add(Attributes.ARMOR, spec.armor())
                .add(Attributes.MOVEMENT_SPEED, spec.speed())
                .add(Attributes.FOLLOW_RANGE, spec.follow())
                .add(Attributes.STEP_HEIGHT, spec.width() >= 2.0f ? 1.5 : 0.6);
    }

    public Spec spec() {
        return this.spec;
    }

    /** A monster's own check turns down anywhere bright, whatever its spawn placement says. DangerZone's day creatures are out in the sun. */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        return this.spec.byDay() || super.checkSpawnRules(level, reason);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, DzBeast.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** DangerZone's reach counts from the edge of the creature. Pathfinding stops the wide ones a stride short of it. */
    @Override
    protected AABB getAttackBoundingBox() {
        double reach = this.spec.reach() + Math.max(0.0, this.spec.width() - 1.0) * 0.5;
        return getBoundingBox().inflate(reach, 0.0, reach);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // A lizard's friends are other lizards of its kind.
        super.setTarget(target != null && target.getType() == getType() ? null : target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (this.spec.shot() != Shot.NONE && target != null && this.random.nextInt(40) == 0 && level() instanceof ServerLevel server
                && !isWithinMeleeAttackRange(target) && getSensing().hasLineOfSight(target)) {
            shoot(server, target);
        }
    }

    private void shoot(ServerLevel level, LivingEntity target) {
        Vec3 aim = target.getEyePosition().subtract(getEyePosition());
        if (this.spec.shot() == Shot.LASER) {
            LaserBall shot = new LaserBall(level, this);
            shot.setPos(getX(), getEyeY(), getZ());
            shot.setNoGravity(true);
            shot.shoot(aim.x, aim.y, aim.z, 1.0f, 1.0f);
            level.addFreshEntity(shot);
        } else {
            ElementShot shot = new ElementShot(level, this, this.spec.shot() == Shot.LAVA ? ElementShot.FIRE : ElementShot.LIGHTNING);
            shot.setPos(getX(), getEyeY(), getZ());
            shot.shoot(aim.x, aim.y, aim.z, 1.0f, 2.0f);
            level.addFreshEntity(shot);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && this.spec.venom() && target instanceof LivingEntity victim) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // The Lightning Lizard throws it, and stands in what it throws.
        return !(this.spec.shot() == Shot.LIGHTNING && source.is(DamageTypeTags.IS_LIGHTNING)) && super.hurt(source, amount);
    }

    private SoundEvent voice(String what) {
        return BuiltInRegistries.SOUND_EVENT.get(OreSpawnDangerZone.id(this.spec.voice() + "." + what));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.spec.ambient() ? voice("ambient") : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return voice("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return voice("death");
    }

    /** The original spins as far as it has moved, each time it's drawn. */
    @Override
    public float getNewangle() {
        this.spin = (this.spin + this.walkAnimation.speed() * 15.0f) % 360.0f;
        return (float) Math.toRadians(this.spin);
    }

    @Override
    public float getHeadNewangle() {
        this.headSpin = (this.headSpin + 15.0f) % 360.0f;
        return (float) Math.toRadians(this.headSpin);
    }

    @Override
    public boolean dzAttacking() {
        return isAggressive();
    }
}
