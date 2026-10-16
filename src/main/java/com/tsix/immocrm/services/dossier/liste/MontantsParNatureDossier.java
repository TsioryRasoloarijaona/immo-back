package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TropPercu;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static com.tsix.immocrm.domain.ColonneDossier.*;

/**
 * Montants par nature de la grille OMRAN (avance, autofinancement, crédit, frais, net à payer, prix contractuel,
 * trop-perçu), formules de {@code ComSaleFolderView} : voir FEATURE-dossiers-liste-complete.md. Deux requêtes par
 * page (échéances, réglé signé) ; le net à payer et le trop-perçu sont lus par {@link TropPercu}, jamais refaits.
 */
@Component
public class MontantsParNatureDossier {

    /** Ce qui exige les requêtes ; {@code prixContractuel} est une colonne de ComFolder. */
    public static final Set<ColonneDossier> COLONNES = EnumSet.of(
            PRIX_CONTRACTUEL_REGLE, PRIX_CONTRACTUEL_RESTE, PRIX_REGLE, PRIX_RESTE, NET_A_PAYER_REGLE,
            NET_A_PAYER_RESTE, TAUX_AVANCE, TAUX_AVANCE_REEL, AVANCE, AVANCE_REGLEE, AVANCE_RESTE, AUTOFINANCEMENT,
            AUTOFINANCEMENT_REGLE, AUTOFINANCEMENT_RESTE, CREDIT, CREDIT_REGLE, CREDIT_RESTE, FRAIS, FRAIS_REGLES,
            FRAIS_RESTE, TROP_PERCU, TROP_PERCU_REGLE, TROP_PERCU_RESTE);

    /** Les colonnes que {@code GET /api/dossiers/totaux} additionne (les taux ne s'additionnent pas). */
    public static final Set<ColonneDossier> SOMMABLES = sommables();

    /** Ids par requête, sous la limite de 2 100 paramètres liés de SQL Server. */
    static final int TAILLE_TRANCHE = 2_000;

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final int ECHELLE = 6;

    // Inner join sur la catégorie, comme ComSaleDocumentLocal : une échéance sans catégorie n'a pas de nature.
    private static final String ECHEANCES = """
            SELECT d.comFolder.id, d.id, d.amount, d.excludeFromPrice, c.isCredit, c.isSaleExpense, c.isSaleAdvance,
                   c.isSaleAutoFinancment, c.isSaleCredit, c.isSaleModification, c.isSalePenality, c.isSaleResiliation
            FROM ComDocument d JOIN d.comSettlementCategory c
            WHERE d.comFolder.id IN :ids""";

    // Même réglé que /montants : signe et exclusion des CANCELED du dépôt des lignes de règlement.
    private static final String REGLES = "SELECT l.comDocument.id, SUM(" + StlSettlementLineRepository.MONTANT_SIGNE
            + ") FROM StlSettlementLine l JOIN l.stlSettlement st WHERE l.comDocument.comFolder.id IN :ids"
            + StlSettlementLineRepository.COMPTE + " GROUP BY l.comDocument.id";

    private final EntityManager em;

    public MontantsParNatureDossier(EntityManager em) {
        this.em = em;
    }

    /** Une échéance : sa ligne de trop-perçu et sa participation au prix contractuel ({@code ExcludeFromPrice = 0}). */
    record Echeance(TropPercu.Ligne ligne, boolean auPrixContractuel) {
    }

    public static boolean requises(Set<ColonneDossier> colonnes) {
        return colonnes.stream().anyMatch(COLONNES::contains);
    }

    /** Montants de chaque dossier, par colonne ; un dossier sans échéance a des montants nuls, pas absents. */
    public Map<UUID, Map<ColonneDossier, BigDecimal>> pour(Collection<ComFolder> dossiers) {
        List<ComFolder> tous = List.copyOf(dossiers);
        Map<UUID, List<Echeance>> echeances = new HashMap<>();
        for (int debut = 0; debut < tous.size(); debut += TAILLE_TRANCHE) {
            List<UUID> ids = tous.subList(debut, Math.min(debut + TAILLE_TRANCHE, tous.size())).stream()
                    .map(ComFolder::getId).toList();
            lire(ids, echeances);
        }
        Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = new HashMap<>();
        for (ComFolder f : tous) {
            parDossier.put(f.getId(), calculer(f, echeances.getOrDefault(f.getId(), List.of())));
        }
        return parDossier;
    }

    private void lire(List<UUID> ids, Map<UUID, List<Echeance>> parDossier) {
        Map<UUID, BigDecimal> regleParEcheance = new HashMap<>();
        for (Object[] r : em.createQuery(REGLES, Object[].class).setParameter("ids", ids).getResultList()) {
            regleParEcheance.put((UUID) r[0], (BigDecimal) r[1]);
        }
        for (Object[] e : em.createQuery(ECHEANCES, Object[].class).setParameter("ids", ids).getResultList()) {
            BigDecimal regle = regleParEcheance.getOrDefault((UUID) e[1], BigDecimal.ZERO);
            TropPercu.Ligne ligne = new TropPercu.Ligne((BigDecimal) e[2], regle, (Boolean) e[4], vrai(e[5]),
                    vrai(e[6]), vrai(e[7]), vrai(e[8]), vrai(e[9]), vrai(e[10]), vrai(e[11]));
            parDossier.computeIfAbsent((UUID) e[0], k -> new ArrayList<>())
                    .add(new Echeance(ligne, Boolean.FALSE.equals(e[3])));
        }
    }

