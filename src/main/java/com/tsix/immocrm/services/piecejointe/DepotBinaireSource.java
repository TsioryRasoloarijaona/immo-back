package com.tsix.immocrm.services.piecejointe;

import java.util.UUID;

/**
 * Binaire dans {@code AdmAttachedFile.AttachedFile} de la source, seule colonne que lit OMRAN. SQL
 * natif, pas de mapping : la colonne n'existe que sur la source (FILESTREAM). Voir
 * {@code FEATURE-source-de-verite-115.md} « Pièces jointes ».
 */
public interface DepotBinaireSource {

    /** La ligne {@code AdmAttachedFile} de {@code pieceId} doit avoir été insérée <b>dans la transaction courante</b>. */
    void deposer(UUID pieceId, byte[] contenu);
}
