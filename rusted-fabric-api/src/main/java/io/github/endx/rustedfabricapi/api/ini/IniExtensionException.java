package io.github.endx.rustedfabricapi.api.ini;

/** Context-rich failure raised while decoding, validating, or applying an extension. */
public final class IniExtensionException extends IllegalArgumentException {
    public IniExtensionException(String message) { super(message); }
    public IniExtensionException(String message, Throwable cause) { super(message, cause); }
}
