package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.RaisonIgnoree;
import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.dto.commission.ResultatGenerationDto;
import com.tsix.immocrm.dto.commission.ResultatGenerationDto.DossierIgnoreDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.ComSalesCommission;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.commission.CalculCommission.Fait;
import com.tsix.immocrm.services.commission.CalculCommission.Montant;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Génération d'un type de commission pour un lot de dossiers d'une tranche (FEATURE-vente-commissions.md §2, §3.1 C1–C3,
 * C9) : contrôles et lectures d'abord, une transaction ; un dossier sans ligne est listé avec sa raison.
 */
@Component
public class GenerationCommissions {

    private static final String DOSSIERS = DossiersCommission.DOSSIERS + "WHERE f.id IN :ids";
    private static final String REGLES = """
            SELECT r FROM ComSalesCommission r LEFT JOIN FETCH r.cfgTier LEFT JOIN FETCH r.comAction
            LEFT JOIN FETCH r.stkItemCategory LEFT JOIN FETCH r.comEmployee WHERE r.cfgTranche.id = :tranche""";

    /** Lignes à écrire pour un dossier, ou la raison de n'en écrire aucune. */
    private record Issue(List<ComCommissionLine> lignes, RaisonIgnoree raison) {

        static Issue ignore(RaisonIgnoree raison) {
            return new Issue(List.of(), raison);
        }
    }

    private record Contexte(UUID trancheId, TypeCommission type, String colonne, List<ComSalesCommission> regles,
                            String auteur, LocalDateTime maintenant) {
    }

