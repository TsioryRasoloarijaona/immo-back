package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.SyntheseMontantDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue P9f : le calcul par lot rend, dossier par dossier, les mêmes montants que le calcul unitaire. */
@SpringBootTest
@Transactional
class MontantsDossierLotRevueTest {

    @Autowired
    MontantsDossier montants;

    @Autowired
    FixtureImpressionsTranche fixture;

    @Autowired
    EntityManager em;

    @Test
    void calculParLot_egalAuCalculUnitaire() {
        Jeu jeu = fixture.jeu();
        String j = jeu.jeton();
        StrStatus statut = fixture.statut(CONCRETISATION);
        ComFolder a = fixture.prix(fixture.dossier(jeu, j + "-LA", statut), "1200000");
        fixture.echeance(a, Nature.AVANCE, "300000", "250000.123456", "1000");
        fixture.echeance(a, Nature.FRAIS, "21250", "21250", null);
        fixture.echeance(a, Nature.CREDIT, "900000", null, null);
        ComFolder b = fixture.prix(fixture.dossier(jeu, j + "-LB", statut), "100000");
        fixture.echeance(b, Nature.AVANCE, "100000", "100000.01", null);
        ComFolder sansEcheance = fixture.prix(fixture.dossier(jeu, j + "-LC", statut), "50000");
        fixture.relire();

        List<ComFolder> dossiers = List.of(em.find(ComFolder.class, a.getId()), em.find(ComFolder.class, b.getId()),
                em.find(ComFolder.class, sansEcheance.getId()));
        Map<UUID, MontantsDossierDto> lot = montants.calculer(dossiers);

        assertThat(lot).hasSize(3);
        for (ComFolder f : dossiers) {
            MontantsDossierDto unitaire = montants.calculer(f);
            MontantsDossierDto parLot = lot.get(f.getId());
            String id = f.getCode();
            egal(id + " prixVente", parLot.prixVente(), unitaire.prixVente());
            egal(id + " netAPayer", parLot.netAPayer(), unitaire.netAPayer());
            assertThat(parLot.dossierSorti()).as(id).isEqualTo(unitaire.dossierSorti());
            assertThat(parLot.tropPercu()).as(id + " tropPercu")
                    .usingComparator(Comparator.nullsFirst(Comparator.<BigDecimal>naturalOrder()))
                    .isEqualTo(unitaire.tropPercu());
            assertThat(parLot.parCategorie()).as(id + " catégories").hasSameSizeAs(unitaire.parCategorie());
            for (CategorieMontantDto u : unitaire.parCategorie()) {
                CategorieMontantDto l = parLot.parCategorie().stream()
                        .filter(c -> c.categorie().equals(u.categorie())).findFirst().orElseThrow();
                assertThat(l.nombreLignes()).as(id + " " + u.categorie()).isEqualTo(u.nombreLignes());
                assertThat(l.montant()).as(id + " " + u.categorie()).isEqualByComparingTo(u.montant());
                assertThat(l.regle()).as(id + " " + u.categorie()).isEqualByComparingTo(u.regle());
                assertThat(l.reste()).as(id + " " + u.categorie()).isEqualByComparingTo(u.reste());
            }
        }
        assertThat(lot.get(a.getId()).prixVente().reste()).isPositive();
        assertThat(lot.get(b.getId()).prixVente().reste()).isNegative();
    }

    private static void egal(String quoi, SyntheseMontantDto a, SyntheseMontantDto b) {
        assertThat(a.montant()).as(quoi + " montant").isEqualByComparingTo(b.montant());
        assertThat(a.regle()).as(quoi + " regle").isEqualByComparingTo(b.regle());
        assertThat(a.reste()).as(quoi + " reste").isEqualByComparingTo(b.reste());
    }
}
