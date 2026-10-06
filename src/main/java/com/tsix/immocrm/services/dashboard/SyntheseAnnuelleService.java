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
 * {@code GET /api/dashboard/annuel} — l'écran d'accueil du logiciel de bureau
 * ({@code instructions/FEATURE-dashboard-omran.md}).
 *
 * <p>Les trois blocs reprennent les requêtes qu'OMRAN exécute réellement, relevées dans sa trace
 * d'audit SQL. Deux écarts assumés avec le légataire, tous deux documentés sur la requête
 * concernée : la contradiction {@code IsSaleExpense} du bloc A n'est pas recopiée (seule son
 * intention l'est), et la jointure {@code StkItem} du bloc C est écartée parce qu'elle porte une
 * double-comptabilisation latente sans rien changer aujourd'hui.
 *
 * <p>Comme {@link DashboardService}, ce service n'utilise pas de {@code Specification} : le
 * périmètre commercial est appliqué <b>explicitement à chaque agrégat</b>, par la variante
 * {@code …DansPerimetre} de chaque requête. C'est le point de fuite le plus probable de la
 * feature — un agrégat resté global montrerait à un commercial restreint un chiffre d'affaires
 * qu'il n'a pas le droit de détailler.
 */
@Service
@Transactional(readOnly = true)
public class SyntheseAnnuelleService {

    /** Les statuts hors course : {@code StatutDossier.isSortie()}, résolu une fois. */
    private static final List<String> CODES_DE_SORTIE = Arrays.stream(StatutDossier.values())
            .filter(StatutDossier::isSortie)
            .map(StatutDossier::getCode)
            .toList();

    /**
     * Repli si le statut de concrétisation n'est pas en base — cas d'une base de test qui ne sème
     * pas les statuts. {@code 0} laisse alors passer tous les statuts plutôt que d'en écarter
     * arbitrairement : un seuil introuvable ne doit pas se transformer en filtre silencieux.
     */
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
     * L'ensemble de dossiers sur lequel portent les trois blocs, résolu une fois.
     *
     * <ul>
     *   <li>{@code dossierIds} non nul — filtre par tranche
     *       ({@code instructions/FEATURE-dashboard-filtre-tranche.md}) : l'assiette de
     *       {@code GET /api/dossiers?trancheId=…}, périmètre commercial déjà compris. Vide = aucun
     *       dossier : chaque bloc vaut alors zéro sans interroger la base ;</li>
     *   <li>sinon {@code perimetre} non nul — commercial restreint ;</li>
     *   <li>sinon — tout, sous la seule visibilité par entité juridique.</li>
     * </ul>
     *
     * <p>Chaque bloc choisit sa requête selon l'assiette puis agrège par <b>le même code</b> : le
     * filtre par tranche est un prédicat de plus ({@code f.id IN :dossierIds}), pas un calcul
     * parallèle (R6).
     */
    private record Assiette(@Nullable List<UUID> perimetre, @Nullable List<UUID> dossierIds) {
        boolean vide() {
            return dossierIds != null && dossierIds.isEmpty();
        }
    }

    /**
     * @param trancheId tranche ({@code CfgTranche.id}) ; {@code null} = toutes, comportement
     *                  inchangé
     */
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
     * Le seuil au-delà duquel un dossier est « concrétisé », lu en base plutôt que figé.
     *
     * <p>Le légataire le lit dans {@code AdmParamsGuid.DefaultComFolderStatusConcretisation}, qui
     * pointe sur le statut {@code CONCRETISATION} ({@code Sorting = 2}). Cette table n'existe pas
     * dans la base de test ; le seuil est donc résolu par le <b>code</b> du statut, ce qui donne
     * le même résultat aujourd'hui — mesuré — sans imposer une table de plus.
     *
     * <p>Ce qu'on perd, explicitement : si l'exploitation déplaçait le paramètre vers un autre
     * statut, ce chiffre cesserait de suivre le leur sans rien signaler.
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

    /**
     * Les douze mois, <b>toujours</b> — un mois sans échéance vaut zéro et reste un point de la
     * courbe. Reboucher les trous côté client supposerait qu'il sache lesquels manquent.
     */
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
     * Substitue {@code BigDecimal.ZERO} à un agrégat vide, <b>en Java et jamais en SQL</b>.
     *
     * <p>Un {@code COALESCE(SUM(x), 0)} côté base dégrade l'échelle de la somme sur le littéral
     * entier : 190,75 DH y devient 191. Le piège s'est déjà refermé une fois sur ce dépôt, sur
     * une somme d'argent qui paraissait juste.
     */
    private static BigDecimal montant(@Nullable Object valeur) {
        return valeur instanceof BigDecimal somme ? somme : BigDecimal.ZERO;
    }

    private static Object @Nullable [] premiere(List<Object[]> lignes) {
        return lignes.isEmpty() ? null : lignes.get(0);
    }
}
