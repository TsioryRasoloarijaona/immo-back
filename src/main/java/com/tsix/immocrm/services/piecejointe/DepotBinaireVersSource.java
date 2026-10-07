package com.tsix.immocrm.services.piecejointe;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

/** Mode deux-bases : écrit le binaire sur la source de vérité. */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class DepotBinaireVersSource implements DepotBinaireSource {

    private static final Logger log = LoggerFactory.getLogger(DepotBinaireVersSource.class);

    private final EntityManager entityManager;
    private final JdbcTemplate surSource;
    private volatile Boolean colonnePresente;

    public DepotBinaireVersSource(EntityManager entityManager,
                                  @Qualifier("ecriture") DataSource source) {
        this.entityManager = entityManager;
        this.surSource = new JdbcTemplate(source);
    }

    // MANDATORY : même connexion routée que l'INSERT des métadonnées ; sur une autre, l'UPDATE
    // attendrait le verrou de l'INSERT non validé. Hibernate vide la session avant une requête native.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deposer(UUID pieceId, byte[] contenu) {
        if (!colonnePresente()) {
            return;
        }
        int lignes = entityManager.createNativeQuery(
                        "UPDATE dbo.AdmAttachedFile SET AttachedFile = ?1 WHERE PKey = ?2")
                .setParameter(1, contenu)
                // En chaîne, pas en UUID : la conversion nvarchar -> uniqueidentifier est définie
                // par SQL Server, là où la représentation binaire d'un paramètre UUID dépend du
                // mapping du pilote.
                .setParameter(2, pieceId.toString())
                .executeUpdate();
        if (lignes != 1) {
            // Appel hors contrat ; l'exception annule tout ensemble, métadonnées et objet S3.
            throw new IllegalStateException(
                    "Binaire non écrit sur la source : ligne AdmAttachedFile introuvable pour " + pieceId);
        }
    }

    // Colonne absente (copie de dev passée par la migration 004) : tolérée avec un avertissement.
    private boolean colonnePresente() {
        Boolean presente = colonnePresente;
        if (presente == null) {
            presente = surSource.queryForObject(
                    "SELECT COL_LENGTH('dbo.AdmAttachedFile', 'AttachedFile')", Integer.class) != null;
            colonnePresente = presente;
            if (!presente) {
                log.warn("La source de vérité n'a pas la colonne AdmAttachedFile.AttachedFile : "
                        + "les pièces déposées par le CRM n'auront pas de binaire côté légataire.");
            }
        }
        return presente;
    }
}
