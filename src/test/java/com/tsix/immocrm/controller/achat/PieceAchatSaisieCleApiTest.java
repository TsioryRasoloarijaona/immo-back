package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
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
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.modele;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.modifier;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a Vérification n° 8 (FEATURE-achat-fiche-creation.md) : aucune clé d'API, même à toutes les portées, n'appelle
 * {@code GET …/modele}, {@code POST /documents}, {@code PUT /documents/{id}} (401/403, jamais 2xx) ; chaque route a son témoin
 * 2xx pour le compte connecté (une route absente répond 404/405, le témoin le révèle).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatSaisieCleApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private Cadre c;
    private Saisie s;
    private AdmUser compte;
    private String cle;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ASK");
        s = fixture.configurationSaisie(c.jeton());
        compte = fixture.compteHabilite(c, "ASK", ADMINISTRATEUR);
        fixture.sansPersonnel(compte);

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

    private UUID bc() {
        UUID id = fixture.piece(c, "-BC", FixtureAchat.BON_COMMANDE);
        fixture.poser(id, "StrEntityId", s.gestion(FixtureAchat.GESTION_BC));
        fixture.poser(id, "BuyDocumentStatusId", s.enCours());
        return id;
    }

    private MvcResult parCompte(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.with(connecte(compte, ROLE_JWT))).andReturn();
    }

    private MvcResult parCle(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.header(ApiKeyAuthenticationFilter.EN_TETE, cle)).andReturn();
    }

    private static void assertRefuse(MvcResult r) throws Exception {
        assertThat(statut(r)).as(corps(r)).isIn(401, 403);
    }

    @Test
    void v8_modele_temoinCompteConnecte_200() throws Exception {
        MvcResult r = parCompte(modele(FixtureAchat.BON_COMMANDE));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    @Test
    void v8_modele_cleApiToutesPortees_refusee() throws Exception {
        assertRefuse(parCle(modele(FixtureAchat.BON_COMMANDE)));
    }

    @Test
    void v8_creation_temoinCompteConnecte_201() throws Exception {
        MvcResult r = parCompte(creer(corpsValide(c, s, FixtureAchat.BON_COMMANDE)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
    }

    @Test
    void v8_creation_cleApiToutesPortees_refusee_rienEcrit() throws Exception {
        MvcResult r = parCle(creer(corpsValide(c, s, FixtureAchat.BON_COMMANDE)));

        assertRefuse(r);
        fixture.oublier();
        assertAll(
                () -> assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero(),
                () -> assertThat(fixture.valeurSequence(FixtureAchat.SEQUENCES_SAISIE.get(FixtureAchat.BON_COMMANDE).code()))
                        .isEqualTo(FixtureAchat.SEQUENCES_SAISIE.get(FixtureAchat.BON_COMMANDE).valeur()));
    }

    @Test
    void v8_modification_temoinCompteConnecte_200() throws Exception {
        MvcResult r = parCompte(modifier(bc(), corpsValide(c, s, FixtureAchat.BON_COMMANDE)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    @Test
    void v8_modification_cleApiToutesPortees_refusee_pieceIntacte() throws Exception {
        UUID piece = bc();

        assertRefuse(parCle(modifier(piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("notes", "par la clé"))));
        fixture.oublier();
        assertThat(fixture.pieceEnBase(piece)).containsEntry("Notes", null);
    }
}
