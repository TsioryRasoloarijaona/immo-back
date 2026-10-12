package com.tsix.immocrm.services.referentiel;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Résolution par {@code Code} et repli sur la PKey de production. Les tests réécrivent le référentiel
 * dans leur transaction (annulée) : la base de CI le sème, {@code yesRemsTest} ne l'a pas.
 */
@SpringBootTest
@Transactional
class ReferencesOmranTest {

    @Autowired
    private ReferencesOmran references;

    @Autowired
    private EntityManager entityManager;

    private void executer(String sql, Object... parametres) {
        var requete = entityManager.createNativeQuery(sql);
        for (int i = 0; i < parametres.length; i++) {
            requete.setParameter(i + 1, parametres[i]);
        }
        requete.executeUpdate();
    }

    private void viderLeReferentiel() {
        executer("DELETE FROM StrEntity WHERE Code IN (?1, ?2, ?3)", ReferencesOmran.CODE_ENTITE_DOSSIER,
                ReferencesOmran.CODE_ENTITE_ENCAISSEMENT, ReferencesOmran.CODE_ENTITE_DECAISSEMENT);
        executer("DELETE FROM AdmModule WHERE Code = ?1", ReferencesOmran.CODE_MODULE_VENTE);
        executer("DELETE FROM AdmParamsGuid WHERE Code = ?1", ReferencesOmran.PARAMETRE_DEVISE_PAR_DEFAUT);
        executer("DELETE FROM StlCurrency WHERE Code = ?1", ReferencesOmran.CODE_DEVISE_REPLI);
    }

    @Test
    void lignesAbsentes_repliSurLesPKeyDeProduction() {
        viderLeReferentiel();

        assertAll(
                () -> assertThat(references.entiteDossier()).isEqualTo(ReferencesOmran.ID_ENTITE_DOSSIER),
                () -> assertThat(references.entiteReglement(true)).isEqualTo(ReferencesOmran.ID_ENTITE_ENCAISSEMENT),
                () -> assertThat(references.entiteReglement(false)).isEqualTo(ReferencesOmran.ID_ENTITE_DECAISSEMENT),
                () -> assertThat(references.moduleVente()).isEqualTo(ReferencesOmran.ID_MODULE_VENTE),
                () -> assertThat(references.deviseParDefaut()).isEqualTo(ReferencesOmran.ID_DEVISE_MAD));
    }

    @Test
    void lignesPresentes_resoluesParCodePasParConstante() {
        viderLeReferentiel();
        UUID encaissement = UUID.randomUUID();
        UUID module = UUID.randomUUID();
        UUID devise = UUID.randomUUID();
        executer("INSERT INTO StrEntity (PKey, Code) VALUES (?1, ?2)", encaissement, ReferencesOmran.CODE_ENTITE_ENCAISSEMENT);
        executer("INSERT INTO AdmModule (PKey, Code) VALUES (?1, ?2)", module, ReferencesOmran.CODE_MODULE_VENTE);
        executer("INSERT INTO StlCurrency (PKey, Code) VALUES (?1, ?2)", devise, "EUR-TEST");
        executer("INSERT INTO AdmParamsGuid (Code, CodeValue) VALUES (?1, ?2)",
                ReferencesOmran.PARAMETRE_DEVISE_PAR_DEFAUT, devise);

        assertAll(
                () -> assertThat(references.entiteReglement(true)).isEqualTo(encaissement),
                () -> assertThat(references.moduleVente()).isEqualTo(module),
                () -> assertThat(references.deviseParDefaut()).isEqualTo(devise));
    }

    @Test
    void sansParametreDevise_deviseMadParCode() {
        viderLeReferentiel();
        UUID mad = UUID.randomUUID();
        executer("INSERT INTO StlCurrency (PKey, Code) VALUES (?1, ?2)", mad, ReferencesOmran.CODE_DEVISE_REPLI);

        assertThat(references.deviseParDefaut()).isEqualTo(mad);
    }
}
