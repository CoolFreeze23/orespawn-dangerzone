package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.ElementShot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A staff of one element: 100 shots of what its dragon throws. Recharge it at an anvil with a Dragon's Eye. */
public class ElementStaffItem extends Item {
    private final int element;

    public ElementStaffItem(int element, Properties properties) {
        super(properties);
        this.element = element;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f,
                1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
        if (!level.isClientSide) {
            ElementShot shot = new ElementShot(level, player, this.element);
            shot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 0.5f);
            level.addFreshEntity(shot);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, 12);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModContent.DRAGONS_EYE.get());
    }
}
