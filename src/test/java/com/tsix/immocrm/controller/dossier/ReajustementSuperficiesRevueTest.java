package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue FEATURE-vente-fiche.md §B6 : clé d'API n8n et « tout ou rien » du réajustement. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReajustementSuperficiesRevueTest {

    private static final String REAJUSTEMENT = "/api/tranches/{id}/reajustement-superficies";
    private static final String ADMINISTRATEUR = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;
    @Autowired private EntityManager em;

    private CfgTranche tranche;
    private ComSettlementCategory autofinancement;

    @BeforeEach
    void setUp() {
        tranche = fixture.tranche("REAJ-REV", true);
        autofinancement = fixture.categorieEcheance("AUTOFIN-REV", false, true, false, null);
    }

    private ComFolder dossier(String code, String superficieStock) {
        ComFolder dossier = fixture.dossier(code, tranche, StatutDossier.CONCRETISATION);
        dossier.setPrice(new BigDecimal("1200000"));
        dossier.setPriceAfterDiscount(new BigDecimal("1200000"));
        dossier.setDiscountAmount(BigDecimal.ZERO);
        folderRepository.save(dossier);
        StkItem article = dossier.getStkItem();
        article.setArea(new BigDecimal(superficieStock));
        stkItemRepository.save(article);
        return dossier;
    }

    private String cleReferentiels() {
        String nom = "n8n-rev-" + UUID.randomUUID().toString().substring(0, 8);
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode(nom);
        compte.setDescription(nom);
        compte.setDisabled(false);
        userRepository.save(compte);

        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom(nom);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.of(PorteeCleApi.REFERENTIELS_LIRE)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    @Test
    void cleApiReferentiels_compteDeServiceSansRole_neLitPasLeReajustement_403() throws Exception {
        dossier("DOS-REV-CLE", "105");
        String cle = cleReferentiels();

        mockMvc.perform(get(REAJUSTEMENT, tranche.getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden());
    }

    @Test
    void valider_unDossierSansEcart_409_etAucuneLigneDuLotNEstModifiee() throws Exception {
        ComFolder avecEcart = dossier("DOS-REV-A", "105");
        ComDocument ligne = fixture.echeance(avecEcart, autofinancement, new BigDecimal("1200000"),
                LocalDateTime.of(2026, 1, 1, 0, 0));
        ComFolder sansEcart = dossier("DOS-REV-B", "100");
        fixture.echeance(sansEcart, autofinancement, new BigDecimal("1200000"), LocalDateTime.of(2026, 1, 1, 0, 0));
        AdmUser admin = fixture.compte("ADM-REV", ADMINISTRATEUR);

        mockMvc.perform(post(REAJUSTEMENT, tranche.getId()).with(connecte(admin, ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"%s\",\"%s\"]}".formatted(avecEcart.getId(), sansEcart.getId())))
                .andExpect(status().isConflict());

        em.flush();
        em.clear();
        ComDocument relue = documentRepository.findById(ligne.getId()).orElseThrow();
        ComFolder relu = folderRepository.findById(avecEcart.getId()).orElseThrow();
        assertAll(
                () -> assertThat(relue.getAmount()).isEqualByComparingTo("1200000"),
                () -> assertThat(relu.getArea()).isEqualByComparingTo("100"));
    }
}
