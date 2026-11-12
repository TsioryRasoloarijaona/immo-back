package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.affecter;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.supprimer;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.transition;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A2b-1, Vérification n° 8 : aucune clé d'API, même à toutes les portées, n'écrit par {@code PATCH …/statut},
 * {@code DELETE …/{id}}, {@code PUT …/bon-commande} (refus, jamais 2xx) ; chaque route a son témoin 2xx pour le compte
 * connecté (une route absente répond 404/405, le témoin le révèle). Compte Administrateur (droits) dont le rôle est lié à
 * la transition S0 → S1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatCleApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private Cadre c;
    private AdmUser compte;
    private UUID s0;
    private UUID s1;
    private UUID gestion;
    private String cle;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ACK");
        fixture.tablesComptables();
        fixture.parametreBooleen(FixtureAchat.LIMITE_QUANTITES, true);
        compte = fixture.compteHabilite(c, "ACK", ADMINISTRATEUR);
        fixture.sansPersonnel(compte);
        gestion = config.gestion("G-" + c.jeton(), "Gestion " + c.jeton());
        s0 = config.statut(Statut.simple("S0-" + c.jeton(), 1));
        s1 = config.statut(Statut.simple("S1-" + c.jeton(), 2));
        config.lier(config.transition(gestion, 1, "Valider", s0, s1), fixture.roleDe(compte));

        cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-" + c.jeton());
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
    }

    private UUID piece(String nature) {
        UUID id = fixture.piece(c, "-" + nature + (rang++), nature);
        fixture.poser(id, "StrEntityId", gestion);
        fixture.poser(id, "BuyDocumentStatusId", s0);
        return id;
    }

    private int parCompte(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult r = mockMvc.perform(requete.with(connecte(compte, ROLE_JWT))).andReturn();
        return statut(r);
    }

    private MvcResult parCle(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.header(ApiKeyAuthenticationFilter.EN_TETE, cle)).andReturn();
    }

    private void assertRefuse(MvcResult r) throws Exception {
        assertThat(statut(r)).as(corps(r)).isIn(401, 403);
    }

    @Test
    void v8_transition_temoinCompteConnecte_200() throws Exception {
        assertThat(parCompte(transition(piece(FixtureAchat.FACTURE), s1, null))).isEqualTo(200);
    }

    @Test
    void v8_transition_cleApiToutesPortees_refusee() throws Exception {
        UUID piece = piece(FixtureAchat.FACTURE);

        assertRefuse(parCle(transition(piece, s1, null)));
        fixture.oublier();
        assertThat(fixture.lienDe(piece, "BuyDocumentStatusId")).isEqualTo(AppelsEcrituresAchat.id(s0));
    }

    @Test
    void v8_suppression_temoinCompteConnecte_204() throws Exception {
        assertThat(parCompte(supprimer(piece(FixtureAchat.DEVIS)))).isEqualTo(204);
    }

    @Test
    void v8_suppression_cleApiToutesPortees_refusee() throws Exception {
        UUID piece = piece(FixtureAchat.DEVIS);

        assertRefuse(parCle(supprimer(piece)));
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "PKey", piece)).isEqualTo(1);
    }

    @Test
    void v8_affectation_temoinCompteConnecte_200() throws Exception {
        assertThat(parCompte(affecter(piece(FixtureAchat.FACTURE), piece(FixtureAchat.BON_COMMANDE)))).isEqualTo(200);
    }

    @Test
    void v8_affectation_cleApiToutesPortees_refusee() throws Exception {
        UUID facture = piece(FixtureAchat.FACTURE);

        assertRefuse(parCle(affecter(facture, piece(FixtureAchat.BON_COMMANDE))));
        fixture.oublier();
        assertThat(fixture.lienDe(facture, "BuyOrderId")).isNull();
    }
}
