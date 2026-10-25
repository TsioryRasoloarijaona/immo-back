package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.dossiers.NomsTiers;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementFlux;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.CONCRETISATIONS_NETTES;
import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.ENCAISSEMENTS;
import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.ENCAISSEMENTS_FRAIS;

/**
 * Détail d'un mois de l'écran (FEATURE-tdb-vente-previsions.md §4.3) : les lignes sources exactes des cellules, lues par
 * {@link FluxVenteMensuel#mouvements} (même population, même règle), puis leurs libellés par ids.
 */
@Component
class DetailPrevisionsVente {

    static final ParametreRapport MOIS_PARAMETRE = new ParametreRapport("mois", "Mois", TypeParametre.ENTIER, true,
            false, null, List.of(), 1, 12);
    private static final Set<MesureFlux> ENCAISSE = EnumSet.of(ENCAISSEMENTS, ENCAISSEMENTS_FRAIS);
    private static final String IDS = "ids";
    private static final String PROJET = "projet";
    private static final String ARTICLE = "article";
    private static final String MONTANT = "montant";
    private static final String REGLE = "regle";
    private static final String CLIENT = "client";
    private static final String CATEGORIE = "categorie";

    private final FluxVenteMensuel flux;
    private final MontantsParNatureDossier montants;
    private final NomsTiers noms;
    private final EntityManager em;

    DetailPrevisionsVente(FluxVenteMensuel flux, MontantsParNatureDossier montants, NomsTiers noms, EntityManager em) {
        this.flux = flux;
        this.montants = montants;
        this.noms = noms;
        this.em = em;
    }

    ResultatRapport executer(ContexteRapport contexte) {
        int annee = RapportFluxVente.annee(contexte);
        int mois = Objects.requireNonNull(contexte.entier(MOIS_PARAMETRE.nom()), "Paramètre mois absent");
        FraisEcran frais = FraisEcran.de(contexte);
        Set<MesureFlux> mesures = EnumSet.of(CONCRETISATIONS_NETTES, ENCAISSEMENTS, ENCAISSEMENTS_FRAIS);
        mesures.addAll(frais.schema());
        // Toutes années : la colonne « regle » d'une échéance compte ses règlements quelle que soit leur date.
        List<MouvementFlux> tous = flux.mouvements(new DemandeFlux(contexte.tranchesVisibles(), null, null, mesures))
                .lignes();
        List<MouvementFlux> duMois = tous.stream()
                .filter(l -> l.date().getYear() == annee && l.date().getMonthValue() == mois)
                .sorted(Comparator.comparing(MouvementFlux::date).reversed()).toList();
        Map<UUID, RapportFluxVente.TrancheProjet> tranches = RapportFluxVente.tranchesProjets(em,
                contexte.tranchesVisibles()).stream().collect(Collectors.toMap(RapportFluxVente.TrancheProjet::tranche,
                Function.identity()));
        return new ResultatRapport(List.of(
                dossiers(de(duMois, Set.of(CONCRETISATIONS_NETTES)), tranches),
                mouvements(de(duMois, frais.reel()), tranches),
                echeances(de(duMois, frais.schema()), tous)), List.of());
    }

    private static List<MouvementFlux> de(List<MouvementFlux> lignes, Collection<MesureFlux> mesures) {
        return lignes.stream().filter(l -> mesures.contains(l.mesure())).toList();
    }

    // ------------------------------------------------------------------ dossiers

