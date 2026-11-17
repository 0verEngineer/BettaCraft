package org.overengineer.bettacraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.Component;

/**
 * M52b: a failed assertion with a plain message, as {@code new GameTestAssertException(String)} was before 1.21.5 (the
 * new exception wants a component and the tick; the GameTest log names the test anyway).
 */
public class GameTestFailure extends GameTestAssertException {
    public GameTestFailure(String message) {
        super(Component.literal(message), -1);
    }

    @Override
    public Component getDescription() {
        return message;
    }
}
