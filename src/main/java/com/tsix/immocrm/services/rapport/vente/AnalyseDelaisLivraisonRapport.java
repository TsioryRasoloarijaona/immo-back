package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.*;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.dossiers.DelaisDossier;
import com.tsix.immocrm.services.rapport.vente.dossiers.DelaisDossier.Dates;
import com.tsix.immocrm.services.rapport.vente.dossiers.NomsTiers;
import com.tsix.immocrm.services.rapport.vente.dossiers.ParametresDossiers;
import com.tsix.immocrm.services.rapport.vente.dossiers.RapportVente;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.rapport.vente.dossiers.DelaisDossier.*;

/**
 * N° 29 ANALYSE DÉLAIS DE LIVRAISONS (FEATURE-tdb-vente-rapports-dossiers.md §5), modèle OMRAN
 * {@code ComSaleDashboardDeliveryDelay} : délais moyens des dossiers ni annulés ni désistés, comptant / crédit et par
 * notaire. Le bloc « notaires comptant » ne garde que le comptant (le {@code NOT IN} du modèle n'excluait rien).
 */
@Component
public class AnalyseDelaisLivraisonRapport extends RapportVente {

    static final String CODE = "ComSaleDashboardDeliveryDelay";
    static final String TITRE_VIDE = "La date de titre foncier n'est renseignée sur aucun article : les délais "
            + "« Titre foncier » sont vides.";

    private static final String NB = "nbDossiers";
    private static final String NOTAIRE = "notaire";

    // LEFT JOIN : un dossier sans statut n'est ni annulé ni désisté, il reste compté.
    private static final String DOSSIERS = """
            SELECT f.CfgLawyerId, i.TitleDate, f.AgreementDate, f.AgreementSavingDate, f.DeliveryDateReal,
                   f.OwnershipCertificateDate, f.CreditApprovalDate, f.CreditPossessionDate,
                   CASE WHEN EXISTS (SELECT 1 FROM ComDocument d WHERE d.ComFolderId = f.PKey
                                       AND d.ComSettlementCategoryId = :credit) THEN 1 ELSE 0 END AS ACredit
            FROM ComFolder f
            JOIN StkItem i ON i.PKey = f.StkItemId
            LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId
            WHERE i.StkHierarchyId IN (:tranches) AND ISNULL(s.BoolField002, 0) = 0 AND ISNULL(s.BoolField003, 0) = 0""";

    private static final List<DelaisDossier> COMPTANT = List.of(TITRE_ENVOI_NOTAIRE, MINUTE_REMISE_CLES,
            TITRE_REMISE_CLES);
    private static final List<DelaisDossier> COMPTANT_NOTAIRES = List.of(ENVOI_MINUTE, MINUTE_FORMALISE);
    private static final List<DelaisDossier> CREDIT = List.of(TITRE_ENVOI_NOTAIRE, MINUTE_REMISE_CLES);
    private static final List<DelaisDossier> CREDIT_NOTAIRES = List.of(ENVOI_CREDIT_EN_COURS,
            CREDIT_EN_COURS_DEBLOQUE, DEBLOQUE_MINUTE, CREDIT_EN_COURS_MINUTE, MINUTE_FORMALISE, TITRE_REMISE_CLES);

    private final NamedParameterJdbcTemplate jdbc;
    private final ParametresDossiers parametresVente;
    private final NomsTiers noms;

