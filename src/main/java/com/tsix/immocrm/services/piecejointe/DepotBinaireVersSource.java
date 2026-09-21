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

/**
 * Mode deux-bases : écrit le binaire sur la source de vérité, <b>dans la transaction qui vient
 * d'insérer la ligne {@code AdmAttachedFile}</b>.
 *
 * <p><b>{@code MANDATORY} n'est pas décoratif</b> (même contrat que
 * {@code GenerateurCodeSequence}) : exécuté dans la transaction d'écriture, l'{@code UPDATE}
 * passe par l'{@code EntityManager}, donc sur la <b>même connexion</b> — routée vers la source —
 * que l'{@code INSERT} des métadonnées. Il voit la ligne non validée, et un rollback emporte les
 * deux ensemble (ainsi que l'objet S3, via la synchronisation posée par
 * {@code PieceJointeSupport}). Sur une connexion séparée, l'{@code UPDATE} attendrait le verrou
 * de l'{@code INSERT} jusqu'au timeout — la ligne n'étant pas encore validée.
 *
 * <p>L'ordre {@code INSERT} puis {@code UPDATE} est garanti par Hibernate lui-même : une requête
 * native a un espace de requête inconnu, il vide donc la session avant de l'exécuter.
 *
 * <p>La colonne est sondée une fois sur la source ({@code COL_LENGTH}) : une source qui ne l'a
 * pas — une copie de développement passée par la migration 004 — est tolérée avec un
 * avertissement, pas une erreur, pour que le montage deux-bases reste testable sur n'importe
 * quelle paire locale. Face au vrai légataire, la colonne existe.
 */
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
            // La ligne doit exister dans cette transaction ; son absence signale un appel hors
            // contrat. L'exception fait tout annuler ensemble : métadonnées, et objet S3.
            throw new IllegalStateException(
                    "Binaire non écrit sur la source : ligne AdmAttachedFile introuvable pour " + pieceId);
        }
    }

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
