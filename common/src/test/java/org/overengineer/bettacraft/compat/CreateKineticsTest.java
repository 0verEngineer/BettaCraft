package org.overengineer.bettacraft.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.compat.create.CreateKinetics;

/** M32 part c: the speed of a Create shaft to the work of a device. */
class CreateKineticsTest {
    @Test
    void speedMapsToWork() {
        assertEquals(0F, CreateKinetics.work(0F));
        assertEquals(0F, CreateKinetics.work(Float.NaN));
        assertEquals(0.25F, CreateKinetics.work(16F), 1e-6);
        assertEquals(0.5F, CreateKinetics.work(32F), 1e-6);
        assertEquals(1F, CreateKinetics.work(64F), 1e-6);
        assertEquals(1F, CreateKinetics.work(256F), 1e-6);
    }
}
