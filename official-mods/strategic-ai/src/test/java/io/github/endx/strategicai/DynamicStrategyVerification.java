package io.github.endx.strategicai;

public final class DynamicStrategyVerification {
    private DynamicStrategyVerification() {
    }

    public static void main(String[] args) {
        require(!StrategicReplanPolicy.acceptFrontlineSwitch(
                        1000.0F, 900.0F, true, false),
                "a small route fluctuation changed the frontline assignment");
        require(StrategicReplanPolicy.acceptFrontlineSwitch(
                        1000.0F, 700.0F, true, false),
                "a materially shorter land route did not replace the frontline");
        require(StrategicReplanPolicy.acceptFrontlineSwitch(
                        1000.0F, 980.0F, false, false),
                "a destroyed frontline position was not replaced");
        require(StrategicReplanPolicy.acceptFrontlineSwitch(
                        1000.0F, 980.0F, true, true),
                "an alliance membership change did not rebuild assignments");

        require(UnitMicroPolicy.select(0.18F, true, true,
                        true, false, true, true)
                        == UnitMicroPolicy.Decision.RETREAT,
                "a critically damaged unit stayed in return fire");
        require(UnitMicroPolicy.select(0.70F, false, true,
                        true, true, true, false)
                        == UnitMicroPolicy.Decision.HOLD_FIRE_WINDOW,
                "a safe one-way-fire position was abandoned");
        require(UnitMicroPolicy.select(0.70F, false, false,
                        true, true, false, false)
                        == UnitMicroPolicy.Decision.STANDOFF,
                "an outranging unit did not approach its firing band");
        require(UnitMicroPolicy.select(0.70F, false, false,
                        false, false, false, false)
                        == UnitMicroPolicy.Decision.SUPPORT,
                "a unit unable to engage was treated as a direct attacker");
        require(UnitMicroPolicy.recovered(0.60F)
                        && !UnitMicroPolicy.recovered(0.45F),
                "retreat recovery hysteresis is incorrect");
        require(!BattleGroupPolicy.shouldCommit(
                        StrategicFrontState.Mode.OPEN, 8, 3, 3),
                "a scattered group was released one unit at a time");
        require(BattleGroupPolicy.shouldCommit(
                        StrategicFrontState.Mode.OPEN, 8, 4, 3),
                "a cohesive open-front group was held indefinitely");
        require(!BattleGroupPolicy.shouldCommit(
                        StrategicFrontState.Mode.MUSTER, 12, 12, 3),
                "a muster line attacked before the strategic state changed");
        require(BattleGroupPolicy.shouldCommit(
                        StrategicFrontState.Mode.ASSAULT, 10, 6, 3),
                "an assembled assault group was not released together");
        require(BattleGroupPolicy.readyForFront(30.0F, 230.0F, 200.0F),
                "a unit at the rally line was not considered ready");
        require(BattleGroupPolicy.readyForFront(300.0F, 120.0F, 200.0F),
                "an engaged vanguard was incorrectly recalled");
        require(!BattleGroupPolicy.readyForFront(300.0F, 500.0F, 200.0F),
                "a distant reinforcement bypassed the rally line");
        require(UnitMicroPolicy.selectLive(0.90F, false,
                        true, true, 200.0F, 200.0F, 197.0F,
                        1.0F, 1.0F, 1.0F)
                        == UnitMicroPolicy.Decision.HOLD_FIRE_WINDOW,
                "equal-range edge control did not hold its firing boundary");
        require(UnitMicroPolicy.selectLive(0.90F, false,
                        true, true, 200.0F, 200.0F, 250.0F,
                        1.0F, 1.0F, 1.0F)
                        == UnitMicroPolicy.Decision.EDGE_CONTROL,
                "an equal-range unit did not move back onto the firing edge");
        require(UnitMicroPolicy.selectLive(0.90F, false,
                        true, true, 150.0F, 220.0F, 230.0F,
                        1.30F, 1.0F, 0.96F)
                        == UnitMicroPolicy.Decision.RUSH,
                "a faster viable close-range unit refused to rush");
        require(UnitMicroPolicy.selectLive(0.90F, false,
                        true, true, 150.0F, 220.0F, 210.0F,
                        1.0F, 1.0F, 0.70F)
                        == UnitMicroPolicy.Decision.DISENGAGE,
                "an outranged losing unit remained inside enemy range");
        require(UnitMicroPolicy.selectLive(0.24F, true,
                        true, true, 220.0F, 180.0F, 170.0F,
                        1.0F, 1.0F, 2.0F)
                        == UnitMicroPolicy.Decision.RETREAT,
                "critical live micro did not override a favourable fight");
        System.out.println("Strategic AI dynamic strategy and micro contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