    private SectionRapport dossiers(List<MouvementFlux> lignes, Map<UUID, RapportFluxVente.TrancheProjet> tranches) {
        Map<UUID, ComFolder> dossiers = LotsTranches.lire(ids(lignes, MouvementFlux::dossierId), lot -> em.createQuery(
                        "SELECT f FROM ComFolder f JOIN FETCH f.stkItem WHERE f.id IN :ids", ComFolder.class)
                .setParameter(IDS, lot).getResultList()).stream()
                .collect(Collectors.toMap(ComFolder::getId, Function.identity()));
        Map<UUID, Map<ColonneDossier, BigDecimal>> regles = montants.pour(dossiers.values());
        Map<UUID, String> clients = noms.noms(dossiers.values().stream().map(ComFolder::getCfgCustomer)
                .filter(Objects::nonNull).map(CfgTier::getId).collect(Collectors.toSet()));
        List<Map<String, Object>> rangees = lignes.stream().map(l -> {
            ComFolder f = Objects.requireNonNull(dossiers.get(l.dossierId()));
            Map<String, Object> r = tranche(l, tranches);
            r.put(ARTICLE, f.getStkItem().getCode());
            r.put("dossier", f.getCode());
            r.put("dateConcretisation", l.date());
            r.put("prixVente", f.getPrice());
            r.put(REGLE, regle(regles.getOrDefault(f.getId(), Map.of())));
            r.put(CLIENT, f.getCfgCustomer() == null ? null : clients.get(f.getCfgCustomer().getId()));
            return r;
        }).toList();
        return section("dossiers", "Liste des dossiers de vente", rangees,
                ColonneRapport.texte("dossier", "Dossier"), date("dateConcretisation", "Date de concrétisation"),
                ColonneRapport.montant("prixVente", "Prix de vente", null), ColonneRapport.montant(REGLE, "Réglé", null),
                ColonneRapport.texte(CLIENT, "Client"));
    }

