package danger.orespawn.dangerzone.entity;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.entity.ThePrinceAdult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;

/**
 * DangerZone's Young Adult Princess, her final form. She rides and fights as the port's Adult Prince does, a diamond
 * takes her back to a Teen, and unlike the Prince she never becomes a King.
 */
public class PrincessYoungAdult extends ThePrinceAdult implements DzAnimated {
    private static final Field GROW_COUNTER = PrincessGrowth.field(ThePrinceAdult.class, "growCounter");

    private float spin;

    public PrincessYoungAdult(EntityType<? extends PrincessYoungAdult> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createPrincessAttributes() {
        return ThePrinceAdult.createAttributes()
                .add(Attributes.MAX_HEALTH, 3000.0)
                .add(Attributes.ATTACK_DAMAGE, 105.0)
                .add(Attributes.ARMOR, 20.0);
    }

    @Override
    protected void customServerAiStep() {
        PrincessGrowth.zero(GROW_COUNTER, this);
        super.customServerAiStep();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTame() && isOwnedBy(player) && stack.is(Items.DIAMOND)) {
            return PrincessGrowth.become(this, ModContent.PRINCESS_TEEN.get(), player, stack);
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
