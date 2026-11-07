package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.ThePrinceTeen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;

/**
 * DangerZone's Teen Princess. She rides and fights as the port's Teen Prince does; what differs is how she grows: a
 * diamond block makes her a Young Adult, a diamond takes her back to a baby, and she never grows on her own.
 */
public class PrincessTeen extends ThePrinceTeen implements DzAnimated {
    /** The port's natural growth turns a Prince Teen into a Prince Adult: zeroing its kill count keeps her out of it. */
    private static final Field KILL_COUNT = PrincessGrowth.field(ThePrinceTeen.class, "killCount");

    private float spin;

    public PrincessTeen(EntityType<? extends PrincessTeen> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createPrincessAttributes() {
        return ThePrinceTeen.createAttributes()
                .add(Attributes.MAX_HEALTH, 1600.0)
                .add(Attributes.ATTACK_DAMAGE, 45.0)
                .add(Attributes.ARMOR, 16.0);
    }

    @Override
    protected void customServerAiStep() {
        PrincessGrowth.zero(KILL_COUNT, this);
        super.customServerAiStep();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean owner = isTame() && isOwnedBy(player);
        // The port's Teen Prince goes to whoever hands him a diamond block, even if he's someone else's. She doesn't.
        if (isTame() && !owner) {
            return InteractionResult.PASS;
        }
        if (owner && stack.is(Blocks.DIAMOND_BLOCK.asItem())) {
            return PrincessGrowth.become(this, ModContent.PRINCESS_YOUNG_ADULT.get(), player, stack);
        }
        if (owner && stack.is(Items.DIAMOND)) {
            return PrincessGrowth.become(this, PrincessGrowth.baby(), player, stack);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public float getNewangle() {
        this.spin = (this.spin + 11.1f) % 360.0f;
        return (float) Math.toRadians(this.spin);
    }

    @Override
    public boolean dzAttacking() {
        return getAttacking() != 0;
    }

    @Override
    public boolean dzFlying() {
        return getActivity() != 0;
    }

    @Override
    public boolean dzStaying() {
        return isOrderedToSit();
    }
}
