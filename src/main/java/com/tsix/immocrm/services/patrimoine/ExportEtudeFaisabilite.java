package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ColonneLibre;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.domain.faisabilite.ValeursFaisabilite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanFaisabilitePrevisionsDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TotauxPrevisionsDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.FinancementFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LignePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneTrancheEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneUniteEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.VentilationEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.VentilationPrevisionDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.domain.TypeExport.DATE;
import static com.tsix.immocrm.domain.TypeExport.MONTANT;
import static com.tsix.immocrm.domain.TypeExport.NOMBRE;
import static com.tsix.immocrm.domain.TypeExport.TEXTE;

/**
 * Export Excel d'une étude en 11 feuilles (FEATURE-patrimoine-faisabilite-workflow.md §7, {@code FeasibilityStudy.cs:button1_Click}) :
 * valeurs lues du bilan stocké, des prévisions et de la trésorerie déjà calculés, jamais recalculées ici.
 */
@Service
@Transactional(readOnly = true)
public class ExportEtudeFaisabilite {

    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String ORDRE = "Ordre";
    private static final String QUANTITE = "Quantité";
    private static final String POIDS = "Poids";
    private static final String DATE_COL = "Date";
    private static final String TAUX_CHARGE = "Taux de charge";
    private static final String UNITE = "Unité";

    /** Colonne d'une feuille : sa clé dans la ligne, son en-tête et son format. */
    record Colonne(String nom, String libelle, TypeExport typeExport) implements ColonneLibre {
        @Override
        public boolean parDefaut() {
            return true;
        }
    }

    private final StkFeasibilityStudyRepository etudes;
    private final GrillesEtude grilles;
    private final PrevisionFaisabiliteService previsions;
    private final FinancementFaisabiliteService financements;
    private final ExportTableur tableur;

    public ExportEtudeFaisabilite(StkFeasibilityStudyRepository etudes, GrillesEtude grilles,
                                  PrevisionFaisabiliteService previsions, FinancementFaisabiliteService financements,
                                  ExportTableur tableur) {
        this.etudes = etudes;
        this.grilles = grilles;
        this.previsions = previsions;
        this.financements = financements;
        this.tableur = tableur;
    }

    public ResponseEntity<byte[]> exporter(UUID etudeId) {
        StkFeasibilityStudy e = etudes.exigerVisible(etudeId);
        List<FinancementFaisabiliteDto> credits = financements.lister(etudeId);
        List<LigneUniteEtudeDto> unites = grilles.unitesDto(etudeId);
        List<LigneTrancheEtudeDto> tranches = grilles.tranchesDto(etudeId);
        BilanFaisabilitePrevisionsDto arbre = previsions.lire(etudeId);
        List<FeuilleExport> feuilles = List.of(identite(e), bilan(e, arbre.totaux(), credits), terrain(e), execution(e),
                delais(FicheEtude.valeurs(e)), financement(e, credits), donnees(etudeId), unites(unites),
                tranches(tranches), unitesParTranche(unites, tranches), bilanDetaille(arbre.lignes(), tranches));
        return tableur.reponseEnMemoire("etude-" + e.getCode(), "Étude de faisabilité " + e.getCode(), feuilles);
    }

    private static FeuilleExport identite(StkFeasibilityStudy e) {
        return uneLigne("Identité", List.of(col(CODE, TEXTE), col(DESCRIPTION, TEXTE), col("Terrain", TEXTE), col("Société", TEXTE)),
                e.getCode(), e.getDescription(), e.getLandDescription(), Champs.de(e.getCfgCompany(), CfgCompany::getDescription));
    }

    private static FeuilleExport bilan(StkFeasibilityStudy e, TotauxPrevisionsDto totaux, List<FinancementFaisabiliteDto> credits) {
        BilanEtudeDto b = FicheEtude.bilan(e);
        BigDecimal credit = credits.stream().map(FinancementFaisabiliteDto::montant).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return uneLigne("Bilan", List.of(col("Coût global HT", MONTANT), col("Coût global TVA", MONTANT),
                        col("Coût global TTC", MONTANT), col("Chiffre d'affaires HT", MONTANT),
                        col("Chiffre d'affaires TVA", MONTANT), col("Chiffre d'affaires TTC", MONTANT),
                        col("Taux de concrétisation", NOMBRE), col("Crédit", MONTANT), col("Taux d'IS", NOMBRE)),
                b.coutTotalHt(), ecart(b.coutTotal(), b.coutTotalHt()), b.coutTotal(), totaux.recetteHt(),
                ecart(b.chiffreAffaires(), totaux.recetteHt()), b.chiffreAffaires(), b.pourcentageConcretisation(), credit,
                b.tauxIs());
    }

