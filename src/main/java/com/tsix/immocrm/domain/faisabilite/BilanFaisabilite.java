package com.tsix.immocrm.domain.faisabilite;

import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Categorie;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Parametres;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Relation;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Saisie;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Unite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanEtudeDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Arbre du bilan et colonnes stockées de l'étude ({@code omran-src/Yes.Rems/Yes.Stk.BalanceSheet/FSCategory.cs}
 * l.135-375, {@code Yes.Stk/StkFeasibilityStudy.cs} l.960-2187) — FEATURE-patrimoine-faisabilite-workflow.md §2.1-2.4.
 * Calcul pur, appelé par l'écriture, l'aperçu et la lecture ; arrondi à l'échelle 6 en sortie seulement.
 */
public final class BilanFaisabilite {

    public static final UUID RACINE_DEPENSE = UUID.fromString("27416223-1A88-4AF7-8532-AD53BCA65B7E");
    public static final UUID RACINE_RECETTE = UUID.fromString("273B2C2D-BDFA-4541-94F7-C708A0E79B2C");
    static final String MSG_HORS_BILAN = "Vous ne pouvez pas selectionnez une catégorie non définie dans le bilan!";
    static final String MSG_HIERARCHIE =
            "Vous ne pouvez pas selectionnez cette catégorie possédant une relation hiérarchique avec la catégorie courante!";
    static final String MSG_BOUCLE = "Vous ne pouvez pas selectionnez cette catégorie car elle crée une boucle infini!";
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final String CATEGORIE = "categorieId : ";

    /** Montants propres d'une ligne : HT et TTC hors enfants. */
    private record Propre(BigDecimal ratio, BigDecimal tva, BigDecimal ht, BigDecimal ttc) {
    }

    /** Part d'un nœud sur une tranche de l'étude. */
    public record Part(UUID trancheId, String code, BigDecimal ht, BigDecimal ttc) {
    }

    /** Nœud de l'arbre, non arrondi ; {@code saisie} null pour un ancêtre ajouté par le serveur. */
    public record Noeud(Categorie categorie, int profondeur, Saisie saisie, BigDecimal ratio, BigDecimal tva,
                        BigDecimal htPropre, BigDecimal propre, BigDecimal ht, BigDecimal ttc, BigDecimal poids,
                        boolean verrouillee, List<Part> ventilation) {

        public LocalDateTime dateApplication() {
            return saisie == null ? null : saisie.dateApplication();
        }

        public boolean pourProjet() {
            return saisie == null || !Boolean.FALSE.equals(saisie.pourProjet());
        }
    }

    /** {@code depenseHorsIndivis} = total TTC de « Dépense » moins les dépenses propres datées. */
    public record Resultat(List<Noeud> noeuds, BilanEtudeDto bilan, BigDecimal depenseHt, BigDecimal depense,
                           BigDecimal recetteHt, BigDecimal recette, BigDecimal depenseHorsIndivis) {
    }

    private final EtatFaisabilite etat;
    private final Map<UUID, Saisie> saisies = new HashMap<>();
    private final Set<UUID> arbre = new LinkedHashSet<>();
    private final Map<UUID, Propre> propres = new HashMap<>();
    private final Map<UUID, BigDecimal[]> totaux = new HashMap<>();
    private final Set<UUID> enCours = new HashSet<>();

    private BilanFaisabilite(EtatFaisabilite etat) {
        this.etat = etat;
    }

    /** 400 (catégorie, unité, référence, montants, boucle) avant tout calcul ; rien n'est écrit par ce calcul. */
    public static Resultat calculer(EtatFaisabilite etat) {
        return new BilanFaisabilite(etat).calculer();
    }

    private Resultat calculer() {
        etat.saisies().forEach(this::retenir);
        saisies.values().forEach(this::valider);
        Stream.concat(saisies.keySet().stream(), etat.relations().keySet().stream()
                        .filter(c -> etat.categories().containsKey(c)))
                .toList().forEach(this::ajouterAvecAncetres);
        saisies.values().stream().filter(s -> s.referenceId() != null).forEach(this::validerReference);
        arbre.forEach(this::total);
        List<Noeud> noeuds = new ArrayList<>();
        enfants(null).forEach(r -> aplatir(r, 0, noeuds));
        return resultat(noeuds);
    }

