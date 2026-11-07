package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.world.DzPlaces;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** An invisible countdown left behind by a death in the Kraken King's chain, as DangerZone does it. */
public class DelayedEvent extends Entity {
    public static final int BABY_SITTER = 0;
    public static final int WARP = 1;
    private static final int DELAY = 150;

    private int kind;
    private int count;
    @Nullable
    private UUID victim;

    public DelayedEvent(EntityType<? extends DelayedEvent> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setInvisible(true);
    }

    public static void schedule(ServerLevel level, Vec3 at, int kind, @Nullable LivingEntity victim) {
        DelayedEvent event = ModContent.DELAYED_EVENT.get().create(level);
        if (event != null) {
            event.setPos(at);
            event.kind = kind;
            event.victim = victim == null ? null : victim.getUUID();
            level.addFreshEntity(event);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && ++this.count > DELAY) {
            fire(level);
            discard();
        }
    }

    /** Public for the tests. */
    public void fire(ServerLevel level) {
        if (this.kind == WARP) {
            ServerPlayer player = this.victim == null ? null : level.getServer().getPlayerList().getPlayer(this.victim);
            if (player != null && player.isAlive()) {
                DzPlaces.warpToRealm(player);
            }
            return;
        }
        LivingEntity target = this.victim != null && level.getEntity(this.victim) instanceof LivingEntity living ? living : null;
        for (int tries = 0; tries < 20; tries++) {
            double x = getX() + this.random.nextInt(10) - this.random.nextInt(10);
            double z = getZ() + this.random.nextInt(10) - this.random.nextInt(10);
            if (Summons.spawnAt(level, ModContent.BABY_SITTER.get(), x, getY(), z, target)) {
                return;
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.kind = tag.getInt("Kind");
        this.count = tag.getInt("Count");
        if (tag.hasUUID("Victim")) {
            this.victim = tag.getUUID("Victim");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", this.kind);
        tag.putInt("Count", this.count);
        if (this.victim != null) {
            tag.putUUID("Victim", this.victim);
        }
    }
}
