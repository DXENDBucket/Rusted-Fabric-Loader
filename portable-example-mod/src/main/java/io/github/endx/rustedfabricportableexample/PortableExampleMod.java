package io.github.endx.rustedfabricportableexample;

import java.util.concurrent.atomic.AtomicLong;

import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIEntrypoint;
import io.github.endx.rustedfabricapi.api.event.CommandEvents;
import io.github.endx.rustedfabricapi.api.event.RuntimeLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.UnitLifecycleEvents;

/** The exact same class is packaged as JVM bytecode and Android DEX. */
public final class PortableExampleMod extends RustedFabricAPIEntrypoint {
    private static final AtomicLong REGISTERED_UNITS = new AtomicLong();
    private static final AtomicLong ISSUED_COMMANDS = new AtomicLong();

    @Override
    protected void onRustedFabricAPI(RustedFabricAPIContext context) {
        RuntimeLifecycleEvents.GAME_READY.register(ignored -> {
            // Platform-neutral initialization belongs here.
        });
        UnitLifecycleEvents.AFTER_UNIT_REGISTER.register(unit ->
                REGISTERED_UNITS.incrementAndGet());
        UnitLifecycleEvents.AFTER_UNIT_UNREGISTER.register(unit ->
                REGISTERED_UNITS.updateAndGet(value -> Math.max(0L, value - 1L)));
        CommandEvents.BEFORE_COMMAND_ISSUE.register(command -> false);
        CommandEvents.AFTER_COMMAND_ISSUE.register(command ->
                ISSUED_COMMANDS.incrementAndGet());
    }

    public static long registeredUnits() {
        return REGISTERED_UNITS.get();
    }

    public static long issuedCommands() {
        return ISSUED_COMMANDS.get();
    }
}