    // ------------------------------------------------------------------ contrôles (§2.3)

    /** Lignes sans rien de saisi : simples ancêtres renvoyés par le front, ignorés. */
    private void retenir(Saisie s) {
        if (s.categorieId() == null) {
            throw new InvalidRequestException("categorieId est obligatoire sur chaque ligne.");
        }
        if (!porteuse(s)) {
            return;
        }
        if (saisies.put(s.categorieId(), s) != null) {
            throw new InvalidRequestException(CATEGORIE + "catégorie en double (%s).".formatted(s.categorieId()));
        }
    }

    private static boolean porteuse(Saisie s) {
        return s.uniteId() != null || s.referenceId() != null || nonNul(s.ratio()) || nonNul(s.montantHt())
                || nonNul(s.montant()) || s.dateApplication() != null || Boolean.FALSE.equals(s.pourProjet());
    }

    private void valider(Saisie s) {
        Categorie c = etat.categories().get(s.categorieId());
        if (c == null || c.sansPrevision()) {
            throw new InvalidRequestException(CATEGORIE + "catégorie inconnue ou sans prévision (%s).".formatted(s.categorieId()));
        }
        if (etat.relations().containsKey(s.categorieId())) {
            throw new InvalidRequestException(CATEGORIE + "ligne verrouillée : son montant est imposé par l'étude.");
        }
        if (s.uniteId() != null && s.referenceId() != null) {
            throw new InvalidRequestException("uniteEtudeId : une ligne est par unité OU en pourcentage, pas les deux.");
        }
        if (s.uniteId() != null && !etat.unites().containsKey(s.uniteId())) {
            throw new InvalidRequestException("uniteEtudeId : ligne d'unité inconnue dans cette étude (%s).".formatted(s.uniteId()));
        }
        if (s.tva() != null && (s.tva().signum() < 0 || s.tva().compareTo(BigDecimal.ONE) > 0)) {
            throw new InvalidRequestException("tauxTva : taux entre 0 et 100 %.");
        }
        negatif(s.ratio(), "ratio");
        negatif(s.montantHt(), "montantHt");
        negatif(s.montant(), "montant");
    }

    private static void negatif(BigDecimal v, String champ) {
        if (v != null && v.signum() < 0) {
            throw new InvalidRequestException(champ + " : valeur positive ou nulle.");
        }
    }

    private void ajouterAvecAncetres(UUID id) {
        for (UUID c = id; c != null && etat.categories().containsKey(c) && arbre.add(c); ) {
            c = etat.categories().get(c).parentId();
        }
    }

    /** Référence hors de l'arbre, ou ancêtre / descendant de la ligne : messages OMRAN ({@code VerifIntersection}). */
    private void validerReference(Saisie s) {
        UUID ref = s.referenceId();
        if (!arbre.contains(ref)) {
            throw new InvalidRequestException(MSG_HORS_BILAN);
        }
        if (ancetre(ref, s.categorieId()) || ancetre(s.categorieId(), ref)) {
            throw new InvalidRequestException(MSG_HIERARCHIE);
        }
    }

    /** {@code a} est {@code b} ou un de ses ancêtres. */
    private boolean ancetre(UUID a, UUID b) {
        for (UUID c = b; c != null; c = parent(c)) {
            if (c.equals(a)) {
                return true;
            }
        }
        return false;
    }

    private UUID parent(UUID c) {
        Categorie categorie = etat.categories().get(c);
        return categorie == null ? null : categorie.parentId();
    }

    // ------------------------------------------------------------------ montants (§2.2)

    /** [HT, TTC] du nœud, enfants compris ; un nœud repassé en cours de calcul = boucle de références. */
    private BigDecimal[] total(UUID id) {
        BigDecimal[] connu = totaux.get(id);
        if (connu != null) {
            return connu;
        }
        if (!enCours.add(id)) {
            throw new InvalidRequestException(MSG_BOUCLE);
        }
        Propre p = propre(id);
        BigDecimal ht = p.ht();
        BigDecimal ttc = p.ttc();
        for (UUID enfant : enfants(id)) {
            BigDecimal[] t = total(enfant);
            ht = ht.add(t[0], MC);
            ttc = ttc.add(t[1], MC);
        }
        enCours.remove(id);
        BigDecimal[] resultat = {ht, ttc};
        totaux.put(id, resultat);
        return resultat;
    }

