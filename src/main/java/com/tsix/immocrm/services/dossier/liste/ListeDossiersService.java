package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.liste.StatutListeDossierDto;
import com.tsix.immocrm.dto.dossier.liste.TotauxDossiersDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Liste, groupes et export des dossiers de vente : une seule spécification ({@link SpecificationsListeDossiers})
 * et le même périmètre, pour que groupes, totaux et export portent sur les lignes de la liste.
 */
@Service
@Transactional(readOnly = true)
public class ListeDossiersService {

    /** Ordre de la grille OMRAN : article, tranche (H1), projet (H2), entité (H3). */
    public static final Sort ORDRE_OMRAN = Sort.by(
            Sort.Order.asc("stkItem.code"),
            Sort.Order.asc("stkItem.stkHierarchy.code"),
            Sort.Order.asc("stkItem.stkHierarchy.cfgProject.code"),
            Sort.Order.asc("stkItem.stkHierarchy.cfgProject.cfgCompany.code"),
            Sort.Order.asc("id"));

    /** Plafond de l'export : au-delà, un classeur n'est plus lisible et la requête bloque la base. */
    static final int LIGNES_EXPORT_MAX = 20_000;

    private final ComFolderRepository folderRepository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final StatutsVisiblesListe statutsVisibles;
    private final CalculsDeLaPage calculs;
    private final LotsLibres lotsLibres;
    private final StrStatusRepository statusRepository;

    public ListeDossiersService(ComFolderRepository folderRepository, PerimetreCommercialService perimetreService,
                                GroupementService groupementService, StatutsVisiblesListe statutsVisibles,
                                CalculsDeLaPage calculs, LotsLibres lotsLibres,
                                StrStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
        this.folderRepository = folderRepository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.statutsVisibles = statutsVisibles;
        this.calculs = calculs;
        this.lotsLibres = lotsLibres;
    }

    /** Dépliage d'un groupe : {@code groupe} non nul, même clé que {@code /groupes}. */
    public record Depliage(ColonneDossier colonne, String valeur, Granularite granularite) {
    }

