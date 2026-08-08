package io.github.endx.rustedfabricapi.api.custom.attachment;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.CustomUnitMetadata;
import rustedwarfare.custom.attachment.AttachmentSlot;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.util.RwArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Slot discovery and safe attachment mutation helpers for custom units. */
public final class Attachments {
    private Attachments() {
    }

    public static List<AttachmentSlot> slots(CustomUnitMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
        RwArrayList values = metadata.attachmentSlots;
        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<AttachmentSlot> result = new ArrayList<AttachmentSlot>(values.size());
        for (Object value : values) {
            if (value instanceof AttachmentSlot) result.add((AttachmentSlot) value);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<AttachmentSlot> slots(CustomUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return slots(unit.unitMetadata);
    }

    public static Optional<AttachmentSlot> findSlot(CustomUnitMetadata metadata, String name) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(metadata.getAttachmentSlotByName(name));
    }

    public static Optional<AttachmentSlot> findSlot(CustomUnit unit, String name) {
        Objects.requireNonNull(unit, "unit");
        return findSlot(unit.unitMetadata, name);
    }

    public static Optional<AttachmentSlot> findSlot(CustomUnit unit, short index) {
        Objects.requireNonNull(unit, "unit");
        return Optional.ofNullable(unit.getAttachmentSlotByIndex(index));
    }

    public static Optional<OrderableUnit> attached(CustomUnit parent, AttachmentSlot slot) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(slot, "slot");
        return Optional.ofNullable(parent.getAttachedUnitInSlot(slot));
    }

    /** Returns attached units ordered by their metadata slot index. */
    public static List<OrderableUnit> snapshot(CustomUnit parent) {
        Objects.requireNonNull(parent, "parent");
        List<OrderableUnit> result = new ArrayList<OrderableUnit>();
        for (AttachmentSlot slot : slots(parent)) {
            OrderableUnit unit = parent.getAttachedUnitInSlot(slot);
            if (unit != null) result.add(unit);
        }
        return Collections.unmodifiableList(result);
    }

    public static Optional<OrderableUnit> parent(OrderableUnit child) {
        Objects.requireNonNull(child, "child");
        return Optional.ofNullable(child.attachmentParentUnit);
    }

    public static Optional<AttachmentSlot> slotOf(OrderableUnit child) {
        Objects.requireNonNull(child, "child");
        return Optional.ofNullable(child.getAttachmentSlot());
    }

    public static boolean attach(CustomUnit parent, AttachmentSlot slot, OrderableUnit child) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(child, "child");
        return parent.attachUnitToSlot(child, slot);
    }

    public static boolean detach(CustomUnit parent, OrderableUnit child) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(child, "child");
        return parent.detachUnit(child);
    }

    public static boolean isProtectedBySlot(CustomUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return unit.isProtectedByAttachmentSlot();
    }

    public static float offsetX(AttachmentSlot slot) {
        return offset(slot, "x", "c");
    }

    public static float offsetY(AttachmentSlot slot) {
        return offset(slot, "y", "d");
    }

    public static float height(AttachmentSlot slot) {
        return offset(slot, "height", "e");
    }

    private static float offset(AttachmentSlot slot, String namedField, String officialField) {
        Objects.requireNonNull(slot, "slot");
        return RustedReflection.getFloatField(slot, new String[]{namedField, officialField});
    }
}
