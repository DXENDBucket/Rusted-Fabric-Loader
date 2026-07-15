package io.github.endx.rustedfabric.android.mod;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Developer-facing command line verifier used by the portable mod build convention. */
public final class JavamodVerifierMain {
    private JavamodVerifierMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: JavamodVerifierMain <mod.javamod>");
        }
        Path archive = Paths.get(args[0]).toAbsolutePath().normalize();
        VerifiedModArchive verified = new RustedFabricModVerifier().verify(archive);
        RustedFabricModMetadata metadata = verified.getMetadata();
        System.out.println("Verified .javamod: " + metadata.getId() + " "
                + metadata.getVersion() + " entrypoint=" + metadata.getEntrypoint()
                + " classes=" + verified.getDefinedClasses().size());
    }
}