    static Map<ColonneDossier, BigDecimal> calculer(ComFolder f, List<Echeance> echeances) {
        List<TropPercu.Ligne> lignes = echeances.stream().map(Echeance::ligne).toList();
        Map<ColonneDossier, BigDecimal> m = new EnumMap<>(ColonneDossier.class);
        nature(m, lignes, TropPercu.Ligne::avance, AVANCE, AVANCE_REGLEE, AVANCE_RESTE);
        nature(m, lignes, TropPercu.Ligne::autoFinancement, AUTOFINANCEMENT, AUTOFINANCEMENT_REGLE,
                AUTOFINANCEMENT_RESTE);
        nature(m, lignes, TropPercu.Ligne::creditBancaire, CREDIT, CREDIT_REGLE, CREDIT_RESTE);
        nature(m, lignes, TropPercu.Ligne::frais, FRAIS, FRAIS_REGLES, FRAIS_RESTE);

        BigDecimal prixRegle = m.get(AVANCE_REGLEE).add(m.get(AUTOFINANCEMENT_REGLE)).add(m.get(CREDIT_REGLE));
        m.put(PRIX_REGLE, prixRegle);
        m.put(PRIX_RESTE, nonNul(f.getPrice()).subtract(prixRegle));

        BigDecimal contractuelRegle = echeances.stream().filter(Echeance::auPrixContractuel)
                .map(e -> nonNul(e.ligne().regle())).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put(PRIX_CONTRACTUEL, nonNul(f.getAgreementPrice()));
        m.put(PRIX_CONTRACTUEL_REGLE, contractuelRegle);
        m.put(PRIX_CONTRACTUEL_RESTE, nonNul(f.getAgreementPrice()).subtract(contractuelRegle));

        m.put(TAUX_AVANCE, pourcentage(m.get(AVANCE), f.getPriceAfterDiscount()));
        m.put(TAUX_AVANCE_REEL, pourcentage(m.get(AVANCE_REGLEE), m.get(AVANCE)));

        boolean sorti = estSorti(f);
        TropPercu.Bilan bilan = TropPercu.bilan(lignes, f.getPrice(), f.getModificationsAmount(), sorti);
        m.put(NET_A_PAYER_REGLE, bilan.totalRegle());
        m.put(NET_A_PAYER_RESTE, bilan.reste());
        TropPercu.Resultat tropPercu = TropPercu.calculer(lignes, f.getPrice(), f.getModificationsAmount(), sorti);
        m.put(TROP_PERCU, tropPercu.tropPercu());
        m.put(TROP_PERCU_REGLE, tropPercu.tropPercu().subtract(tropPercu.tropPercuReste()));
        m.put(TROP_PERCU_RESTE, tropPercu.tropPercuReste());
        return m;
    }

    /** Montant = échéances de la nature au crédit ; réglé = toutes ses échéances, comme la vue. */
    private static void nature(Map<ColonneDossier, BigDecimal> m, List<TropPercu.Ligne> lignes,
                               Predicate<TropPercu.Ligne> deLaNature, ColonneDossier montant, ColonneDossier regle,
                               ColonneDossier reste) {
        BigDecimal du = lignes.stream().filter(deLaNature.and(l -> Boolean.TRUE.equals(l.sens())))
                .map(l -> nonNul(l.montant())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paye = lignes.stream().filter(deLaNature)
                .map(l -> nonNul(l.regle())).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put(montant, du);
        m.put(regle, paye);
        m.put(reste, du.subtract(paye));
    }

    /** {@code part / base × 100} ; 0 si la base est nulle ou absente (pas de division par zéro). */
    static BigDecimal pourcentage(BigDecimal part, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return nonNul(part).multiply(CENT).divide(base, ECHELLE, RoundingMode.HALF_UP);
    }

    /** Statut d'annulation ou de résiliation ({@code IsCancellation} de la vue) ; code inconnu = en cours. */
    static boolean estSorti(ComFolder f) {
        if (f.getStrStatus() == null || f.getStrStatus().getCode() == null) {
            return false;
        }
        try {
            return StatutDossier.fromCode(f.getStrStatus().getCode()).isSortie();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Set<ColonneDossier> sommables() {
        Set<ColonneDossier> s = EnumSet.copyOf(COLONNES);
        s.add(PRIX_CONTRACTUEL);
        s.remove(TAUX_AVANCE);
        s.remove(TAUX_AVANCE_REEL);
        return s;
    }

    private static boolean vrai(Object drapeau) {
        return Boolean.TRUE.equals(drapeau);
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}
