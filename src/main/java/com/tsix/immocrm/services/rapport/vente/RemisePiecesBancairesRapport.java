package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.rapport.*;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.dossiers.NomsTiers;
import com.tsix.immocrm.services.rapport.vente.dossiers.RapportVente;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * N° 32 REMISE DE PIÈCES BANCAIRES (FEATURE-tdb-vente-rapports-dossiers.md §6), modèle OMRAN
 * {@code ComTdbRemisePiecesBancaires} : encaissements du module COM dont la pièce est remise au centre dans la période.
 * Seules les lignes de règlement sur des tranches visibles sont lues (tranche, clients, montant).
 */
@Component
public class RemisePiecesBancairesRapport extends RapportVente {

    static final String CODE = "ComTdbRemisePiecesBancaires";
    static final String PARTIELLEMENT_VISIBLE = "Règlement partiellement visible : seules ses lignes sur vos tranches "
            + "sont comptées dans son montant.";

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String TRANCHE = "tranche";
    private static final String ARTICLE = "article";
    private static final String ECHEANCE = "echeance";
    private static final String MONTANT = "montant";

    // Bornes en date comparées à CAST(… AS date) : jours entiers inclus (piège smalldatetime du socle).
    private static final String LIGNES = "SELECT s.PKey AS Reglement, s.ItemNumber, s.EcheanceDate, "
            + "s.DeliveredToCenterDate, s.Amount AS MontantReglement, "
            + "(SELECT COUNT(*) FROM StlSettlementLine t WHERE t.StlSettlementId = s.PKey) AS NbLignes, "
            + StatutReglement.SQL_MONTANT_SIGNE + " AS MontantLigne, f.PKey AS Dossier, tr.Description AS Tranche "
            + "FROM StlSettlement s JOIN AdmModule m ON m.PKey = s.AdmModuleId AND m.Code = N'COM' "
            + "JOIN StlSettlementLine l ON l.StlSettlementId = s.PKey JOIN ComDocument d ON d.PKey = l.ComDocumentId "
            + "JOIN ComFolder f ON f.PKey = d.ComFolderId JOIN StkItem i ON i.PKey = f.StkItemId "
            + "JOIN CfgTranche tr ON tr.PKey = i.StkHierarchyId "
            // Société directe du règlement, comme filtreEncaissements() (VisibiliteEntites.CLAUSE_JPQL en SQL).
            + "LEFT JOIN CfgCompany soc ON soc.PKey = s.CfgCompanyId "
            + "WHERE (soc.IsActive = 1 OR soc.PKey IS NULL) AND i.StkHierarchyId IN (:tranches) AND s.IsCredit = 1 AND s.IsDcoumentDeliveredToCenter = 1 "
            + "AND CAST(s.DeliveredToCenterDate AS date) >= :du AND CAST(s.DeliveredToCenterDate AS date) <= :au AND "
            + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX;

    private final NamedParameterJdbcTemplate jdbc;
    private final NomsTiers noms;
    private final Clock clock;

    public RemisePiecesBancairesRapport(NamedParameterJdbcTemplate jdbc, NomsTiers noms, Clock clock) {
        super(CODE);
        this.jdbc = jdbc;
        this.noms = noms;
        this.clock = clock;
    }

    private record Ligne(UUID reglement, String article, LocalDateTime echeance, LocalDateTime remise,
                         BigDecimal montantReglement, int nbLignes, BigDecimal montantLigne, UUID dossier,
                         String tranche) {
    }

    /** Un règlement et ses lignes visibles. */
    private record Remise(Ligne entete, List<Ligne> lignes) {

        boolean partiel() {
            return lignes.size() < entete.nbLignes();
        }

        BigDecimal montant() {
            return partiel() ? lignes.stream().map(Ligne::montantLigne).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : entete.montantReglement();
        }
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate aujourdhui = LocalDate.now(clock);
        return List.of(ParametreRapport.date(DU, "Du", aujourdhui.withDayOfMonth(1)),
                ParametreRapport.date(AU, "Au", aujourdhui), ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        // Jamais nulles : le socle applique les défauts de parametres().
        Map<String, Object> bornes = Map.of(DU, Objects.requireNonNull(contexte.date(DU)),
                AU, Objects.requireNonNull(contexte.date(AU)));
        Map<UUID, List<Ligne>> parReglement = new LinkedHashMap<>();
        LotsTranches.lire(contexte.tranchesVisibles(), lot -> {
            Map<String, Object> parametres = new LinkedHashMap<>(bornes);
            parametres.put("tranches", LotsTranches.textes(lot));
            return jdbc.query(LIGNES, parametres, (rs, n) -> ligne(rs));
        }).forEach(l -> parReglement.computeIfAbsent(l.reglement(), k -> new ArrayList<>()).add(l));
        List<Remise> remises = parReglement.values().stream().map(l -> new Remise(l.get(0), l))
                .sorted(Comparator.comparing((Remise r) -> r.entete().echeance(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(r -> r.entete().article(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<UUID, List<String>> clients = noms.clients(parReglement.values().stream().flatMap(List::stream)
                .map(Ligne::dossier).collect(Collectors.toSet()));
        List<Map<String, Object>> lignes = remises.stream().map(r -> ligne(r, clients)).toList();
        List<String> avertissements = remises.stream().anyMatch(Remise::partiel) ? List.of(PARTIELLEMENT_VISIBLE)
                : List.of();
        return new ResultatRapport(List.of(new SectionRapport("remises", "Remises de pièces bancaires", colonnes(),
                lignes, TRANCHE, null, List.of())), avertissements);
    }

    private static Ligne ligne(ResultSet rs) throws SQLException {
        return new Ligne(UUID.fromString(rs.getString("Reglement")), rs.getString("ItemNumber"),
                date(rs.getTimestamp("EcheanceDate")), date(rs.getTimestamp("DeliveredToCenterDate")),
                rs.getBigDecimal("MontantReglement"), rs.getInt("NbLignes"), rs.getBigDecimal("MontantLigne"),
                UUID.fromString(rs.getString("Dossier")), rs.getString("Tranche"));
    }

    private static LocalDateTime date(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    private static Map<String, Object> ligne(Remise r, Map<UUID, List<String>> clients) {
        Set<String> tranches = new TreeSet<>();
        Set<String> nomsClients = new TreeSet<>();
        r.lignes().forEach(l -> {
            if (l.tranche() != null) {
                tranches.add(l.tranche());
            }
            clients.getOrDefault(l.dossier(), List.of()).stream().filter(Objects::nonNull).forEach(nomsClients::add);
        });
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(TRANCHE, String.join(", ", tranches));
        ligne.put(ARTICLE, r.entete().article());
        ligne.put(ECHEANCE, r.entete().echeance());
        ligne.put("dateRemise", r.entete().remise());
        ligne.put("clients", String.join(", ", nomsClients));
        ligne.put(MONTANT, r.montant());
        return ligne;
    }

    private static List<ColonneRapport> colonnes() {
        return List.of(ColonneRapport.texte(TRANCHE, "Tranche"), ColonneRapport.texte(ARTICLE, "Article"),
                date(ECHEANCE, "Date"), date("dateRemise", "Date de remise"),
                ColonneRapport.texte("clients", "Client(s)"), ColonneRapport.montant(MONTANT, "Montant", null));
    }

    private static ColonneRapport date(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.DATE, null, FonctionTotal.AUCUNE, true, 0);
    }
}
