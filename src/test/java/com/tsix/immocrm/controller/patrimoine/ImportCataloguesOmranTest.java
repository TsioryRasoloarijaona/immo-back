package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.entite.EntiteJuridiqueEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainEcritureDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.catalogue;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.lireColonnes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROJETS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROSPECTIONS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TRANCHES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogues déclaratifs de l'import générique — FEATURE-patrimoine-import-generique.md §1.3 (« chaque entete du catalogue
 * figure dans la liste DisplayName/IsExportable du fichier OMRAN cité », types, propriété = champ du DTO d'écriture),
 * §0.4 et décision du manager 25/10 n° 3 (pivot = {@code code} seul), §1.5 (« Archivée », « Validité du statut » ignorées).
 * Listes relevées dans {@code omran-src/Yes.Rems/Yes.Cfg/CfgCompany.cs} (34), {@code CfgProject.cs} (23),
 * {@code CfgTranche.cs} (83) et {@code Yes.Lnd/Prospection.cs} (55) : DisplayName de chaque propriété {@code [IsExportable]}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportCataloguesOmranTest {

    private static final Set<String> TYPES = Set.of("TEXTE", "ENTIER", "DECIMAL", "DATE", "BOOLEEN", "REFERENCE");

    private static final List<String> OMRAN_ENTITE = List.of("Code", "Description", "Forme juridique",
            "Matricule fiscal", "Registre du commerce", "N° Sécurité sociale", "Patente",
            "Identifiant Commun de l'Entreprise", "Date de création", "Capital", "Devise", "Groupe d'actionnaires",
            "Nombre actionnaires", "Nombre de parts (actions)", "Part maison", "Date liquidation", "Active", "Adresse 1",
            "Adresse 2", "Adresse 3", "Code postal", "Ville", "Pays", "Téléphone", "Fax", "Mobile", "E-mail", "Site web",
            "Courrier fait à", "Représentant légal", "Courrier signataire", "Responsable technique",
            "Responsable commercial", "Description détaillée");

    private static final List<String> OMRAN_PROGRAMME = List.of("Entité", "Code", "Description", "Description détaillée",
            "Date lancement des traveaux", "Date fin des traveaux", "Date début de commercialisation",
            "Date fin de commercialisation", "Adresse 1", "Adresse 2", "Adresse 3", "Code postal", "Ville", "Pays", "Email",
            "Mobile", "Tél.", "Fax", "Site web", "Courrier fait à", "Courrier signataire", "Responsable technique",
            "Responsable commercial");

    private static final List<String> OMRAN_TRANCHE = List.of("Programme", "Code", "Description",
            "Visible dans le module Vente", "Visible dans le module Projet", "Visible dans le module Location",
            "Visible dans le module Location-Vente", "Visible dans le module Achat",
            "Visible dans le module service après vente", "Visible dans le module gestion immobilière",
            "Responsable S.A.V.", "Standing", "Nbr. étage", "Rubrique", "Adresse SAV",
            "Avec octroi d'un cadeau pour un ancien client", "Avec octroi d'un cadeau pour un parrain",
            "Avec octroi d'un cadeau pour une source de contact", "Avec possibilité de cumul des cadeaux",
            "Date lancement des traveaux", "Date fin des traveaux", "Date octroi des T.F", "Date permis d'habiter",
            "Date début de commercialisation", "Date fin des concrétisations", "Date début des finalisations",
            "Date fin de commercialisation", "Date réception", "Date de la 1ère relance", "Adresse 1", "Adresse 2",
            "Adresse 3", "Code postal", "Pays", "Email", "Mobile", "Tél.", "Fax", "Site web", "Pré-réservation",
            "Avance basée sur le montant", "Concrétisation", "Frais de dossier", "Pénalité de désistement",
            "Pénalité de désistement basée sur le prix de vente / encaissement", "Pénalité de transfert produit",
            "Pénalité de transfert tranche", "Pénalité de transfert client", "Autre frais", "Frais de syndic",
            "Période de syndic", "Délai d’attente après première notification T.F. (en jours)",
            "Délai d’attente après deuxième notification T.F. (en jours)", "Nombre maximum de visite par jours",
            "Pénalité de non finalisation de la vente", "Déduction après clôture du dossier de vente",
            "Délai de mise à disposition du solde des versements (en jour)", "Durée du service après-vente (en mois)",
            "Durée du service après-vente supp. (en mois)", "Tribunal de commerce", "Article légal pour la résiliation",
            "Délai de rétractation (heures)", "Délai max des échéances", "Avec vérification du délai max des échéances",
            "Crédit", "Avec vérification du crédit", "Remise sur la pré-réservation",
            "Seuil de versement lors de la concrétisation", "Active", "Remise sur les frais de dossier",
            "Remise sur les pénalités de désistement", "Remise sur les pénalités de transfert produit",
            "Remise sur les pénalitsé de transfert tranche", "Remise sur les pénalités de transfert client",
            "Remise sur les frais de syndic", "Remise sur autre frais",
            "Délai entre la date d'envoi au notaire et la date minute",
            "Délai entre la date minute et l'encaissement des restes à régler", "Description détaillée",
            "Ecart-type concrétisation", "Ecart-type finalisation", "Tier autrui", "Plan compt.");

    private static final List<String> OMRAN_PROSPECTION = List.of("Code", "Date", "Description", "Catégorie", "Type",
            "Niveau", "Vocation", "Pays", "Ville", "Validité du statut", "Le courtier", "Notaire / avocat", "Lien G.Map",
            "Aménagé/Brut", "Orientation", "Adresse", "Propriétaire", "Notes", "Superficie(m²)",
            "Superficie Voie d'aménagement (m²)", "Coef équipements et voies d'aménagement",
            "Superficie équipement à céder (m²)", "Superficie à régler (m²)", "Superficie à céder (m²)",
            "Superficie Nette (m²)", "Emprise au sol Batiments (COS)", "Superficie Emprise au sol bâtiment(m²)",
            "Coef plancher couvert (cuf)", "Superficie plancher couvert (cuf) (m²)", "Coef Totale projetée",
            "Superficie Totale projetée", "Coef Commerce", "Superficie Commerce", "Coef Bureau", "Superficie Bureau",
            "Superficie Habitat", "Coef Parking sous-sol", "Superficie Parking sous-sol", "Arrêté d'alignement(m)",
            "Hauteur max autorisée(m)", "Prix du m² du propriétaire", "Prix total du propriétaire", "Prix proposé",
            "Date enregistrement", "N° enregistrement", "Lieu enregistrement", "C.P.F. Immatriculation",
            "Date Immatriculation", "Archivée", "Acquisition", "Anomalies", "Identification de la zone",
            "Conditions de raccordement concessionnaire", "Assiette foncière", "Levée topographique");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
    }

    static Stream<Arguments> omran() {
        return Stream.of(
                Arguments.of(ENTITES, OMRAN_ENTITE),
                Arguments.of(PROJETS, OMRAN_PROGRAMME),
                Arguments.of(TRANCHES, OMRAN_TRANCHE),
                Arguments.of(PROSPECTIONS, OMRAN_PROSPECTION));
    }

    static Stream<Arguments> dtos() {
        return Stream.of(
                Arguments.of(ENTITES, EntiteJuridiqueEcritureDto.class),
                Arguments.of(PROJETS, ProjetEcritureDto.class),
                Arguments.of(TRANCHES, TrancheEcritureDto.class),
                Arguments.of(PROSPECTIONS, ProspectionTerrainEcritureDto.class));
    }

    private List<Map<String, Object>> catalogueDe(String objet) throws Exception {
        return catalogue(corps(mockMvc.perform(lireColonnes(objet, connecte(admin)))
                .andExpect(status().isOk())
                .andReturn()));
    }

    private static List<String> composantes(Class<?> dto) {
        return Arrays.stream(dto.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("omran")
    void r1_3_entetesDuCatalogue_dansLesDisplayNameExportablesOmran(String objet, List<String> omran) throws Exception {
        List<Map<String, Object>> colonnes = catalogueDe(objet);

        assertThat(colonnes).as(objet).isNotEmpty()
                .allSatisfy(c -> assertThat(omran).as("en-tête OMRAN de " + objet).contains(String.valueOf(c.get("entete"))));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ENTITES, PROJETS, TRANCHES, PROSPECTIONS})
    void decision3_unSeulPivot_code(String objet) throws Exception {
        List<Map<String, Object>> pivots = catalogueDe(objet).stream()
                .filter(c -> Boolean.TRUE.equals(c.get("pivot"))).toList();

        assertThat(pivots).as(objet).singleElement().satisfies(p -> assertThat(p)
                .containsEntry("propriete", "code").containsEntry("entete", "Code"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dtos")
    void r1_3_typesConnus_etProprietesNonIgnoreesSontDesChampsDuDto(String objet, Class<?> dto) throws Exception {
        List<Map<String, Object>> colonnes = catalogueDe(objet);
        List<String> champs = composantes(dto);

        assertAll(
                () -> assertThat(colonnes).isNotEmpty().allSatisfy(c -> assertThat(TYPES).as("type de " + c)
                        .contains(String.valueOf(c.get("type")))),
                () -> assertThat(colonnes.stream().filter(c -> !Boolean.TRUE.equals(c.get("ignoree"))).toList())
                        .isNotEmpty()
                        .allSatisfy(c -> assertThat(champs).as("propriété de " + c).contains(String.valueOf(c.get("propriete")))));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Archivée", "Validité du statut"})
    void r1_5_colonnesDeWorkflowDesProspections_ignorees(String entete) throws Exception {
        List<Map<String, Object>> colonnes = catalogueDe(PROSPECTIONS).stream()
                .filter(c -> entete.equals(c.get("entete"))).toList();

        assertThat(colonnes).singleElement().satisfies(c -> assertThat(c).containsEntry("ignoree", true));
    }
}
