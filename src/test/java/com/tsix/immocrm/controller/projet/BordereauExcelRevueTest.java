package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_PU_TTC;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.COL_TVA;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.TYPE_DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.article;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.avec;
import static com.tsix.immocrm.controller.projet.BordereauExcelSupport.classeur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue LC2 : R4 avec TVA = -100 % (division par zéro) doit rester un 400, pas un 500. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BordereauExcelRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureBordereauxEcriture b;

    @BeforeEach
    void setUp() {
        parite.genresDeLot();
        b.tableVentilation();
    }

    @Test
    void tvaMoinsCentAvecPuTtcDonne400() throws Exception {
        String jeton = FixtureProjetLots.jeton();
        UUID projet = parite.projet("RVP-" + jeton, parite.entiteJuridique("RVE-" + jeton));
        UUID statut = parite.statut(ENTREPRENEURS, "RVO-" + jeton);
        b.ouvrirEstimatif(statut);
        UUID categorie = b.categorie("RVC-" + jeton);
        RequestPostProcessor admin = connecte(parite.administrateur("lot.rv." + jeton, projet), "Administrateur");
        UUID lot = parite.lot(projet, "RV-" + jeton, ENTREPRENEURS, statut);
        b.defauts(lot, "0.2", categorie);

        byte[] fichier = classeur(avec(avec(article("A1", 2, null), COL_TVA, -100), COL_PU_TTC, 100));
        Reponse r = new BordereauExcelSupport(mockMvc).importer(admin, lot, TYPE_DEFINITIF, fichier);

        assertThat(r.statut()).isEqualTo(400);
    }
}
