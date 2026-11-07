package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.ShrinkBolt;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A Freakin' Shrink Ray. 500 shots; recharged at an anvil with amethyst. */
public class ShrinkRayItem extends Item {
    public ShrinkRayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.blockPosition(), ModContent.SND_RAY.get(), SoundSource.PLAYERS, 0.5f,
                1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
        if (!level.isClientSide) {
            ShrinkBolt bolt = new ShrinkBolt(level, player);
            bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 0.1f);
            level.addFreshEntity(bolt);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.AMETHYST_SHARD);
    }
}
