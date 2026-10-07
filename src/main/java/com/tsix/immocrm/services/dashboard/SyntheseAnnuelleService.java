package com.tsix.immocrm.services.dashboard;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dashboard.EncaissementsAnnuelsDto;
import com.tsix.immocrm.dto.dashboard.PointMensuelDto;
import com.tsix.immocrm.dto.dashboard.SyntheseAnnuelleDto;
import com.tsix.immocrm.dto.dashboard.VentesConcretiseesDto;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Écran d'accueil d'OMRAN ({@code FEATURE-dashboard-omran.md}). Pas de {@code Specification} : le
 * périmètre s'applique à chaque agrégat par la variante {@code …DansPerimetre} de chaque requête.
 */
@Service
@Transactional(readOnly = true)
public class SyntheseAnnuelleService {

    private static final List<String> CODES_DE_SORTIE = Arrays.stream(StatutDossier.values())
            .filter(StatutDossier::isSortie)
            .map(StatutDossier::getCode)
            .toList();

    /** Statut introuvable (base de test) : 0 laisse tout passer plutôt que de filtrer en silence. */
    private static final int SEUIL_PAR_DEFAUT = 0;

    private final ComDocumentRepository documentRepository;
    private final ComFolderRepository folderRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final StrStatusRepository strStatusRepository;
    private final PerimetreCommercialService perimetreService;
    private final DossierService dossierService;

    public SyntheseAnnuelleService(ComDocumentRepository documentRepository,
                                   ComFolderRepository folderRepository,
                                   StlSettlementLineRepository settlementLineRepository,
                                   StrStatusRepository strStatusRepository,
                                   PerimetreCommercialService perimetreService,
                                   DossierService dossierService) {
        this.documentRepository = documentRepository;
        this.folderRepository = folderRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.strStatusRepository = strStatusRepository;
        this.perimetreService = perimetreService;
        this.dossierService = dossierService;
    }

    /**
     * {@code dossierIds} (tranche) porte déjà le périmètre — vide ⇒ zéros sans requête ; sinon
     * {@code perimetre} si restreint ; sinon tout, sous la visibilité par entité.
     */
    private record Assiette(@Nullable List<UUID> perimetre, @Nullable List<UUID> dossierIds) {
        boolean vide() {
            return dossierIds != null && dossierIds.isEmpty();
        }
    }

    public SyntheseAnnuelleDto get(@Nullable Integer annee, @Nullable UUID trancheId) {
        int cible = annee != null ? annee : LocalDate.now().getYear();
        LocalDateTime debut = LocalDate.of(cible, 1, 1).atStartOfDay();
        LocalDateTime finExclue = LocalDate.of(cible + 1, 1, 1).atStartOfDay();

        Assiette assiette = trancheId != null
                ? new Assiette(null, List.copyOf(dossierService.idsDossiersDeTranche(trancheId)))
                : new Assiette(perimetreService.perimetreCourant().orElse(null), null);
        int seuil = seuilDeConcretisation();

        return new SyntheseAnnuelleDto(
                cible,
                echeancier(cible, seuil, assiette),
                ventes(debut, finExclue, seuil, assiette),
                encaissements(debut, finExclue, assiette));
    }

    /**
     * Le légataire lit {@code AdmParamsGuid}, non mappée : résolu par le code du statut, ce qui cesserait
     * de suivre un paramètre déplacé ({@code FEATURE-dashboard-omran.md} §5).
     */
    private int seuilDeConcretisation() {
        StrStatus concretisation = strStatusRepository.findByStrConfigEntityIdAndCode(
                StatutDossier.CONFIG_ENTITY_ID, StatutDossier.CONCRETISATION.getCode());
        if (concretisation == null || concretisation.getSorting() == null) {
            return SEUIL_PAR_DEFAUT;
        }
        return concretisation.getSorting();
    }

    // ---------------------------------------------------------------- bloc A