    /**
     * Dossiers d'abord, puis les lots libres si demandés : la pagination court sur la concaténation.
     *
     * @param ordreOmran remplace le tri reçu par {@link #ORDRE_OMRAN}
     */
    public Page<DossierListItemDto> lister(FiltresListeDossiers filtres, Depliage depliage, boolean ordreOmran,
                                           Pageable pageable, Set<ColonneDossier> colonnes) {
        Pageable page = ordreOmran ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDRE_OMRAN)
                : pageable;
        List<LigneDossier> lignes = new ArrayList<>();
        long nombreDossiers = 0;
        if (filtres.lignes().avecDossiers()) {
            Page<ComFolder> dossiers = folderRepository.findAll(specification(filtres, depliage), page);
            nombreDossiers = dossiers.getTotalElements();
            lignes.addAll(lignesDeDossiers(dossiers.getContent(), colonnes));
        }
        if (depliage != null) {
            return new PageImpl<>(versDto(lignes), page, nombreDossiers);
        }
        long nombreLibres = lotsLibres.compter(filtres);
        long debutLibres = Math.max(0, page.getOffset() - nombreDossiers);
        int place = page.getPageSize() - lignes.size();
        lotsLibres.tranche(filtres, debutLibres, place).stream()
                .map(MappingListeDossier::depuisLotLibre)
                .forEach(lignes::add);
        return new PageImpl<>(versDto(lignes), page, nombreDossiers + nombreLibres);
    }

    /** Même spécification et même périmètre que la liste : la somme des groupes retombe sur son total. */
    public ResultatGroupement grouper(FiltresListeDossiers filtres, ColonneDossier groupeBy, Granularite granularite,
                                      List<AgregatDemande> agregats, TriGroupe tri, boolean inclureSansValeur,
                                      Pageable pageable) {
        return groupementService.grouper(ComFolder.class, specification(filtres, null), groupeBy.chemin(),
                granularite, agregats, tri, inclureSansValeur, pageable);
    }

    /** Toutes les lignes de la liste, sans pagination, dans l'ordre demandé (export). */
    public List<DossierListItemDto> toutes(FiltresListeDossiers filtres, boolean ordreOmran, Sort tri,
                                           Set<ColonneDossier> colonnes) {
        Sort ordre = ordreOmran || tri == null || tri.isUnsorted() ? ORDRE_OMRAN : tri;
        Pageable tout = PageRequest.of(0, LIGNES_EXPORT_MAX, ordre);
        return lister(filtres, null, false, tout, colonnes).getContent();
    }

    /** Statuts de dossier visibles de l'appelant, dans l'ordre d'OMRAN. */
    public List<StatutListeDossierDto> statuts() {
        Optional<Set<UUID>> visibles = statutsVisibles.pourUtilisateurCourant();
        return statusRepository.findByStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID).stream()
                .filter(s -> visibles.map(ids -> ids.contains(s.getId())).orElse(true))
                .sorted(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(s -> new StatutListeDossierDto(s.getCode(), s.getDescription(), s.getSorting(), !s.estActif()))
                .toList();
    }

    private Specification<ComFolder> specification(FiltresListeDossiers filtres, Depliage depliage) {
        Specification<ComFolder> spec = SpecificationsListeDossiers.de(filtres, statutsVisibles.pourUtilisateurCourant());
        if (depliage != null) {
            // Même expression de clé que /groupes : une valeur réémise retombe sur les lignes comptées.
            spec = spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(
                    root, cb, depliage.colonne().chemin(), depliage.granularite(), depliage.valeur()));
        }
        return spec.and(perimetreService.filtreDossiers());
    }

    /**
     * Σ des montants par nature sur toutes les lignes de la liste : même spécification, même périmètre et mêmes
     * statuts visibles que {@link #lister}, même calcul par dossier que ses colonnes.
     */
    public TotauxDossiersDto totaux(FiltresListeDossiers filtres) {
        if (!filtres.lignes().avecDossiers()) {
            return new TotauxDossiersDto(0, sommesNulles());
        }
        List<ComFolder> dossiers = folderRepository.findAll(specification(filtres, null));
        Map<String, BigDecimal> sommes = sommesNulles();
        Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = calculs.montants().pour(dossiers);
        for (ComFolder f : dossiers) {
            // prixContractuel est une colonne de ComFolder (nulle en ligne si absente) : lue ici, pas recalculée.
            Map<ColonneDossier, BigDecimal> montants = new EnumMap<>(parDossier.get(f.getId()));
            montants.put(ColonneDossier.PRIX_CONTRACTUEL,
                    f.getAgreementPrice() != null ? f.getAgreementPrice() : BigDecimal.ZERO);
            MontantsParNatureDossier.SOMMABLES.forEach(c -> sommes.merge(c.nom(), montants.get(c), BigDecimal::add));
        }
        return new TotauxDossiersDto(dossiers.size(), sommes);
    }

    private static Map<String, BigDecimal> sommesNulles() {
        Map<String, BigDecimal> sommes = new LinkedHashMap<>();
        MontantsParNatureDossier.SOMMABLES.forEach(c -> sommes.put(c.nom(), BigDecimal.ZERO));
        return sommes;
    }

    private List<LigneDossier> lignesDeDossiers(List<ComFolder> dossiers, Set<ColonneDossier> colonnes) {
        Map<UUID, ValeursCalculees> valeurs = ColonnesCalculeesDossier.requises(colonnes)
                ? calculs.colonnes().pour(dossiers.stream().map(ComFolder::getId).toList(),
                        PerimetreCommercialService.admUserIdCourant().orElse(null))
                : Map.of();
        Map<UUID, Map<ColonneDossier, BigDecimal>> montants = MontantsParNatureDossier.requises(colonnes)
                ? calculs.montants().pour(dossiers)
                : Map.of();
        return dossiers.stream()
                .map(f -> {
                    LigneDossier ligne = MappingListeDossier.depuisDossier(f, colonnes, valeurs.get(f.getId()));
                    montants.getOrDefault(f.getId(), Map.of()).forEach(ligne::met);
                    return ligne;
                })
                .toList();
    }

    private static List<DossierListItemDto> versDto(List<LigneDossier> lignes) {
        return lignes.stream().map(LigneDossier::versDto).toList();
    }
}
