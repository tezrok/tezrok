package io.tezrok.api.builder;

import java.io.Writer;

/**
 * Single file generator (builder).
 */
public interface Builder extends Initable {
    /**
     * Returns relative location of the file.
     */
    String getPath();

    /**
     * Returns file name.
     */
    String getFileName();

    /**
     * Generate the file.
     */
    void build(Writer writer);

    /**
     * Indicates that file will be generated if one does not already exists.
     */
    boolean isCustomCode();
}