    private static FeuilleExport terrain(StkFeasibilityStudy e) {
        return uneLigne("Terrain", List.of(col("Description du terrain", TEXTE), col("Superficie", NOMBRE),
                        col("Superficie équipements", NOMBRE), col("Coefficient d'emprise", NOMBRE), col("CUF", NOMBRE),
                        col("Coefficient vendable", NOMBRE), col("Prix d'achat du m²", MONTANT),
                        col("Coefficient frais d'enregistrement", NOMBRE), col("Coefficient autres frais", NOMBRE)),
                e.getLandDescription(), e.getArea(), e.getEquipementsAmount(), e.getOccupationSolRatio(), e.getCuf(),
                e.getCoefSaleableArea(), e.getPurchaseUnitCost(), e.getRegistryCostsRatio(), e.getOtherCostsLandRatio());
    }

    private static FeuilleExport execution(StkFeasibilityStudy e) {
        return uneLigne("Exécution", List.of(col("Nombre d'articles", NOMBRE), col("Taux d'application de la retenue", NOMBRE),
                        col("Taux de retenue de garantie", NOMBRE), col("Délai de retour (jours)", NOMBRE)),
                e.getNumberOfItemForSale(), e.getBackGuaranteeApplicationRate(), e.getBackGuaranteeRate(), e.getBackTimeOnDays());
    }

    private static FeuilleExport delais(ValeursFaisabilite v) {
        List<Colonne> colonnes = List.of(col("Phase", TEXTE), col("Début", DATE), col("Durée (mois)", NOMBRE), col("Fin", DATE));
        List<Map<String, Object>> lignes = new ArrayList<>(List.of(
                ligne(colonnes, "Achat du terrain", v.debutAchatTerrain(), v.dureeAchatTerrain(), v.finAchatTerrain()),
                ligne(colonnes, "Période de validation", v.debutPeriodeValidation(), v.dureePeriodeValidation(),
                        v.finPeriodeValidation()),
                ligne(colonnes, "Permis de bâtir", v.debutPermisBatir(), v.dureePermisBatir(), v.finPermisBatir()),
                ligne(colonnes, "Études", v.debutEtudes(), v.dureeEtudes(), v.finEtudes()),
                ligne(colonnes, "Travaux", v.debutTravaux(), v.dureeTravaux(), v.finTravaux()),
                ligne(colonnes, "Concrétisation", v.debutConcretisation(), v.dureeConcretisation(), v.finConcretisation()),
                ligne(colonnes, "Finalisation", v.debutFinalisation(), v.dureeFinalisation(), v.finFinalisation())));
        lignes.addAll(List.of(
                delai(colonnes, "Délai achat du terrain → période de validation", v.delaiAchatTerrainPeriodeValidation()),
                delai(colonnes, "Délai période de validation → permis de bâtir", v.delaiPeriodeValidationPermisBatir()),
                delai(colonnes, "Délai permis de bâtir → études", v.delaiPermisBatirEtudes()),
                delai(colonnes, "Délai permis de bâtir → travaux", v.delaiPermisBatirTravaux()),
                delai(colonnes, "Délai travaux → concrétisation", v.delaiTravauxConcretisation()),
                delai(colonnes, "Délai travaux → finalisation", v.delaiTravauxFinalisation())));
        return new FeuilleExport("Délais", colonnes, lignes, null);
    }

    private static Map<String, Object> delai(List<Colonne> colonnes, String libelle, Integer mois) {
        return ligne(colonnes, libelle, null, mois, null);
    }

    /** Première ligne : TVA sur les intérêts (dans la colonne des taux), puis un crédit par ligne. */
    private static FeuilleExport financement(StkFeasibilityStudy e, List<FinancementFaisabiliteDto> credits) {
        List<Colonne> colonnes = List.of(col(DESCRIPTION, TEXTE), col(DATE_COL, DATE), col("Déblocage", MONTANT),
                col("Montant à régler", MONTANT), col("Nombre de remboursements", NOMBRE), col("Périodicité (mois)", NOMBRE),
                col("Période de grâce (mois)", NOMBRE), col(TAUX_CHARGE, NOMBRE));
        List<Map<String, Object>> lignes = new ArrayList<>();
        lignes.add(Map.of(DESCRIPTION, "TVA sur intérêts", TAUX_CHARGE, Objects.requireNonNullElse(e.getInterestVatRatio(),
                BigDecimal.ZERO)));
        credits.forEach(c -> lignes.add(ligne(colonnes, c.description(), c.dateDeblocage(), c.montant(), c.montantARegler(),
                c.nombreRemboursements(), c.periodiciteMois(), c.periodeGraceMois(), c.tauxCharge())));
        return new FeuilleExport("Financement", colonnes, lignes, null);
    }

    private FeuilleExport donnees(UUID etudeId) {
        List<Colonne> colonnes = List.of(col(DATE_COL, DATE), col("Débit", MONTANT), col("Crédit", MONTANT), col(DESCRIPTION, TEXTE));
        return new FeuilleExport("Données", colonnes, previsions.tresorerie(etudeId, true).lignes().stream()
                .map(l -> ligne(colonnes, l.date(), l.debit(), l.credit(), l.libelle())).toList(), null);
    }

