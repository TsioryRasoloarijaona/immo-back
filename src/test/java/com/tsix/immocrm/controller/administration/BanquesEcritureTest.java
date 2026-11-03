package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureBanques;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsMorale;
import static com.tsix.immocrm.fixtures.AppelsTiers.creer;
import static com.tsix.immocrm.fixtures.AppelsTiers.detailNormalise;
import static com.tsix.immocrm.fixtures.AppelsTiers.idDe;
import static com.tsix.immocrm.fixtures.AppelsTiers.lire;
import static com.tsix.immocrm.fixtures.AppelsTiers.modifier;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureBanques.PARAMETRE_BANQUE;
import static com.tsix.immocrm.fixtures.FixtureBanques.PARAMETRE_ORGANISME;
import static com.tsix.immocrm.fixtures.FixtureBanques.corps;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.BANQUES;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.ORGANISMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Écriture réduite — FEATURE-admin-banques-organismes.md, section Back et règles 2 à 8. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BanquesEcritureTest {

    private static final String BAN = BANQUES.famille();
    private static final String ORG = ORGANISMES.famille();
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String PAYS_ID = "paysId";
    private static final String COMPTE_ID = "compteComptableId";
    private static final String JOURNAL_ID = "journalId";
    private static final String MATRICULE = "matriculeFiscal";
    private static final String CODE_SQL = "Code";
    private static final String DESCRIPTION_SQL = "Description";
    private static final String RAISON_SQL = "SocialReason";
    private static final String TYPE_SQL = "CfgTierTypeId";
    private static final String COMPTE_SQL = "ActAccountId";
    private static final String JOURNAL_SQL = "ActJournalId";
    private static final String MATRICULE_SQL = "IDNumber";
    private static final String DEVISE_SQL = "StlCurrencyId";
    private static final String FORME_SQL = "AdmSocialFormId";
    private static final String PAYS_RESIDENCE_SQL = "AdmCountryResidenceId";
    private static final String TABLE_COMPTE = "ActAccount";
    private static final String TABLE_JOURNAL = "ActJournal";
    private static final int COMPTEUR = 870_000;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBanques banques;
    @Autowired private FixtureTiers fixture;

    private Jeu j;
    private String code;
    private String libelle;

    @BeforeEach
    void setUp() {
        j = banques.jeu();
        code = j.code("BQ");
        libelle = "BANQUE " + j.jeton();
    }

    private static String reponse(MvcResult resultat) throws Exception {
        return resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String minuscule(UUID id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    private String ok(MockHttpServletRequestBuilder requete) throws Exception {
        return reponse(mockMvc.perform(requete).andExpect(status().isOk()).andReturn());
    }

    private UUID cree(String famille, Map<String, Object> corps) throws Exception {
        return idDe(reponse(mockMvc.perform(creer(famille, corps, connecte(j.admin())))
                .andExpect(status().isCreated()).andReturn()));
    }

    private String refus(Map<String, Object> corps) throws Exception {
        return reponse(mockMvc.perform(creer(BAN, corps, connecte(j.admin())))
                .andExpect(status().isBadRequest()).andReturn());
    }

    private void refusModification(UUID id, Map<String, Object> corps) throws Exception {
        mockMvc.perform(modifier(BAN, id, corps, connecte(j.admin()))).andExpect(status().isBadRequest());
    }

    private void modifie(UUID id, Map<String, Object> corps) throws Exception {
        ok(modifier(BAN, id, corps, connecte(j.admin())));
    }

    private Map<String, Object> corpsAvec(String champ, Object valeur) {
        Map<String, Object> corps = corps(j, code, libelle);
        corps.put(champ, valeur);
        return corps;
    }

    // ------------------------------------------------------------------ règle 2

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r2_creation_201_codeSaisi_ficheRendue(TypeTiers type) throws Exception {
        String reponse = reponse(mockMvc.perform(creer(type.famille(), corps(j, code, libelle), connecte(j.admin())))
                .andExpect(status().isCreated()).andReturn());

        assertAll(
                () -> assertThat(fixture.ligne(idDe(reponse))).containsEntry(CODE_SQL, code),
                () -> assertThat((String) JsonPath.read(reponse, "$.code")).isEqualTo(code));
    }

    @Test
    void r2_compteurTiers_jamaisConsomme() throws Exception {
        fixture.fixerCompteurTiers(COMPTEUR);

        cree(BAN, corps(j, code, libelle));

        assertThat(fixture.compteurTiers()).isEqualTo(COMPTEUR);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void r2_codeVide_400_champNomme(String valeur) throws Exception {
        String refus = refus(corpsAvec(CODE, valeur));

        assertThat(detailNormalise(refus)).contains(CODE);
    }

    @Test
    void r2_codeAbsent_400() throws Exception {
        Map<String, Object> corps = corps(j, code, libelle);
        corps.remove(CODE);

        refus(corps);

        assertThat(fixture.parRaisonSociale(libelle)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r2_codePrisDansLeType_409_rienCree(TypeTiers type) throws Exception {
        j.tiers(type, "PRIS").inserer();

        mockMvc.perform(creer(type.famille(), corps(j, j.code("PRIS"), libelle), connecte(j.admin())))
                .andExpect(status().isConflict());

        assertThat(fixture.parRaisonSociale(libelle)).isEmpty();
    }

    @Test
    void r2_memeCodeDansUnAutreType_accepte() throws Exception {
        j.tiers(ORGANISMES, "AUTRE").inserer();
        j.tiers(FOURNISSEURS, "FOU").inserer();

        UUID a = cree(BAN, corps(j, j.code("AUTRE"), libelle));
        UUID b = cree(BAN, corps(j, j.code("FOU"), libelle + " B"));

        assertAll(
                () -> assertThat(fixture.existe(a)).isTrue(),
                () -> assertThat(fixture.existe(b)).isTrue());
    }

    // ------------------------------------------------------------------ règle 3

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void r3_descriptionVide_400_champNomme(String valeur) throws Exception {
        String refus = refus(corpsAvec(DESCRIPTION, valeur));

        assertThat(detailNormalise(refus)).contains(DESCRIPTION);
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r3_description_enMajuscules_raisonSocialeEgale(TypeTiers type) throws Exception {
        UUID id = cree(type.famille(), corps(j, code, libelle.toLowerCase(Locale.ROOT)));

        assertThat(fixture.ligne(id)).containsEntry(DESCRIPTION_SQL, libelle).containsEntry(RAISON_SQL, libelle);
    }

    @Test
    void r3_modification_majusculesEtRaisonSociale() throws Exception {
        UUID id = j.tiers(BANQUES, "B1").inserer();

        modifie(id, corps(j, j.code("B1"), "renommee " + j.jeton().toLowerCase(Locale.ROOT)));

        assertThat(fixture.ligne(id)).containsEntry(DESCRIPTION_SQL, "RENOMMEE " + j.jeton())
                .containsEntry(RAISON_SQL, "RENOMMEE " + j.jeton());
    }

    @Test
    void r3_raisonSocialeEnvoyee_ignoree() throws Exception {
        UUID id = cree(BAN, corpsAvec("raisonSociale", "Autre raison " + j.jeton()));

        assertThat(fixture.ligne(id)).containsEntry(RAISON_SQL, libelle);
    }

    // ------------------------------------------------------------------ règle 4

    @Test
    void r4_paysAbsent_400_champNomme() throws Exception {
        Map<String, Object> corps = corps(j, code, libelle);
        corps.remove(PAYS_ID);

        assertThat(detailNormalise(refus(corps))).contains("pays");
    }

    @Test
    void r4_paysInconnu_400() throws Exception {
        refus(corpsAvec(PAYS_ID, UUID.randomUUID()));

        assertThat(fixture.parRaisonSociale(libelle)).isEmpty();
    }

    @Test
    void r4_paysDeResidence_egalPays_saisieIgnoree() throws Exception {
        UUID id = cree(BAN, corpsAvec("paysResidenceId", j.parent("AdmCountry")));

        assertThat(banques.colonnes(id)).containsEntry(PAYS_RESIDENCE_SQL, minuscule(j.pays()))
                .containsEntry("AdmCountryId", minuscule(j.pays()));
    }

    @Test
    void r4_modification_paysDeResidenceSuit() throws Exception {
        UUID id = cree(BAN, corps(j, code, libelle));
        UUID autre = j.parent("AdmCountry");

        Map<String, Object> corps = corps(j, code, libelle);
        corps.put(PAYS_ID, autre);
        modifie(id, corps);

        assertThat(fixture.ligne(id)).containsEntry(PAYS_RESIDENCE_SQL, minuscule(autre));
    }

    // ------------------------------------------------------------------ règle 5

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r5_sansMobileNiEmail_creationAcceptee(TypeTiers type) throws Exception {
        UUID id = cree(type.famille(), corps(j, code, libelle));

        assertThat(fixture.ligne(id)).containsEntry("Mobile", null).containsEntry("EMail", null);
    }

    @Test
    void r5_sansMobileNiEmail_modificationAcceptee() throws Exception {
        UUID id = j.tiers(BANQUES, "B2").avec("Mobile", "0612345678").inserer();

        modifie(id, corps(j, j.code("B2"), libelle));

        assertThat(fixture.ligne(id)).containsEntry("Mobile", null);
    }

    // ------------------------------------------------------------------ règle 6

    @ParameterizedTest
    @CsvSource({"BANQUES, " + PARAMETRE_BANQUE, "ORGANISMES, " + PARAMETRE_ORGANISME})
    void r6_formeSociale_parametreOmran(TypeTiers type, String parametre) throws Exception {
        banques.formeParDefaut(parametre, j.formeMorale());

        UUID id = cree(type.famille(), corps(j, code, libelle));

        assertThat(fixture.ligne(id)).containsEntry(FORME_SQL, minuscule(j.formeMorale()));
    }

    @ParameterizedTest
    @CsvSource({"BANQUES, " + PARAMETRE_BANQUE, "ORGANISMES, " + PARAMETRE_ORGANISME})
    void r6_formeSociale_nulleSansParametre(TypeTiers type, String parametre) throws Exception {
        banques.formeParDefaut(parametre, null);

        UUID id = cree(type.famille(), corps(j, code, libelle));

        assertThat(fixture.ligne(id)).containsEntry(FORME_SQL, null);
    }

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"BANQUES", "ORGANISMES"})
    void r6_typeDeLaFamille_cinqBooleensAFalse(TypeTiers type) throws Exception {
        UUID id = cree(type.famille(), corps(j, code, libelle));

        assertThat(fixture.ligne(id)).containsEntry(TYPE_SQL, minuscule(j.typeId(type)))
                .containsEntry("IsProspect", 0).containsEntry("IsOldCustomer", 0).containsEntry("IsRntCustomer", 0)
                .containsEntry("IsVisibleInList", 0).containsEntry("IsFundReader", 0);
    }

    // ------------------------------------------------------------------ règle 7

    @Test
    void r7_matriculeFiscal_recopieDansIdNumber() throws Exception {
        UUID id = cree(BAN, corpsAvec(MATRICULE, "MAT1"));

        assertThat(banques.colonnes(id)).containsEntry(MATRICULE_SQL, "MAT1").containsEntry("FiscalCode", "MAT1");
    }

    @Test
    void r7_modification_idNumberSuit() throws Exception {
        UUID id = cree(BAN, corpsAvec(MATRICULE, "MAT1"));

        modifie(id, corpsAvec(MATRICULE, "MAT2"));

        assertThat(banques.colonnes(id)).containsEntry(MATRICULE_SQL, "MAT2");
    }

    // ------------------------------------------------------------------ règle 8

    @ParameterizedTest
    @ValueSource(strings = {COMPTE_ID, JOURNAL_ID})
    void r8_compteOuJournalInconnu_400_enCreation(String champ) throws Exception {
        refus(corpsAvec(champ, UUID.randomUUID()));

        assertThat(fixture.parRaisonSociale(libelle)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {COMPTE_ID, JOURNAL_ID})
    void r8_compteOuJournalInconnu_400_enModification(String champ) throws Exception {
        UUID id = j.tiers(BANQUES, "B3").inserer();
        Map<String, Object> corps = corps(j, j.code("B3"), libelle);
        ok(modifier(BAN, id, corps, connecte(j.admin())));
        corps.put(champ, UUID.randomUUID());

        refusModification(id, corps);
    }

    @Test
    void r8_compteEtJournal_enregistresEtRelusSurLaFiche() throws Exception {
        UUID compte = j.parent(TABLE_COMPTE);
        UUID journal = j.parent(TABLE_JOURNAL);
        Map<String, Object> corps = corpsAvec(COMPTE_ID, compte);
        corps.put(JOURNAL_ID, journal);

        UUID id = cree(BAN, corps);

        Map<String, Object> fiche = JsonPath.read(ok(lire(BAN, id, connecte(j.admin()))), "$");
        assertAll(
                () -> assertThat(banques.colonnes(id)).containsEntry(COMPTE_SQL, minuscule(compte))
                        .containsEntry(JOURNAL_SQL, minuscule(journal)),
                () -> assertThat(fiche).containsEntry(CODE, code).containsEntry(DESCRIPTION, libelle)
                        .containsEntry(COMPTE_ID, compte.toString()).containsEntry(JOURNAL_ID, journal.toString()));
    }

    @Test
    void r8_modification_codeModifiable() throws Exception {
        UUID id = j.tiers(BANQUES, "B4").inserer();

        modifie(id, corps(j, j.code("NOUVEAU"), libelle));

        assertThat(fixture.ligne(id)).containsEntry(CODE_SQL, j.code("NOUVEAU"));
    }

    @Test
    void r8_modification_sonPropreCode_accepte() throws Exception {
        UUID id = j.tiers(BANQUES, "B5").inserer();

        modifie(id, corps(j, j.code("B5"), libelle));

        assertThat(fixture.ligne(id)).containsEntry(DESCRIPTION_SQL, libelle);
    }

    @Test
    void r8_modification_codePrisDansLeType_409_rienNeChange() throws Exception {
        j.tiers(BANQUES, "PRIS").inserer();
        UUID id = j.tiers(BANQUES, "B6").inserer();
        Map<String, Object> avant = fixture.ligne(id);

        mockMvc.perform(modifier(BAN, id, corps(j, j.code("PRIS"), libelle), connecte(j.admin())))
                .andExpect(status().isConflict());

        fixture.oublierEcritureRefusee();
        assertThat(fixture.ligne(id)).isEqualTo(avant);
    }

    @Test
    void r8_modification_codeVide_400() throws Exception {
        UUID id = j.tiers(BANQUES, "B7").inserer();

        refusModification(id, corps(j, "  ", libelle));
    }

    @Test
    void r8_modification_typeEtAuteurFiges_auteurDeModificationPose() throws Exception {
        UUID id = j.tiers(ORGANISMES, "O1").avec("UserCreate", "Auteur initial").inserer();

        ok(modifier(ORG, id, corps(j, j.code("O1"), libelle), connecte(j.admin())));

        assertThat(fixture.ligne(id)).containsEntry(TYPE_SQL, minuscule(j.typeId(ORGANISMES)))
                .containsEntry("UserCreate", "Auteur initial").containsEntry("UserUpdate", j.admin().getDescription())
                .extractingByKey("UserUpdateDateTime").isNotNull();
    }

    // ------------------------------------------------------------------ écriture réduite

    @Test
    void back_champsDeLEcritureReduite_relusSurLaFiche() throws Exception {
        UUID nature = j.nature(BANQUES);
        UUID ville = j.parent("AdmCity");
        Map<String, Object> corps = corps(j, code, libelle);
        corps.putAll(Map.of("natureId", nature, MATRICULE, "MAT1", "registreCommerce", "RC1", "ice", "ICE1",
                "mobileIndicatif", "+212", "mobile", "0661000001", "telephoneIndicatif", "+213", "telephone", "0522000001"));
        corps.putAll(Map.of("telephone1Indicatif", "+214", "telephone1", "0522000002", "telephone2Indicatif", "+215",
                "telephone2", "0522000003", "faxIndicatif", "+216", "fax", "0522000004", "email", "b@test.ma",
                "siteWeb", "b.ma"));
        corps.putAll(Map.of("adresse", "1 rue A", "adresse2", "Etage 2", "adresse3", "Bloc 3", "codePostal", "20000",
                "villeId", ville, "notes", "Notes B"));

        UUID id = cree(BAN, corps);

        Map<String, Object> fiche = JsonPath.read(ok(lire(BAN, id, connecte(j.admin()))), "$");
        assertThat(fiche).containsEntry("natureId", nature.toString()).containsEntry(PAYS_ID, j.pays().toString())
                .containsEntry(MATRICULE, "MAT1").containsEntry("registreCommerce", "RC1").containsEntry("ice", "ICE1")
                .containsEntry("mobileIndicatif", "+212").containsEntry("mobile", "0661000001")
                .containsEntry("telephoneIndicatif", "+213").containsEntry("telephone", "0522000001")
                .containsEntry("telephone1Indicatif", "+214").containsEntry("telephone1", "0522000002")
                .containsEntry("telephone2Indicatif", "+215").containsEntry("telephone2", "0522000003")
                .containsEntry("faxIndicatif", "+216").containsEntry("fax", "0522000004")
                .containsEntry("email", "b@test.ma").containsEntry("siteWeb", "b.ma").containsEntry("adresse", "1 rue A")
                .containsEntry("adresse2", "Etage 2").containsEntry("adresse3", "Bloc 3").containsEntry("codePostal", "20000")
                .containsEntry("villeId", ville.toString()).containsEntry("notes", "Notes B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"natureId", "villeId"})
    void back_identifiantInconnuDeLaFiche_400(String champ) throws Exception {
        refus(corpsAvec(champ, UUID.randomUUID()));
    }

    @Test
    void back_natureDUnAutreType_400() throws Exception {
        refus(corpsAvec("natureId", j.nature(ORGANISMES)));
    }

    @Test
    void back_champsHorsEcritureReduite_ignores_personneMoraleEtRequisDuSocleInutiles() throws Exception {
        Map<String, Object> corps = corps(j, code, libelle);
        corps.putAll(Map.of("personneMorale", false, "deviseId", UUID.randomUUID(), "formeSocialeId", UUID.randomUUID(),
                "banqueId", UUID.randomUUID(), "noteId", UUID.randomUUID(), "rib", "RIB-IGNORE"));
        banques.formeParDefaut(PARAMETRE_BANQUE, null);

        UUID id = cree(BAN, corps);

        assertThat(banques.colonnes(id)).containsEntry(DEVISE_SQL, null).containsEntry(FORME_SQL, null)
                .containsEntry("Rib", null);
    }

    @Test
    void back_modification_champsHorsEcritureReduite_gardes_champOmisDeLaFicheVide() throws Exception {
        UUID id = j.tiers(BANQUES, "B8").avec(DEVISE_SQL, j.devise()).avec("Rib", "RIB8").avec("BankAgency", "Agence 8")
                .avec("CodeCountable", "CC8").avec("Notes", "Ancienne note").inserer();

        modifie(id, corps(j, j.code("B8"), libelle));

        assertThat(banques.colonnes(id)).containsEntry(DEVISE_SQL, minuscule(j.devise())).containsEntry("Rib", "RIB8")
                .containsEntry("BankAgency", "Agence 8").containsEntry("CodeCountable", "CC8");
        assertThat(fixture.ligne(id)).containsEntry("Notes", null);
    }

    // ------------------------------------------------------------------ champs ajoutés, ignorés pour le socle

    @ParameterizedTest
    @EnumSource(value = TypeTiers.class, names = {"FOURNISSEURS", "CONTACTS"})
    void back_socle_creation_champsAjoutesIgnores(TypeTiers type) throws Exception {
        Map<String, Object> corps = corpsMorale(j, libelle);
        corps.putAll(Map.of(CODE, "PIRATE", DESCRIPTION, "AUTRE", COMPTE_ID, UUID.randomUUID(), JOURNAL_ID, UUID.randomUUID()));

        UUID id = cree(type.famille(), corps);

        assertThat(banques.colonnes(id)).containsEntry(DESCRIPTION_SQL, libelle).containsEntry(COMPTE_SQL, null)
                .containsEntry(JOURNAL_SQL, null).extractingByKey(CODE_SQL).isNotEqualTo("PIRATE");
    }

    @Test
    void back_socle_modification_compteGarde() throws Exception {
        UUID compte = j.parent(TABLE_COMPTE);
        UUID id = j.tiers(CONTACTS, "C1").avec(COMPTE_SQL, compte).inserer();
        Map<String, Object> corps = corpsMorale(j, libelle);
        corps.put(COMPTE_ID, j.parent(TABLE_COMPTE));

        ok(modifier(CONTACTS.famille(), id, corps, connecte(j.admin())));

        assertThat(fixture.ligne(id)).containsEntry(COMPTE_SQL, minuscule(compte));
    }
}