    private Propre propre(UUID id) {
        Parametres p = etat.parametres();
        Relation relation = etat.relations().get(id);
        Propre propre = relation != null ? impose(relation, p) : saisi(saisies.get(id), p);
        propres.put(id, propre);
        return propre;
    }

    /** {@code FSCategory.CategoryAmountHT/CategoryAmount} des 4 relations : montants de l'étude, TVA imposée. */
    private Propre impose(Relation relation, Parametres p) {
        return switch (relation) {
            case PRIX_TERRAIN -> new Propre(BigDecimal.ZERO, z(p.tvaTerrain()), z(p.prixTerrainHt()), z(p.prixTerrain()));
            case ENREGISTREMENT -> new Propre(BigDecimal.ZERO, z(p.tvaTerrain()), z(p.enregistrementHt()), z(p.enregistrement()));
            case AUTRES_FRAIS -> new Propre(BigDecimal.ZERO, z(p.tvaTerrain()), z(p.autresFraisHt()), z(p.autresFrais()));
            case INTERETS -> {
                BigDecimal interets = etat.credits().stream().flatMap(c -> c.echeances().stream())
                        .map(FinancementFaisabilite.Echeance::interets).reduce(BigDecimal.ZERO, BigDecimal::add);
                yield new Propre(BigDecimal.ZERO, z(p.tvaInterets()), interets,
                        interets.multiply(BigDecimal.ONE.add(z(p.tvaInterets())), MC));
            }
        };
    }