    /** Réglé d'un dossier : avance, autofinancement et crédit réglés de {@link MontantsParNatureDossier}, pas refait. */
    private static BigDecimal regle(Map<ColonneDossier, BigDecimal> m) {
        return Stream.of(ColonneDossier.AVANCE_REGLEE, ColonneDossier.AUTOFINANCEMENT_REGLE, ColonneDossier.CREDIT_REGLE)
                .map(c -> m.getOrDefault(c, BigDecimal.ZERO)).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ mouvements

    private SectionRapport mouvements(List<MouvementFlux> lignes, Map<UUID, RapportFluxVente.TrancheProjet> tranches) {
        Map<UUID, Object[]> libelles = parId(LotsTranches.lire(ids(lignes, MouvementFlux::ligneId), lot -> em.createQuery(
                        "SELECT l.id, i.code, f.docDate, k.description, s.description, COALESCE(t.id, c.id) "
                                + "FROM StlSettlementLine l JOIN l.stlSettlement s LEFT JOIN s.tier t "
                                + "JOIN l.comDocument d LEFT JOIN d.comSettlementCategory k JOIN d.comFolder f "
                                + "JOIN f.stkItem i LEFT JOIN f.cfgCustomer c WHERE l.id IN :ids", Object[].class)
                .setParameter(IDS, lot).getResultList()));
        Map<UUID, String> clients = noms.noms(tiers(libelles.values(), 5));
        List<Map<String, Object>> rangees = lignes.stream().map(l -> {
            Object[] x = Objects.requireNonNull(libelles.get(l.ligneId()));
            Map<String, Object> r = tranche(l, tranches);
            r.put(ARTICLE, x[1]);
            r.put("dateDossier", x[2] == null ? null : ((LocalDateTime) x[2]).toLocalDate());
            r.put("dateMouvement", l.date());
            r.put(CATEGORIE, x[3]);
            r.put(MONTANT, l.valeur());
            r.put("libelle", x[4]);
            r.put(CLIENT, clients.get((UUID) x[5]));
            return r;
        }).toList();
        return section("mouvements", "Liste des mouvements", rangees,
                date("dateDossier", "Date du dossier"), date("dateMouvement", "Date du mouvement"),
                ColonneRapport.texte(CATEGORIE, "Catégorie"), ColonneRapport.montant(MONTANT, "Montant", null),
                ColonneRapport.texte("libelle", "Libellé"), ColonneRapport.texte(CLIENT, "Client"));
    }

    /** Section dont les trois premières colonnes sont projet, tranche, article. */
    private static SectionRapport section(String id, String titre, List<Map<String, Object>> rangees,
                                          ColonneRapport... suivantes) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(PROJET, "Projet"),
                ColonneRapport.texte(RapportFluxVente.TRANCHE, "Tranche"), ColonneRapport.texte(ARTICLE, "Article")));
        colonnes.addAll(List.of(suivantes));
        return new SectionRapport(id, titre, colonnes, rangees, null, null, List.of());
    }

    // ------------------------------------------------------------------ échéances

    /** {@code regle} = lignes de l'échéance retenues en encaissement (règle partagée), toutes dates. */
    private SectionRapport echeances(List<MouvementFlux> lignes, List<MouvementFlux> tous) {
        List<UUID> ids = ids(lignes, MouvementFlux::ligneId);
        Map<UUID, Object[]> libelles = parId(LotsTranches.lire(ids, lot -> em.createQuery(
                        "SELECT d.id, k.description, c.id FROM ComDocument d JOIN d.comFolder f "
                                + "LEFT JOIN f.cfgCustomer c LEFT JOIN d.comSettlementCategory k WHERE d.id IN :ids",
                        Object[].class).setParameter(IDS, lot).getResultList()));
        Map<UUID, BigDecimal> encaisse = tous.stream().filter(l -> ENCAISSE.contains(l.mesure()))
                .collect(Collectors.toMap(MouvementFlux::ligneId, MouvementFlux::valeur, BigDecimal::add));
        Map<UUID, BigDecimal> regles = new HashMap<>();
        LotsTranches.lire(ids, lot -> em.createQuery("SELECT l.id, l.comDocument.id FROM StlSettlementLine l "
                        + "WHERE l.comDocument.id IN :ids", Object[].class).setParameter(IDS, lot).getResultList())
                .forEach(x -> regles.merge((UUID) x[1], encaisse.getOrDefault((UUID) x[0], BigDecimal.ZERO),
                        BigDecimal::add));
        Map<UUID, String> clients = noms.noms(tiers(libelles.values(), 2));
        List<Map<String, Object>> rangees = lignes.stream().map(l -> {
            Object[] x = Objects.requireNonNull(libelles.get(l.ligneId()));
            BigDecimal regle = regles.getOrDefault(l.ligneId(), BigDecimal.ZERO);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put(CLIENT, clients.get((UUID) x[2]));
            r.put("date", l.date());
            r.put(CATEGORIE, x[1]);
            r.put(MONTANT, l.valeur());
            r.put(REGLE, regle);
            r.put("reste", l.valeur().subtract(regle));
            return r;
        }).toList();
        return new SectionRapport("echeances", "Schémas de financements", List.of(
                ColonneRapport.texte(CLIENT, "Client"), date("date", "Date"), ColonneRapport.texte(CATEGORIE, "Catégorie"),
                ColonneRapport.montant(MONTANT, "Montant", null), ColonneRapport.montant(REGLE, "Réglé", null),
                ColonneRapport.montant("reste", "Reste", null)), rangees, null, null, List.of());
    }

    // ------------------------------------------------------------------ outils

    private static ColonneRapport date(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.DATE, null, FonctionTotal.AUCUNE, true, 0);
    }

    private static List<UUID> ids(List<MouvementFlux> lignes, Function<MouvementFlux, UUID> id) {
        return lignes.stream().map(id).filter(Objects::nonNull).distinct().toList();
    }

    private static Map<UUID, Object[]> parId(List<Object[]> lignes) {
        return lignes.stream().collect(Collectors.toMap(x -> (UUID) x[0], Function.identity(), (a, b) -> a));
    }

    private static Set<UUID> tiers(Collection<Object[]> lignes, int colonne) {
        return lignes.stream().map(x -> (UUID) x[colonne]).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Map<String, Object> tranche(MouvementFlux l, Map<UUID, RapportFluxVente.TrancheProjet> tranches) {
        Optional<RapportFluxVente.TrancheProjet> t = Optional.ofNullable(tranches.get(l.trancheId()));
        Map<String, Object> r = new LinkedHashMap<>();
        r.put(PROJET, t.map(RapportFluxVente.TrancheProjet::libelleProjet).orElse(null));
        r.put(RapportFluxVente.TRANCHE, t.map(RapportFluxVente.TrancheProjet::libelle).orElse(null));
        return r;
    }
}
