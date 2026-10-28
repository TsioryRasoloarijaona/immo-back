package com.tsix.immocrm.controller.modele;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Adresse;
import com.tsix.immocrm.utils.LecturePdf;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste {@code destinataires} du catalogue {@code DOSSIER_VENTE} (FEATURE-vente-formulaire-barid.md §3 « Catalogue », §8.1
 * « Catalogue / valeurs ») : rendue par un modèle de test qui imprime chaque champ entre crochets, une page par élément.
 * <pre>
 *   T (société active : 10 BD SOCIETE, ETAGE 4, casablanca, 20000, pays PS)
 *     D : statut en cours, principal P (Mr, 12 RUE A / APPT 3, 20250, ville du référentiel V, pays PC)
 *         co-acquéreurs saisis C2 (Mme, code postal « B12 ») puis C1 (sans titre ni code postal) ; C1 est le plus ancien
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class DestinatairesBaridTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String GENERER = BASE + "/{id}/generer";
    private static final String FICHE_ID = "ficheId";
    private static final String DETAIL = "$.detail";
    private static final String AUCUN_DESTINATAIRE = "Aucun destinataire";
    private static final String ACQUEREUR = "role=[Acquéreur]";
    private static final String CO_ACQUEREUR = "role=[Co-acquéreur]";
    private static final List<String> CLES = List.of("titre", "nom", "adresse", "adresseSansCodePostal", "codePostal",
            "societe", "societeAdresseSansCodePostal", "societeCodePostal", "dossier", "tranche", "statut", "role");

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureFormulairesBarid fixture;
    @Autowired private FixtureEtatVentes ev;

    private String p;
    private AdmCountry paysClient;
    private AdmCity ville;
    private CfgTranche t;
    private StrStatus enCours;
    private ComFolder dossier;
    private String modele;

    @BeforeEach
    void setUp() throws Exception {
        p = FixtureEtatVentes.prefixe("BAMV");
        paysClient = fixture.pays("PAYSCLI-" + p);
        ville = fixture.ville("CODEVILLE" + p, paysClient);
        t = fixture.tranche("T-" + p, true, fixture.pays("PAYSSOC-" + p));
        enCours = ev.statutEnCours("ENC-" + p);
        CfgTier principal = fixture.client("PRINCIPAL " + p, fixture.titre("Mr" + p),
                new Adresse("12 RUE A", "APPT 3", null, "20250", ville, paysClient));
        dossier = fixture.dossier(t, "DOS-" + p, enCours, principal);
        CfgTier co2 = fixture.client("COACQDEUX " + p, fixture.titre("Mme" + p),
                new Adresse("7 RUE C", null, null, "B12", null, null));
        CfgTier co1 = fixture.client("COACQUN " + p, null, new Adresse("5 AV B", null, null, null, null, null));
        fixture.coAcquereur(dossier, co2, LocalDateTime.of(2025, 2, 1, 10, 0));
        fixture.coAcquereur(dossier, co1, LocalDateTime.of(2025, 1, 1, 10, 0));
        fixture.relire();
        modele = creerModele("TST_DEST_" + p.substring(4), contenuValeurs());
    }

    /** Un paragraphe {@code cle=[valeur]} par champ, dans un {@code div} répété une page par destinataire. */
    private static String contenuValeurs() {
        StringBuilder html = new StringBuilder("<div data-repeter=\"destinataires\" data-saut-page>");
        for (String cle : CLES) {
            html.append("<p>").append(cle).append("=[<span data-champ=\"destinataire.").append(cle).append("\">")
                    .append(cle).append("</span>]</p>");
        }
        return html.append("</div>").toString();
    }

    private String creerModele(String code, String contenu) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", code);
        m.put("libelle", code);
        m.put("contenu", contenu);
        String reponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(m)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("id").asString();
    }

    private byte[] pdf(ComFolder fiche) throws Exception {
        return mockMvc.perform(get(GENERER, modele).param(FICHE_ID, fiche.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    void catalogue_listeDestinataires_douzeChamps() throws Exception {
        String corps = mockMvc.perform(get(BASE + "/categories/{c}/champs", "DOSSIER_VENTE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> cles = JsonPath.read(corps, "$.listes[?(@.cle == 'destinataires')].champs[*].cle");
        assertThat(cles).containsExactlyInAnyOrderElementsOf(CLES.stream().map(c -> "destinataire." + c).toList());
    }

    @Test
    void catalogue_champDestinataireHorsRepetition_400() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", "TST_DEST_HORS_" + p.substring(4));
        m.put("libelle", "hors");
        m.put("contenu", "<p><span data-champ=\"destinataire.nom\">Nom</span></p>");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(m)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ éléments d'un dossier

    @Test
    void elements_principalPuisCoAcquereursParDateDeSaisie_unePageChacun() throws Exception {
        byte[] pdf = pdf(dossier);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).isEqualTo(3);
        soft.assertThat(LecturePdf.textePage(pdf, 1)).contains("nom=[PRINCIPAL " + p + "]").contains(ACQUEREUR);
        soft.assertThat(LecturePdf.textePage(pdf, 2)).contains("nom=[COACQUN " + p + "]").contains(CO_ACQUEREUR);
        soft.assertThat(LecturePdf.textePage(pdf, 3)).contains("nom=[COACQDEUX " + p + "]").contains(CO_ACQUEREUR);
        soft.assertAll();
    }

    @Test
    void valeurs_acquereurPrincipal_adressesSocieteDossier() throws Exception {
        String page = LecturePdf.textePage(pdf(dossier), 1);

        assertThat(page)
                .contains("titre=[Mr" + p + "]")
                .contains("adresse=[12 RUE A APPT 3 20250 CODEVILLE" + p + " PAYSCLI-" + p + "]")
                .contains("adresseSansCodePostal=[12 RUE A APPT 3 CODEVILLE" + p + " PAYSCLI-" + p + "]")
                .contains("codePostal=[2 0 2 5 0]")
                .contains("societe=[" + t.getCfgProject().getCfgCompany().getDescription() + "]")
                .contains("societeAdresseSansCodePostal=[10 BD SOCIETE ETAGE 4 casablanca PAYSSOC-" + p + "]")
                .contains("societeCodePostal=[2 0 0 0 0]")
                .contains("dossier=[DOS-" + p + "]")
                .contains("tranche=[" + t.getDescription() + "]")
                .contains("statut=[ENC-" + p + "]");
    }

    @Test
    void valeurs_villeEstLeCodeDuReferentiel_jamaisLaVilleTexteLibre() throws Exception {
        byte[] pdf = pdf(dossier);

        SoftAssertions soft = new SoftAssertions();
        for (int page = 1; page <= 3; page++) {
            soft.assertThat(LecturePdf.textePage(pdf, page)).as("page %d", page)
                    .doesNotContain(FixtureFormulairesBarid.VILLE_TEXTE_LIBRE);
        }
        soft.assertThat(LecturePdf.textePage(pdf, 1)).contains("CODEVILLE" + p);
        soft.assertAll();
    }

    @Test
    void valeurs_titreEtCodePostalAbsents_chaineVide() throws Exception {
        String page = LecturePdf.textePage(pdf(dossier), 2);

        assertThat(page).contains("titre=[]").contains("codePostal=[]").contains("adresse=[5 AV B]")
                .contains("adresseSansCodePostal=[5 AV B]");
    }

    @Test
    void valeurs_codePostalNonNumerique_telQuel() throws Exception {
        String page = LecturePdf.textePage(pdf(dossier), 3);

        assertThat(page).contains("codePostal=[B12]").contains("adresse=[7 RUE C B12]");
    }

    @Test
    void elements_tiersPrincipalEtCoAcquereurDuMemeDossier_deuxPages() throws Exception {
        CfgTier lesDeux = fixture.client("DOUBLE " + p, null, Adresse.aucune());
        ComFolder d = fixture.dossier(t, "DOS2-" + p, enCours, lesDeux);
        fixture.coAcquereur(d, lesDeux,LocalDateTime.of(2025, 3, 1, 10, 0));
        fixture.relire();

        byte[] pdf = pdf(d);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).isEqualTo(2);
        soft.assertThat(LecturePdf.textePage(pdf, 1)).contains("nom=[DOUBLE " + p + "]").contains(ACQUEREUR);
        soft.assertThat(LecturePdf.textePage(pdf, 2)).contains("nom=[DOUBLE " + p + "]").contains(CO_ACQUEREUR);
        soft.assertAll();
    }

    @Test
    void elements_dossierSansStatut_pagesPresentes() throws Exception {
        ComFolder d = fixture.dossier(t, "DOS3-" + p, null, fixture.client("SANSSTATUT " + p, null, Adresse.aucune()));
        fixture.relire();

        byte[] pdf = pdf(d);

        assertThat(LecturePdf.textePage(pdf, 1)).contains("nom=[SANSSTATUT " + p + "]").contains("statut=[]");
    }

    @Test
    void elements_dossierAnnule_aucunePage_404AucunDestinataire() throws Exception {
        ComFolder annule = fixture.dossier(t, "DOS4-" + p, ev.statutSortie("ANN-" + p, true),
                fixture.client("ANNULE " + p, null, Adresse.aucune()));
        fixture.relire();

        mockMvc.perform(get(GENERER, modele).param(FICHE_ID, annule.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(DETAIL).value(containsString(AUCUN_DESTINATAIRE)));
    }

    @Test
    void elements_dossierDesiste_aucunePage_404AucunDestinataire() throws Exception {
        ComFolder desiste = fixture.dossier(t, "DOS5-" + p, ev.statutSortie("DES-" + p, false),
                fixture.client("DESISTE " + p, null, Adresse.aucune()));
        fixture.relire();

        mockMvc.perform(get(GENERER, modele).param(FICHE_ID, desiste.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(DETAIL).value(containsString(AUCUN_DESTINATAIRE)));
    }

    @Test
    void valeurs_nomContenantUneBalise_resteDuTexte() throws Exception {
        ComFolder d = fixture.dossier(t, "DOS6-" + p, enCours,
                fixture.client("<script>alert(1)</script> " + p, null, Adresse.aucune()));
        fixture.relire();

        assertThat(LecturePdf.textePage(pdf(d), 1)).contains("nom=[<script>alert(1)</script> " + p + "]");
    }
}
