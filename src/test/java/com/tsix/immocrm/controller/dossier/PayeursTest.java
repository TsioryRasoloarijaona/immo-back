package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmIdentityDocumentTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/payeurs} — la liste qui alimente le champ « Réglé par », devenu
 * obligatoire à la création d'un règlement. Voir {@link com.tsix.immocrm.services.dossier.PayeurService}
 * pour pourquoi cette liste est indicative et non contraignante.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PayeursTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComFolderCustomerRepository folderCustomerRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private AdmIdentityDocumentTypeRepository identityDocumentTypeRepository;

    private CfgTier newTier(String nom) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("TIER-PAY-" + UUID.randomUUID());
        tier.setDescription(nom);
        return tierRepository.save(tier);
    }

    private ComFolder newFolder(CfgTier acquereur) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PAY-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-PAY-" + UUID.randomUUID());
        folder.setStkVocation(vocation);
        folder.setCfgCustomer(acquereur);
        return folderRepository.save(folder);
    }

    private void addCoAcquereur(ComFolder folder, CfgTier tier, String quotePart) {
        ComFolderCustomer lien = new ComFolderCustomer();
        lien.setId(UUID.randomUUID());
        lien.setComFolder(folder);
        lien.setCfgTier(tier);
        if (quotePart != null) {
            lien.setPartPercent(new BigDecimal(quotePart));
        }
        folderCustomerRepository.save(lien);
    }

    @Test
    void getPayeurs_renvoieAcquereurPrincipalPuisCoAcquereurs() throws Exception {
        CfgTier acquereur = newTier("Fouad EL IHFADI");
        ComFolder folder = newFolder(acquereur);
        addCoAcquereur(folder, newTier("Salma BENANI"), "40.00");

        mockMvc.perform(get("/api/dossiers/{id}/payeurs", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(acquereur.getId().toString()))
                .andExpect(jsonPath("$[0].nom").value("Fouad EL IHFADI"))
                .andExpect(jsonPath("$[0].role").value("ACQUEREUR_PRINCIPAL"))
                // L'acquéreur principal n'a pas de ligne ComFolderCustomer, donc pas de quote-part.
                .andExpect(jsonPath("$[0].quotePart").doesNotExist())
                .andExpect(jsonPath("$[1].nom").value("Salma BENANI"))
                .andExpect(jsonPath("$[1].role").value("CO_ACQUEREUR"))
                .andExpect(jsonPath("$[1].quotePart").value(40.00));
    }

    /** Le format doit être celui imprimé sur l'autorisation d'encaissement, pour désigner la même
     * personne de la même façon à l'écran et sur le document. */
    @Test
    void getPayeurs_exposeLaPieceIdentiteAuFormatDuDocument() throws Exception {
        AdmIdentityDocumentType cin = new AdmIdentityDocumentType();
        cin.setId(UUID.randomUUID());
        cin.setCode("CIN");
        cin.setDescription("Carte d'identité nationale");
        AdmIdentityDocumentType cinPersiste = identityDocumentTypeRepository.save(cin);

        CfgTier acquereur = newTier("Fouad EL IHFADI");
        acquereur.setAdmIdentityDocumentType(cinPersiste);
        acquereur.setIDNumber("E400556");
        tierRepository.save(acquereur);

        ComFolder folder = newFolder(acquereur);

        mockMvc.perform(get("/api/dossiers/{id}/payeurs", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pieceIdentite").value("CIN: E400556"));
    }

    @Test
    void getPayeurs_sansPieceIdentite_omisPlutotQueVide() throws Exception {
        ComFolder folder = newFolder(newTier("Karim ALAMI"));

        mockMvc.perform(get("/api/dossiers/{id}/payeurs", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pieceIdentite").doesNotExist());
    }

    /** Un même tiers peut être à la fois {@code CfgCustomerId} et ligne {@code ComFolderCustomer} :
     * la liste déroulante ne doit pas le proposer deux fois. */
    @Test
    void getPayeurs_neDupliquePasUnTiersALaFoisAcquereurEtCoAcquereur() throws Exception {
        CfgTier acquereur = newTier("Fouad EL IHFADI");
        ComFolder folder = newFolder(acquereur);
        addCoAcquereur(folder, acquereur, "50.00");
        addCoAcquereur(folder, newTier("Salma BENANI"), "50.00");

        mockMvc.perform(get("/api/dossiers/{id}/payeurs", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("ACQUEREUR_PRINCIPAL"))
                .andExpect(jsonPath("$[1].nom").value("Salma BENANI"));
    }

    @Test
    void getPayeurs_dossierSansAcquereur_renvoieListeVide() throws Exception {
        ComFolder folder = newFolder(null);

        mockMvc.perform(get("/api/dossiers/{id}/payeurs", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPayeurs_dossierInconnu_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/payeurs", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    /** Les co-acquéreurs d'un autre dossier ne doivent pas fuir dans cette liste. */
    @Test
    void getPayeurs_neMelangePasLesDossiers() throws Exception {
        ComFolder dossier1 = newFolder(newTier("Fouad EL IHFADI"));
        ComFolder dossier2 = newFolder(newTier("Karim ALAMI"));
        addCoAcquereur(dossier2, newTier("Salma BENANI"), "50.00");

        mockMvc.perform(get("/api/dossiers/{id}/payeurs", dossier1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("Fouad EL IHFADI"));
    }
}