    public AnalyseDelaisLivraisonRapport(NamedParameterJdbcTemplate jdbc, ParametresDossiers parametresVente,
                                         NomsTiers noms) {
        super(CODE);
        this.jdbc = jdbc;
        this.parametresVente = parametresVente;
        this.noms = noms;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<String> avertissements = new ArrayList<>();
        Optional<UUID> credit = parametresVente.lire(ParametresDossiers.CREDIT, avertissements);
        List<Dates> dossiers = credit.map(c -> LotsTranches.lire(contexte.tranchesVisibles(), lot -> jdbc.query(
                DOSSIERS, Map.of("tranches", LotsTranches.textes(lot), "credit", c.toString()),
                (rs, n) -> dates(rs)))).orElse(List.of());
        if (!dossiers.isEmpty() && dossiers.stream().allMatch(d -> d.titre() == null)) {
            avertissements.add(TITRE_VIDE);
        }
        List<Dates> comptant = dossiers.stream().filter(d -> !d.aCredit()).toList();
        List<Dates> aCredit = dossiers.stream().filter(Dates::aCredit).toList();
        Map<UUID, String> nomsNotaires = noms.noms(dossiers.stream().map(Dates::notaire).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return new ResultatRapport(List.of(
                section("comptant", "Au comptant", false, COMPTANT, global(comptant, COMPTANT)),
                section("comptant-notaires", "Notaires (comptant)", true, COMPTANT_NOTAIRES,
                        parNotaire(comptant, COMPTANT_NOTAIRES, nomsNotaires)),
                section("credit", "À crédit", false, CREDIT, global(aCredit, CREDIT)),
                section("credit-notaires", "Notaires (crédit)", true, CREDIT_NOTAIRES,
                        parNotaire(aCredit, CREDIT_NOTAIRES, nomsNotaires))), avertissements);
    }

    private static Dates dates(ResultSet rs) throws SQLException {
        String notaire = rs.getString("CfgLawyerId");
        return new Dates(notaire == null ? null : UUID.fromString(notaire), rs.getInt("ACredit") == 1,
                jour(rs, "TitleDate"), jour(rs, "AgreementDate"), jour(rs, "AgreementSavingDate"),
                jour(rs, "DeliveryDateReal"), jour(rs, "OwnershipCertificateDate"), jour(rs, "CreditApprovalDate"),
                jour(rs, "CreditPossessionDate"));
    }

    private static LocalDate jour(ResultSet rs, String colonne) throws SQLException {
        Timestamp t = rs.getTimestamp(colonne);
        return t == null ? null : t.toLocalDateTime().toLocalDate();
    }

    /** Une ligne pour la population entière, aucune si elle est vide. */
    private static List<Map<String, Object>> global(List<Dates> dossiers, List<DelaisDossier> delais) {
        return dossiers.isEmpty() ? List.of() : List.of(ligne(new LinkedHashMap<>(), dossiers, delais));
    }

    /** Une ligne par notaire, triées par nom ; les dossiers sans notaire n'y figurent pas. */
    private static List<Map<String, Object>> parNotaire(List<Dates> dossiers, List<DelaisDossier> delais,
                                                       Map<UUID, String> noms) {
        Map<UUID, List<Dates>> parNotaire = dossiers.stream().filter(d -> d.notaire() != null)
                .collect(Collectors.groupingBy(Dates::notaire));
        return parNotaire.entrySet().stream()
                .map(e -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put(NOTAIRE, noms.getOrDefault(e.getKey(), e.getKey().toString()));
                    return ligne(ligne, e.getValue(), delais);
                })
                .sorted(Comparator.comparing(l -> Objects.toString(l.get(NOTAIRE), "")))
                .toList();
    }

    private static Map<String, Object> ligne(Map<String, Object> ligne, List<Dates> dossiers,
                                             List<DelaisDossier> delais) {
        ligne.put(NB, dossiers.size());
        delais.forEach(d -> ligne.put(d.nom(), d.moyenne(dossiers)));
        return ligne;
    }

    private static SectionRapport section(String id, String titre, boolean parNotaire, List<DelaisDossier> delais,
                                          List<Map<String, Object>> lignes) {
        Stream<ColonneRapport> tete = parNotaire
                ? Stream.of(ColonneRapport.texte(NOTAIRE, "Notaire"), nombre(NB, "Nombre de dossiers", 0))
                : Stream.of(nombre(NB, "Nombre de dossiers", 0));
        List<ColonneRapport> colonnes = Stream.concat(tete, delais.stream().map(d -> nombre(d.nom(), d.libelle(), 1)))
                .toList();
        return new SectionRapport(id, titre, colonnes, lignes, null, null, List.of());
    }

    /** Moyennes non additives : aucun total (le modèle n'en a pas). */
    private static ColonneRapport nombre(String nom, String libelle, int decimales) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE, true, decimales);
    }
}
