package io.github.endx.rustedfabricapi.api.asset.condition;

/** A decoded, reusable predicate controlling whether one data resource is loaded. */
@FunctionalInterface
public interface ResourceCondition {
    boolean test(ResourceConditionContext context);
}
