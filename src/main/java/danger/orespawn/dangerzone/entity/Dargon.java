package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.ThePrinceAdult;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

/**
 * The beast who rules them all. He flies and carries a rider as the port's Adult Prince does, and breathes volleys of
 * purple fireballs. Bring him under a quarter of his health and an empty hand tames him. He never turns into a King.
 */
public class Dargon extends ThePrinceAdult implements DzAnimated {
    private static final Field GROW_COUNTER = PrincessGrowth.field(ThePrinceAdult.class, "growCounter");
    private static final double SIGHT = 64.0;
    private static final int MAX_VOLLEY = 15;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int breath;
    private float spin;

    public Dargon(EntityType<? extends Dargon> type, Level level) {
        super(type, level);
        this.xpReward = 20000;
    }

    public static AttributeSupplier.Builder createDargonAttributes() {
        return ThePrinceAdult.createAttributes()
                .add(Attributes.MAX_HEALTH, 7000.0)
                .add(Attributes.ATTACK_DAMAGE, 200.0)
                .add(Attributes.ARMOR, 22.0)
                .add(Attributes.FOLLOW_RANGE, 120.0);
    }

    @Override
    protected void customServerAiStep() {
        PrincessGrowth.zero(GROW_COUNTER, this);
        super.customServerAiStep();
        this.bossBar.setProgress(getHealth() / getMaxHealth());
        if (isTame() || !(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = getTarget();
        if ((target == null || !target.isAlive()) && this.tickCount % 20 == 0) {
            target = level.getNearestPlayer(getX(), getY(), getZ(), SIGHT, p -> !((Player) p).isCreative() && !p.isSpectator());
            setTarget(target);
        }
        // The port's Adult Prince only takes off when something hits him. Dargon comes for you.
        if (target != null && getActivity() == 0) {
            setActivity(1);
        }
        if (target != null && --this.breath <= 0 && distanceToSqr(target) < SIGHT * SIGHT) {
            this.breath = 60 + this.random.nextInt(40);
            volley(level, target);
        }
    }

    /** A burst of 6 to 15 fireballs. Returns how many. Public for the tests. */
    public int volley(ServerLevel level, LivingEntity target) {
        int shots = 6 + this.random.nextInt(MAX_VOLLEY - 5);
        Vec3 aim = target.getEyePosition().subtract(getX(), getY(0.6), getZ());
        for (int i = 0; i < shots; i++) {
            PurpleFireball ball = new PurpleFireball(level, this);
            ball.setPos(getX(), getY(0.6), getZ());
            ball.shoot(aim.x, aim.y, aim.z, 2.0f, 8.0f);
            level.addFreshEntity(ball);
        }
        return shots;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isTame()) {
            if (player.getItemInHand(hand).isEmpty() && getHealth() / getMaxHealth() < 0.25f) {
                if (!level().isClientSide) {
                    tame(player);
                    setTarget(null);
                    setPersistenceRequired();
                    level().broadcastEntityEvent(this, (byte) 7);
                    // As the port's mount does: awake and in flight.
                    setOrderedToSit(false);
                    setActivity(1);
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }
        // The port's Adult Prince goes back to a Teen Prince for a diamond. Dargon doesn't.
        if (player.getItemInHand(hand).is(Items.DIAMOND)) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public float getNewangle() {
        this.spin = (this.spin + 11.1f) % 360.0f;
        return (float) Math.toRadians(this.spin);
    }

    @Override
    public boolean dzAttacking() {
        return getAttacking() != 0 || getTarget() != null;
    }

    @Override
    public boolean dzFlying() {
        return getActivity() != 0;
    }

    @Override
    public boolean dzStaying() {
        return isOrderedToSit();
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
