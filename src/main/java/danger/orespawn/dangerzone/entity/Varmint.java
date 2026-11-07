package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The little dragons of the Lair, the Varmint and the half-sized Vermin. Wild, they hunt whatever they can see. Feed one
 * raw meat and it may come round, then it flies with you, fights what fights you and heals a little on each kill.
 */
public class Varmint extends TamableAnimal implements DzAnimated {
    private static final int TAME_ONE_IN = 3;
    private static final double WANDER = 16.0;
    private static final double TOO_FAR = 48.0;

    private final double speed;
    @Nullable
    private Vec3 wander;
    private int strike;

    public Varmint(EntityType<? extends Varmint> type, Level level, double speed) {
        super(type, level);
        this.speed = speed;
        setNoGravity(true);
    }

    public static Varmint varmint(EntityType<? extends Varmint> type, Level level) {
        Varmint made = new Varmint(type, level, 0.4);
        made.xpReward = 45;
        return made;
    }

    public static Varmint vermin(EntityType<? extends Varmint> type, Level level) {
        Varmint made = new Varmint(type, level, 0.36);
        made.xpReward = 15;
        return made;
    }

    public static AttributeSupplier.Builder varmintAttributes() {
        return attributes(40.0, 8.0, 9.0, 50.0);
    }

    public static AttributeSupplier.Builder verminAttributes() {
        return attributes(10.0, 2.0, 0.0, 40.0);
    }

    private static AttributeSupplier.Builder attributes(double health, double damage, double armor, double follow) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, follow);
    }

    @Override
    public void tick() {
        super.tick();
        if (isNoAi() || isDeadOrDying() || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (!isTame() && level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (isOrderedToSit()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        LivingEntity owner = getOwner();
        if (owner != null && owner.level() == level && distanceToSqr(owner) > TOO_FAR * TOO_FAR) {
            teleportTo(owner.getX(), owner.getY() + 2.0, owner.getZ());
        }
        LivingEntity target = findTarget(level, owner);
        setTarget(target);
        setAggressive(target != null);
        Vec3 want;
        if (target != null) {
            want = target.getEyePosition().subtract(position());
            if (--this.strike <= 0 && want.length() < 2.0 + getBbWidth() / 2.0 + target.getBbWidth() / 2.0) {
                this.strike = 20;
                if (doHurtTarget(target) && target.isDeadOrDying()) {
                    heal(1.0f);
                }
            }
        } else {
            if (this.wander == null || this.random.nextInt(50) == 1 || this.wander.distanceToSqr(position()) < 9.0) {
                Vec3 centre = owner != null && owner.level() == level ? owner.position().add(0.0, 2.0, 0.0) : position();
                this.wander = centre.add((this.random.nextDouble() - 0.5) * WANDER * 2.0, this.random.nextDouble() * 6.0 - 2.0, (this.random.nextDouble() - 0.5) * WANDER * 2.0);
            }
            want = this.wander.subtract(position());
        }
        double length = want.length();
        double pace = owner != null && distanceTo(owner) > 20.0 ? this.speed * 1.75 : this.speed;
        setDeltaMovement(length < 0.5 ? Vec3.ZERO : want.scale(Math.min(pace, length) / length));
        this.hasImpulse = true;
        if (length > 0.1) {
            float yaw = (float) Math.toDegrees(Math.atan2(-want.x, want.z));
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel level, @Nullable LivingEntity owner) {
        LivingEntity current = getTarget();
        boolean keep = current != null && current.isAlive() && !(current instanceof Player player && (player.isCreative() || player.isSpectator()));
        if (keep && (owner == null || current != owner)) {
            return current;
        }
        if (owner == null) {
            return level.getNearestPlayer(getX(), getY(), getZ(), getAttributeValue(Attributes.FOLLOW_RANGE), p -> !((Player) p).isCreative() && !p.isSpectator());
        }
        // Tame: whatever hurt its owner, whatever its owner hurt, and then any monster close by.
        LivingEntity enemy = owner.getLastHurtByMob();
        if (enemy == null || !enemy.isAlive() || enemy == this) {
            enemy = owner.getLastHurtMob();
        }
        if (enemy != null && enemy.isAlive() && enemy != this && !isAlliedTo(enemy) && !(enemy instanceof TamableAnimal pet && pet.isOwnedBy(owner))) {
            return enemy;
        }
        return level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16.0), mob -> mob instanceof Enemy && mob.isAlive()).stream().findFirst().orElse(null);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemTags.MEAT) && (!isTame() || isOwnedBy(player))) {
            if (!level().isClientSide) {
                if (isTame()) {
                    heal(10.0f);
                } else if (this.random.nextInt(TAME_ONE_IN) == 0) {
                    tame(player);
                    setTarget(null);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            stack.consume(1, player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isOwnedBy(player) && stack.isEmpty()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            setOrderedToSit(false);
            // Its owner's stray swing doesn't turn it on them.
            if (source.getEntity() instanceof LivingEntity attacker && !isOwnedBy(attacker)) {
                setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isTame();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModContent.SND_VARMINT_AMBIENT.get();
    }

    @Override
    protected float getSoundVolume() {
        return isTame() ? 0.25f : 0.5f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.SND_VARMINT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.SND_VARMINT_DEATH.get();
    }

    @Override
    public boolean dzAttacking() {
        return isAggressive();
    }

    @Override
    public boolean dzStaying() {
        return isOrderedToSit();
    }

    @Override
    public float getNewangle() {
        return 0.0f;
    }
}
