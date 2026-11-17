package org.overengineer.bettacraft.gametest.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * M52b: a GameTest method (static, one {@code GameTestHelper} argument). Minecraft 1.21.5 made GameTests data-driven and
 * dropped its annotation; this one keeps the attributes of the old {@code net.minecraft.gametest.framework.GameTest}, and
 * {@link BettaGameTests} turns every annotated method into a test function, a test instance and (per batch) a test
 * environment on both loaders.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
    int timeoutTicks() default 100;

    String batch() default "defaultBatch";

    int rotationSteps() default 0;

    boolean required() default true;

    boolean manualOnly() default false;

    /** The structure (path; namespace: {@link #templateNamespace()} or the holder's); empty = class.method. */
    String template() default "";

    String templateNamespace() default "";

    long setupTicks() default 0L;

    int attempts() default 1;

    int requiredSuccesses() default 1;

    boolean skyAccess() default false;
}
