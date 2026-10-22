package com.tsix.immocrm.exceptions;

import java.util.UUID;

/** Prospection terrain déjà transformée → 409 avec {@code terrainId} (FEATURE-patrimoine-prospections-terrain.md §5.7). */
public class ProspectionDejaTransformeeException extends ConflictException {

    private final UUID terrainId;

    public ProspectionDejaTransformeeException(String message, UUID terrainId) {
        super(message);
        this.terrainId = terrainId;
    }

    public UUID getTerrainId() {
        return terrainId;
    }
}
