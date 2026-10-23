package com.tsix.immocrm.services.rapport.vente.dossiers;

import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Noms de tiers (notaires, clients) lus en une requête par lot, même priorité que {@link TierNomFormatter}. */
@Component
public class NomsTiers {

    private static final String COLONNES = "t.PKey, t.SocialReason, t.Description, t.FirstName, t.LastName";
    private static final String PAR_IDS = "SELECT " + COLONNES + " FROM CfgTier t WHERE t.PKey IN (:ids)";
    private static final String NOTAIRES = "SELECT " + COLONNES + " FROM CfgTier t WHERE t.PKey IN "
            + "(SELECT f.CfgLawyerId FROM ComFolder f WHERE f.CfgLawyerId IS NOT NULL)";
    // Acquéreur principal et co-acquéreurs de chaque dossier.
    private static final String CLIENTS = """
            SELECT f.PKey AS Dossier, t.SocialReason, t.Description, t.FirstName, t.LastName
            FROM ComFolder f
            CROSS APPLY (SELECT f.CfgCustomerId AS TierId
                         UNION SELECT fc.CfgTierId FROM ComFolderCustomer fc WHERE fc.ComFolderId = f.PKey) a
            JOIN CfgTier t ON t.PKey = a.TierId
            WHERE f.PKey IN (:ids)""";

    private final NamedParameterJdbcTemplate jdbc;

    public NomsTiers(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private record Nom(UUID id, String nom) {
    }

    public Map<UUID, String> noms(Collection<UUID> ids) {
        Map<UUID, String> noms = new HashMap<>();
        LotsTranches.lire(List.copyOf(ids), lot -> jdbc.query(PAR_IDS, Map.of("ids", LotsTranches.textes(lot)),
                (rs, n) -> nom(rs, "PKey"))).forEach(n -> noms.put(n.id(), n.nom()));
        return noms;
    }

    /** Notaires ayant au moins un dossier, triés par nom : options du filtre {@code notaireId}. */
    public List<ParametreRapport.Option> notaires() {
        return jdbc.query(NOTAIRES, Map.of(), (rs, n) -> nom(rs, "PKey")).stream()
                .sorted(Comparator.comparing(n -> Objects.toString(n.nom(), "")))
                .map(n -> new ParametreRapport.Option(n.id().toString(), Objects.toString(n.nom(), n.id().toString())))
                .toList();
    }

    /** Noms distincts des clients (principal et co-acquéreurs) de chaque dossier. */
    public Map<UUID, List<String>> clients(Collection<UUID> dossiers) {
        Map<UUID, List<String>> parDossier = new HashMap<>();
        LotsTranches.lire(List.copyOf(dossiers), lot -> jdbc.query(CLIENTS, Map.of("ids", LotsTranches.textes(lot)),
                        (rs, n) -> nom(rs, "Dossier")))
                .forEach(n -> parDossier.computeIfAbsent(n.id(), k -> new ArrayList<>()).add(n.nom()));
        return parDossier;
    }

    private static Nom nom(ResultSet rs, String cle) throws SQLException {
        return new Nom(UUID.fromString(rs.getString(cle)), TierNomFormatter.nom(rs.getString("SocialReason"),
                rs.getString("Description"), rs.getString("FirstName"), rs.getString("LastName")));
    }
}
