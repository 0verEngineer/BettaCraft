package org.overengineer.bettacraft.gametest.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * M52b: a class of GameTests of the normal suite (the build lists every class with it in {@code bettacraft_gametests.txt},
 * {@link BettaGameTests} registers them). {@code value}: the namespace of the structures.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameTestHolder {
    String value() default "minecraft";
}
