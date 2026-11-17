package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;

/**
 * M52b: chunk ticket types. Since Minecraft 1.21.5 a ticket type is a registered record (timeout in ticks, flags) instead
 * of {@code TicketType.create(name, comparator, timeout)}. Both of ours load and simulate the chunks and are not saved,
 * like the region tickets of 1.21.1.
 */
public final class ModTickets {
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, BettaCraft.MOD_ID);

    /** The interior region of a tank being prepared or grown into (renewed while in use, 300 ticks). */
    public static final Supplier<TicketType> INTERIOR = TICKETS.register("interior",
            () -> new TicketType(300L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    /** The chunks of a tank a player is inside of (60 ticks, renewed). */
    public static final Supplier<TicketType> OCCUPIED_TANK = TICKETS.register("occupied_tank",
            () -> new TicketType(60L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    private ModTickets() {}

    public static void register() {
        TICKETS.register();
    }
}
