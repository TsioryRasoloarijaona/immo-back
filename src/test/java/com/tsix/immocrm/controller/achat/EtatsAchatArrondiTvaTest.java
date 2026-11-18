package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureEtatsAchat.date;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue N7a — R1 : OMRAN somme les TVA de ligne non arrondies ({@code VatStatement.VatTotal}, {@code DocumentLine.VatAmount}).
 * Trois lignes 1 × 0,05 à 10 % : Σ exacte 0,015 → total 0,02 ; Σ des lignes arrondies une à une → 0,03.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatArrondiTvaTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureEtatsAchat fixture;

    private AdmUser compte;

    @BeforeEach
    void setUp() {
        String jeton = FixtureAchat.jeton("N7R");
        UUID societe = achat.societe("SOC-" + jeton);
        UUID projet = achat.projet("PRJ-" + jeton, societe);
        String nom = "Fournisseur arrondi " + jeton;
        UUID fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        UUID valide = achat.statut("VAL-" + jeton, true, true, 10);
        compte = fixture.compte("N7R-" + jeton, societe, projet, FixtureEtatsAchat.DROIT_TVA);
        UUID facture = fixture.piece(jeton + "-F", FixtureAchat.FACTURE, date(5, 5), fournisseur, nom, societe, projet,
                valide, "1");
        fixture.ligne(facture, 1, "1", "0.05", "0", "0.10");
        fixture.ligne(facture, 2, "1", "0.05", "0", "0.10");
        fixture.ligne(facture, 3, "1", "0.05", "0", "0.10");
    }

    @Test
    void r1_totalTva_sommeDesTvaDeLigneNonArrondies_commeOmran() throws Exception {
        String json = corps(mockMvc, annee(TVA, connecte(compte, FixtureEtatsAchat.role())));

        assertThat(nombre(json, "$.totaux.montantTva")).isEqualByComparingTo("0.02");
    }
}
