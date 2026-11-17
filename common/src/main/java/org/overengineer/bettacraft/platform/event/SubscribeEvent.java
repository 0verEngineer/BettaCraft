package org.overengineer.bettacraft.platform.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** M52: a static handler of a {@link GameEvent} in a class listed in {@link GameEvents#HANDLERS} (like NeoForge's annotation). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;

    /** Also called when an earlier handler cancelled the event. */
    boolean receiveCanceled() default false;
}
