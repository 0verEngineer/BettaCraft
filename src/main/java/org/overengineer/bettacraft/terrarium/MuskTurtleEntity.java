package org.overengineer.bettacraft.terrarium;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

/**
 * M31: common musk turtle (<i>Sternotherus odoratus</i>, the "stinkpot"), about 10 cm: a small turtle that walks on the
 * bottom of the water more than it swims and only now and then climbs onto land or a root to bask. It lives in a
 * <b>paludarium</b> with a small land part (5-60 % land), in water of 20-28 °C and air of 18-32 °C; it needs no UV lamp
 * (it basks seldom, a basking spot is welcome). Eats pellets, frozen and live food. Caught in a bucket of water like a
 * fish. The first species of the terrarium care hook ({@link TerrariumNeeds}); part b made it a {@link HerpEntity}
 * (breeding with females at both ends of the temperature range, wild in swamps and rivers).
 */
public class MuskTurtleEntity extends HerpEntity {
    public static final TerrariumNeeds NEEDS = HerpSpecies.MUSK.needs();

    public MuskTurtleEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level, HerpSpecies.MUSK);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return HerpEntity.createAttributes(HerpSpecies.MUSK);
    }
}
