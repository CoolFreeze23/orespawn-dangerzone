package danger.orespawn.dangerzone.gametest;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/** The GameTests, and on a client with -Dorespawn_dangerzone.shots=true a run that photographs each creature and exits. */
@Mod("orespawn_dangerzone_tests")
public final class TestsMod {
    public TestsMod() {
        if (FMLEnvironment.dist == Dist.CLIENT && Boolean.getBoolean("orespawn_dangerzone.shots")) {
            if (Boolean.getBoolean("orespawn_dangerzone.drive")) {
                NeoForge.EVENT_BUS.register(TestDrive.class);
            } else {
                NeoForge.EVENT_BUS.register(ShotsClient.class);
            }
        }
    }
}
