package com.tsix.immocrm.services.piecejointe;

import java.util.UUID;

/**
 * Écrit le binaire d'une pièce jointe dans la colonne {@code AdmAttachedFile.AttachedFile} de la
 * source de vérité — la seule colonne que le logiciel de bureau lit pour afficher un fichier.
 * Sans elle, une pièce déposée par le CRM serait pour lui une case de checklist vide.
 *
 * <p>Le backend, lui, ne lit jamais cette colonne : ses binaires vivent sur S3, pointés par
 * {@code AppFileObject}, et la colonne a été physiquement supprimée de la copie de lecture
 * ({@code 004_drop_attachedfile_filestream.sql} — elle est FILESTREAM côté légataire, et
 * FILESTREAM n'existe pas sur SQL Server Linux). C'est pourquoi l'écriture se fait en SQL natif
 * plutôt qu'en remappant la colonne sur l'entité : la remapper casserait tout déploiement où la
 * colonne n'existe pas, c'est-à-dire tous sauf la source.
 *
 * <p>Deux implémentations, choisies par la même condition d'activation que
 * {@link com.tsix.immocrm.config.DataSourceConfig} : en mono-base il n'y a nulle part où écrire
 * ({@link DepotBinaireAbsent}) ; en deux-bases l'écriture rejoint la transaction qui vient
 * d'insérer les métadonnées ({@link DepotBinaireVersSource}).
 */
public interface DepotBinaireSource {

    /**
     * Dépose {@code contenu} sur la ligne {@code AdmAttachedFile} de {@code pieceId} — qui doit
     * avoir été insérée <b>dans la transaction courante</b>.
     */
    void deposer(UUID pieceId, byte[] contenu);
}
