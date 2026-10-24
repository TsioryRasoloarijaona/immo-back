package com.tsix.immocrm.services.rapport.vente.commercial;

import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.rapport.LotsTranches;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Effectifs groupés des n° 19 et 31 par le moteur des écrans ({@code /api/prospections/groupes},
 * {@code /api/suivi-actions/groupes}) : mêmes chemins, mêmes filtres de périmètre, bornés aux tranches visibles par lots.
 * Une ligne sans tranche n'est comptée que si aucune tranche n'est demandée (FEATURE-tdb-vente-rapports-commercial.md §2).
 */
@Component
public class GroupesCommerciaux {

    private static final String ID = "id";
    private static final String DOC_DATE = "docDate";
    private static final PageRequest TOUS_LES_GROUPES = PageRequest.of(0, 10_000);

    private final GroupementService groupementService;
    private final PerimetreCommercialService perimetreService;

    public GroupesCommerciaux(GroupementService groupementService, PerimetreCommercialService perimetreService) {
        this.groupementService = groupementService;
        this.perimetreService = perimetreService;
    }

    /**
     * Prospections visibles datées dans [{@code debut} ; {@code fin}[, restreintes par {@code filtre} ; hors
     * pré-prospections comme l'écran (le modèle OMRAN les compte : écart voulu, égalité avec l'écran).
     */
    public Map<Object, Long> prospections(ContexteRapport contexte, LocalDateTime debut, LocalDateTime fin,
                                          Specification<ComProspection> filtre, Chemin cle, Granularite granularite) {
        Specification<ComProspection> base = perimetreService.filtreProspections().and(entre(debut, fin))
                .and(DiscriminantPreProspection.nEstPasPreProspection()).and(filtre);
        return grouper(contexte, ComProspection.class, base, r -> r.join("stkHierarchy", JoinType.LEFT).get(ID),
                cle, granularite);
    }

    /** Actions de catégorie Prospection rattachées à une prospection, toutes celles du périmètre (Q3). */
    public Map<Object, Long> actions(ContexteRapport contexte, LocalDateTime debut, LocalDateTime fin, Chemin cle,
                                     Granularite granularite) {
        UUID prospection = UUID.fromString(InteractionKpi.CATEGORIE_PROSPECTION);
        Specification<ComActionMessage> base = perimetreService.filtreInteractions()
                .and(entre(debut, fin))
                .and((r, q, cb) -> cb.and(cb.isNotNull(r.get("comProspection")),
                        cb.equal(r.get("comActionMessageCategory").get(ID), prospection)));
        return grouper(contexte, ComActionMessage.class, base, r -> r.join("trancheResolue", JoinType.LEFT).get(ID),
                cle, granularite);
    }

    /** Clé de groupe en libellé ; la clé absente devient « Non définie ». */
    public static Map<String, Long> parLibelle(Map<Object, Long> groupes) {
        Map<String, Long> parLibelle = new LinkedHashMap<>();
        groupes.forEach((cle, n) -> parLibelle.merge(cle == null ? SectionsCommerciales.NON_DEFINIE
                : String.valueOf(cle), n, Long::sum));
        return parLibelle;
    }

    /** Groupes mensuels (clé {@code aaaamm}) en douze lignes « Janvier »…, zéro sans donnée, en courbe. */
    public static SectionRapport mensuel(String id, String titre, Map<Object, Long> groupes) {
        long[] comptes = new long[12];
        groupes.forEach((cle, n) -> {
            if (cle instanceof Number aaaamm) {
                comptes[aaaamm.intValue() % 100 - 1] += n;
            }
        });
        Map<String, Long> parMois = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            parMois.put(PeriodesRapport.mois(m), comptes[m - 1]);
        }
        return SectionsCommerciales.serie(id, titre, "mois", "Mois", parMois, TypeGraphique.LIGNES);
    }

    private static <E> Specification<E> entre(LocalDateTime debut, LocalDateTime fin) {
        return (r, q, cb) -> cb.and(cb.greaterThanOrEqualTo(r.get(DOC_DATE), debut), cb.lessThan(r.get(DOC_DATE), fin));
    }

    private <E> Map<Object, Long> grouper(ContexteRapport contexte, Class<E> racine, Specification<E> base,
                                          Function<Root<E>, Path<UUID>> tranche, Chemin cle,
                                          Granularite granularite) {
        List<UUID> visibles = contexte.tranchesVisibles();
        boolean sansTranche = contexte.choix(ParametreRapport.TRANCHE_ID).isEmpty();
        Map<Object, Long> groupes = new LinkedHashMap<>();
        LotsTranches.lire(visibles, lot -> {
            // Les lignes sans tranche une seule fois : avec le premier lot.
            boolean avecSansTranche = sansTranche && lot.get(0).equals(visibles.get(0));
            Specification<E> dansLot = (r, q, cb) -> {
                Path<UUID> t = tranche.apply(r);
                Predicate dans = t.in(lot);
                return avecSansTranche ? cb.or(dans, cb.isNull(t)) : dans;
            };
            return groupementService.groupes(racine, base.and(dansLot), cle, granularite, TriGroupe.NOMBRE, true,
                    TOUS_LES_GROUPES);
        }).forEach((GroupeDto g) -> groupes.merge(g.cle(), g.nombre(), Long::sum));
        return groupes;
    }
}
