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
 * packet never overtakes the one before), and every second interior delta (the miniature's block changes) never arrives:
 * the client has to notice the gap in the versions and ask for the whole interior again. The multiplayer test then has to
 * pass all the same.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class DevNetLag {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    static final boolean ON = Boolean.getBoolean("bettacraft.mp.lag");
    static final int MIN_MS = 40, MAX_MS = 250;
    public static final AtomicInteger DROPPED = new AtomicInteger(), DELAYED = new AtomicInteger(), DELTAS = new AtomicInteger();

    private DevNetLag() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ON || !(event.getEntity() instanceof ServerPlayer p) || p.connection == null) return;
        var channel = p.connection.getConnection().channel();
        if (channel == null || channel.pipeline().get("bettacraft_lag") != null || channel.pipeline().get("packet_handler") == null) return;
        channel.pipeline().addBefore("packet_handler", "bettacraft_lag", new Lag(p.getGameProfile().getName().hashCode()));
        LOGGER.info("[smoketest] MP lag on for {}: {}-{} ms both ways, every second interior delta dropped", p.getGameProfile().getName(), MIN_MS, MAX_MS);
    }

    @SubscribeEvent
    public static void onStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        if (ON) LOGGER.info("[smoketest] MP lag: {} packets delayed, {} interior deltas seen, {} dropped", DELAYED.get(), DELTAS.get(), DROPPED.get());
    }

    /**
     * Delays both directions and drops a share of the interior deltas. Each direction is one queue drained in order by one task
     * (single scheduled writes could swap two packets whose times were equal: a menu's data before the menu).
     */
    static final class Lag extends ChannelDuplexHandler {
        private final Random random;
        private long nextOut, nextIn;
        private final java.util.ArrayDeque<Object[]> outQ = new java.util.ArrayDeque<>(), inQ = new java.util.ArrayDeque<>();
        private boolean outScheduled, inScheduled;

        Lag(long seed) {
            this.random = new Random(seed);
        }

        /** The time this packet is due: random lag, never before the one before it (as over TCP). */
        private long due(boolean out) {
            long at = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(MIN_MS + random.nextInt(MAX_MS - MIN_MS + 1));
            if (out) return nextOut = Math.max(nextOut, at);
            return nextIn = Math.max(nextIn, at);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket b) {
                for (var sub : b.subPackets()) if (sub instanceof ClientboundCustomPayloadPacket p && p.payload() instanceof InteriorDeltaPayload) DELTAS.incrementAndGet();
            }
            if (msg instanceof ClientboundCustomPayloadPacket pkt && pkt.payload() instanceof InteriorDeltaPayload) {
                // every second one (the multiplayer test sends only a handful): the client must notice the gap and ask again
                if (DELTAS.incrementAndGet() % 2 == 0) {
                    DROPPED.incrementAndGet();
                    promise.trySuccess();
                    return;
                }
            }
            DELAYED.incrementAndGet();
            outQ.add(new Object[]{due(true), msg, promise});
            if (!outScheduled) drainOut(ctx);
        }

        private void drainOut(ChannelHandlerContext ctx) {
            outScheduled = false;
            long now = System.nanoTime();
            boolean wrote = false;
            while (!outQ.isEmpty() && (long) outQ.peek()[0] <= now) {
                Object[] e = outQ.poll();
                ctx.write(e[1], (ChannelPromise) e[2]);
                wrote = true;
            }
            if (wrote) ctx.flush();
            if (!outQ.isEmpty()) {
                outScheduled = true;
                ctx.executor().schedule(() -> drainOut(ctx), Math.max(0, (long) outQ.peek()[0] - now), TimeUnit.NANOSECONDS);
            }
        }

        @Override
        public void flush(ChannelHandlerContext ctx) {
            // the queue flushes what it writes
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            inQ.add(new Object[]{due(false), msg});
            if (!inScheduled) drainIn(ctx);
        }

        private void drainIn(ChannelHandlerContext ctx) {
            inScheduled = false;
            long now = System.nanoTime();
            while (!inQ.isEmpty() && (long) inQ.peek()[0] <= now) ctx.fireChannelRead(inQ.poll()[1]);
            if (!inQ.isEmpty()) {
                inScheduled = true;
                ctx.executor().schedule(() -> drainIn(ctx), Math.max(0, (long) inQ.peek()[0] - now), TimeUnit.NANOSECONDS);
            }
        }
    }
}
