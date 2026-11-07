package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.entity.TowerFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Plants the flag on the block you use it on, which starts the event. */
public class TowerFlagItem extends Item {
    public TowerFlagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        TowerFlag flag = ModContent.TOWER_FLAG.get().create(server);
        if (flag == null) {
            return InteractionResult.FAIL;
        }
        Vec3 feet = Vec3.atBottomCenterOf(pos);
        flag.moveTo(feet.x, feet.y, feet.z, 0.0f, 0.0f);
        server.addFreshEntity(flag);
        ItemStack stack = context.getItemInHand();
        stack.consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
