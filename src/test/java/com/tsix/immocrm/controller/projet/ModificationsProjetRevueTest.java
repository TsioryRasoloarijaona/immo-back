package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Compte;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFS_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.MODIFICATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue N2a (FEATURE-n2-travaux-chantier.md) : colonne Téléphone de la liste Modifications. OMRAN
 * ({@code Modification.cs:240}) affiche Mobile + Phone + Phone1 + Phone2 ; en prod 324 acquéreurs sur 395 n'ont que le
 * mobile. Le @Schema de {@code telephone} annonce « fixe, autres, mobile ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModificationsProjetRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void telephone_acquereurAvecSeulementUnMobile_estRendu() throws Exception {
        fixture.preparer();
        AppelsTravauxChantier appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        String jeton = FixtureProjetLots.jeton();
        CfgTranche tranche = vente.tranche("TRV-" + jeton, true);
        ComFolder dossier = fixture.dossier("DRV-" + jeton, tranche, false);
        UUID modification = fixture.modification(dossier, "ENCOURS", "Mobile " + jeton,
                LocalDateTime.of(2077, 3, 10, 0, 0));
        fixture.flush();
        jdbc.update("UPDATE CfgTier SET Mobile = N'0661000000', Phone = NULL, Phone1 = NULL, Phone2 = NULL"
                + " WHERE PKey = (SELECT CfgCustomerId FROM ComFolder WHERE PKey = ?)", dossier.getId().toString());
        Compte lecteur = fixture.compte(jeton + "m", false, List.of(), DROIT_MODIFS_LIRE);
        fixture.habiliterTranche(lecteur.user(), tranche.getId());

        Reponse r = appels.appeler(get(MODIFICATIONS + "/" + modification), lecteur.connexion());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String telephone = r.lire("$.telephone");
        assertThat(telephone).as(r.corps()).isNotNull().contains("0661000000");
    }
}
