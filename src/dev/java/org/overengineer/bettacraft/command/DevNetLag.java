package org.overengineer.bettacraft.command;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.network.InteriorDeltaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M42 part b, dev only ({@code gradlew runMultiplayerTest -Pmplag}, system property {@code bettacraft.mp.lag}): a bad connection
 * between the dedicated server and each player. Every packet both ways waits a random 40 to 250 ms (in order, as over TCP: a
 * packet never overtakes the one before), and a quarter of the interior deltas (the miniature's block changes) never arrive:
 * the client has to notice the gap in the versions and ask for the whole interior again. The multiplayer test then has to
 * pass all the same.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class DevNetLag {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    static final boolean ON = Boolean.getBoolean("bettacraft.mp.lag");
    static final int MIN_MS = 40, MAX_MS = 250;
    static final double DROP_DELTAS = 0.25;
    public static final AtomicInteger DROPPED = new AtomicInteger(), DELAYED = new AtomicInteger();

    private DevNetLag() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ON || !(event.getEntity() instanceof ServerPlayer p) || p.connection == null) return;
        var channel = p.connection.getConnection().channel();
        if (channel == null || channel.pipeline().get("bettacraft_lag") != null || channel.pipeline().get("packet_handler") == null) return;
        channel.pipeline().addBefore("packet_handler", "bettacraft_lag", new Lag(p.getGameProfile().getName().hashCode()));
        LOGGER.info("[smoketest] MP lag on for {}: {}-{} ms both ways, {}% of the interior deltas dropped", p.getGameProfile().getName(), MIN_MS, MAX_MS,
                Math.round(DROP_DELTAS * 100));
    }

    @SubscribeEvent
    public static void onStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        if (ON) LOGGER.info("[smoketest] MP lag: {} packets delayed, {} interior deltas dropped", DELAYED.get(), DROPPED.get());
    }

    /** Delays both directions in order and drops a share of the interior deltas. */
    static final class Lag extends ChannelDuplexHandler {
        private final Random random;
        private long nextOut, nextIn;

        Lag(long seed) {
            this.random = new Random(seed);
        }

        private synchronized long delay(boolean out) {
            long now = System.nanoTime();
            long at = now + TimeUnit.MILLISECONDS.toNanos(MIN_MS + random.nextInt(MAX_MS - MIN_MS + 1));
            if (out) at = nextOut = Math.max(nextOut, at);
            else at = nextIn = Math.max(nextIn, at);
            return Math.max(0, at - now);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof ClientboundCustomPayloadPacket pkt && pkt.payload() instanceof InteriorDeltaPayload) {
                boolean drop;
                synchronized (this) {
                    drop = random.nextDouble() < DROP_DELTAS;
                }
                if (drop) {
                    DROPPED.incrementAndGet();
                    promise.trySuccess();
                    return;
                }
            }
            DELAYED.incrementAndGet();
            ctx.executor().schedule(() -> {
                ctx.writeAndFlush(msg, promise);
            }, delay(true), TimeUnit.NANOSECONDS);
        }

        @Override
        public void flush(ChannelHandlerContext ctx) {
            // every delayed write flushes itself
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ctx.executor().schedule(() -> {
                ctx.fireChannelRead(msg);
            }, delay(false), TimeUnit.NANOSECONDS);
        }
    }
}
