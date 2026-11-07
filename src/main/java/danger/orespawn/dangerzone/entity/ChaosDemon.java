package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Kraken and the Dragon's child, and the first step to the Kraken King: it teleports to whoever hits it, keeps
 * changing shape, plants explosives, and when it dies the Baby Sitter comes for its killer. Lives in Chaos.
 */
public class ChaosDemon extends Monster {
    private final float[] faces = new float[6];
    private int body;
    private int feet;
    private int tail;

    public ChaosDemon(EntityType<? extends ChaosDemon> type, Level level) {
        super(type, level);
        this.xpReward = 1999;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 42.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ChaosDemon.class, BabySitter.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** A monster's own check turns down anywhere bright, whatever its spawn placement says. This one doesn't mind the sun. */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < this.faces.length; i++) {
                this.faces[i] += 2.0f + i;
            }
            return;
        }
        if (this.random.nextInt(200) == 1 && onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, 0.6, 0.0));
            this.hasImpulse = true;
        }
        if (getTarget() != null && level() instanceof ServerLevel server && server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            if (this.random.nextInt(600) == 1) {
                PrimedTnt tnt = new PrimedTnt(server, getX(), getY() + 1.0, getZ(), this);
                tnt.setFuse(60);
                server.addFreshEntity(tnt);
            }
            if (this.random.nextInt(200) == 1) {
                BlockPos at = blockPosition().offset(this.random.nextInt(5) - 2, 2, this.random.nextInt(5) - 2);
                if (server.getBlockState(at).isAir()) {
                    server.setBlockAndUpdate(at, this.random.nextBoolean() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && source.getEntity() instanceof LivingEntity attacker) {
            teleportToward(this, attacker);
        }
        return hurt;
    }

    /** Within ten blocks of the attacker, onto a spot it fits. Shared with the Baby Sitter. */
    static void teleportToward(Monster self, LivingEntity attacker) {
        for (int tries = 0; tries < 10; tries++) {
            double x = attacker.getX() + self.getRandom().nextInt(10) - self.getRandom().nextInt(10);
            double z = attacker.getZ() + self.getRandom().nextInt(10) - self.getRandom().nextInt(10);
            for (int dy = -4; dy <= 5; dy++) {
                BlockPos pos = BlockPos.containing(x, attacker.getY() + dy, z);
                if (self.level().getBlockState(pos.below()).isSolid()
                        && self.level().noCollision(self, self.getDimensions(self.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(pos)))) {
                    self.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    return;
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // Its killer, by name: a Baby Sitter is on her way.
        if (level() instanceof ServerLevel server && source.getEntity() instanceof LivingEntity killer) {
            DelayedEvent.schedule(server, position(), DelayedEvent.BABY_SITTER, killer);
        }
    }

    /** Where one of the six faces inside the body has floated to, from -1 to 1. */
    public float getFacePos(int which) {
        return which >= 0 && which <= 5 ? (float) Math.sin(Math.toRadians(this.faces[which])) : 0.0f;
    }

    public int getbody() {
        if (this.random.nextInt(150) == 1) {
            this.body = this.random.nextInt(3);
        }
        return this.body;
    }

    public int getfeet() {
        if (this.random.nextInt(150) == 1) {
            this.feet = this.random.nextInt(3);
        }
        return this.feet;
    }

    public int gettail() {
        if (this.random.nextInt(150) == 1) {
            this.tail = this.random.nextInt(2);
        }
        return this.tail;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target instanceof BabySitter || target instanceof ChaosDemon ? null : target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orespawn", "alo_living"));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orespawn", "alo_hurt"));
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("orespawn", "alo_death"));
    }
}
