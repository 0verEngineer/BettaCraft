package org.overengineer.bettacraft.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Criterion {@code bettacraft:event} (M6): one trigger for all Bettacraft milestones. An advancement names the event
 * ({@code catch_betta}, {@code nest_built}, ...) and optionally a range for its value (tank blocks, generation, fill %).
 * The events are fired by {@link Progress}.
 */
public final class BettaEventTrigger extends SimpleCriterionTrigger<BettaEventTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String event, int value) {
        trigger(player, i -> i.matches(event, value));
    }

    public record Instance(Optional<Holder<LootItemCondition>> player, String event, MinMaxBounds.Ints value) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("value", MinMaxBounds.Ints.ANY).forGetter(Instance::value)
        ).apply(i, Instance::new));

        public boolean matches(String event, int value) {
            return this.event.equals(event) && this.value.matches(value);
        }

        public static Criterion<Instance> of(String event) {
            return ModTriggers.EVENT.get().createCriterion(new Instance(Optional.empty(), event, MinMaxBounds.Ints.ANY));
        }

        public static Criterion<Instance> atLeast(String event, int min) {
            return ModTriggers.EVENT.get().createCriterion(new Instance(Optional.empty(), event, MinMaxBounds.Ints.atLeast(min)));
        }
    }
}
