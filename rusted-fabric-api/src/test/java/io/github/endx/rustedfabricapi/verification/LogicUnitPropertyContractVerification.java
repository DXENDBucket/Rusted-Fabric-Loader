package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.logic.LogicUnitPropertyDefinition;

final class LogicUnitPropertyContractVerification {
    private LogicUnitPropertyContractVerification() { }

    static void verify() {
        LogicUnitPropertyDefinition number = LogicUnitPropertyDefinition.numberProperty(
                "self.contractNumber", LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                unit -> 12.5F);
        require(number.valueType() == LogicUnitPropertyDefinition.ValueType.NUMBER
                        && number.evaluateNumber(null) == 12.5F,
                "numeric unit-property definition drifted");

        LogicUnitPropertyDefinition clientBoolean =
                LogicUnitPropertyDefinition.booleanProperty(
                        "self.contractSelected",
                        LogicUnitPropertyDefinition.Locality.CLIENT_LOCAL,
                        unit -> true);
        require(clientBoolean.valueType() == LogicUnitPropertyDefinition.ValueType.BOOLEAN
                        && clientBoolean.evaluateBoolean(null)
                        && clientBoolean.locality()
                        == LogicUnitPropertyDefinition.Locality.CLIENT_LOCAL,
                "boolean unit-property definition drifted");

        LogicUnitPropertyDefinition string = LogicUnitPropertyDefinition.stringProperty(
                "self.contractType", LogicUnitPropertyDefinition.Locality.SYNCHRONIZED,
                unit -> "move");
        require(string.valueType() == LogicUnitPropertyDefinition.ValueType.STRING
                        && "move".equals(string.evaluateString(null)),
                "string unit-property definition drifted");

        try {
            number.evaluateBoolean(null);
            throw new AssertionError("unit property accepted an incompatible evaluator type");
        } catch (IllegalStateException expected) {
            // Expected.
        }
        try {
            LogicUnitPropertyDefinition.numberProperty("activeWaypointX",
                    LogicUnitPropertyDefinition.Locality.SYNCHRONIZED, unit -> 0.0F);
            throw new AssertionError("unit property without self prefix was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