    private static FeuilleExport unites(List<LigneUniteEtudeDto> unites) {
        List<Colonne> colonnes = List.of(col(ORDRE, NOMBRE), col(CODE, TEXTE), col(QUANTITE, NOMBRE));
        return new FeuilleExport("Unités", colonnes,
                unites.stream().map(u -> ligne(colonnes, u.sorting(), u.uniteCode(), u.quantite())).toList(), null);
    }

    private static FeuilleExport tranches(List<LigneTrancheEtudeDto> tranches) {
        List<Colonne> colonnes = List.of(col(ORDRE, NOMBRE), col(CODE, TEXTE), col(POIDS, NOMBRE), col("Tranche", TEXTE));
        return new FeuilleExport("Tranches", colonnes,
                tranches.stream().map(t -> ligne(colonnes, t.sorting(), t.code(), t.poids(), t.trancheLibelle())).toList(), null);
    }

    /** Quantité ventilée et poids par tranche ; à défaut de ventilation, poids de la tranche × quantité (comme OMRAN). */
    private static FeuilleExport unitesParTranche(List<LigneUniteEtudeDto> unites, List<LigneTrancheEtudeDto> tranches) {
        List<Colonne> colonnes = new ArrayList<>(List.of(col(UNITE, TEXTE), col("Quantité de base", NOMBRE)));
        tranches.forEach(t -> colonnes.addAll(List.of(col(t.code() + " (Qte)", NOMBRE), col(t.code() + " (%)", NOMBRE))));
        List<Map<String, Object>> lignes = tranches.isEmpty() ? List.of() : unites.stream().map(u -> {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put(UNITE, u.uniteCode());
            ligne.put("Quantité de base", u.quantite());
            for (LigneTrancheEtudeDto t : tranches) {
                VentilationEtudeDto v = Objects.requireNonNullElse(t.unites(), List.<VentilationEtudeDto>of()).stream()
                        .filter(x -> Objects.equals(x.uniteEtudeId(), u.id())).findFirst().orElse(null);
                BigDecimal poids = v != null && v.poids() != null ? v.poids() : t.poids();
                ligne.put(t.code() + " (Qte)", v != null ? v.quantite() : produit(poids, u.quantite()));
                ligne.put(t.code() + " (%)", poids);
            }
            return ligne;
        }).toList();
        return new FeuilleExport("Unités par tranche", colonnes, lignes, null);
    }

    /** Un nœud par ligne, indenté de 4 espaces par niveau ; montants totaux du nœud, puis HT / TTC par tranche. */
    private static FeuilleExport bilanDetaille(List<LignePrevisionDto> arbre, List<LigneTrancheEtudeDto> tranches) {
        List<Colonne> colonnes = new ArrayList<>(List.of(col(DESCRIPTION, TEXTE), col("Visible module Projet", TypeExport.BOOLEEN),
                col("Observation", NOMBRE), col("Montant HT", MONTANT), col("Montant", MONTANT), col(POIDS, NOMBRE)));
        tranches.forEach(t -> colonnes.addAll(List.of(col(t.code() + " (HT)", MONTANT), col(t.code() + " (TTC)", MONTANT))));
        return new FeuilleExport("Bilan détaillé", colonnes, arbre.stream().map(ExportEtudeFaisabilite::noeud).toList(), null);
    }

    private static Map<String, Object> noeud(LignePrevisionDto l) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put(DESCRIPTION, "    ".repeat(l.profondeur()) + l.libelle());
        ligne.put("Visible module Projet", l.estPourProjet());
        ligne.put("Observation", l.ratio());
        ligne.put("Montant HT", l.montantHt());
        ligne.put("Montant", l.montant());
        ligne.put(POIDS, l.poids());
        for (VentilationPrevisionDto v : Objects.requireNonNullElse(l.ventilation(), List.<VentilationPrevisionDto>of())) {
            ligne.put(v.trancheCode() + " (HT)", v.montantHt());
            ligne.put(v.trancheCode() + " (TTC)", v.montant());
        }
        return ligne;
    }

    // ------------------------------------------------------------------ outils

    private static Colonne col(String libelle, TypeExport type) {
        return new Colonne(libelle, libelle, type);
    }

    private static FeuilleExport uneLigne(String nom, List<Colonne> colonnes, Object... valeurs) {
        return new FeuilleExport(nom, colonnes, List.of(ligne(colonnes, valeurs)), null);
    }

    /** Valeurs dans l'ordre des colonnes ; une valeur nulle laisse la cellule vide. */
    private static Map<String, Object> ligne(List<Colonne> colonnes, Object... valeurs) {
        Map<String, Object> ligne = new HashMap<>();
        for (int i = 0; i < valeurs.length; i++) {
            ligne.put(colonnes.get(i).nom(), valeurs[i]);
        }
        return ligne;
    }

    private static BigDecimal ecart(BigDecimal ttc, BigDecimal ht) {
        return ttc == null || ht == null ? null : ttc.subtract(ht);
    }

    private static BigDecimal produit(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? null : a.multiply(b);
    }
}
