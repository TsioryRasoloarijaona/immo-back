package com.tsix.immocrm.services.impaye;

import com.tsix.immocrm.domain.ChampDateImpaye;
import com.tsix.immocrm.domain.EchuA;
import com.tsix.immocrm.domain.Impaye;
import com.tsix.immocrm.dto.impaye.ImpayeDto;
import com.tsix.immocrm.dto.impaye.ImpayesPageDto;
import com.tsix.immocrm.dto.impaye.TotauxImpayesDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.services.dossier.ParametresVente;
import com.tsix.immocrm.services.dossier.SocieteDossier;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Liste des impayés de vente ({@code FEATURE-impayes.md}). Deux temps : les échéances candidates en SQL
 * ({@link SpecImpayes}, périmètre des dossiers compris), puis le réglé par la requête de l'échéancier
 * ({@code StlSettlementLineRepository.MONTANT_SIGNE}) ; le filtre « reste &gt; 0 », le tri et la page en mémoire.
 */
@Service
@Transactional(readOnly = true)
public class ImpayeService {

    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final PerimetreCommercialService perimetreService;
    private final ParametresVente parametres;
    private final Clock clock;

    public ImpayeService(ComDocumentRepository documentRepository,
                         StlSettlementLineRepository settlementLineRepository,
                         PerimetreCommercialService perimetreService, ParametresVente parametres, Clock clock) {
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.perimetreService = perimetreService;
        this.parametres = parametres;
        this.clock = clock;
    }

    /** Filtres de {@code GET /api/impayes}, tels que reçus du contrôleur ; {@code echu} null vaut ECHEANCE. */
    public record FiltresImpayes(List<UUID> projetIds, List<UUID> trancheIds, EchuA echu,
                                 ChampDateImpaye champDate, LocalDate du, LocalDate au, UUID commercialId,
                                 UUID categorieId, String client, String article, String q) {
    }

    /** Page demandée et totaux de <b>toutes</b> les lignes du filtre (R7). */
    public ImpayesPageDto findAll(FiltresImpayes filtres, Pageable pageable) {
        List<ImpayeDto> lignes = lignes(filtres, pageable.getSort());
        int debut = (int) Math.min(pageable.getOffset(), lignes.size());
        int fin = Math.min(debut + pageable.getPageSize(), lignes.size());
        int totalPages = (lignes.size() + pageable.getPageSize() - 1) / pageable.getPageSize();
        return new ImpayesPageDto(List.copyOf(lignes.subList(debut, fin)), pageable.getPageNumber(),
                pageable.getPageSize(), lignes.size(), totalPages, totaux(lignes));
    }

    /** Toutes les lignes du filtre, triées : la liste et l'export lisent la même chose. */
    public List<ImpayeDto> lignes(FiltresImpayes filtres, Sort sort) {
        LocalDate aujourdhui = LocalDate.now(clock);
        int age = parametres.ageRecouvrement();
        List<ComDocument> candidats = documentRepository.findAll(
                SpecImpayes.de(filtres, aujourdhui, age).and(perimetreService.filtreEcheances()));
        Map<UUID, BigDecimal> regles = settlementLineRepository.reglesParEcheance(
                candidats.stream().map(ComDocument::getId).toList());
        return candidats.stream()
                .map(d -> versLigne(d, regles.get(d.getId()), aujourdhui, age))
                .filter(l -> l.montantReste().signum() > 0)
                .sorted(TriImpayes.comparateur(sort))
                .toList();
    }

    /**
     * Échéances impayées d'un dossier, par la règle de l'écran (bandeau de la fiche, §1 de
     * FEATURE-dossier-durcissement.md) ; sans périmètre : l'appelant a déjà contrôlé l'accès au dossier.
     */
    public List<UUID> echeancesImpayees(UUID dossierId) {
        LocalDate aujourdhui = LocalDate.now(clock);
        List<ComDocument> candidats = documentRepository.findAll(SpecImpayes.duDossier(dossierId, aujourdhui));
        Map<UUID, BigDecimal> regles = settlementLineRepository.reglesParEcheance(
                candidats.stream().map(ComDocument::getId).toList());
        return candidats.stream()
                .filter(d -> Impaye.resteDu(d.getAmount(), regles.get(d.getId())))
                .map(ComDocument::getId)
                .toList();
    }

    private static TotauxImpayesDto totaux(List<ImpayeDto> lignes) {
        BigDecimal aPayer = BigDecimal.ZERO;
        BigDecimal regle = BigDecimal.ZERO;
        BigDecimal reste = BigDecimal.ZERO;
        for (ImpayeDto l : lignes) {
            aPayer = aPayer.add(l.montantAPayer());
            regle = regle.add(l.montantRegle());
            reste = reste.add(l.montantReste());
        }
        long dossiers = lignes.stream().map(ImpayeDto::dossierId).distinct().count();
        return new TotauxImpayesDto(aPayer, regle, reste, dossiers);
    }

    private static ImpayeDto versLigne(ComDocument d, BigDecimal regleBrut, LocalDate aujourdhui, int age) {
        BigDecimal montant = requireNonNullElse(d.getAmount(), BigDecimal.ZERO);
        BigDecimal regle = requireNonNullElse(regleBrut, BigDecimal.ZERO);
        ComFolder dossier = d.getComFolder();
        StkItem article = dossier.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgCompany societe = SocieteDossier.de(dossier);
        CfgTier client = dossier.getCfgCustomer();
        LocalDateTime echeance = d.getDocDate();
        return new ImpayeDto(
                d.getId(),
                dossier.getId(),
                dossier.getCode(),
                Champs.de(dossier.getStrStatus(), StrStatus::getCode),
                echeance,
                echeance != null ? echeance.plusDays(age) : null,
                libelle(Champs.de(d.getComSettlementCategory(), ComSettlementCategory::getDescription),
                        Champs.de(d.getComSettlementCategory(), ComSettlementCategory::getCode)),
                d.getDescription(),
                Champs.de(client, CfgTier::getId),
                Champs.de(client, TierNomFormatter::nom),
                libelle(Champs.de(projet, CfgProject::getDescription), Champs.de(projet, CfgProject::getCode)),
                libelle(Champs.de(tranche, CfgTranche::getDescription), Champs.de(tranche, CfgTranche::getCode)),
                Champs.de(article, StkItem::getCode),
                libelle(Champs.de(societe, CfgCompany::getDescription), Champs.de(societe, CfgCompany::getCode)),
                Champs.de(dossier.getCfgCommercial(), TierNomFormatter::nom),
                montant,
                regle,
                Impaye.reste(montant, regle),
                joursRetard(echeance, aujourdhui),
                Champs.de(d.getComPenaltyDocument(), ComDocument::getId),
                d.getNotes());
    }

    private static long joursRetard(LocalDateTime echeance, LocalDate aujourdhui) {
        if (echeance == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(echeance.toLocalDate(), aujourdhui));
    }

    private static String libelle(String description, String code) {
        return StringUtils.hasText(description) ? description : code;
    }
}
