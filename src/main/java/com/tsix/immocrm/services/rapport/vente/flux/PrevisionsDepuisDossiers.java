package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Prévus du n° 15 calculés sur les dossiers ni annulés ni désistés (FEATURE-tdb-vente-rapports-flux.md §4.7), au mois de la
 * date du dossier décalée du délai de sa tranche (jours) ; délai nul ⇒ dossier ignoré et signalé. N'est pas une
 * {@link SourcePrevu} : le prévu du flux reste celui de {@link PrevuNonSaisi}.
 */
@Service
@Transactional(readOnly = true)
public class PrevisionsDepuisDossiers {

    private static final String TRANCHES = "tranches";

    private static final String DOSSIERS = """
            SELECT f.PKey AS Dossier, t.Code AS Tranche, t.DelayLawyerMinute, t.DelayMinuteAllSettled, st.PKey AS Statut,
                   f.Price, f.PriceAfterDiscount, f.AgreementDate, f.AgreementSavingDate, f.AgreementDatePrevisionel,
                   f.IsConfirmedAgreementDate
            FROM ComFolder f
            JOIN StkVocation v ON v.PKey = f.StkVocationId AND v.Code = 'VENTE'
            JOIN StkItem i ON i.PKey = f.StkItemId
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId AND t.IsForSale = 1
            JOIN StrStatus st ON st.PKey = f.StrStatusId AND st.BoolField002 = 0 AND st.BoolField003 = 0
            WHERE i.StkHierarchyId IN (:tranches)
              AND (f.AgreementDate IS NOT NULL OR f.AgreementSavingDate IS NOT NULL
                   OR f.AgreementDatePrevisionel IS NOT NULL)""";

    // Réglé avant l'acte : la règle d'encaissement partagée (§3.3), mouvement antérieur au jour de l'acte.
    private static final String REGLE_AVANT_ACTE = "SELECT f.PKey AS Dossier, SUM(" + RegleEncaissement.MONTANT
            + ") AS Regle " + RegleEncaissement.LIGNES + "WHERE i.StkHierarchyId IN (:tranches) AND"
            + RegleEncaissement.ENCAISSEMENT + "AND CAST(" + RegleEncaissement.MOUVEMENT
            + " AS date) < CAST(f.AgreementSavingDate AS date) AND" + RegleEncaissement.COMPTE + "GROUP BY f.PKey";

    /** Les six séries du n° 15. */
    public enum Prevision {
        /** Σ {@code Price}, mois de {@code AgreementDate} + délai notaire. */
        CA_NOTAIRE,
        /** Σ ({@code PriceAfterDiscount} − réglé avant l'acte), mois de l'acte + délai de solde. */
        RESTE_APRES_ACTE,
        /** Dossiers au statut de concrétisation, date prévisionnelle non confirmée + délai notaire : nombre, Σ prix. */
        CONCRETISATION_PREVUE_NOMBRE,
        CONCRETISATION_PREVUE_MONTANT,
        /** Idem, date prévisionnelle confirmée. */
        CONCRETISATION_CONFIRMEE_NOMBRE,
        CONCRETISATION_CONFIRMEE_MONTANT
    }

    /** Valeurs par série et par mois (1–12) de l'année demandée ; avertissements (délais nuls, paramètre absent). */
    public record Previsions(Map<Prevision, Map<Integer, BigDecimal>> parMois, List<String> avertissements) {

        public BigDecimal mois(Prevision p, int mois) {
            return parMois.getOrDefault(p, Map.of()).getOrDefault(mois, BigDecimal.ZERO);
        }
    }

    private record Dossier(UUID id, String tranche, Integer delaiNotaire, Integer delaiSolde, UUID statut,
                           BigDecimal prix, BigDecimal prixRemise, LocalDateTime envoiNotaire, LocalDateTime acte,
                           LocalDateTime previsionnelle, Boolean confirmee) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final SeuilsStatutsVente seuils;

    public PrevisionsDepuisDossiers(NamedParameterJdbcTemplate jdbc, SeuilsStatutsVente seuils) {
        this.jdbc = jdbc;
        this.seuils = seuils;
    }

