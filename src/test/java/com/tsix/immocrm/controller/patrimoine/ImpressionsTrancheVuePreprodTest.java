package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.services.patrimoine.impressions.SelectionRelance2;
import com.tsix.immocrm.services.patrimoine.impressions.SelectionRelance2.Retenu;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Test manuel P9f (FEATURE-patrimoine-impressions-tranche.md §6), hors CI : {@code -Dmesure.prod=true}, sur yesRemsProd.
 * Rapproche la sélection de la vue OMRAN {@code ComSaleFolderView} et imprime les écarts de montants, dossier par dossier.
 */
@EnabledIfSystemProperty(named = "mesure.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false"})
@Transactional
class ImpressionsTrancheVuePreprodTest {

    private static final String VUE = "SELECT CAST(PKey AS varchar(36)), PriceRest, PriceSettled FROM ComSaleFolderView "
            + "WHERE StrStatusCode IN ('CONCRETISATION','MEILLEURTAUX','ENVOI AU NOTAIRE','CREDIT EN COURS','CREDIT ACCORDE')";
    private static final String TRANCHES = "SELECT DISTINCT CAST(i.StkHierarchyId AS varchar(36)) FROM ComFolder f "
            + "JOIN StrStatus s ON s.PKey = f.StrStatusId JOIN StkItem i ON i.PKey = f.StkItemId "
            + "WHERE s.Code IN ('CONCRETISATION','MEILLEURTAUX','ENVOI AU NOTAIRE','CREDIT EN COURS','CREDIT ACCORDE')";

    @Autowired private SelectionRelance2 selection;
    @Autowired private EntityManager em;

    @Test
    @SuppressWarnings("unchecked")
    void rapprochementAvecLaVue() {
        Map<UUID, Object[]> vue = new HashMap<>();
        for (Object[] l : (List<Object[]>) em.createNativeQuery(VUE).getResultList()) {
            vue.put(UUID.fromString((String) l[0]), l);
        }
        List<Retenu> retenus = new ArrayList<>();
        for (Object t : em.createNativeQuery(TRANCHES).getResultList()) {
            retenus.addAll(selection.selectionner(UUID.fromString((String) t), null).retenus());
        }
        BigDecimal resteVue = BigDecimal.ZERO;
        BigDecimal regleVue = BigDecimal.ZERO;
        for (Retenu r : retenus) {
            Object[] l = vue.get(r.dossier().getId());
            if (l == null) {
                System.out.printf("hors vue : %s%n", r.dossier().getCode());
                continue;
            }
            BigDecimal reste = new BigDecimal(l[1].toString());
            BigDecimal regle = new BigDecimal(l[2].toString());
            resteVue = resteVue.add(reste);
            regleVue = regleVue.add(regle);
            if (reste.compareTo(r.prix().reste()) != 0 || regle.compareTo(r.prix().regle()) != 0) {
                System.out.printf("écart %s : vue reste %s réglé %s / Arpent reste %s réglé %s%n", r.dossier().getCode(),
                        reste, regle, r.prix().reste(), r.prix().regle());
            }
        }
        long lettres = retenus.stream().mapToLong(r -> 1L + r.coAcquereurs().size()).sum();
        BigDecimal sommeReste = resteVue;
        BigDecimal sommeRegle = regleVue;
        assertAll(
                () -> assertThat(retenus).hasSize(80),
                () -> assertThat(lettres).as("lettres avant écartés").isEqualTo(90),
                () -> assertThat(sommeReste).isEqualByComparingTo("38114913.70"),
                () -> assertThat(sommeRegle).isEqualByComparingTo("36389002.00"));
    }
}
