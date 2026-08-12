package io.github.endx.rustedfabricapi.api.custom.event;

import io.github.endx.rustedfabricapi.api.custom.CustomUnitHandle;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.custom.logic.VariableScope;
import rustedwarfare.unit.Unit;

import java.util.Objects;
import java.util.Optional;

/** Portable view of one native {@code newMessage} notification before configured actions run. */
public final class CustomUnitMessageContext {
    private final CustomUnitHandle recipient;
    private final Object senderIdentity;
    private final CustomTagList tags;
    private final CustomUnitEventData data;

    public CustomUnitMessageContext(CustomUnit recipient, Unit sender, CustomTagList tags,
                                    VariableScope data) {
        this.recipient = CustomUnitHandle.of(Objects.requireNonNull(recipient, "recipient"));
        this.senderIdentity = sender;
        this.tags = tags;
        this.data = CustomUnitEventData.wrap(Objects.requireNonNull(data, "data"));
    }

    public CustomUnitHandle recipient() { return recipient; }
    public Optional<Object> senderIdentity() { return Optional.ofNullable(senderIdentity); }
    public boolean hasTag(String name) { return UnitTags.contains(tags, UnitTags.tag(name)); }
    public CustomUnitEventData data() { return data; }
}
