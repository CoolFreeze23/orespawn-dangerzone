package danger.orespawn.dangerzone.gametest;

import danger.orespawn.dangerzone.client.MorphClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * The client half of a real multiplayer test: a hidden window that joins the server by itself, holds the keys a rider
 * would, and says in chat what it sees of other players. Logs what happens to it as [DZ2C] lines.
 */
public final class TwoPlayerClient {
    private static final String ADDRESS = System.getProperty("orespawn_dangerzone.connect");
    private static final Map<String, String> SEEN = new HashMap<>();
    private static boolean joined;
    private static boolean connected;
    private static int wait;
    private static boolean report;
    private static int deadFor;
    private static int retryWait;

    private TwoPlayerClient() {
    }

    private static void log(String text) {
        System.out.println("[DZ2C] " + text);
    }

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() != null) {
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(true);
        } else if (connected) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
        }
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent.System event) {
        if ("report".equals(event.getMessage().getString())) {
            report = true;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!joined) {
            if (mc.getOverlay() == null && mc.screen != null && ++wait > 40) {
                joined = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                log("connecting to " + ADDRESS + " as " + mc.getUser().getName());
                ServerData data = new ServerData("test", ADDRESS, ServerData.Type.OTHER);
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(ADDRESS), data, false, null);
            }
            return;
        }
        if (mc.player != null && mc.getConnection() != null) {
            if (!connected) {
                connected = true;
                log("connected");
            }
            if (mc.player.isDeadOrDying() && ++deadFor > 40) {
                deadFor = 0;
                log("respawning");
                mc.player.respawn();
                mc.setScreen(null);
            }
            if (report) {
                report = false;
                mc.getConnection().sendChat("all fine");
            }
            if (mc.level != null && mc.level.getGameTime() % 10 == 0) {
                for (AbstractClientPlayer other : mc.level.players()) {
                    if (other == mc.player) {
                        continue;
                    }
                    String name = other.getGameProfile().getName();
                    String form = MorphClient.form(other.getId());
                    if (!form.equals(SEEN.getOrDefault(name, ""))) {
                        SEEN.put(name, form);
                        mc.getConnection().sendChat("saw " + name + " as " + (form.isEmpty() ? "themselves" : form));
                    }
                }
            }
            return;
        }
        // Before it's been on: the server may just not be up yet. After: the test is over for this client.
        if (mc.screen instanceof DisconnectedScreen && !connected) {
            if (++retryWait > 100) {
                retryWait = 0;
                log("trying again");
                ServerData data = new ServerData("test", ADDRESS, ServerData.Type.OTHER);
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(ADDRESS), data, false, null);
            }
        } else if (mc.screen instanceof DisconnectedScreen) {
            log("disconnected: " + mc.screen.getTitle().getString());
            mc.stop();
        } else if (connected && mc.screen instanceof TitleScreen) {
            log("back at the title screen");
            mc.stop();
        }
    }
}
