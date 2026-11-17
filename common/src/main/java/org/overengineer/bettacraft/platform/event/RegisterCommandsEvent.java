package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class RegisterCommandsEvent extends GameEvent {
    private final com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher;
    private final net.minecraft.commands.CommandBuildContext buildContext;

    public RegisterCommandsEvent(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher, net.minecraft.commands.CommandBuildContext buildContext) {
        this.dispatcher = dispatcher;
        this.buildContext = buildContext;
    }

    public com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    public net.minecraft.commands.CommandBuildContext getBuildContext() {
        return buildContext;
    }
}
