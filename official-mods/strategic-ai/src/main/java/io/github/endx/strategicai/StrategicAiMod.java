package io.github.endx.strategicai;

import io.github.endx.rustedfabricapi.api.ai.AiControllers;
import io.github.endx.rustedfabricapi.api.ai.event.AiControlEvents;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import net.fabricmc.api.ModInitializer;

/** Installs one independent controller for every otherwise-unclaimed native AI team. */
public final class StrategicAiMod implements ModInitializer {
    public static final String MOD_ID = "strategic_ai";
    private static final Identifier OWNER = Identifier.of(MOD_ID, "team_controller");

    @Override
    public void onInitialize() {
        if (!Boolean.parseBoolean(System.getProperty(
                "rusted.fabric.strategicAi.enabled", "true"))) {
            System.out.println("[Strategic AI] Installed but disabled by system property");
            return;
        }
        AiControlEvents.BEFORE_TICK.register(context -> {
            if (AiControllers.isAssigned(context.rawTeam())) return;
            AiControllers.assign(context.rawTeam(), OWNER,
                    new StrategicAiController(context.team().id()));
            System.out.println("[Strategic AI] Claimed AI team " + context.team().id());
        });
        System.out.println("[Strategic AI] Waiting for native AI teams");
    }
}
