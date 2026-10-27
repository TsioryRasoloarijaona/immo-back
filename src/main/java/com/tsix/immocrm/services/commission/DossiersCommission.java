package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.dto.commission.DemandesCommission.FiltresCommission;
import com.tsix.immocrm.dto.commission.DossierCommissionDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.services.commission.DatesCommission.Finalisations;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Onglet Dossiers : tous les dossiers de vente de la tranche, sans statut compris (Q8), et leurs commissions. */
@Component
public class DossiersCommission {

    /** Dossier, article, personnes, prospection et action chargés d'avance ; suivi d'un {@code WHERE}. */
    static final String DOSSIERS = """
            SELECT f FROM ComFolder f JOIN FETCH f.stkItem i LEFT JOIN FETCH i.stkItemCategory LEFT JOIN FETCH f.cfgCustomer
            LEFT JOIN FETCH f.cfgCommercial LEFT JOIN FETCH f.cfgSaleManager LEFT JOIN FETCH f.cfgSaleManager2Id
            LEFT JOIN FETCH f.strStatus LEFT JOIN FETCH f.comProspection p LEFT JOIN FETCH p.comAction
            """;

    private final CommissionsVisibles visibles;
    private final DatesCommission dates;
    private final ComCommissionLineRepository lignes;
    private final SoldesCommission soldes;
    private final EntityManager em;

    public DossiersCommission(CommissionsVisibles visibles, DatesCommission dates, ComCommissionLineRepository lignes,
                              SoldesCommission soldes, EntityManager em) {
        this.visibles = visibles;
        this.dates = dates;
        this.lignes = lignes;
        this.soldes = soldes;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<DossierCommissionDto> lister(UUID trancheId, FiltresCommission filtres) {
        visibles.tranche(trancheId);
        Finalisations fin = dates.finalisations(trancheId);
        relireEnBase(em);
        List<ComFolder> dossiers = em.createQuery(DOSSIERS + "WHERE i.stkHierarchy.id = :tranche ORDER BY f.code",
                ComFolder.class).setParameter("tranche", trancheId).getResultList().stream()
                .filter(retenu(filtres, fin)).toList();
        List<ComCommissionLine> duLot = LotsTranches.lire(dossiers.stream().map(ComFolder::getId).toList(),
                lignes::findByComFolderIdIn);
        Map<UUID, BigDecimal> regles = soldes.reglesParLigne(duLot);
        Map<UUID, List<ComCommissionLine>> parDossier = duLot.stream()
                .collect(Collectors.groupingBy(ComCommissionLine::getComFolderId));
        return dossiers.stream().map(f -> dto(f, fin, parDossier.getOrDefault(f.getId(), List.of()), regles)).toList();
    }

    /** Une date de dossier écrite hors JPA dans la même transaction ne se voit pas sur l'entité déjà chargée. */
    static void relireEnBase(EntityManager em) {
        em.flush();
        em.clear();
    }

    private Predicate<ComFolder> retenu(FiltresCommission f, Finalisations fin) {
        TypeCommission type = f.type() == null ? TypeCommission.CONCRETISATION : f.type();
        String colonne = type == TypeCommission.FINALISATION_2 ? fin.colonne2() : fin.colonne1();
        boolean surLaDate = f.type() != null || f.du() != null || f.au() != null;
        String code = f.dossier() == null ? null : f.dossier().trim().toLowerCase(Locale.ROOT);
        Predicate<ComFolder> periode = d -> !surLaDate || dansLaPeriode(CalculCommission.jour(dates.date(d, type, colonne)), f);
        return periode
                .and(d -> f.commercialId() == null || personnes(d).anyMatch(f.commercialId()::equals))
                .and(d -> code == null || Objects.toString(d.getCode(), "").toLowerCase(Locale.ROOT).contains(code));
    }

    /** Jour présent et entre {@code du} et {@code au} inclus (borne absente = ouverte). */
    static boolean dansLaPeriode(LocalDate jour, FiltresCommission f) {
        return jour != null && (f.du() == null || !jour.isBefore(f.du())) && (f.au() == null || !jour.isAfter(f.au()));
    }

    private static Stream<UUID> personnes(ComFolder f) {
        return Stream.of(f.getCfgCommercial(), f.getCfgSaleManager(), f.getCfgSaleManager2Id()).filter(Objects::nonNull)
                .map(CfgTier::getId);
    }

    private DossierCommissionDto dto(ComFolder f, Finalisations fin, List<ComCommissionLine> duDossier,
                                     Map<UUID, BigDecimal> regles) {
        StkItem article = f.getStkItem();
        SoldesCommission.Solde solde = SoldesCommission.solde(duDossier, regles);
        return new DossierCommissionDto(f.getId(), f.getCode(), Champs.de(f.getStrStatus(), StrStatus::getDescription),
                article.getCode(), article.getDescription(),
                Champs.de(article.getStkItemCategory(), StkItemCategory::getDescription),
                VueLignesCommission.nom(f.getCfgCustomer()),
                id(f.getCfgCommercial()), VueLignesCommission.nom(f.getCfgCommercial()), externe(f.getCfgCommercial()),
                id(f.getCfgSaleManager()), VueLignesCommission.nom(f.getCfgSaleManager()), externe(f.getCfgSaleManager()),
                id(f.getCfgSaleManager2Id()), VueLignesCommission.nom(f.getCfgSaleManager2Id()),
                externe(f.getCfgSaleManager2Id()),
                Champs.de(f.getComProspection(), ComProspection::getCode), CalculCommission.jour(f.getAdvanceDate()),
                CalculCommission.jour(f.getAgreementSavingDate()), CalculCommission.jour(f.getAgreementDate()),
                CalculCommission.jour(f.getCancelDate()), finalisation(f, TypeCommission.FINALISATION_1, fin.colonne1()),
                finalisation(f, TypeCommission.FINALISATION_2, fin.colonne2()), f.getPriceAfterDiscount(),
                somme(duDossier, TypeCommission.CONCRETISATION.libelle()::equals),
                somme(duDossier, TypeCommission.FINALISATION_1.libelle()::equals),
                somme(duDossier, TypeCommission.FINALISATION_2.libelle()::equals),
                somme(duDossier, TypeCommission::negatif), solde.regle(), solde.genere(), solde.reste());
    }

    private LocalDate finalisation(ComFolder f, TypeCommission type, String colonne) {
        return CalculCommission.jour(dates.date(f, type, colonne));
    }

    private static BigDecimal somme(List<ComCommissionLine> duDossier, Predicate<String> type) {
        return duDossier.stream().filter(l -> type.test(l.getCommissionType())).map(SoldesCommission::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static UUID id(CfgTier t) {
        return Champs.de(t, CfgTier::getId);
    }

    private static boolean externe(CfgTier t) {
        return t != null && Boolean.TRUE.equals(t.getIsExtern());
    }
}
