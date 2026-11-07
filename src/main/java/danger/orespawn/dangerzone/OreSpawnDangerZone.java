package danger.orespawn.dangerzone;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/** Creatures, items and things to do from DangerZone, TheyCallMeDanger's game, on the OreSpawn port. */
@Mod(OreSpawnDangerZone.MOD_ID)
public final class OreSpawnDangerZone {
    public static final String MOD_ID = "orespawn_dangerzone";

    public OreSpawnDangerZone(IEventBus modBus, ModContainer container) {
        ModContent.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, DzConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