    /** Toujours douze points : un mois sans échéance vaut zéro, le client n'a pas à boucher les trous. */
    private List<PointMensuelDto> echeancier(int annee, int seuil, Assiette assiette) {
        List<Object[]> lignes;
        if (assiette.vide()) {
            lignes = List.of();
        } else if (assiette.dossierIds() != null) {
            lignes = documentRepository.echeancierParMoisPourDossiers(annee, seuil, CODES_DE_SORTIE,
                    assiette.dossierIds());
        } else if (assiette.perimetre() != null) {
            lignes = documentRepository.echeancierParMoisDansPerimetre(annee, seuil, CODES_DE_SORTIE,
                    assiette.perimetre());
        } else {
            lignes = documentRepository.echeancierParMois(annee, seuil, CODES_DE_SORTIE);
        }

        Map<Integer, Object[]> parMois = new HashMap<>();
        for (Object[] ligne : lignes) {
            parMois.put(((Number) ligne[0]).intValue(), ligne);
        }

        List<PointMensuelDto> points = new ArrayList<>(12);
        for (int mois = 1; mois <= 12; mois++) {
            Object[] ligne = parMois.get(mois);
            points.add(new PointMensuelDto(
                    mois,
                    ligne != null ? montant(ligne[1]) : BigDecimal.ZERO,
                    ligne != null ? ((Number) ligne[2]).longValue() : 0L));
        }
        return points;
    }

    // ---------------------------------------------------------------- bloc B

    private VentesConcretiseesDto ventes(LocalDateTime debut, LocalDateTime finExclue, int seuil,
                                         Assiette assiette) {
        List<UUID> dossierIds = assiette.dossierIds();
        List<UUID> perimetre = assiette.perimetre();
        List<Object[]> agregat;
        BigDecimal regle;
        long sansDateAvance;
        if (assiette.vide()) {
            agregat = List.of();
            regle = null;
            sansDateAvance = 0L;
        } else if (dossierIds != null) {
            agregat = folderRepository.ventesConcretiseesPourDossiers(debut, finExclue, seuil, CODES_DE_SORTIE, dossierIds);
            regle = settlementLineRepository.montantReglePourVentesPourDossiers(debut, finExclue, seuil, CODES_DE_SORTIE, dossierIds);
            sansDateAvance = folderRepository.compterDossiersSansDateAvancePourDossiers(dossierIds);
        } else if (perimetre != null) {
            agregat = folderRepository.ventesConcretiseesDansPerimetre(debut, finExclue, seuil, CODES_DE_SORTIE, perimetre);
            regle = settlementLineRepository.montantReglePourVentesDansPerimetre(debut, finExclue, seuil, CODES_DE_SORTIE, perimetre);
            sansDateAvance = folderRepository.compterDossiersSansDateAvanceDansPerimetre(perimetre);
        } else {
            agregat = folderRepository.ventesConcretisees(debut, finExclue, seuil, CODES_DE_SORTIE);
            regle = settlementLineRepository.montantReglePourVentes(debut, finExclue, seuil, CODES_DE_SORTIE);
            sansDateAvance = folderRepository.compterDossiersSansDateAvance();
        }

        Object[] ligne = premiere(agregat);
        return new VentesConcretiseesDto(
                ligne != null ? ((Number) ligne[0]).longValue() : 0L,
                ligne != null ? montant(ligne[1]) : BigDecimal.ZERO,
                montant(regle),
                sansDateAvance);
    }

    // ---------------------------------------------------------------- bloc C

    private EncaissementsAnnuelsDto encaissements(LocalDateTime debut, LocalDateTime finExclue,
                                                  Assiette assiette) {
        List<Object[]> agregat;
        if (assiette.vide()) {
            agregat = List.of();
        } else if (assiette.dossierIds() != null) {
            agregat = settlementLineRepository.encaissementsDeLAnneePourDossiers(debut, finExclue, assiette.dossierIds());
        } else if (assiette.perimetre() != null) {
            agregat = settlementLineRepository.encaissementsDeLAnneeDansPerimetre(debut, finExclue, assiette.perimetre());
        } else {
            agregat = settlementLineRepository.encaissementsDeLAnnee(debut, finExclue);
        }

        Object[] ligne = premiere(agregat);
        return new EncaissementsAnnuelsDto(
                ligne != null ? ((Number) ligne[0]).longValue() : 0L,
                ligne != null ? montant(ligne[1]) : BigDecimal.ZERO);
    }

    // ---------------------------------------------------------------- utilitaires

    /**
     * Zéro substitué en Java, jamais en SQL : {@code COALESCE(SUM(x), 0)} ramène la somme à l'échelle
     * du littéral entier (190,75 devient 191).
     */
    private static BigDecimal montant(@Nullable Object valeur) {
        return valeur instanceof BigDecimal somme ? somme : BigDecimal.ZERO;
    }

    private static Object @Nullable [] premiere(List<Object[]> lignes) {
        return lignes.isEmpty() ? null : lignes.get(0);
    }
}
