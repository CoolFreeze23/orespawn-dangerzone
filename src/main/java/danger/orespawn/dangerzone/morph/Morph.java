package danger.orespawn.dangerzone.morph;

import com.mojang.serialization.Codec;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.client.MorphClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * What the Morph Staff does to a player: they look and sound like the creature, hit as hard and take a hit as well, up
 * to a limit, since a boss's numbers aren't for carrying around. Their hitbox stays their own, unlike in DangerZone,
 * where a player could become something too big to fit through the world.
 */
@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID)
public final class Morph {
    public static final double MAX_ATTACK = 20.0;
    public static final double MAX_ARMOR = 20.0;
    private static final int AMBIENT_ONE_IN = 600;
    private static final ResourceLocation MODIFIER = OreSpawnDangerZone.id("morph");

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OreSpawnDangerZone.MOD_ID);
    private static final Supplier<AttachmentType<String>> FORM = ATTACHMENTS.register("morph", () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    /** Tells a client what a player looks like. An empty form is themselves. */
    public record Sync(int player, String form) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(OreSpawnDangerZone.id("morph"));
        public static final StreamCodec<ByteBuf, Sync> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Sync::player, ByteBufCodecs.STRING_UTF8, Sync::form, Sync::new);

        @Override
        public Type<Sync> type() {
            return TYPE;
        }
    }

    private Morph() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(Morph::payloads);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Sync.TYPE, Sync.CODEC, (sync, context) -> MorphClient.receive(sync.player(), sync.form()));
    }

    public static String form(Player player) {
        return player.getData(FORM);
    }

    /** Whether something can be turned into. Trophies, flags and other things that aren't really creatures can't. */
    public static boolean canBecome(Entity target) {
        return target instanceof Mob && !target.getType().is(ModContent.NO_TROPHY);
    }

    /** Takes on a creature's form, or with an empty string, goes back. */
    public static void set(ServerPlayer player, String form) {
        if (form(player).equals(form)) {
            return;
        }
        player.setData(FORM, form);
        applyStats(player);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), form.isEmpty() ? ModContent.SND_UNMORPH.get() : ModContent.SND_MORPH.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Sync(player.getId(), form));
    }

    /** Public for the tests. */
    public static void applyStats(ServerPlayer player) {
        String form = form(player);
        EntityType<?> type = form.isEmpty() ? null : BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(form));
        AttributeSupplier stats = type != null && DefaultAttributes.hasSupplier(type) ? supplier(type) : null;
        applyStat(player.getAttribute(Attributes.ATTACK_DAMAGE), stats == null || !stats.hasAttribute(Attributes.ATTACK_DAMAGE) ? 0.0
                : Math.min(stats.getBaseValue(Attributes.ATTACK_DAMAGE), MAX_ATTACK));
        applyStat(player.getAttribute(Attributes.ARMOR), stats == null || !stats.hasAttribute(Attributes.ARMOR) ? 0.0
                : Math.min(stats.getBaseValue(Attributes.ARMOR), MAX_ARMOR));
    }

    @SuppressWarnings("unchecked")
    private static AttributeSupplier supplier(EntityType<?> type) {
        return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
    }

    private static void applyStat(AttributeInstance attribute, double amount) {
        if (attribute != null) {
            attribute.removeModifier(MODIFIER);
            if (amount > 0.0) {
                attribute.addTransientModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    private static void resync(ServerPlayer player) {
        applyStats(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Sync(player.getId(), form(player)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resync(player);
        }
    }

    /** Death ends it: the new body starts with nothing, and everyone who could see the old one needs telling. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resync(player);
        }
    }

    /**
     * Someone walks into view of a player. Told even when that player is themselves: a client remembers the last form it
     * heard about, and it hears nothing while they're out of sight, or in the moment a respawned player isn't tracked yet.
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player seen && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new Sync(seen.getId(), form(seen)));
        }
    }

    /** Milk clears it, as it does any effect. */
    @SubscribeEvent
    public static void onFinishItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.MILK_BUCKET)) {
            set(player, "");
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FIRE) && !form(player).isEmpty()
                && BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(form(player))).fireImmune()) {
            event.setCanceled(true);
            player.clearFire();
        }
    }

    /** Now and then it makes the creature's noise. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !form(player).isEmpty() && player.getRandom().nextInt(AMBIENT_ONE_IN) == 0
                && BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(form(player))).create(player.level()) instanceof Mob body) {
            body.setPos(player.position());
            body.playAmbientSound();
            body.discard();
        }
    }
}
