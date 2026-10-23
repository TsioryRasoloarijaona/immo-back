package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.*;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.dossiers.RapportVente;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * N° 33 VÉRIFICATION DES ATTACHEMENTS, Vente (FEATURE-tdb-vente-rapports-dossiers.md §7), modèle OMRAN
 * {@code ComVerifAttachment} : pièces ({@code AdmAttachedFile.CategoryId} = dossier, quelle que soit {@code Category})
 * par type, une ligne par dossier de vente, sans pièce compris. Les pièces « vérifiées non jointes » restent comptées.
 */
@Component
public class VerificationAttachementsVenteRapport extends RapportVente {

    static final String CODE = "ComVerifAttachment";

    private static final String STATUT = "statut";
    private static final String TRANCHE = "tranche";
    private static final String ARTICLE = "article";
    private static final String SANS_TYPE = "sansType";
    private static final String TOTAL = "total";
    private static final String COCHES = "coches";
    private static final String IDS = "ids";

    // Tous statuts (annulés compris, comme le modèle) ; filtre « statut contient » facultatif.
    private static final String DOSSIERS = """
            SELECT f.PKey, f.Code, i.Description AS Article, s.Code AS Statut, tr.Description AS Tranche
            FROM ComFolder f
            JOIN StkVocation v ON v.PKey = f.StkVocationId AND v.Code = 'VENTE'
            JOIN StkItem i ON i.PKey = f.StkItemId
            JOIN CfgTranche tr ON tr.PKey = i.StkHierarchyId
            LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId
            WHERE i.StkHierarchyId IN (:tranches) AND (:tous = 1 OR s.Code LIKE :motif)""";

    private static final String PIECES = """
            SELECT a.CategoryId AS Dossier, ty.PKey AS TypeId, ty.Description AS TypePiece, COUNT(*) AS Nb,
                   SUM(CASE WHEN a.IsCheckedButNotAttached = 1 THEN 1 ELSE 0 END) AS Coches
            FROM AdmAttachedFile a
            LEFT JOIN AdmAttachedFileType ty ON ty.PKey = a.AdmAttachedFileTypeId
            WHERE a.CategoryId IN (:ids)
            GROUP BY a.CategoryId, ty.PKey, ty.Description""";

    private final NamedParameterJdbcTemplate jdbc;

    public VerificationAttachementsVenteRapport(NamedParameterJdbcTemplate jdbc) {
        super(CODE);
        this.jdbc = jdbc;
    }

    private record Dossier(UUID id, String tranche, String article) {
    }

    private record Pieces(UUID dossier, UUID type, String libelle, long nombre, long coches) {
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.tranches(), new ParametreRapport(STATUT, "Statut", TypeParametre.TEXTE,
                false, false, null, List.of(), null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        String statut = contexte.texte(STATUT);
        List<Dossier> dossiers = LotsTranches.lire(contexte.tranchesVisibles(), lot -> {
            Map<String, Object> p = new HashMap<>();
            p.put("tranches", LotsTranches.textes(lot));
            p.put("tous", statut == null ? 1 : 0);
            p.put("motif", statut == null ? "%" : "%" + echapper(statut) + "%");
            return jdbc.query(DOSSIERS, p, (rs, n) -> new Dossier(UUID.fromString(rs.getString("PKey")),
                    rs.getString("Tranche"), libelle(rs.getString("Article"), rs.getString("Code"),
                    rs.getString("Statut"))));
        });
        List<Pieces> pieces = LotsTranches.lire(dossiers.stream().map(Dossier::id).toList(), lot -> jdbc.query(
                PIECES, Map.of(IDS, LotsTranches.textes(lot)), (rs, n) -> new Pieces(
                        UUID.fromString(rs.getString("Dossier")), uuid(rs.getString("TypeId")),
                        rs.getString("TypePiece"), rs.getLong("Nb"), rs.getLong("Coches"))));
        // Une colonne par type présent, triées par libellé ; nom technique « typeN » dans cet ordre.
        Map<UUID, String> types = new HashMap<>();
        pieces.stream().filter(p -> p.type() != null).forEach(p -> types.put(p.type(), p.libelle()));
        List<UUID> ordre = types.keySet().stream()
                .sorted(Comparator.comparing((UUID t) -> Objects.toString(types.get(t), "")).thenComparing(t -> t))
                .toList();
        Map<UUID, String> noms = new HashMap<>();
        for (int i = 0; i < ordre.size(); i++) {
            noms.put(ordre.get(i), "type" + (i + 1));
        }
        boolean avecSansType = pieces.stream().anyMatch(p -> p.type() == null);
        Map<UUID, List<Pieces>> parDossier = pieces.stream().collect(Collectors.groupingBy(Pieces::dossier));
        List<Map<String, Object>> lignes = dossiers.stream()
                .sorted(Comparator.comparing((Dossier d) -> Objects.toString(d.tranche(), ""))
                        .thenComparing(Dossier::article))
                .map(d -> ligne(d, parDossier.getOrDefault(d.id(), List.of()), ordre, noms, avecSansType))
                .toList();
        return new ResultatRapport(List.of(new SectionRapport("pieces",
                "Vérification des attachements dossier de vente", colonnes(ordre, types, noms, avecSansType), lignes,
                TRANCHE, null, List.of())), List.of());
    }

    private static List<ColonneRapport> colonnes(List<UUID> ordre, Map<UUID, String> types, Map<UUID, String> noms,
                                                 boolean avecSansType) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(TRANCHE, "Tranche"),
                ColonneRapport.texte(ARTICLE, "Article (dossier statut)")));
        ordre.forEach(t -> colonnes.add(ColonneRapport.effectif(noms.get(t), types.get(t), null)));
        if (avecSansType) {
            colonnes.add(ColonneRapport.effectif(SANS_TYPE, "Sans type", null));
        }
        colonnes.add(ColonneRapport.effectif(TOTAL, "Fichiers attachés", null));
        colonnes.add(ColonneRapport.effectif(COCHES, "dont vérifiées non jointes", null));
        return colonnes;
    }

    private static Map<String, Object> ligne(Dossier d, List<Pieces> pieces, List<UUID> ordre, Map<UUID, String> noms,
                                             boolean avecSansType) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(TRANCHE, d.tranche());
        ligne.put(ARTICLE, d.article());
        Map<UUID, Long> parType = new TreeMap<>();
        pieces.stream().filter(p -> p.type() != null).forEach(p -> parType.merge(p.type(), p.nombre(), Long::sum));
        ordre.forEach(t -> ligne.put(noms.get(t), parType.getOrDefault(t, 0L)));
        if (avecSansType) {
            ligne.put(SANS_TYPE, pieces.stream().filter(p -> p.type() == null).mapToLong(Pieces::nombre).sum());
        }
        ligne.put(TOTAL, pieces.stream().mapToLong(Pieces::nombre).sum());
        ligne.put(COCHES, pieces.stream().mapToLong(Pieces::coches).sum());
        return ligne;
    }

    /** « libellé article (code dossier code statut) », sans statut : « (code dossier) ». */
    private static String libelle(String article, String dossier, String statut) {
        String entre = Stream.of(dossier, statut).filter(Objects::nonNull).collect(Collectors.joining(" "));
        return Objects.toString(article, "") + " (" + entre + ")";
    }

    /** Texte saisi pris littéralement dans le {@code LIKE} : {@code %}, {@code _} et {@code [} échappés. */
    private static String echapper(String texte) {
        return texte.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    private static UUID uuid(String valeur) {
        return valeur == null ? null : UUID.fromString(valeur);
    }
}
