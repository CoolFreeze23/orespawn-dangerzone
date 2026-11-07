package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.item.DimensionStaffItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** OreSpawn's way of travel: right-click the ant and you're in its dimension. In its home dimension it sends you back. */
public class PortalAnt extends PathfinderMob {
    private final ResourceKey<Level> home;

    public PortalAnt(EntityType<? extends PortalAnt> type, Level level, ResourceKey<Level> home) {
        super(type, level);
        this.home = home;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && player.getItemInHand(hand).isEmpty()) {
            ResourceKey<Level> goal = level().dimension() == this.home ? Level.OVERWORLD : this.home;
            ServerLevel target = server.server.getLevel(goal);
            if (target != null) {
                DimensionStaffItem.jump(server, target);
                level().playSound(null, blockPosition(), SoundEvents.PORTAL_TRAVEL, getSoundSource(), 0.25f, 1.0f);
            }
            return InteractionResult.CONSUME;
        }
        return player.getItemInHand(hand).isEmpty() ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
    }
}
