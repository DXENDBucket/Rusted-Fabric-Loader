package io.github.endx.rustedfabricloader;

public final class RustedFabricLoaderMain {
    private RustedFabricLoaderMain() {}

    public static void main(String[] args) {
        net.fabricmc.loader.impl.launch.knot.KnotClient.main(args);
    }
}
