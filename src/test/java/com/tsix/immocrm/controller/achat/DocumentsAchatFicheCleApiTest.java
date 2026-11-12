package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A2a, E6 + décision du user 12/11 (clés API n8n) : aucune clé d'API, même à toutes les portées, ne lit la fiche
 * {@code GET /api/achat/documents/{id}} ; témoin 200 pour le compte connecté (Vérification n° 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFicheCleApiTest {

    private static final String ROLE = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private UUID piece;
    private AdmUser compte;
    private String cle;

    @BeforeEach
    void setUp() {
        String jeton = FixtureAchat.jeton("AFK");
        UUID societe = fixture.societe("SOC-" + jeton);
        UUID projet = fixture.projet("P-" + jeton, societe);
        String nom = "Fournisseur " + jeton;
        piece = fixture.piece(Piece.facture(jeton + "-BC", LocalDate.of(2026, 3, 10),
                fixture.fournisseur("FOU-" + jeton, nom), nom, societe, projet, new BigDecimal("100"))
                .nature(FixtureAchat.BON_COMMANDE));
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
        fixture.poser(piece, "CfgTierTriggerId", compte.getCfgEmployee().getId());

        cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
    }

    @Test
    void e6_temoin_compteConnecte_fiche200() throws Exception {
        int statut = mockMvc.perform(get(AppelsAchat.DOCUMENT, piece).with(connecte(compte, ROLE)))
                .andReturn().getResponse().getStatus();

        assertThat(statut).isEqualTo(200);
    }

    @Test
    void e6_cleApiToutesPortees_ficheRefusee_niPas200NiPas404() throws Exception {
        int statut = mockMvc.perform(get(AppelsAchat.DOCUMENT, piece).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andReturn().getResponse().getStatus();

        assertThat(statut).isIn(401, 403);
    }
}
