package danger.orespawn.dangerzone.item;

import danger.orespawn.dangerzone.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.Vec3;

/** Every hit blinks the wielder to a random safe spot nearby, facing the target. */
public class DragonsEyeSwordItem extends SwordItem {
    public DragonsEyeSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (attacker instanceof ServerPlayer player) {
            blink(player, target);
        }
        return result;
    }

    /** Ten tries, as in DangerZone, within the target's width plus five. Returns whether a jump happened. */
    public static boolean blink(ServerPlayer player, Entity target) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        int spread = (int) target.getBbWidth() + 5;
        for (int tries = 0; tries < 10; tries++) {
            int x = player.getBlockX() + random.nextInt(spread) - random.nextInt(spread);
            int z = player.getBlockZ() + random.nextInt(spread) - random.nextInt(spread);
            for (int dy = -7; dy <= 9; dy++) {
                BlockPos pos = new BlockPos(x, player.getBlockY() + dy, z);
                if (SafeSpots.canStand(level, player, pos)) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.8, 0.3, 0.01);
                    Vec3 feet = Vec3.atBottomCenterOf(pos);
                    Vec3 aim = target.getEyePosition().subtract(feet.add(0.0, player.getEyeHeight(), 0.0));
                    float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
                    float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z)));
                    player.teleportTo(level, feet.x, feet.y, feet.z, yaw, pitch);
                    level.playSound(null, pos, ModContent.SND_TELEPORT.get(), SoundSource.PLAYERS, 0.75f, 1.0f);
                    return true;
                }
            }
        }
        return false;
    }
}
