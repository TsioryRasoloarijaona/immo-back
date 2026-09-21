package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le {@code MERGE} de {@link RemonteeLecture} et, surtout, l'ordre de ses paramètres.
 *
 * <p>C'est la partie du rapatriement qui se trompe sans bruit. Un {@code ?} de décalage entre la
 * clause {@code SET} et la clause {@code VALUES} ne lève aucune erreur si les types concordent :
 * il recopie simplement la mauvaise valeur dans la mauvaise colonne. D'où un test qui compte les
 * substituts et vérifie l'ordre exact, plutôt qu'un test qui se contente de regarder si le SQL
 * « ressemble » à un {@code MERGE}.
 *
 * <p>Le rapatriement réel, lui, demande deux bases distinctes et ne peut pas être éprouvé par
 * cette suite, qui n'en configure qu'une. Il se vérifie à la main — cf. la section Vérification
 * du plan de la feature.
 */
class RemonteeLectureSqlTest {

    private static final List<String> COLONNES = List.of("PKey", "Code", "FirstName", "LastName");

    @Test
    void laCleDeJointurePuisLeSetPuisLInsert() {
        UUID pkey = UUID.fromString("92E51043-F279-42FE-B51D-0003BF098D53");
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("PKey", pkey);
        valeurs.put("Code", "26/14504");
        valeurs.put("FirstName", "Karim");
        valeurs.put("LastName", "MAZZI");

        Object[] parametres = RemonteeLecture.parametres("PKey", COLONNES, valeurs, pkey);

        assertThat(parametres).containsExactly(
                pkey,                                        // USING (SELECT ? AS Cle)
                "26/14504", "Karim", "MAZZI",                // UPDATE SET — PKey exclue
                pkey, "26/14504", "Karim", "MAZZI");         // INSERT VALUES — PKey comprise
    }

    @Test
    void autantDeSubstitutsQueDeParametres() {
        String sql = RemonteeLecture.fusion("CfgTier", "PKey", COLONNES);
        long substituts = sql.chars().filter(c -> c == '?').count();

        // 1 pour la jointure + (n-1) pour le SET + n pour l'INSERT
        assertThat(substituts).isEqualTo(1 + (COLONNES.size() - 1) + COLONNES.size());
    }

    @Test
    void laCleNEstJamaisReaffecteeParLeSet() {
        String sql = RemonteeLecture.fusion("CfgTier", "PKey", COLONNES);
        String clauseSet = sql.substring(sql.indexOf("UPDATE SET"), sql.indexOf("WHEN NOT MATCHED"));

        // Réaffecter la colonne de jointure est refusé par SQL Server, et n'aurait aucun sens :
        // la ligne est justement identifiée par elle.
        assertThat(clauseSet).doesNotContain("[PKey]");
    }

    @Test
    void verrouilleLaLigneLeTempsDeDeciderInsertionOuMiseAJour() {
        // Sans HOLDLOCK, deux rapatriements simultanes sur la meme cle peuvent tous deux
        // conclure « pas trouve » et inserer.
        assertThat(RemonteeLecture.fusion("CfgTier", "PKey", COLONNES)).contains("WITH (HOLDLOCK)");
    }

    @Test
    void crocheteLesIdentifiantsCarLeSchemaLegataireEmploieDesMotsReserves() {
        String sql = RemonteeLecture.fusion("ComFolder", "PKey", List.of("PKey", "Usage"));
        assertThat(sql).contains("dbo.[ComFolder]").contains("[Usage]");
    }

    /**
     * {@code AppFileObject} n'a pas de {@code PKey} : sa clé est {@code AdmAttachedFileId},
     * partagée avec sa ligne {@code AdmAttachedFile}. La fusion doit joindre — et ne jamais
     * réaffecter — cette colonne-là.
     */
    @Test
    void uneTableSansPKeyJointEtProtegeSaPropreCle() {
        List<String> colonnes = List.of("AdmAttachedFileId", "StorageKey", "SizeBytes");
        String sql = RemonteeLecture.fusion("AppFileObject", "AdmAttachedFileId", colonnes);

        assertThat(sql).contains("ON c.[AdmAttachedFileId] = s.Cle");

        String clauseSet = sql.substring(sql.indexOf("UPDATE SET"), sql.indexOf("WHEN NOT MATCHED"));
        assertThat(clauseSet).doesNotContain("[AdmAttachedFileId]").contains("[StorageKey]");

        UUID cle = UUID.fromString("92E51043-F279-42FE-B51D-0003BF098D53");
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("AdmAttachedFileId", cle);
        valeurs.put("StorageKey", "dossiers/x.pdf");
        valeurs.put("SizeBytes", 42L);

        assertThat(RemonteeLecture.parametres("AdmAttachedFileId", colonnes, valeurs, cle))
                .containsExactly(cle, "dossiers/x.pdf", 42L, cle, "dossiers/x.pdf", 42L);
    }
}