    private final CommissionsVisibles visibles;
    private final DatesCommission dates;
    private final ComCommissionLineRepository lignes;
    private final VueLignesCommission vue;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public GenerationCommissions(CommissionsVisibles visibles, DatesCommission dates, ComCommissionLineRepository lignes,
                                 VueLignesCommission vue, UtilisateurAffiche utilisateur, EntityManager em, Clock clock) {
        this.visibles = visibles;
        this.dates = dates;
        this.lignes = lignes;
        this.vue = vue;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    @Transactional
    public ResultatGenerationDto generer(UUID trancheId, TypeCommission type, List<UUID> dossierIds) {
        visibles.tranche(trancheId);
        List<ComFolder> dossiers = dossiersDeLaTranche(trancheId, dossierIds);
        String colonne = type == TypeCommission.FINALISATION_1 || type == TypeCommission.FINALISATION_2
                ? dates.finalisations(trancheId).exiger(type) : null;
        List<ComSalesCommission> regles = type == TypeCommission.DESISTEMENT ? List.of()
                : em.createQuery(REGLES, ComSalesCommission.class).setParameter("tranche", trancheId).getResultList();
        Contexte ctx = new Contexte(trancheId, type, colonne, regles, utilisateur.courant().nom(), LocalDateTime.now(clock));
        Map<UUID, List<ComCommissionLine>> existantes = lignes.findByComFolderIdIn(dossierIds).stream()
                .collect(Collectors.groupingBy(ComCommissionLine::getComFolderId));

        List<ComCommissionLine> creees = new ArrayList<>();
        List<DossierIgnoreDto> ignores = new ArrayList<>();
        for (ComFolder f : dossiers) {
            List<ComCommissionLine> duDossier = existantes.getOrDefault(f.getId(), List.of());
            Issue issue = type == TypeCommission.DESISTEMENT ? desister(f, duDossier, ctx) : calculer(f, duDossier, ctx);
            if (issue.raison() != null) {
                ignores.add(new DossierIgnoreDto(f.getId(), f.getCode(), issue.raison(), issue.raison().libelle()));
            }
            issue.lignes().forEach(em::persist);
            creees.addAll(issue.lignes());
        }
        return new ResultatGenerationDto(vue.dtos(creees), ignores);
    }

    /** Dossiers demandés, tous de la tranche du chemin (C8), dans l'ordre de la demande ; sinon 400. */
    private List<ComFolder> dossiersDeLaTranche(UUID trancheId, List<UUID> ids) {
        List<UUID> uniques = ids.stream().distinct().toList();
        DossiersCommission.relireEnBase(em);
        Map<UUID, ComFolder> trouves = uniques.isEmpty() ? Map.of()
                : em.createQuery(DOSSIERS, ComFolder.class).setParameter("ids", uniques).getResultStream()
                .collect(Collectors.toMap(ComFolder::getId, f -> f));
        List<ComFolder> dossiers = new ArrayList<>();
        for (UUID id : uniques) {
            ComFolder f = trouves.get(id);
            UUID tranche = f == null ? null : Champs.de(f.getStkItem().getStkHierarchy(), CfgTranche::getId);
            if (!trancheId.equals(tranche)) {
                throw new InvalidRequestException("Le dossier " + id + " n'appartient pas à la tranche.");
            }
            dossiers.add(f);
        }
        return dossiers;
    }

    private Issue calculer(ComFolder f, List<ComCommissionLine> existantes, Contexte ctx) {
        if (existantes.stream().anyMatch(l -> TypeCommission.negatif(l.getCommissionType()))) {
            return Issue.ignore(RaisonIgnoree.DOSSIER_DESISTE);
        }
        CfgTier personne = personne(f, ctx.type());
        if (personne == null) {
            return Issue.ignore(RaisonIgnoree.SANS_PERSONNE);
        }
        LocalDateTime date = dates.date(f, ctx.type(), ctx.colonne());
        if (date == null) {
            return Issue.ignore(RaisonIgnoree.SANS_DATE);
        }
        if (existantes.stream().anyMatch(l -> ctx.type().libelle().equals(l.getCommissionType())
                && personne.getId().equals(l.getCfgEmployeeId()))) {
            return Issue.ignore(RaisonIgnoree.DEJA_GENERE);
        }
        StkItem article = f.getStkItem();
        Fait fait = new Fait(personne.getId(), Champs.de(Champs.de(f.getComProspection(), ComProspection::getComAction),
                ComAction::getId), Champs.de(article.getStkItemCategory(), StkItemCategory::getId), date.toLocalDate());
        Optional<ComSalesCommission> regle = CalculCommission.regle(ctx.regles(), fait);
        if (regle.isEmpty()) {
            return Issue.ignore(RaisonIgnoree.SANS_REGLE);
        }
        Montant m = CalculCommission.montant(regle.get(), ctx.type(), Boolean.TRUE.equals(personne.getIsExtern()),
                f.getPriceAfterDiscount());
        if (m.valeur().signum() <= 0) {
            return Issue.ignore(RaisonIgnoree.MONTANT_NUL);
        }
        return new Issue(List.of(ligne(f, ctx, ctx.type().libelle(), personne.getId(), m.valeur(), date, m.mode())), null);
    }

    /** Une reprise négative par ligne d'origine, au plus autant de reprises par personne que de lignes d'origine (C3). */
    private Issue desister(ComFolder f, List<ComCommissionLine> existantes, Contexte ctx) {
        if (f.getCancelDate() == null) {
            return Issue.ignore(RaisonIgnoree.NON_DESISTE);
        }
        List<ComCommissionLine> origines = existantes.stream()
                .filter(l -> !TypeCommission.negatif(l.getCommissionType()) && SoldesCommission.montant(l).signum() > 0)
                .sorted(Comparator.comparing(ComCommissionLine::getDocDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(l -> l.getId().toString()))
                .toList();
        if (origines.isEmpty()) {
            return Issue.ignore(RaisonIgnoree.SANS_COMMISSION_A_REPRENDRE);
        }
        List<ComCommissionLine> dejaReprises = new ArrayList<>(existantes.stream()
                .filter(l -> TypeCommission.negatif(l.getCommissionType())).toList());
        List<ComCommissionLine> nonReprises = new ArrayList<>(origines);
        // Une reprise se rattache d'abord à l'origine de même personne et de montant opposé ; une reprise au montant
        // modifié couvre ensuite la plus ancienne origine restante de sa personne (jamais deux reprises pour une origine).
        apparier(nonReprises, dejaReprises, (o, r) -> SoldesCommission.montant(o).compareTo(SoldesCommission.montant(r)
                .negate()) == 0);
        apparier(nonReprises, dejaReprises, (o, r) -> true);
        List<ComCommissionLine> reprises = nonReprises.stream().map(o -> ligne(f, ctx,
                o.getCommissionType() != null && o.getCommissionType().endsWith("(support)")
                        ? TypeCommission.DESISTEMENT.libelle() + " (support)" : TypeCommission.DESISTEMENT.libelle(),
                o.getCfgEmployeeId(), o.getCommissionAmount().negate(), f.getCancelDate(), o.getCalculationMethod()))
                .toList();
        return reprises.isEmpty() ? Issue.ignore(RaisonIgnoree.DEJA_GENERE) : new Issue(reprises, null);
    }

    /** Retire de {@code origines} et de {@code reprises} chaque paire de même personne qui vérifie {@code critere}. */
    private static void apparier(List<ComCommissionLine> origines, List<ComCommissionLine> reprises,
                                 BiPredicate<ComCommissionLine, ComCommissionLine> critere) {
        Iterator<ComCommissionLine> it = origines.iterator();
        while (it.hasNext()) {
            ComCommissionLine o = it.next();
            reprises.stream().filter(r -> Objects.equals(r.getCfgEmployeeId(), o.getCfgEmployeeId()) && critere.test(o, r))
                    .findFirst().ifPresent(r -> {
                        reprises.remove(r);
                        it.remove();
                    });
        }
    }

    private static CfgTier personne(ComFolder f, TypeCommission type) {
        return switch (type) {
            case CONCRETISATION, DESISTEMENT -> f.getCfgCommercial();
            case FINALISATION_1 -> f.getCfgSaleManager();
            case FINALISATION_2 -> f.getCfgSaleManager2Id();
        };
    }

    private static ComCommissionLine ligne(ComFolder f, Contexte ctx, String type, UUID personne, BigDecimal montant,
                                           LocalDateTime date, String mode) {
        ComCommissionLine l = new ComCommissionLine();
        l.setId(UUID.randomUUID());
        l.setComFolderId(f.getId());
        l.setCfgTrancheId(ctx.trancheId());
        l.setStkItemId(f.getStkItem().getId());
        l.setStkItemCategoryId(Champs.de(f.getStkItem().getStkItemCategory(), StkItemCategory::getId));
        l.setCfgEmployeeId(personne);
        l.setCommissionType(type);
        l.setCommissionAmount(montant);
        l.setDocDate(date);
        l.setCalculationMethod(mode);
        l.setUserCreate(ctx.auteur());
        l.setUserCreateDateTime(ctx.maintenant());
        l.setUserUpdate(ctx.auteur());
        l.setUserUpdateDateTime(ctx.maintenant());
        return l;
    }
}
