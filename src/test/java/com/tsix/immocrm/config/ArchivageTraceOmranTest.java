package com.tsix.immocrm.config;

import com.tsix.immocrm.config.ArchivageTraceOmran.LigneAudit;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.tsix.immocrm.config.ArchivageTraceOmran.APPLICATION_OMRAN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * La partie du cycle qui se trompe cher : l'audit n'offre aucune clé naturelle (mesuré —
 * 18 848 lignes strictement identiques dans le fichier brut), donc l'exactement-une-fois
 * repose entièrement sur la forme de la requête de lecture : borne large ({@code >=}, le tick
 * frontière purgé est rejoué), lecture ordonnée (un échec laisse un préfixe temporel, pas des
 * trous), borne haute (ne jamais lire un tick encore en cours d'écriture). Le cycle réel
 * demande deux bases et l'audit actif ; il se vérifie à la main
 * (cf. FEATURE-archivage-trace-omran.md).
 */
class ArchivageTraceOmranTest {

    private static final LocalDateTime BORNE = LocalDateTime.of(2026, 9, 22, 8, 0);

    @Test
    void sansFiligraneLaPremierePasseLitTout() {
        assertThat(ArchivageTraceOmran.requeteLecture(null))
                .doesNotContain("event_time >=");
        assertThat(ArchivageTraceOmran.parametresLecture("C:\\backup\\*.sqlaudit", null))
                .containsExactly("C:\\backup\\*.sqlaudit", APPLICATION_OMRAN);
    }

    @Test
    void avecFiligraneLaBorneEstRejoueeEnLargeEtNonEnStrict() {
        // >= et non > : la passe purge le tick frontière de l'archive puis le relit entier.
        // Un > supposerait le tick complet — faux après un échec à mi-lot, batchUpdate
        // commite instruction par instruction.
        assertThat(ArchivageTraceOmran.requeteLecture(BORNE)).contains("event_time >= ?");
        assertThat(ArchivageTraceOmran.parametresLecture("C:\\backup\\*.sqlaudit", BORNE))
                .containsExactly("C:\\backup\\*.sqlaudit", APPLICATION_OMRAN, BORNE);
    }

    @Test
    void laLectureEstOrdonneeParHorodatage() {
        // Sans cet ordre, un échec à mi-passe laisserait des trous temporels derrière le
        // MAX archivé — que >= ne comblerait jamais.
        assertThat(ArchivageTraceOmran.requeteLecture(null)).endsWith(" ORDER BY event_time");
        assertThat(ArchivageTraceOmran.requeteLecture(BORNE)).endsWith(" ORDER BY event_time");
    }

    @Test
    void laBorneHauteEcarteLesTicksEncoreEnCoursDEcriture() {
        // Évaluée sur l'horloge de la source (SYSUTCDATETIME — l'audit horodate en UTC),
        // pas sur celle du backend.
        assertThat(ArchivageTraceOmran.requeteLecture(null))
                .contains("event_time <= DATEADD(MINUTE, -"
                        + ArchivageTraceOmran.MARGE_MINUTES + ", SYSUTCDATETIME())");
    }

    @Test
    void seulesLesRequetesOmranSontFiltrees() {
        // Le filtre s'applique côté serveur : la synchro JDBC et l'outillage, majoritaires
        // dans l'audit, ne traversent jamais le réseau.
        assertThat(ArchivageTraceOmran.requeteLecture(null))
                .contains("WHERE application_name = ?");
        assertThat(APPLICATION_OMRAN).isEqualTo("OMRAN estate");
    }

    @Test
    void lesParametresDInsertionSuiventLOrdreDesColonnes() {
        LigneAudit ligne = new LigneAudit(BORNE, "SL", true, "sasa", "yesRems", "dbo",
                "ComProspection", "SELECT * FROM ComProspection", "127.0.0.1",
                "C:\\backup\\audit_1.sqlaudit", 1999872L, 1);

        assertThat(ArchivageTraceOmran.parametresInsertion(ligne)).containsExactly(
                BORNE, "SL", true, "sasa", "yesRems", "dbo", "ComProspection",
                "SELECT * FROM ComProspection", "127.0.0.1",
                "C:\\backup\\audit_1.sqlaudit", 1999872L, 1);
    }

    @Test
    void lActionIdEstNormaliseDesEspacesDeCompletion() {
        // sys.fn_get_audit_file rend un action_id complété d'espaces ('SL  ') : sans
        // normalisation, WHERE ActionId = 'SL' ne rendrait rien à l'analyse.
        LigneAudit ligne = new LigneAudit(BORNE, "SL  ", true, null, null, null, null,
                null, null, "f", 0L, 1);
        assertThat(ligne.actionId()).isEqualTo("SL");
    }
}
