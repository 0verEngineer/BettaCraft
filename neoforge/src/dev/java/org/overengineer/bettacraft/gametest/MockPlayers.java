package org.overengineer.bettacraft.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

/**
 * Mock server players for GameTests, like {@link GameTestHelper#makeMockServerPlayerInLevel()}, but their fake
 * connection accepts every custom payload. Mods that greet joining players with their own packets (Better Fish Tanks
 * sends its egg loot table) would otherwise make NeoForge throw "Payload ... may not be sent to the client".
 */
final class MockPlayers {
    private MockPlayers() {}

    /**
     * NeoForge's server config sync: a plain mock player (class ServerPlayer itself) on that channel makes NeoForge expect a
     * login it never had, and the next config reload crashes the server ("configsToSync should contain an entry", seen in
     * the M52c integration run). The mock players do not listen on it.
     */
    private static final Identifier CONFIG_SYNC = Identifier.fromNamespaceAndPath("neoforge", "config_file");

    /** The connection claims to listen on every channel (but the config sync); the packets end up in the embedded channel. */
    private static final class AnyChannel extends AbstractSet<Identifier> {
        @Override
        public boolean contains(Object o) {
            return !CONFIG_SYNC.equals(o);
        }

        @Override
        public boolean add(Identifier id) {
            return false;
        }

        @Override
        public Iterator<Identifier> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /** NeoForge: its ad-hoc channel attribute (M52: by reflection, the Fabric GameTests share this class). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void anyChannel(Connection connection) {
        try {
            Class<?> attrs = Class.forName("net.neoforged.neoforge.network.registration.ChannelAttributes");
            io.netty.util.AttributeKey key = (io.netty.util.AttributeKey) attrs.getField("ADHOC_CHANNELS").get(null);
            io.netty.channel.Channel channel = (io.netty.channel.Channel) Connection.class.getMethod("channel").invoke(connection);
            channel.attr(key).set(new AnyChannel());
        } catch (ClassNotFoundException e) {
            // Fabric: no such check
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static ServerPlayer serverPlayer(GameTestHelper h) {
        return serverPlayer(h, false);
    }

    /**
     * M45: {@code plain} makes an instance of ServerPlayer itself (survival), not of a subclass: Architectury (FTB Quests)
     * counts every subclass as a fake player and skips it in its player tick.
     */
    static ServerPlayer serverPlayer(GameTestHelper h, boolean plain) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = plain ? new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation())
                : new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        anyChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    /**
     * M16: whether a fishing catch was replaced by another mod's loot modifier after ours (Critters and Companions turns
     * 15 % of all catches into koi or clams): every item comes from a mod other than Minecraft or Bettacraft.
     */
    static boolean replacedByOtherMod(java.util.List<net.minecraft.world.item.ItemStack> loot) {
        if (loot.isEmpty()) return false;
        for (var s : loot) {
            String ns = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace();
            if (ns.equals("minecraft") || ns.equals(org.overengineer.bettacraft.BettaCraft.MOD_ID)) return false;
        }
        return true;
    }
}
