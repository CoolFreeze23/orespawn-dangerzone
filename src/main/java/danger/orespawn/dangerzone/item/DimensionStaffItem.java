package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Walks the world's dimensions in a ring at the same coordinates: use for the next one, sneak and use for the one
 * before. DangerZone drops you wherever that is. Here you land on the nearest footing in that column, and on a small
 * platform if the column has none, since several of the port's dimensions are mostly sky.
 */
public class DimensionStaffItem extends Item {
    private static final int COOLDOWN = 40;

    public DimensionStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel target = neighbour(serverPlayer.server, serverPlayer.serverLevel().dimension(), !player.isShiftKeyDown());
        if (target == null) {
            return InteractionResultHolder.fail(stack);
        }
        jump(serverPlayer, target);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.consume(stack);
    }

    /** The next or previous dimension in the ring, or null when the ring has only this one. */
    @Nullable
    public static ServerLevel neighbour(MinecraftServer server, ResourceKey<Level> from, boolean forward) {
        List<? extends String> denied = DzConfig.STAFF_DENYLIST.get();
        List<ResourceKey<Level>> ring = server.levelKeys().stream()
                .filter(key -> key.equals(from) || !denied.contains(key.location().toString()))
                .sorted(Comparator.comparing(key -> key.location().toString()))
                .toList();
        if (ring.size() < 2) {
            return null;
        }
        int here = ring.indexOf(from);
        int next = Math.floorMod(here + (forward ? 1 : -1), ring.size());
        return server.getLevel(ring.get(next));
    }

    /** Sends the player across and returns where they stand. */
    public static BlockPos jump(ServerPlayer player, ServerLevel target) {
        ServerLevel origin = player.serverLevel();
        origin.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.8, 0.3, 0.01);
        origin.playSound(null, player.blockPosition(), ModContent.SND_TELEPORT.get(), SoundSource.PLAYERS, 0.75f, 1.0f);

        int x = player.getBlockX();
        int z = player.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos stand = SafeSpots.standNear(target, player, x, player.getBlockY(), z);
        if (stand == null) {
            stand = platform(target, x, player.getBlockY(), z);
        }
        player.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(stand), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        target.playSound(null, stand, ModContent.SND_TELEPORT.get(), SoundSource.PLAYERS, 0.75f, 1.0f);
        return stand;
    }

    private static BlockPos platform(ServerLevel level, int x, int y, int z) {
        int floor = level.getMinBuildHeight() + 2;
        int ceiling = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 4;
        int top = Math.max(floor, Math.min(ceiling, y));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(x + dx, top - 1, z + dz), Blocks.OBSIDIAN.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(x + dx, top, z + dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(x + dx, top + 1, z + dz), Blocks.AIR.defaultBlockState());
            }
        }
        return new BlockPos(x, top, z);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.orespawn_dangerzone.dimension_staff.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
