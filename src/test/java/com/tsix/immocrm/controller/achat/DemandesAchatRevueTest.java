package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.TOUS_LES_DROITS;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue N6a : contrat documenté de {@code executerTransition} (api-docs DemandesAchatController.md) contre le code. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat fixture;

    private DemandesAchatAppels appels;
    private Cadre c;
    private UUID lancer;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = fixture.cadre("RV");
        lancer = fixture.transition(c.entite(), c.prep(), c.depouil(), "Lancer " + c.jeton(), 10);
    }

    /** api-docs : « 404 → 403 (lecture) → … » ; la fiche est refusée (403) mais le POST la renvoie en 200. */
    @Test
    void executer_sansDroitDeLecture_403CommeLaFiche() throws Exception {
        AdmUser sansDroit = fixture.restreint(c, "n6.rv.sd." + c.jeton(), List.of());
        fixture.intervenantRole(c.entite(), fixture.roleDe(sansDroit), lancer, false);
        UUID dia = fixture.demande(c, c.code("RV1"), c.prep());

        Reponse fiche = appels.fiche(qui(sansDroit), dia);
        Reponse r = appels.executer(qui(sansDroit), dia, lancer);

        assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(403);
        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    /** Invariant 3 et api-docs : 400 (transitionId absent) avant 409 (lecture seule). */
    @Test
    void executer_nonDerniere_transitionIdAbsent_400AvantLe409() throws Exception {
        AdmUser restreint = fixture.restreint(c, "n6.rv.nd." + c.jeton(), TOUS_LES_DROITS);
        fixture.intervenantRole(c.entite(), fixture.roleDe(restreint), lancer, false);
        UUID dia = fixture.demande(c, c.code("RV2"), c.prep());
        fixture.poser(FixtureDemandesAchat.DEMANDE, dia, "IsTheLast", false);

        Reponse r = appels.executer(qui(restreint), dia, corps());

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }
}
