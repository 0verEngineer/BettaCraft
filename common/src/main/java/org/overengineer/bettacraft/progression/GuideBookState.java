package org.overengineer.bettacraft.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Player attachment {@code bettacraft:guide_book} (M6): whether the player already got the handbook. Persisted, copied
 * on death and versioned via the {@code schema} field.
 */
public record GuideBookState(int schema, boolean given) {
    public static final int CURRENT_SCHEMA = 1;

    public static final Codec<GuideBookState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema", 0).forGetter(GuideBookState::schema),
            Codec.BOOL.optionalFieldOf("given", false).forGetter(GuideBookState::given)
    ).apply(i, GuideBookState::new));

    public static final GuideBookState NONE = new GuideBookState(CURRENT_SCHEMA, false);
    public static final GuideBookState GIVEN = new GuideBookState(CURRENT_SCHEMA, true);
}
