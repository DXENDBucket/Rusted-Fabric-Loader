package io.github.endx.vulkanmod.resourcestream;

/** A structurally invalid or unsupported reliable ResourceStream. */
public final class ResourceStreamFormatException extends IllegalArgumentException {
    public ResourceStreamFormatException(String message) { super(message); }
}
