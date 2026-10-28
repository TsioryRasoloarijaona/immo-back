package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.dossier.ActeReservationData.Destinataire;
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Seule définition des destinataires du formulaire Barid (FEATURE-vente-formulaire-barid.md §3) : l'acquéreur principal puis
 * les co-acquéreurs ({@code ComFolderCustomer} par date de saisie) d'un dossier ni annulé ni désisté, sans dédoublonnage.
 */
@Component
public class DestinatairesBarid {

    static final String ACQUEREUR = "Acquéreur";
    static final String CO_ACQUEREUR = "Co-acquéreur";
    private static final Comparator<String> NOM = Comparator.nullsFirst(Collator.getInstance(Locale.FRENCH)::compare);

    // Rang 0 = acquéreur principal ; ville du tiers = code du référentiel, ville de la société = texte libre (comme OMRAN).
    private static final String SELECT = """
            SELECT f.Code AS Dossier, tr.Description AS Tranche, s.Description AS Statut, x.Rang, t.Description AS Nom,
                   tt.Code AS Titre, t.Address, t.Address2, t.Address3, t.ZipCode, ci.Code AS Ville, pa.Description AS Pays,
                   soc.Description AS Societe, soc.Address AS SocAddress, soc.Address2 AS SocAddress2,
                   soc.Address3 AS SocAddress3, soc.City AS SocVille, soc.ZipCode AS SocZipCode, ps.Description AS SocPays
            FROM ComFolder f
            LEFT JOIN StkItem i ON i.PKey = f.StkItemId
            LEFT JOIN CfgTranche tr ON tr.PKey = i.StkHierarchyId
            LEFT JOIN CfgProject pr ON pr.PKey = tr.CfgProjectId
            LEFT JOIN CfgCompany soc ON soc.PKey = pr.CfgCompanyId
            LEFT JOIN AdmCountry ps ON ps.PKey = soc.AdmCountryId
            LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId
            CROSS APPLY (SELECT CAST(0 AS bigint) AS Rang, f.CfgCustomerId AS TierId
                         UNION ALL
                         SELECT ROW_NUMBER() OVER (ORDER BY fc.UserCreateDateTime, fc.PKey), fc.CfgTierId
                         FROM ComFolderCustomer fc WHERE fc.ComFolderId = f.PKey) x
            JOIN CfgTier t ON t.PKey = x.TierId
            LEFT JOIN AdmTierTitle tt ON tt.PKey = t.AdmTierTitleId
            LEFT JOIN AdmCity ci ON ci.PKey = t.AdmCityId
            LEFT JOIN AdmCountry pa ON pa.PKey = t.AdmCountryId
            WHERE ISNULL(s.BoolField002, 0) = 0 AND ISNULL(s.BoolField003, 0) = 0 AND\s""";
    private static final String DU_DOSSIER = SELECT + "f.PKey = :dossier";
    private static final String DES_TRANCHES = SELECT + "i.StkHierarchyId IN (:ids)";
    private static final String DES_TRANCHES_ET_STATUTS = DES_TRANCHES + " AND f.StrStatusId IN (:statuts)";

    private final NamedParameterJdbcTemplate jdbc;

    public DestinatairesBarid(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private record Ligne(Destinataire destinataire, long rang) {
    }

    /** Destinataires d'un dossier, dans l'ordre : acquéreur principal puis co-acquéreurs. */
    public List<Destinataire> duDossier(UUID dossier) {
        return jdbc.query(DU_DOSSIER, Map.of("dossier", dossier.toString()), DestinatairesBarid::ligne).stream()
                .sorted(Comparator.comparingLong(Ligne::rang)).map(Ligne::destinataire)
                .filter(DestinatairesBarid::imprimable).toList();
    }

    /** Un tiers sans nom ne fait pas de page (le lot le signale par un avertissement). */
    public static boolean imprimable(Destinataire destinataire) {
        return destinataire.nom() != null && !destinataire.nom().isBlank();
    }

    /**
     * Pages des dossiers des tranches données (vide = aucune), tous dossiers confondus, triées par nom, dossier puis rang.
     * {@code statuts} non vide : dossiers de ces statuts seulement (un dossier sans statut est exclu).
     */
    public List<Destinataire> desTranches(List<UUID> tranches, Collection<UUID> statuts) {
        List<String> codesStatuts = LotsTranches.textes(List.copyOf(statuts));
        return LotsTranches.lire(tranches, lot -> codesStatuts.isEmpty()
                        ? jdbc.query(DES_TRANCHES, Map.of("ids", LotsTranches.textes(lot)), DestinatairesBarid::ligne)
                        : jdbc.query(DES_TRANCHES_ET_STATUTS, Map.of("ids", LotsTranches.textes(lot), "statuts", codesStatuts),
                        DestinatairesBarid::ligne)).stream()
                .sorted(Comparator.comparing((Ligne l) -> l.destinataire().nom(), NOM)
                        .thenComparing(l -> l.destinataire().dossier(), Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparingLong(Ligne::rang))
                .map(Ligne::destinataire).toList();
    }

    /** Code postal numérique écrit chiffre par chiffre (une case du formulaire par chiffre), sinon tel quel. */
    static String codePostal(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String code = valeur.trim();
        return code.chars().allMatch(c -> c >= '0' && c <= '9') ? String.join("  ", code.split("")) : code;
    }

    private static Ligne ligne(ResultSet rs, int n) throws SQLException {
        long rang = rs.getLong("Rang");
        Destinataire d = new Destinataire(rs.getString("Titre"), rs.getString("Nom"),
                joindre(rs.getString("Address"), rs.getString("Address2"), rs.getString("Address3"),
                        rs.getString("ZipCode"), rs.getString("Ville"), rs.getString("Pays")),
                joindre(rs.getString("Address"), rs.getString("Address2"), rs.getString("Address3"),
                        rs.getString("Ville"), rs.getString("Pays")),
                codePostal(rs.getString("ZipCode")), rs.getString("Societe"),
                joindre(rs.getString("SocAddress"), rs.getString("SocAddress2"), rs.getString("SocAddress3"),
                        rs.getString("SocVille"), rs.getString("SocPays")),
                codePostal(rs.getString("SocZipCode")), rs.getString("Dossier"), rs.getString("Tranche"),
                rs.getString("Statut"), rang == 0 ? ACQUEREUR : CO_ACQUEREUR);
        return new Ligne(d, rang);
    }

    /** Parties non vides jointes par une espace ; null si aucune. */
    private static String joindre(String... parties) {
        String texte = Stream.of(parties).filter(Objects::nonNull).map(String::trim).filter(p -> !p.isEmpty())
                .collect(Collectors.joining(" "));
        return texte.isEmpty() ? null : texte;
    }
}
