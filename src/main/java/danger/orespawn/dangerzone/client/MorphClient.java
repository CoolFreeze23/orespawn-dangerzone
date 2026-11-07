package danger.orespawn.dangerzone.client;

import danger.orespawn.dangerzone.OreSpawnDangerZone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Draws a morphed player as the creature, through the creature's own renderer, walking as the player walks. */
@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID, value = Dist.CLIENT)
public final class MorphClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("OreSpawnDangerZone");
    private static final Map<Integer, String> FORMS = new HashMap<>();
    private static final Map<Integer, LivingEntity> BODIES = new HashMap<>();
    private static final Set<String> BROKEN = new HashSet<>();

    private MorphClient() {
    }

    public static void receive(int player, String form) {
        BODIES.remove(player);
        if (form.isEmpty()) {
            FORMS.remove(player);
        } else {
            FORMS.put(player, form);
        }
    }

    /** What this client thinks a player looks like; empty if themselves. Public for the tests. */
    public static String form(int player) {
        return FORMS.getOrDefault(player, "");
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FORMS.clear();
        BODIES.clear();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        String form = FORMS.get(player.getId());
        if (form == null || BROKEN.contains(form)) {
            return;
        }
        LivingEntity body = BODIES.get(player.getId());
        if (body == null || body.level() != player.level()) {
            Entity made = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(form)).create(player.level());
            if (!(made instanceof LivingEntity living)) {
                BROKEN.add(form);
                return;
            }
            body = living;
            BODIES.put(player.getId(), body);
        }
        follow(body, player);
        event.setCanceled(true);
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(body)
                    .render(body, player.getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException e) {
            // A creature whose renderer needs a live world: the player goes back to looking like a player.
            LOGGER.warn("Can't draw a player as {}", form, e);
            BROKEN.add(form);
        }
    }

    private static void follow(LivingEntity body, Player player) {
        if (body.tickCount != player.tickCount) {
            body.tickCount = player.tickCount;
            body.walkAnimation.update(player.walkAnimation.speed(), 0.4f);
        }
        body.setPos(player.getX(), player.getY(), player.getZ());
        body.xo = player.xo;
        body.yo = player.yo;
        body.zo = player.zo;
        body.xOld = player.xOld;
        body.yOld = player.yOld;
        body.zOld = player.zOld;
        body.setYRot(player.getYRot());
        body.yRotO = player.yRotO;
        body.setXRot(player.getXRot());
        body.xRotO = player.xRotO;
        body.yBodyRot = player.yBodyRot;
        body.yBodyRotO = player.yBodyRotO;
        body.yHeadRot = player.yHeadRot;
        body.yHeadRotO = player.yHeadRotO;
        body.attackAnim = player.attackAnim;
        body.oAttackAnim = player.oAttackAnim;
        body.swinging = player.swinging;
        body.setOnGround(player.onGround());
    }
}
