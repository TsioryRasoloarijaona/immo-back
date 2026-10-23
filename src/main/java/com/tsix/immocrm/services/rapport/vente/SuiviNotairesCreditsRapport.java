package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.rapport.*;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.dossiers.DelaisDossier;
import com.tsix.immocrm.services.rapport.vente.dossiers.NomsTiers;
import com.tsix.immocrm.services.rapport.vente.dossiers.ParametresDossiers;
import com.tsix.immocrm.services.rapport.vente.dossiers.RapportVente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * N° 28 ÉTAT SUIVI NOTAIRES CRÉDITS (FEATURE-tdb-vente-rapports-dossiers.md §4), modèle OMRAN
 * {@code ComSaleDashboardSuiviNotairesCredits} : cohorte des dossiers envoyés au notaire dans l'année, par mois d'envoi.
 * Crédit par {@link MontantsParNatureDossier} ; « débloqué » = règlements de type {@code CHEQUE NOTAIRE} (décision 1).
 */
@Component
public class SuiviNotairesCreditsRapport extends RapportVente {

    static final String CODE = "ComSaleDashboardSuiviNotairesCredits";
    static final String ATTESTATIONS_ABSENTES = "Aucun type de règlement « attestation de consignation » en base : "
            + "les colonnes d'attestations non débloquées valent 0.";
    static final List<String> MOIS = List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet",
            "Août", "Septembre", "Octobre", "Novembre", "Décembre");

    /** Id de la section et nom de sa colonne de mois. */
    private static final String SECTION = "mois";
    private static final String ANNEE = "annee";
    private static final String NOTAIRE_ID = "notaireId";
    private static final String NB = "nbDossiers";
    private static final String CA = "ca";
    private static final String CREDIT = "credit";
    private static final String DEBLOQUE = "creditDebloque";
    private static final String ATTESTATIONS = "attestationsNonDebloquees";
    private static final String ECHEANCES_ATTESTATIONS = "echeancesAttestationsNonDebloquees";
    private static final String DELAI = "delaiDeblocage";
    private static final String TRANCHES = "tranches";
    private static final String CATEGORIE = "credit";

    // Statut obligatoire : un dossier sans statut n'a jamais d'envoi notaire (§4).
    private static final String COHORTE = """
            SELECT f FROM ComFolder f JOIN FETCH f.stkItem i JOIN FETCH f.strStatus s
            WHERE i.stkHierarchy.id IN :tranches AND f.cfgLawyer IS NOT NULL
              AND f.agreementDate >= :debut AND f.agreementDate < :fin AND s.sorting >= :seuil
              AND COALESCE(s.annulation, false) = false AND COALESCE(s.resiliation, false) = false
              AND EXISTS (SELECT d.id FROM ComDocument d WHERE d.comFolder = f AND d.comSettlementCategory.id = :credit)""";
    private static final String DES_NOTAIRES = COHORTE + " AND f.cfgLawyer.id IN :notaires";

    // Réglé signé hors CANCELED (règle partagée), par règlement, sur les échéances crédit du dossier.
    private static final String CHEQUES_NOTAIRE = "SELECT d.ComFolderId, s.DocDate, s.EcheanceDate, SUM("
            + StatutReglement.SQL_MONTANT_SIGNE + ") AS Montant FROM StlSettlementLine l "
            + "JOIN StlSettlement s ON s.PKey = l.StlSettlementId JOIN ComDocument d ON d.PKey = l.ComDocumentId "
            + "JOIN StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId "
            + "WHERE d.ComFolderId IN (:dossiers) AND d.ComSettlementCategoryId = :credit AND s.IsCredit = 1 "
            + "AND ty.Code = N'CHEQUE NOTAIRE' AND ISNULL(ty.IsNotContabilized, 0) = 0 AND "
            + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX + " GROUP BY d.ComFolderId, s.PKey, s.DocDate, s.EcheanceDate";

    private final EntityManager em;
    private final NamedParameterJdbcTemplate jdbc;
    private final MontantsParNatureDossier montants;
    private final ParametresDossiers parametresVente;
    private final NomsTiers noms;
    private final Clock clock;

    public SuiviNotairesCreditsRapport(EntityManager em, NamedParameterJdbcTemplate jdbc,
                                       MontantsParNatureDossier montants, ParametresDossiers parametresVente, NomsTiers noms,
                                       Clock clock) {
        super(CODE);
        this.em = em;
        this.jdbc = jdbc;
        this.montants = montants;
        this.parametresVente = parametresVente;
        this.noms = noms;
        this.clock = clock;
    }

    private record Cheque(UUID dossier, BigDecimal montant, Long delai) {
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(
                new ParametreRapport(ANNEE, "Année", TypeParametre.ANNEE, false, false,
                        String.valueOf(LocalDate.now(clock).getYear()), List.of(), null, null),
                ParametreRapport.tranches(),
                new ParametreRapport(NOTAIRE_ID, "Notaires", TypeParametre.CHOIX, false, true, null, noms.notaires(),
                        null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<String> avertissements = new ArrayList<>(List.of(ATTESTATIONS_ABSENTES));
        Optional<UUID> credit = parametresVente.lire(ParametresDossiers.CREDIT, avertissements);
        Optional<Integer> seuil = parametresVente.lire(ParametresDossiers.STATUT_NOTAIRE, avertissements)
                .map(id -> em.find(StrStatus.class, id)).map(StrStatus::getSorting);
        if (credit.isEmpty() || seuil.isEmpty()) {
            return new ResultatRapport(List.of(section(List.of())), avertissements);
        }
        int annee = Objects.requireNonNull(contexte.annee(ANNEE), "annee (défaut du socle)");
        List<ComFolder> cohorte = cohorte(contexte, annee, seuil.get(), credit.get());
        if (cohorte.isEmpty()) {
            avertissements.add("Aucune donnée pour " + annee);
        }
        return new ResultatRapport(List.of(section(lignes(cohorte, credit.get()))), avertissements);
    }

    private List<ComFolder> cohorte(ContexteRapport contexte, int annee, int seuil, UUID credit) {
        List<String> notaires = contexte.choix(NOTAIRE_ID);
        LocalDateTime debut = LocalDate.of(annee, Month.JANUARY, 1).atStartOfDay();
        return LotsTranches.lire(contexte.tranchesVisibles(), lot -> {
            TypedQuery<ComFolder> q = em.createQuery(notaires.isEmpty() ? COHORTE : DES_NOTAIRES, ComFolder.class)
                    .setParameter(TRANCHES, lot).setParameter("debut", debut)
                    .setParameter("fin", debut.plusYears(1)).setParameter("seuil", seuil)
                    .setParameter(CATEGORIE, credit);
            if (!notaires.isEmpty()) {
                q.setParameter("notaires", notaires.stream().map(UUID::fromString).toList());
            }
            return q.getResultList();
        });
    }

    private List<Map<String, Object>> lignes(List<ComFolder> cohorte, UUID credit) {
        Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = montants.pour(cohorte);
        Map<UUID, List<Cheque>> cheques = new HashMap<>();
        LotsTranches.lire(cohorte.stream().map(ComFolder::getId).toList(), lot -> jdbc.query(CHEQUES_NOTAIRE,
                        Map.of("dossiers", LotsTranches.textes(lot), CATEGORIE, credit.toString()),
                        (rs, n) -> new Cheque(UUID.fromString(rs.getString("ComFolderId")),
                                rs.getBigDecimal("Montant"), jours(rs.getTimestamp("DocDate"),
                                rs.getTimestamp("EcheanceDate")))))
                .forEach(c -> cheques.computeIfAbsent(c.dossier(), k -> new ArrayList<>()).add(c));
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Month mois : Month.values()) {
            List<ComFolder> duMois = cohorte.stream().filter(f -> f.getAgreementDate().getMonth() == mois).toList();
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(SECTION, MOIS.get(mois.ordinal()));
            ligne.put(NB, duMois.size());
            ligne.put(CA, somme(duMois.stream().map(ComFolder::getPrice).toList()));
            ligne.put(CREDIT, somme(duMois.stream().map(f -> parDossier.getOrDefault(f.getId(), Map.of()).get(ColonneDossier.CREDIT))
                    .toList()));
            List<List<Cheque>> chequesDuMois = duMois.stream().map(f -> cheques.getOrDefault(f.getId(), List.of()))
                    .filter(c -> !c.isEmpty()).toList();
            ligne.put(DEBLOQUE, somme(chequesDuMois.stream().flatMap(List::stream).map(Cheque::montant).toList()));
            ligne.put(ATTESTATIONS, BigDecimal.ZERO);
            ligne.put(ECHEANCES_ATTESTATIONS, BigDecimal.ZERO);
            ligne.put(DELAI, delai(chequesDuMois));
            lignes.add(ligne);
        }
        return lignes;
    }

    /** Moyenne, sur les dossiers ayant un chèque notaire daté, de leur délai moyen ; {@code null} sinon. */
    private static BigDecimal delai(List<List<Cheque>> parDossier) {
        List<BigDecimal> moyennes = parDossier.stream()
                .map(c -> c.stream().map(Cheque::delai).filter(Objects::nonNull).toList())
                .filter(d -> !d.isEmpty())
                .map(d -> BigDecimal.valueOf(d.stream().mapToLong(Long::longValue).sum())
                        .divide(BigDecimal.valueOf(d.size()), 6, RoundingMode.HALF_UP))
                .toList();
        return DelaisDossier.moyenneDe(moyennes);
    }

    private static Long jours(Timestamp debut, Timestamp fin) {
        return debut == null || fin == null ? null
                : ChronoUnit.DAYS.between(debut.toLocalDateTime().toLocalDate(), fin.toLocalDateTime().toLocalDate());
    }

    private static BigDecimal somme(List<BigDecimal> valeurs) {
        return valeurs.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static SectionRapport section(List<Map<String, Object>> lignes) {
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(SECTION, "Mois"),
                ColonneRapport.effectif(NB, "Nbr de dossiers traités", null),
                ColonneRapport.montant(CA, "C.A.", null),
                ColonneRapport.montant(CREDIT, "Mnt crédit", null),
                ColonneRapport.montant(DEBLOQUE, "Mt crédit débloquées", null),
                ColonneRapport.montant(ATTESTATIONS, "Mnt attestations non débloquées", null),
                ColonneRapport.montant(ECHEANCES_ATTESTATIONS, "Échéances attestations non débloquées", null),
                new ColonneRapport(DELAI, "Délai de déblocage (j)", TypeColonne.NOMBRE, null, FonctionTotal.MOYENNE,
                        true, 1));
        return new SectionRapport(SECTION, "État suivi notaires crédits",
                colonnes, lignes, null, null, List.of());
    }
}
