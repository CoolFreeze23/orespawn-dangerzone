package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.LaserBall;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** The UFO's foot soldiers: half carry ray guns, half bows. */
public class Martian extends Monster implements RangedAttackMob {
    private boolean rayGun;
    private boolean chosen;

    public Martian(EntityType<? extends Martian> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 50.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 30, 40, 20.0f));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Martian.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean hasRayGun() {
        if (!this.chosen) {
            this.chosen = true;
            this.rayGun = this.random.nextBoolean();
        }
        return this.rayGun;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof Martian || target instanceof UfoCarrier || target instanceof Ufo || target instanceof UfoFighter ? null : target);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getEyeY();
        double dz = target.getZ() - getZ();
        if (hasRayGun()) {
            LaserBall shot = new LaserBall(level, this);
            shot.setPos(getX(), getEyeY(), getZ());
            // A laser goes straight: with the shot's gravity it hit the ground well short of anyone at range.
            shot.setNoGravity(true);
            shot.shoot(dx, dy, dz, 1.0f, 1.0f);
            level.addFreshEntity(shot);
            playSound(ModContent.SND_RAY.get(), 0.8f, 1.0f);
        } else {
            Arrow arrow = new Arrow(level, this, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            // As a skeleton aims: at the chest, plus enough for the drop.
            double chest = target.getY(1.0 / 3.0) - arrow.getY();
            arrow.shoot(dx, chest + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6f, 4.0f);
            level.addFreshEntity(arrow);
            playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT, 1.0f, 1.0f);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_MARTIAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_MARTIAN_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RayGun", hasRayGun());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.rayGun = tag.getBoolean("RayGun");
        this.chosen = true;
    }
}
