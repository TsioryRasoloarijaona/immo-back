package com.tsix.immocrm.controller.suivicommerciaux;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.JOUR;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.a;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.cle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R8 — FEATURE-suivi-commerciaux.md §2, §7.2–7.3 (règle 6, seconde revue). Jeu :
 * <pre>
 *   projet P1 : chef C (Commercial Chef) et membre M (Commercial) affectés ; projet P2 : O (Commercial) affecté
 *   JOUR : appels joints de C, M et O sur des prospections de P1 ; M joint aussi une personne d'une entité inactive
 *   admin A (Administrateur) et directeur D (Direction Générale), non commerciaux, ont un appel joint sur P1
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxVisibiliteTest {

    private static final String URL = "/api/suivi-commerciaux";
    private static final String URL_SYNTHESE = URL + "/synthese";
    private static final String COMMERCIAL = "Commercial";
    private static final String CHEF = "Commercial Chef";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String DIRECTION = "Direction Générale";
    private static final String DU = "du";
    private static final String AU = "au";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviCommerciaux fixture;

    private String prefixe;
    private AdmUser chef;
    private AdmUser membre;
    private AdmUser autre;
    private AdmUser admin;
    private AdmUser directeur;
    private ComActionMessage appelChef;
    private ComActionMessage appelMembre;
    private ComActionMessage appelInactif;
    private String personneInactive;
    private String personneMembre;

    @BeforeEach
    void setUp() {
        prefixe = FixtureSuiviCommerciaux.prefixe("SCV");
        CfgTranche t1 = fixture.tranche("TR1-" + prefixe, true);
        CfgTranche t2 = fixture.tranche("TR2-" + prefixe, true);
        CfgTranche inactive = fixture.tranche("TRI-" + prefixe, false);
        chef = fixture.compte("scv.chef." + prefixe, CHEF);
        membre = fixture.compte("scv.membre." + prefixe, COMMERCIAL);
        autre = fixture.compte("scv.autre." + prefixe, COMMERCIAL);
        admin = fixture.compte("scv.admin." + prefixe, ADMINISTRATEUR);
        directeur = fixture.compte("scv.dg." + prefixe, DIRECTION);
        fixture.affecter(chef, t1);
        fixture.affecter(membre, t1);
        fixture.affecter(autre, t2);

        ComActionMessageType sortant = fixture.appelSortant();
        ComActionMessageSubCategory rdv = fixture.resultat(FixtureSuiviCommerciaux.RDV_PRIS);
        personneMembre = prefixe + "-PM";
        personneInactive = prefixe + "-PI";
        appelChef = fixture.interaction(chef, fixture.prospection(t1, prefixe + "-PC"), sortant, rdv, a(JOUR, 9, 0));
        appelMembre = fixture.interaction(membre, fixture.prospection(t1, personneMembre), sortant, rdv,
                a(JOUR, 10, 0));
        appelInactif = fixture.interaction(membre, fixture.prospection(inactive, personneInactive), sortant, rdv,
                a(JOUR, 11, 0));
        fixture.interaction(autre, fixture.prospection(t1, prefixe + "-PO"), sortant, rdv, a(JOUR, 12, 0));
        fixture.interaction(admin, fixture.prospection(t1, prefixe + "-PA"), sortant, rdv, a(JOUR, 13, 0));
        fixture.interaction(directeur, fixture.prospection(t1, prefixe + "-PD"), sortant, rdv, a(JOUR, 14, 0));
        fixture.relire();
    }

    private ResultActions synthese(RequestPostProcessor appelant) throws Exception {
        return mockMvc.perform(get(URL_SYNTHESE).param(DU, JOUR.toString()).param(AU, JOUR.toString())
                        .with(appelant))
                .andExpect(status().isOk());
    }

    private static String ligne(AdmUser compte) {
        return "$.lignes[?(@.commercialCle=='" + cle(compte) + "')]";
    }

    private static String interaction(ComActionMessage m) {
        return "$.content[?(@.id=='" + m.getId() + "')]";
    }

    // ------------------------------------------------------------------ synthèse : qui voit quelles lignes

    @Test
    void r8_commercial_neVoitQueLui() throws Exception {
        synthese(connecte(membre, COMMERCIAL))
                .andExpect(jsonPath(ligne(membre)).isNotEmpty())
                .andExpect(jsonPath(ligne(chef)).isEmpty())
                .andExpect(jsonPath(ligne(autre)).isEmpty())
                .andExpect(jsonPath("$.lignes[?(@.commercialCle!='" + cle(membre) + "')]").isEmpty());
    }

    @Test
    void r8_chef_voitSonEquipeEtLuiMeme_pasLesAutres() throws Exception {
        synthese(connecte(chef, CHEF))
                .andExpect(jsonPath(ligne(chef)).isNotEmpty())
                .andExpect(jsonPath(ligne(membre)).isNotEmpty())
                .andExpect(jsonPath(ligne(autre)).isEmpty());
    }

    @Test
    void r8_administrateur_voitTousLesCommerciaux() throws Exception {
        synthese(connecte(admin, ADMINISTRATEUR))
                .andExpect(jsonPath(ligne(chef)).isNotEmpty())
                .andExpect(jsonPath(ligne(membre)).isNotEmpty())
                .andExpect(jsonPath(ligne(autre)).isNotEmpty());
    }

    @Test
    void r8_directionGenerale_voitTousLesCommerciaux() throws Exception {
        synthese(connecte(directeur, DIRECTION))
                .andExpect(jsonPath(ligne(chef)).isNotEmpty())
                .andExpect(jsonPath(ligne(membre)).isNotEmpty())
                .andExpect(jsonPath(ligne(autre)).isNotEmpty());
    }

    /** §0 : la direction et les administrateurs non commerciaux voient tout mais ne sont pas des lignes. */
    @Test
    void r8_directionEtAdminNonCommerciaux_absentsDesLignes() throws Exception {
        synthese(connecte(admin, ADMINISTRATEUR))
                .andExpect(jsonPath(ligne(admin)).isEmpty())
                .andExpect(jsonPath(ligne(directeur)).isEmpty());
    }

    /** L'interaction d'une entité inactive n'est comptée pour personne, administrateur compris. */
    @Test
    void r8_interactionEntiteInactive_nonComptee() throws Exception {
        synthese(connecte(membre, COMMERCIAL))
                .andExpect(jsonPath(ligne(membre) + ".personnesJointes").value(1));
        synthese(connecte(admin, ADMINISTRATEUR))
                .andExpect(jsonPath(ligne(membre) + ".personnesJointes").value(1))
                .andExpect(jsonPath(ligne(membre) + ".contactsJoints").value(1));
    }

    // ------------------------------------------------------------------ 403 hors portée

    @Test
    void r8_commercialDemandantUnAutre_rend403() throws Exception {
        mockMvc.perform(get(URL_SYNTHESE).param(DU, JOUR.toString()).param(AU, JOUR.toString())
                        .param("commercialIds", cle(autre))
                        .with(connecte(membre, COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void r8_chefDemandantUnCommercialHorsEquipe_rend403() throws Exception {
        mockMvc.perform(get(URL_SYNTHESE).param(DU, JOUR.toString()).param(AU, JOUR.toString())
                        .param("commercialIds", cle(autre))
                        .with(connecte(chef, CHEF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void r8_chefDemandantUnMembre_rend200() throws Exception {
        mockMvc.perform(get(URL_SYNTHESE).param(DU, JOUR.toString()).param(AU, JOUR.toString())
                        .param("commercialIds", cle(membre))
                        .with(connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(membre)).isNotEmpty());
    }

    @Test
    void r8_listeDUnAutreCommercial_rend403() throws Exception {
        mockMvc.perform(get(URL).param("commercialCle", cle(autre)).param("jour", JOUR.toString())
                        .with(connecte(membre, COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ liste et export

    @Test
    void r8_liste_commercialNeVoitQueSesInteractions_horsEntiteInactive() throws Exception {
        mockMvc.perform(get(URL).param("jour", JOUR.toString()).param("size", "200")
                        .with(connecte(membre, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(interaction(appelMembre)).isNotEmpty())
                .andExpect(jsonPath(interaction(appelChef)).isEmpty())
                .andExpect(jsonPath(interaction(appelInactif)).isEmpty());
    }

    @Test
    void r8_liste_administrateur_sansEntiteInactive() throws Exception {
        mockMvc.perform(get(URL).param("commercialCle", cle(membre)).param("jour", JOUR.toString())
                        .with(connecte(admin, ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath(interaction(appelMembre)).isNotEmpty());
    }

    @Test
    void r9_export_memeBaseQueLaListe_sansEntiteInactive() throws Exception {
        List<String> valeurs = cellules(get(URL + "/export").param("commercialCle", cle(membre))
                .param("jour", JOUR.toString()).param("colonnes", "personneNom")
                .with(connecte(admin, ADMINISTRATEUR)));

        assertThat(String.join("|", valeurs)).contains(personneMembre).doesNotContain(personneInactive);
    }

    private List<String> cellules(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete).andExpect(request().asyncStarted()).andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        List<String> valeurs = new ArrayList<>();
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            for (Row row : feuille) {
                for (Cell cell : row) {
                    valeurs.add(cell.toString());
                }
            }
        }
        return valeurs;
    }
}
