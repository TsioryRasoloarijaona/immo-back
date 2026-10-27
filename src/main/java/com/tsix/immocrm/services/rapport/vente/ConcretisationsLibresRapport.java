package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsFlux;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * N° 22 CONCRÉTISATIONS PAR RAPPORT AUX LIBRES, modèle {@code ComTdbConcretisationAndFree}
 * (FEATURE-tdb-vente-rapports-rythme.md §4.5) : situation toutes dates ; sous-article = {@code IsUnderItem} (décision 25/10).
 */
@Component
public class ConcretisationsLibresRapport extends RapportFluxVente {

    static final String CODE = "ComTdbConcretisationAndFree";
    static final String CATEGORIE = "categorie";
    static final String SOUS_ARTICLE = "Une concrétisation au moins porte sur un sous-article : compté au numérateur, "
            + "pas au dénominateur (libres sous-estimés).";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String NB_CONCRETISATIONS = "nbConcretisations";
    private static final String NB_LIBRES = "nbLibres";
    private static final String TAUX_CONCRETISATION = "tauxConcretisation";
    private static final String TAUX_LIBRES = "tauxLibres";

    static final String CATEGORIES ="SELECT DISTINCT c.id, c.description FROM StkItem i JOIN i.stkItemCategory c "
            + "JOIN i.stkHierarchy t LEFT JOIN t.cfgProject p LEFT JOIN p.cfgCompany soc "
            + "WHERE i.stkVocation.code = 'VENTE' AND c.forUnderItem = false AND t.isForSale = true AND "
            + VisibiliteEntites.CLAUSE_JPQL + " ORDER BY c.description";

    private final ValorisationTranche valorisation;
    private final EntityManager em;

    public ConcretisationsLibresRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, EntityManager em,
                                        Clock clock) {
        super(CODE, flux, clock);
        this.valorisation = valorisation;
        this.em = em;
    }

    @Override
    public List<ParametreRapport> parametres() {
        List<ParametreRapport.Option> options = em.createQuery(CATEGORIES, Object[].class).getResultList().stream()
                .map(r -> new ParametreRapport.Option(r[0].toString(), (String) r[1])).toList();
        return List.of(ParametreRapport.tranches(), new ParametreRapport(CATEGORIE, "Catégories", TypeParametre.CHOIX,
                false, true, null, options, null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<UUID> visibles = contexte.tranchesVisibles();
        Set<UUID> categories = contexte.choix(CATEGORIE).stream().map(UUID::fromString).collect(Collectors.toSet());
        Map<UUID, Long> articles = valorisation.articlesParCategorie(visibles, categories);
        MouvementsFlux mouvements = flux.mouvements(new DemandeFlux(visibles, null, null,
                Set.of(MesureFlux.CONCRETISATIONS_NETTES)));
        List<MouvementFlux> retenus = mouvements.lignes().stream().filter(l -> articles.containsKey(l.trancheId())
                && (categories.isEmpty() || categories.contains(l.categorieArticleId()))).toList();
        Map<UUID, BigDecimal> concretisations = retenus.stream().collect(Collectors.groupingBy(MouvementFlux::trancheId,
                Collectors.reducing(BigDecimal.ZERO, MouvementFlux::valeur, BigDecimal::add)));
        List<Map<String, Object>> lignes = new ArrayList<>();
        BigDecimal nbTotal = BigDecimal.ZERO;
        BigDecimal concretTotal = BigDecimal.ZERO;
        for (TrancheEnVente t : tranchesEnVente(em, visibles).stream().filter(t -> articles.containsKey(t.id()))
                .sorted(Comparator.comparing(t -> Objects.toString(t.code(), ""))).toList()) {
            BigDecimal nb = BigDecimal.valueOf(articles.get(t.id()));
            BigDecimal concret = concretisations.getOrDefault(t.id(), BigDecimal.ZERO);
            lignes.add(chiffres(t.code(), nb, concret));
            nbTotal = nbTotal.add(nb);
            concretTotal = concretTotal.add(concret);
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(TRANCHE, "Tranche"),
                ColonneRapport.effectif(NB_ARTICLES, "Articles", null),
                ColonneRapport.effectif(NB_CONCRETISATIONS, "Concrétisations", null),
                ColonneRapport.effectif(NB_LIBRES, "Libres", null),
                ColonneRapport.pourcentage(TAUX_CONCRETISATION, "Taux de concrétisation", null),
                ColonneRapport.pourcentage(TAUX_LIBRES, "Taux de libres", null));
        Map<String, Object> totaux = chiffres(null, nbTotal, concretTotal);
        totaux.keySet().retainAll(Set.of(TAUX_CONCRETISATION, TAUX_LIBRES));
        SectionRapport section = new SectionRapport("tranches", "Concrétisations par rapport aux libres", colonnes, lignes,
                null, totaux, List.of(new GraphiqueRapport(TypeGraphique.COLONNES, "Concrétisés et libres par tranche",
                TRANCHE, List.of(TAUX_CONCRETISATION, TAUX_LIBRES))));
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(mouvements.anomalies()));
        if (retenus.stream().anyMatch(MouvementFlux::sousArticle)) {
            avertissements.add(SOUS_ARTICLE);
        }
        return new ResultatRapport(List.of(section), avertissements);
    }

    private static Map<String, Object> chiffres(String tranche, BigDecimal nb, BigDecimal concret) {
        BigDecimal libres = nb.subtract(concret);
        return ligne(TRANCHE, tranche, NB_ARTICLES, nb, NB_CONCRETISATIONS, concret, NB_LIBRES, libres,
                TAUX_CONCRETISATION, LignesMensuelles.taux(concret, nb), TAUX_LIBRES, LignesMensuelles.taux(libres, nb));
    }
}
