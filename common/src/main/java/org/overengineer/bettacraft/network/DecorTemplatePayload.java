package org.overengineer.bettacraft.network;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.decor.DecorTemplate;

/**
 * Both directions: the client asks for a decoration structure ({@code template} empty) for the placement hologram,
 * the server answers with its blocks. Structures live in server data packs, so the client cannot read them itself.
 */
public record DecorTemplatePayload(Identifier id, Optional<DecorTemplate> template) implements CustomPacketPayload {
    public static final Type<DecorTemplatePayload> TYPE = new Type<>(BettaCraft.id("decor_template"));

    public static final StreamCodec<FriendlyByteBuf, DecorTemplatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeIdentifier(p.id);
                buf.writeBoolean(p.template.isPresent());
                p.template.ifPresent(t -> t.write(buf));
            },
            buf -> {
                Identifier id = buf.readIdentifier();
                return new DecorTemplatePayload(id, buf.readBoolean() ? Optional.of(DecorTemplate.read(buf)) : Optional.empty());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
