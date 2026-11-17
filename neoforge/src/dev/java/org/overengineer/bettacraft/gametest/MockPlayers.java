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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/**
 * Mock server players for GameTests, like {@link GameTestHelper#makeMockServerPlayerInLevel()}, but their fake
 * connection accepts every custom payload. Mods that greet joining players with their own packets (Better Fish Tanks
 * sends its egg loot table) would otherwise make NeoForge throw "Payload ... may not be sent to the client".
 */
final class MockPlayers {
    private MockPlayers() {}

    /** The connection claims to listen on every channel; the packets just end up in the embedded channel. */
    private static final class AnyChannel extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public boolean add(ResourceLocation id) {
            return false;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
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
        connection.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new AnyChannel());
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
