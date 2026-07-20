package io.github.endx.rustedfabricapi.api.unit.transport;

import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.unit.TransportUnitInterface;
import rustedwarfare.unit.Unit;
import rustedwarfare.unit.air.DropshipUnit;
import rustedwarfare.unit.land.HovercraftUnit;
import rustedwarfare.util.RwArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Strongly typed helpers for transport capacity, cargo and unloading. */
public final class TransportUnits {
    private TransportUnits() {
    }

    public static boolean isTransport(Unit unit) {
        return unit instanceof TransportUnitInterface;
    }

    public static Optional<TransportUnitInterface> asTransport(Unit unit) {
        return unit instanceof TransportUnitInterface
                ? Optional.of((TransportUnitInterface) unit)
                : Optional.empty();
    }

    public static Optional<Unit> containingUnit(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        return Optional.ofNullable(unit.getContainingUnit());
    }

    public static Optional<AttachmentSlot> attachmentSlot(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        return Optional.ofNullable(unit.getAttachmentSlot());
    }

    public static int slotsNeeded(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        return unit.getTransportSlotsNeeded();
    }

    public static int usedSlots(Unit transport) {
        Objects.requireNonNull(transport, "transport");
        return transport.getTransportBarUsedSlots();
    }

    public static int maxSlots(Unit transport) {
        Objects.requireNonNull(transport, "transport");
        return transport.getTransportBarMaxSlots();
    }

    public static int cargoCount(Unit transport) {
        return requireTransport(transport).getTransportedUnitCount();
    }

    public static boolean hasCapacity(Unit transport) {
        Objects.requireNonNull(transport, "transport");
        return transport.hasTransportCapacity();
    }

    public static boolean isUnloading(Unit transport) {
        return requireTransport(transport).isTransportUnloading();
    }

    public static boolean canLoad(Unit transport, Unit candidate, boolean allowPartial) {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(candidate, "candidate");
        return transport.canTransportUnit(candidate, allowPartial);
    }

    public static boolean canLoadIgnoringCurrentContainer(Unit transport, Unit candidate,
                                                           boolean allowPartial) {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(candidate, "candidate");
        return transport.canTransportUnitIgnoringCurrentContainer(candidate, allowPartial);
    }

    /** Attempts to load through the game's normal validation and bookkeeping path. */
    public static boolean tryLoad(Unit transport, Unit candidate, boolean allowPartial) {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(candidate, "candidate");
        return transport.tryAddUnitToTransport(candidate, allowPartial);
    }

    /** Removes a cargo unit through the transport interface. */
    public static void remove(Unit transport, Unit cargo) {
        Objects.requireNonNull(cargo, "cargo");
        requireTransport(transport).removeUnitFromTransport(cargo);
    }

    /** Returns an immutable point-in-time cargo list for built-in and custom transports. */
    public static List<Unit> cargo(Unit transport) {
        Objects.requireNonNull(transport, "transport");
        RwArrayList values;
        if (transport instanceof CustomUnit) {
            values = ((CustomUnit) transport).getTransportedUnits();
        } else if (transport instanceof DropshipUnit) {
            values = ((DropshipUnit) transport).getTransportedUnits();
        } else if (transport instanceof HovercraftUnit) {
            values = ((HovercraftUnit) transport).getTransportedUnits();
        } else {
            requireTransport(transport);
            return Collections.emptyList();
        }

        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<Unit> result = new ArrayList<Unit>(values.size());
        for (Object value : values) {
            if (value instanceof Unit) result.add((Unit) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static void startUnloading(Unit transport) {
        if (transport instanceof CustomUnit) {
            ((CustomUnit) transport).startTransportUnloading();
        } else if (transport instanceof DropshipUnit) {
            ((DropshipUnit) transport).startTransportUnloading();
        } else if (transport instanceof HovercraftUnit) {
            ((HovercraftUnit) transport).startTransportUnloading();
        } else {
            throw unsupportedTransport(transport);
        }
    }

    public static void stopUnloading(Unit transport) {
        if (transport instanceof CustomUnit) {
            ((CustomUnit) transport).stopTransportUnloading();
        } else if (transport instanceof DropshipUnit) {
            ((DropshipUnit) transport).stopTransportUnloading();
        } else if (transport instanceof HovercraftUnit) {
            ((HovercraftUnit) transport).stopTransportUnloading();
        } else {
            throw unsupportedTransport(transport);
        }
    }

    public static boolean unloadNext(CustomUnit transport, boolean forced) {
        Objects.requireNonNull(transport, "transport");
        return transport.unloadNextTransportedUnit(forced);
    }

    public static boolean unload(CustomUnit transport, Unit cargo,
                                 boolean forcePlacement, boolean alternateSide) {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(cargo, "cargo");
        return transport.unloadSpecificTransportedUnit(cargo, forcePlacement, alternateSide);
    }

    public static void releaseAll(CustomUnit transport, boolean killUnits) {
        Objects.requireNonNull(transport, "transport");
        transport.releaseAllTransportedUnits(killUnits);
    }

    private static TransportUnitInterface requireTransport(Unit unit) {
        Objects.requireNonNull(unit, "unit");
        if (!(unit instanceof TransportUnitInterface)) throw unsupportedTransport(unit);
        return (TransportUnitInterface) unit;
    }

    private static IllegalArgumentException unsupportedTransport(Unit unit) {
        return new IllegalArgumentException("unit does not expose the mapped transport API: "
                + (unit == null ? "null" : unit.getClass().getName()));
    }
}
