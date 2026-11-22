package org.overengineer.bettacraft.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M62, both ways. Server -> client: the tanks the external filter at {@code filter} can feed ({@code options}; {@code
 * chosen} the index of the current one, -1 none): the client opens the choice. Client -> server: the player chose {@code
 * chosen} (the {@code options} the client got back, the server checks them again).
 */
public record FilterLinkPayload(BlockPos filter, List<Option> options, int chosen) implements CustomPacketPayload {
    public static final Type<FilterLinkPayload> TYPE = new Type<>(BettaCraft.id("filter_link"));

    /** One tank in reach: the wall block the hose goes up, the tank's controller, its name, how far below the filter stands. */
    public record Option(BlockPos wall, BlockPos tank, String name, int below, int sizeX, int sizeY, int sizeZ) {
        public static final StreamCodec<ByteBuf, Option> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Option decode(ByteBuf b) {
                return new Option(BlockPos.STREAM_CODEC.decode(b), BlockPos.STREAM_CODEC.decode(b), ByteBufCodecs.stringUtf8(64).decode(b),
                        ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b));
            }

            @Override
            public void encode(ByteBuf b, Option o) {
                BlockPos.STREAM_CODEC.encode(b, o.wall());
                BlockPos.STREAM_CODEC.encode(b, o.tank());
                ByteBufCodecs.stringUtf8(64).encode(b, o.name().length() > 64 ? o.name().substring(0, 64) : o.name());
                ByteBufCodecs.VAR_INT.encode(b, o.below());
                ByteBufCodecs.VAR_INT.encode(b, o.sizeX());
                ByteBufCodecs.VAR_INT.encode(b, o.sizeY());
                ByteBufCodecs.VAR_INT.encode(b, o.sizeZ());
            }
        };
    }

    public static final StreamCodec<ByteBuf, FilterLinkPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FilterLinkPayload::filter,
            Option.STREAM_CODEC.apply(ByteBufCodecs.list(16)), FilterLinkPayload::options,
            ByteBufCodecs.VAR_INT, FilterLinkPayload::chosen,
            FilterLinkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
