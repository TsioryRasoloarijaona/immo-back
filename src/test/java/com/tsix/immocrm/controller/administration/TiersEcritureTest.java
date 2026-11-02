package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.LISTE;
import static com.tsix.immocrm.fixtures.AppelsTiers.corpsMorale;
import static com.tsix.immocrm.fixtures.AppelsTiers.corpsPhysique;
import static com.tsix.immocrm.fixtures.AppelsTiers.creer;
import static com.tsix.immocrm.fixtures.AppelsTiers.detailNormalise;
import static com.tsix.immocrm.fixtures.AppelsTiers.idDe;
import static com.tsix.immocrm.fixtures.AppelsTiers.json;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Création et modification — FEATURE-admin-tiers.md, règles 5 à 10. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersEcritureTest {

    private static final String FOU = FOURNISSEURS.famille();
    private static final String ID_TYPE = "CfgTierTypeId";
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String RAISON = "SocialReason";
    private static final String UTILISATEUR_CREATION = "UserCreate";
    private static final String RAISON_FIL = "raisonSociale";
    private static final int COMPTEUR = 880_000;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;
    private String raison;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
        raison = "Societe " + j.jeton();
    }

    private String corps(MvcResult resultat) throws Exception {
        return resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private UUID cree(String famille, Map<String, Object> corps) throws Exception {
        return idDe(corps(mockMvc.perform(creer(famille, corps, connecte(j.admin())))
                .andExpect(status().isCreated()).andReturn()));
    }

    private String refusCreation(Map<String, Object> corps) throws Exception {
        return corps(mockMvc.perform(creer(FOU, corps, connecte(j.admin())))
                .andExpect(status().isBadRequest()).andReturn());
    }

    private void refusModification(UUID id, Map<String, Object> corps) throws Exception {
        mockMvc.perform(modifier(FOU, id, corps, connecte(j.admin()))).andExpect(status().isBadRequest());
    }

    private void modifie(String famille, UUID id, Map<String, Object> corps) throws Exception {
        mockMvc.perform(modifier(famille, id, corps, connecte(j.admin()))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 5

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void r5_creation_201_typeDeLaFamille_ficheRendue(TypeTiers type) throws Exception {
        String reponse = corps(mockMvc.perform(creer(type.famille(), corpsMorale(j, raison), connecte(j.admin())))
                .andExpect(status().isCreated()).andReturn());

        Map<String, Object> ligne = fixture.ligne(idDe(reponse));
        assertAll(
                () -> assertThat(ligne).containsEntry(ID_TYPE, j.typeId(type).toString().toLowerCase()),
                () -> assertThat((String) JsonPath.read(reponse, "$.code")).isEqualTo(ligne.get(CODE)));
    }

    @Test
    void r5_code_vientDuCompteurTiersPartage_valeurEnvoyeeIgnoree() throws Exception {
        fixture.fixerCompteurTiers(COMPTEUR);
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put("code", "PIRATE");

        UUID id = cree(FOU, corps);

        assertAll(
                () -> assertThat((String) fixture.ligne(id).get(CODE)).matches("\\d+/0*" + (COMPTEUR + 1)),
                () -> assertThat(fixture.compteurTiers()).isEqualTo(COMPTEUR + 1));
    }

    @Test
    void r5_indicateurs_aFalseNonNull_auteurEtDatePoses() throws Exception {
        UUID id = cree(FOU, corpsMorale(j, raison));

        assertThat(fixture.ligne(id))
                .containsEntry("IsProspect", 0).containsEntry("IsOldCustomer", 0).containsEntry("IsRntCustomer", 0)
                .containsEntry("IsVisibleInList", 0).containsEntry("IsFundReader", 0)
                .containsEntry(UTILISATEUR_CREATION, j.admin().getDescription())
                .extractingByKey("UserCreateDateTime").isNotNull();
    }

    @Test
    void r5_estPrestataire_valeurRecue_falseSiAbsente() throws Exception {
        Map<String, Object> prestataire = corpsMorale(j, raison);
        prestataire.put("estPrestataire", true);

        UUID oui = cree(FOU, prestataire);
        UUID absent = cree(FOU, corpsMorale(j, raison + " B"));

        assertAll(
                () -> assertThat(fixture.ligne(oui)).containsEntry("IsExtern", 1),
                () -> assertThat(fixture.ligne(absent)).containsEntry("IsExtern", 0));
    }

    @Test
    void creation_champsSaisis_relusSurLaFiche() throws Exception {
        UUID nature = j.nature(FOURNISSEURS);
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.putAll(Map.of("natureId", nature, "assujettissementId", j.assujettissement(), "noteId", j.note(),
                "paysId", j.pays(), "ville", "Casablanca", "codePostal", "20000", "adresse", "1 rue A", "adresse2", "Etage 2",
                "email", "a@test.ma", "siteWeb", "a.ma"));
        corps.putAll(Map.of("notes", "Notes A", "ice", "ICE1", "matriculeFiscal", "MAT1", "registreCommerce", "RC1",
                "douane", "DOU1", "codeComptable", "CC1", "codeComptableCollectif", "CCC1", "agence", "Agence",
                "rib", "RIB1", "iban", "IBAN1"));
        corps.putAll(Map.of("mobileIndicatif", "+212", "telephoneIndicatif", "+212", "telephone", "0522000001",
                "telephone1", "0522000002", "telephone2", "0522000003", "fax", "0522000004", "delaiPaiement", 30,
                "tauxRetenue", new BigDecimal("12.5"), "tauxTva", new BigDecimal("20")));

        UUID id = cree(FOU, corps);

        String lu = corps(mockMvc.perform(lire(FOU, id, connecte(j.admin()))).andExpect(status().isOk()).andReturn());
        Map<String, Object> fiche = JsonPath.read(lu, "$");
        assertThat(fiche).containsEntry("personneMorale", true).containsEntry("natureId", nature.toString())
                .containsEntry("assujettissementId", j.assujettissement().toString())
                .containsEntry("noteId", j.note().toString()).containsEntry("ville", "Casablanca")
                .containsEntry("codePostal", "20000").containsEntry("adresse", "1 rue A").containsEntry("adresse2", "Etage 2")
                .containsEntry("email", "a@test.ma").containsEntry("siteWeb", "a.ma").containsEntry("notes", "Notes A")
                .containsEntry("ice", "ICE1").containsEntry("matriculeFiscal", "MAT1").containsEntry("registreCommerce", "RC1")
                .containsEntry("douane", "DOU1").containsEntry("codeComptable", "CC1")
                .containsEntry("codeComptableCollectif", "CCC1").containsEntry("agence", "Agence")
                .containsEntry("rib", "RIB1").containsEntry("iban", "IBAN1").containsEntry("mobileIndicatif", "+212")
                .containsEntry("telephoneIndicatif", "+212").containsEntry("telephone", "0522000001")
                .containsEntry("telephone1", "0522000002").containsEntry("telephone2", "0522000003")
                .containsEntry("fax", "0522000004").containsEntry("delaiPaiement", 30);
        assertThat(new BigDecimal(String.valueOf(fiche.get("tauxRetenue")))).isEqualByComparingTo("12.5");
    }

    // ------------------------------------------------------------------ règle 6

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void r6_morale_raisonSocialeBlanche_400_champNomme(String valeur) throws Exception {
        Map<String, Object> corps = corpsMorale(j, valeur);

        assertThat(detailNormalise(refusCreation(corps))).contains("raisonsociale");
    }

    @Test
    void r6_morale_raisonSocialeAbsente_400_champNomme() throws Exception {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.remove(RAISON_FIL);

        assertThat(detailNormalise(refusCreation(corps))).contains("raisonsociale");
    }

    @ParameterizedTest
    @CsvSource({"prenom, prenom", "nom, nom", "titreId, titre", "sexeId, sexe"})
    void r6_physique_champManquant_400_champNomme(String champ, String nomme) throws Exception {
        Map<String, Object> corps = corpsPhysique(j, "Amal", raison);
        corps.remove(champ);

        assertThat(detailNormalise(refusCreation(corps))).contains(nomme);
    }

    @Test
    void r6_personneMoraleAbsente_400() throws Exception {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.remove("personneMorale");

        refusCreation(corps);
    }

    @Test
    void r6_physique_raisonSocialeEtDescription_prenomNom_saisieIgnoree() throws Exception {
        Map<String, Object> corps = corpsPhysique(j, "Amal", "Benali");
        corps.put(RAISON_FIL, "A ignorer");

        UUID id = cree(FOU, corps);

        assertThat(fixture.ligne(id)).containsEntry(RAISON, "Amal Benali").containsEntry(DESCRIPTION, "Amal Benali")
                .containsEntry("FirstName", "Amal").containsEntry("LastName", "Benali");
    }

    // ------------------------------------------------------------------ règle 7

    @Test
    void r7_description_egaleRaisonSociale_aChaqueEcriture() throws Exception {
        UUID id = cree(FOU, corpsMorale(j, raison));
        assertThat(fixture.ligne(id)).containsEntry(DESCRIPTION, raison);

        modifie(FOU, id, corpsMorale(j, raison + " renommee"));

        assertThat(fixture.ligne(id)).containsEntry(DESCRIPTION, raison + " renommee")
                .containsEntry(RAISON, raison + " renommee");
    }

    @Test
    void r7_physique_description_suitLeNom_enModification() throws Exception {
        UUID id = cree(CONTACTS.famille(), corpsPhysique(j, "Amal", "Benali"));

        modifie(CONTACTS.famille(), id, corpsPhysique(j, "Amal", "Idrissi"));

        assertThat(fixture.ligne(id)).containsEntry(DESCRIPTION, "Amal Idrissi").containsEntry(RAISON, "Amal Idrissi");
    }

    // ------------------------------------------------------------------ règle 8

    private Map<String, Object> sansCoordonnees(String mobile, String email) {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put("mobile", mobile);
        corps.put("email", email);
        return corps;
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, NULL", "'', ''", "'   ', '  '"}, nullValues = "NULL")
    void r8_mobileEtEmailVides_400_enCreation(String mobile, String email) throws Exception {
        refusCreation(sansCoordonnees(mobile, email));

        assertThat(fixture.parRaisonSociale(raison)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, NULL", "'', ''", "'   ', '  '"}, nullValues = "NULL")
    void r8_mobileEtEmailVides_400_enModification(String mobile, String email) throws Exception {
        UUID id = cree(FOU, corpsMorale(j, raison));

        refusModification(id, sansCoordonnees(mobile, email));
    }

    @ParameterizedTest
    @CsvSource(value = {"0612345678, NULL", "NULL, seul@test.ma"}, nullValues = "NULL")
    void r8_unDesDeux_suffit(String mobile, String email) throws Exception {
        UUID id = cree(FOU, sansCoordonnees(mobile, email));

        assertThat(fixture.existe(id)).isTrue();
    }

    // ------------------------------------------------------------------ règle 9

    @ParameterizedTest
    @ValueSource(strings = {"paysResidenceId", "deviseId", "formeSocialeId"})
    void r9_champRequisAbsent_400(String champ) throws Exception {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.remove(champ);

        refusCreation(corps);
    }

    @ParameterizedTest
    @ValueSource(strings = {"titreId", "sexeId", "natureId", "formeSocialeId", "deviseId", "assujettissementId", "paysId",
            "paysResidenceId", "villeId", "banqueId", "noteId", "situationFamilialeId", "nationaliteId", "professionId",
            "typePieceId"})
    void r9_identifiantInconnu_400_jamais500(String champ) throws Exception {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put(champ, UUID.randomUUID());

        refusCreation(corps);

        assertThat(fixture.parRaisonSociale(raison)).isEmpty();
    }

    @Test
    void r9_identifiantInconnu_400_enModification() throws Exception {
        UUID id = cree(FOU, corpsMorale(j, raison));
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put("deviseId", UUID.randomUUID());

        refusModification(id, corps);
    }

    @Test
    void r9_identifiantIllisible_400_jamais500() throws Exception {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put("paysResidenceId", "pas-un-uuid");

        mockMvc.perform(post(LISTE, FOU).contentType(MediaType.APPLICATION_JSON).content(json(corps))
                .with(connecte(j.admin()))).andExpect(status().isBadRequest());
    }

    @Test
    void r9_natureDUnAutreType_400() throws Exception {
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put("natureId", j.nature(CONTACTS));

        refusCreation(corps);
    }

    // ------------------------------------------------------------------ règle 10

    @Test
    void r10_modification_figeCodeTypeAuteur_poseAuteurEtDate() throws Exception {
        UUID id = j.tiers(FOURNISSEURS, "F1").avec(UTILISATEUR_CREATION, "Auteur initial").inserer();
        Map<String, Object> corps = corpsMorale(j, raison);
        corps.put("code", "PIRATE");

        modifie(FOU, id, corps);

        assertThat(fixture.ligne(id)).containsEntry(CODE, j.code("F1"))
                .containsEntry(ID_TYPE, j.typeId(FOURNISSEURS).toString().toLowerCase())
                .containsEntry(UTILISATEUR_CREATION, "Auteur initial")
                .containsEntry("UserUpdate", j.admin().getDescription())
                .extractingByKey("UserUpdateDateTime").isNotNull();
    }

    @Test
    void r10_modification_neTouchePasPaieGrhComptes() throws Exception {
        UUID compte = j.parent("ActAccount");
        UUID id = j.tiers(FOURNISSEURS, "F2").avec("SocialSecurityNumber", "SECU1")
                .avec("SalaryBase", new BigDecimal("1234.5")).avec("BaseMonthNumber", new BigDecimal("13"))
                .avec("HireDate", LocalDateTime.of(2020, 1, 15, 0, 0)).avec("ActAccountId", compte).inserer();
        Map<String, Object> avant = fixture.ligne(id);

        modifie(FOU, id, corpsMorale(j, raison));

        Map<String, Object> apres = fixture.ligne(id);
        List<String> colonnes = List.of("SocialSecurityNumber", "SalaryBase", "BaseMonthNumber", "HireDate", "ActAccountId");
        assertThat(colonnes).hasSize(5).allSatisfy(c -> assertThat(apres.get(c)).as(c).isNotNull().isEqualTo(avant.get(c)));
    }

    @Test
    void put_remplaceLesChamps_unChampOmisEstVide() throws Exception {
        UUID id = j.tiers(FOURNISSEURS, "F3").avec("Notes", "Ancienne note").inserer();

        modifie(FOU, id, corpsMorale(j, raison));

        assertThat(fixture.ligne(id)).containsEntry("Notes", null);
    }
}
