package com.tsix.immocrm.domain.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Propriétés {@code [IsWorkFlow]} de {@code StkFeasibilityStudy} (omran-src/Yes.Rems/Yes.Stk/StkFeasibilityStudy.cs), cochables
 * comme champs obligatoires d'un statut ou d'une transition ; {@code MandatoryFields} les stocke avec une virgule initiale.
 */
public final class ChampsObligatoiresFaisabilite {

    @Schema(description = "Champ cochable comme obligatoire d'un statut ou d'une transition d'étude.")
    public record ChampObligatoire(
            @Schema(description = "Nom OMRAN stocké dans MandatoryFields.", example = "Area") String nom,
            @Schema(description = "Champ du CRM (fiche de l'étude).", example = "superficie") String champ,
            @Schema(description = "Libellé OMRAN (DisplayName).") String libelle) {
    }

    public static final List<ChampObligatoire> CATALOGUE = List.of(
            c("Code", "code", "Code"),
            c("Description", "description", "Description"),
            c("DocDate", "dateDocument", "Date"),
            c("Area", "superficie", "Surface terrain"),
            c("AreaNet", "superficieNette", "Surface terrain Nette"),
            c("Cos", "coefEmprise", "Coef Emprise au sol Batiments"),
            c("AreaCos", "empriseAuSol", "Emprise au sol Batiments(m²)"),
            c("Cuf", "cuf", "Coef Plancher Couvert"),
            c("AreaCuf", "plancherCouvert", "Plancher Couvert (m²)"),
            c("CoefSaleableArea", "coefVendable", "Coef superficie vendable"),
            c("SalableArea", "plancherVendable", "Superficie Vendable"),
            c("EquipementsCoef", "coefEquipements", "Coef équipements et voies d'aménagement"),
            c("EquipementsAmount", "superficieEquipements", "Superficie équipements et voies d'aménagement (m2)"),
            c("NoBuiltArea", "surfaceNonBatie", "Superficie non batie"),
            c("PurchaseUnitCostHT", "prixAchatM2Ht", "Prix d'achat du m² HT"),
            c("PurchaseUnitCostVatAmount", "prixAchatM2Tva", "Prix d'achat du m² TVA"),
            c("PurchaseUnitCost", "prixAchatM2", "Prix d'achat du m²"),
            c("PriceAreaHT", "prixTerrainHt", "Prix du terrain HT"),
            c("PriceAreaVatAmount", "prixTerrainTva", "Prix du terrain TVA"),
            c("PriceArea", "prixTerrain", "Prix du terrain"),
            c("RegistryCostsRatio", "tauxFraisEnregistrement", "Coef frais d'enregistrement"),
            c("RegistryCostsHT", "fraisEnregistrementHt", "Frais d'enregistrement HT"),
            c("RegistryCostsVatAmount", "fraisEnregistrementTva", "Frais d'enregistrement TVA"),
            c("RegistryCosts", "fraisEnregistrement", "Frais d'enregistrement"),
            c("OtherCostsLandRatio", "tauxAutresFraisTerrain", "Coef d'autre frais"),
            c("OtherCostsLandHT", "autresFraisTerrainHt", "Autre frais HT"),
            c("OtherCostsLand", "autresFraisTerrain", "Autre frais"),
            c("TotalCostLand", "coutTotalTerrain", "Coût total Terrain"),
            c("UnitCostConstructionBuilt", "coutConstructionM2Bati", "Coût de construction du m² Bati"),
            c("CostAreaBuilt", "coutBati", "coût de la superficie Batie"),
            c("UnitCostConstructionNoBuilt", "coutConstructionM2NonBati", "Coût de construction du m² non bati"),
            c("CostAreaNoBuilt", "coutNonBati", "Coût de la superficie non bati"),
            c("OtherCostsExecutionRatio", "tauxAutresFraisExecution", "Coef d'autre frais d'execution"),
            c("OtherCostsExecution", "autresFraisExecution", "Autre frais d'execution"),
            c("UnitBackCost", "coutRevientM2", "Coût de revient du m²"),
            c("TotalCostExecutionHT", "coutExecutionHt", "Coût global d'exécution HT"),
            c("TotalCostExecution", "coutExecution", "Coût global d'exécution"),
            c("TotalCostHT", "coutTotalHt", "Coût global HT"),
            c("TotalCost", "coutTotal", "Coût global"));

    private static final Set<String> NOMS = CATALOGUE.stream().map(ChampObligatoire::nom).collect(Collectors.toSet());

    private ChampsObligatoiresFaisabilite() {
    }

    private static ChampObligatoire c(String nom, String champ, String libelle) {
        return new ChampObligatoire(nom, champ, libelle);
    }

    /** Champs du catalogue cochés dans {@code mandatoryFields}, dans l'ordre du catalogue ; un nom inconnu est ignoré. */
    public static List<ChampObligatoire> coches(String mandatoryFields) {
        List<String> noms = noms(mandatoryFields);
        return CATALOGUE.stream().filter(ch -> noms.contains(ch.nom())).toList();
    }

    /** Noms inconnus d'une saisie (400 à la charge de l'appelant). */
    public static List<String> inconnus(List<String> noms) {
        return noms.stream().filter(n -> !NOMS.contains(n)).toList();
    }

    /** Format OMRAN : virgule initiale ({@code ",Code,Area"}), {@code ""} si aucun champ ; doublons retirés. */
    public static String stocker(List<String> noms) {
        return noms.stream().distinct().map(n -> "," + n).collect(Collectors.joining());
    }

    /**
     * {@code VerifStatusMandatories} / {@code VerifWorkflowMandatories} : seuls texte et date peuvent être vides ; un décimal
     * OMRAN n'est jamais null, donc jamais vide, même à 0.
     */
    public static boolean renseigne(ChampObligatoire ch, String code, String description, LocalDateTime date) {
        return switch (ch.nom()) {
            case "Code" -> code != null && !code.isEmpty();
            case "Description" -> description != null && !description.isEmpty();
            case "DocDate" -> date != null;
            default -> true;
        };
    }

    /** Une ligne « Le champ « X » est obligatoire » par champ coché vide ; vide si tout est renseigné. */
    public static List<String> manquants(String mandatoryFields, Predicate<ChampObligatoire> renseigne) {
        return coches(mandatoryFields).stream().filter(renseigne.negate())
                .map(ch -> "Le champ « " + ch.libelle() + " » est obligatoire").toList();
    }

    /** Noms connus du catalogue, ordre de stockage ; un nom inconnu stocké en base est ignoré. */
    public static List<String> noms(String mandatoryFields) {
        return mandatoryFields == null ? List.of() : Arrays.stream(mandatoryFields.split(","))
                .map(String::trim).filter(NOMS::contains).distinct().toList();
    }
}
