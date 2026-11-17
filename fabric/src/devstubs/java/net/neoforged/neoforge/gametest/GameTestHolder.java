package net.neoforged.neoforge.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** M52: a stub of NeoForge's annotation for the shared GameTest classes in the Fabric dev runs (no meaning there). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameTestHolder {
    String value() default "minecraft";
}