    /** Par unité (base = quantité) ou en pourcentage (base = total HT de la référence) ; sans base : 0. */
    private Propre saisi(Saisie s, Parametres p) {
        if (s == null) {
            return new Propre(BigDecimal.ZERO, z(p.tvaDefaut()), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal tva = s.tva() != null ? s.tva() : z(p.tvaDefaut());
        Unite unite = s.uniteId() == null ? null : etat.unites().get(s.uniteId());
        BigDecimal base = BigDecimal.ZERO;
        if (unite != null) {
            base = z(unite.quantite());
        } else if (s.referenceId() != null) {
            base = total(s.referenceId())[0];
        }
        BigDecimal htSaisi = s.montantHt();
        if (htSaisi == null && s.montant() != null) {
            htSaisi = s.montant().divide(BigDecimal.ONE.add(tva), MC);
        }
        CalculFaisabilite.Paire paire = base.signum() == 0
                ? new CalculFaisabilite.Paire(z(s.ratio()), BigDecimal.ZERO)
                : CalculFaisabilite.paire(s.ratio(), htSaisi, base);
        BigDecimal ht = paire.montant();
        BigDecimal ttc = ht.multiply(BigDecimal.ONE.add(tva), MC);
        if (p.proportionTerrain() && unite != null && unite.surfaceArticle()) {
            BigDecimal terrainM2 = z(p.plancherVendable()).signum() == 0 ? BigDecimal.ZERO
                    : z(p.coutTotalTerrain()).divide(p.plancherVendable(), MC);
            ttc = ht.add(ht.subtract(terrainM2.multiply(z(unite.quantite()), MC), MC).multiply(tva, MC), MC);
        }
        return new Propre(paire.coef(), tva, ht, ttc);
    }

    // ------------------------------------------------------------------ arbre à plat, bilan

    private List<UUID> enfants(UUID parent) {
        return arbre.stream().filter(c -> Objects.equals(etat.categories().get(c).parentId(), parent)
                        || (parent == null && !arbre.contains(etat.categories().get(c).parentId())))
                .sorted(Comparator.comparingInt((UUID c) -> etat.categories().get(c).ordre())
                        .thenComparing(c -> Objects.toString(etat.categories().get(c).libelle(), "")))
                .toList();
    }

    private void aplatir(UUID id, int profondeur, List<Noeud> noeuds) {
        Categorie c = etat.categories().get(id);
        Propre p = propres.get(id);
        BigDecimal[] t = totaux.get(id);
        BigDecimal racine = totaux.get(racineDe(id))[1];
        BigDecimal poids = racine.signum() == 0 ? BigDecimal.ZERO : t[1].divide(racine, MC);
        List<Part> parts = etat.tranches().stream().map(tr -> new Part(tr.id(), tr.code(),
                t[0].multiply(z(tr.poids()), MC), t[1].multiply(z(tr.poids()), MC))).toList();
        noeuds.add(new Noeud(c, profondeur, saisies.get(id), p.ratio(), p.tva(), p.ht(), p.ttc(), t[0], t[1], poids,
                etat.relations().containsKey(id), parts));
        enfants(id).forEach(e -> aplatir(e, profondeur + 1, noeuds));
    }

    private UUID racineDe(UUID id) {
        UUID c = id;
        while (arbre.contains(parent(c))) {
            c = parent(c);
        }
        return c;
    }

    private Resultat resultat(List<Noeud> noeuds) {
        BigDecimal[] depense = totaux.getOrDefault(RACINE_DEPENSE, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal[] recette = totaux.getOrDefault(RACINE_RECETTE, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal indivis = noeuds.stream().filter(n -> !n.categorie().recette() && n.dateApplication() != null)
                .map(Noeud::propre).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resultat(noeuds, bilan(depense, recette), depense[0], depense[1], recette[0], recette[1],
                depense[1].subtract(indivis, MC));
    }

    /** Colonnes stockées de l'étude (§2.2) ; retours NULL quand le dénominateur est nul. */
    private BilanEtudeDto bilan(BigDecimal[] depense, BigDecimal[] recette) {
        Parametres p = etat.parametres();
        BigDecimal pct = z(p.pourcentageConcretisation());
        BigDecimal coutHt = depense[0];
        BigDecimal cout = depense[1];
        BigDecimal concretisation = pct.multiply(recette[1], MC);
        BigDecimal credit = etat.credits().stream().map(c -> z(c.montant())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interets = etat.credits().stream().flatMap(c -> c.echeances().stream())
                .map(FinancementFaisabilite.Echeance::interets).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tauxClient = diviser(concretisation, cout);
        BigDecimal tauxCredit = diviser(credit, cout);
        BigDecimal fondsPropresHt = coutHt.subtract(pct.multiply(recette[0], MC), MC).subtract(credit, MC);
        BigDecimal brute = recette[0].subtract(coutHt, MC);
        BigDecimal is = brute.signum() > 0 ? brute.multiply(z(p.tauxIs()), MC) : BigDecimal.ZERO;
        BigDecimal nette = brute.subtract(is, MC);
        BigDecimal coutRevient = z(p.plancherVendable()).signum() == 0 ? p.coutRevientStocke()
                : cout.divide(p.plancherVendable(), MC);
        return new BilanEtudeDto(r(recette[1]), r(cout), r(coutHt), r(cout), r(coutHt), r(coutRevient), r(p.prixVenteM2()), r(pct),
                r(concretisation), r(recette[1].subtract(concretisation, MC)), r(interets),
                r(BigDecimal.ONE.subtract(tauxClient.add(tauxCredit, MC), MC)),
                r(cout.subtract(concretisation, MC).subtract(credit, MC)), r(tauxClient), r(tauxCredit), r(brute),
                r(p.tauxIs()), r(is), r(nette), retour(nette, coutHt), retour(nette, recette[0]),
                retour(nette, fondsPropresHt));
    }

    private static BigDecimal retour(BigDecimal nette, BigDecimal denominateur) {
        return denominateur.signum() == 0 ? null : r(nette.divide(denominateur, MC));
    }

    private static BigDecimal diviser(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? BigDecimal.ZERO : a.divide(b, MC);
    }

    private static boolean nonNul(BigDecimal v) {
        return v != null && v.signum() != 0;
    }

    public static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Échelle des colonnes {@code DECIMAL(26,6)}. */
    public static BigDecimal r(BigDecimal v) {
        return v == null ? null : v.setScale(6, RoundingMode.HALF_UP);
    }
}