    /** {@code tranches} = tranches visibles ; vide ⇒ aucune requête. */
    public Previsions pour(List<UUID> tranches, int annee) {
        if (tranches.isEmpty()) {
            return new Previsions(Map.of(), List.of());
        }
        Optional<UUID> concretisation = seuils.valeur(SeuilsStatutsVente.CONCRETISATION);
        Map<UUID, BigDecimal> reglesAvantActe = new HashMap<>();
        LotsTranches.lire(tranches, lot -> jdbc.query(REGLE_AVANT_ACTE, Map.of(TRANCHES, LotsTranches.textes(lot)),
                        (rs, n) -> Map.entry(UUID.fromString(rs.getString("Dossier")), rs.getBigDecimal("Regle"))))
                .forEach(e -> reglesAvantActe.merge(e.getKey(), nonNul(e.getValue()), BigDecimal::add));
        Calcul calcul = new Calcul(annee);
        for (Dossier d : LotsTranches.lire(tranches, lot -> jdbc.query(DOSSIERS,
                Map.of(TRANCHES, LotsTranches.textes(lot)), (rs, n) -> dossier(rs)))) {
            calcul.placer(d.envoiNotaire(), d.delaiNotaire(), d.tranche(), Map.of(Prevision.CA_NOTAIRE, nonNul(d.prix())));
            if (d.acte() != null) {
                calcul.placer(d.acte(), d.delaiSolde(), d.tranche(), Map.of(Prevision.RESTE_APRES_ACTE,
                        nonNul(d.prixRemise()).subtract(reglesAvantActe.getOrDefault(d.id(), BigDecimal.ZERO))));
            }
            if (concretisation.isPresent() && concretisation.get().equals(d.statut())) {
                boolean confirmee = Boolean.TRUE.equals(d.confirmee());
                calcul.placer(d.previsionnelle(), d.delaiNotaire(), d.tranche(), Map.of(
                        confirmee ? Prevision.CONCRETISATION_CONFIRMEE_NOMBRE : Prevision.CONCRETISATION_PREVUE_NOMBRE,
                        BigDecimal.ONE,
                        confirmee ? Prevision.CONCRETISATION_CONFIRMEE_MONTANT : Prevision.CONCRETISATION_PREVUE_MONTANT,
                        nonNul(d.prix())));
            }
        }
        List<String> avertissements = new ArrayList<>();
        calcul.sansDelai.forEach(t -> avertissements.add(
                "Délai notaire / solde non renseigné pour la tranche %s : ses dossiers sont ignorés.".formatted(t)));
        if (concretisation.isEmpty()) {
            avertissements.add("Paramètre %s absent : la section des concrétisations prévues vaut 0."
                    .formatted(SeuilsStatutsVente.CONCRETISATION));
        }
        return new Previsions(calcul.parMois, avertissements);
    }

    private static Dossier dossier(ResultSet rs) throws SQLException {
        return new Dossier(UUID.fromString(rs.getString("Dossier")), rs.getString("Tranche"),
                rs.getObject("DelayLawyerMinute", Integer.class), rs.getObject("DelayMinuteAllSettled", Integer.class),
                UUID.fromString(rs.getString("Statut")), rs.getBigDecimal("Price"), rs.getBigDecimal("PriceAfterDiscount"),
                date(rs, "AgreementDate"), date(rs, "AgreementSavingDate"), date(rs, "AgreementDatePrevisionel"),
                rs.getObject("IsConfirmedAgreementDate", Boolean.class));
    }

    private static LocalDateTime date(ResultSet rs, String colonne) throws SQLException {
        Timestamp valeur = rs.getTimestamp(colonne);
        return valeur == null ? null : valeur.toLocalDateTime();
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    private static final class Calcul {

        private final int annee;
        private final Map<Prevision, Map<Integer, BigDecimal>> parMois = new EnumMap<>(Prevision.class);
        private final Set<String> sansDelai = new TreeSet<>();

        Calcul(int annee) {
            this.annee = annee;
        }

        /** Date nulle : rien ; délai nul : tranche signalée ; sinon valeurs au mois de date + délai, si dans l'année. */
        void placer(LocalDateTime date, Integer delai, String tranche, Map<Prevision, BigDecimal> valeurs) {
            if (date == null) {
                return;
            }
            if (delai == null) {
                sansDelai.add(String.valueOf(tranche));
                return;
            }
            LocalDateTime prevue = date.plusDays(delai);
            if (prevue.getYear() == annee) {
                valeurs.forEach((p, v) -> parMois.computeIfAbsent(p, k -> new HashMap<>())
                        .merge(prevue.getMonthValue(), v, BigDecimal::add));
            }
        }
    }
}
