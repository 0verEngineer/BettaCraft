package org.overengineer.bettacraft.gametest.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** M52b: whether the structure name gets the lower-case class name as a prefix (old NeoForge default: yes). */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PrefixGameTestTemplate {
    boolean value() default true;
}
