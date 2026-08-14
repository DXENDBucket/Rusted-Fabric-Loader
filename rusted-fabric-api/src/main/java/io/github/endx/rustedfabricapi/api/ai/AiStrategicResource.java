package io.github.endx.rustedfabricapi.api.ai;

import io.github.endx.rustedfabricapi.api.game.UnitView;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Dynamic ownership, pressure, reachability, and suggested objective for a resource site. */
public final class AiStrategicResource {
    private final AiResourceSite site;
    private final UnitView occupant;
    private final AiResourceControl control;
    private final AiCellControl localControl;
    private final float friendlyInfluence;
    private final float enemyInfluence;
    private final Set<AiMovementDomain> reachableDomains;
    private final AiResourceObjectiveKind objective;
    private final float priority;

    AiStrategicResource(AiResourceSite site, UnitView occupant, AiResourceControl control,
            AiCellControl localControl, float friendlyInfluence, float enemyInfluence,
            Set<AiMovementDomain> reachableDomains, AiResourceObjectiveKind objective,
            float priority) {
        this.site = site;
        this.occupant = occupant;
        this.control = control;
        this.localControl = localControl;
        this.friendlyInfluence = friendlyInfluence;
        this.enemyInfluence = enemyInfluence;
        this.reachableDomains = Collections.unmodifiableSet(reachableDomains.isEmpty()
                ? EnumSet.noneOf(AiMovementDomain.class)
                : EnumSet.copyOf(reachableDomains));
        this.objective = objective;
        this.priority = priority;
    }

    public AiResourceSite site() { return site; }
    public Optional<UnitView> occupant() { return Optional.ofNullable(occupant); }
    public AiResourceControl control() { return control; }
    public AiCellControl localControl() { return localControl; }
    public float friendlyInfluence() { return friendlyInfluence; }
    public float enemyInfluence() { return enemyInfluence; }
    /** Movement domains whose connected terrain region reaches this resource from the AI anchor. */
    public Set<AiMovementDomain> reachableDomains() { return reachableDomains; }
    public boolean reachable(AiMovementDomain domain) {
        return domain != null && reachableDomains.contains(domain);
    }
    public boolean landReachable() { return reachable(AiMovementDomain.LAND); }
    public AiResourceObjectiveKind objective() { return objective; }
    /** Normalized 0..1 urgency/fitness hint, not a mandatory action value. */
    public float priority() { return priority; }
}
