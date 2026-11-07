package danger.orespawn.dangerzone.entity;

import danger.orespawn.MobStats;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.Kraken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Kraken's King. He fights as the port's Kraken does, grabbing what's below him and carrying it up, at DangerZone's
 * eight times its health and six and a half times its bite. The first hit brings the Young Adult Princess to his side.
 */
public class KrakenKing extends Kraken implements DzAnimated {
    private static final java.lang.reflect.Field LONG_ENOUGH = PrincessGrowth.field(Kraken.class, "longEnough");
    private static final int STAY = 3600;
    private static final List<String> SCENE = List.of("No!!! Stop!!! Don't hurt him!!!", "I love him!!!", "We are a family now.");

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean princessCame;
    private int scene = -1;
    private int sceneWait;

    public KrakenKing(EntityType<? extends KrakenKing> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        try {
            // The port's Kraken, badly hurt, calls ten more of its own type. Ten more Kings is not a fight.
            PrincessGrowth.field(Kraken.class, "callReinforcements").setInt(this, 1);
            // He has his own bar: a second, marked Kraken, is one too many.
            ((ServerBossEvent) PrincessGrowth.field(Kraken.class, "bossEvent").get(this)).setVisible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static AttributeSupplier.Builder createKingAttributes() {
        return Kraken.createAttributes()
                .add(Attributes.MAX_HEALTH, MobStats.KRAKEN.maxHealth() * 8.0)
                .add(Attributes.ATTACK_DAMAGE, MobStats.KRAKEN.attackDamage() * 6.4)
                .add(Attributes.ARMOR, 22.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.princessCame && level() instanceof ServerLevel server && source.getEntity() instanceof LivingEntity attacker) {
            this.princessCame = true;
            callPrincess(server, attacker);
        }
        return hurt;
    }

    /** She arrives between him and the attacker, and the scene begins. Public for the tests. */
    public void callPrincess(ServerLevel level, LivingEntity attacker) {
        PrincessYoungAdult princess = ModContent.PRINCESS_YOUNG_ADULT.get().create(level);
        if (princess != null) {
            princess.moveTo(getX(), getY() + 8.0, getZ(), 0.0f, 0.0f);
            princess.setPersistenceRequired();
            princess.setTarget(attacker);
            level.addFreshEntity(princess);
            this.scene = 0;
            this.sceneWait = 0;
        }
    }

    @Override
    protected void customServerAiStep() {
        // A wild Kraken gets bored after three minutes and climbs away for good. A King holds his castle.
        try {
            LONG_ENOUGH.setInt(this, STAY);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        super.customServerAiStep();
        this.bossBar.setProgress(getHealth() / getMaxHealth());
        if (this.scene >= 0 && this.scene < SCENE.size() && --this.sceneWait <= 0 && level() instanceof ServerLevel server) {
            server.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("entity.orespawn_dangerzone.princess_young_adult").append(": " + SCENE.get(this.scene)), false);
            this.scene++;
            this.sceneWait = 60;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PrincessCame", this.princessCame);
        tag.putInt("Scene", this.scene);
        tag.putInt("SceneWait", this.sceneWait);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.princessCame = tag.getBoolean("PrincessCame");
        this.scene = tag.contains("Scene") ? tag.getInt("Scene") : -1;
        this.sceneWait = tag.getInt("SceneWait");
    }

    @Override
    public boolean dzAttacking() {
        return getTarget() != null;
    }

    @Override
    public float getNewangle() {
        return 0.0f;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
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
