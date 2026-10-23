package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * N° 16 SUIVI DES CADEAUX (FEATURE-tdb-vente-rapports-commercial.md §8, décision 4), modèle {@code ComTdbGift} :
 * cadeaux des dossiers des tranches visibles, dossier annulé compris. Écart voulu : {@code LEFT JOIN ComGift}
 * (le modèle, en jointure interne, perd les cadeaux sans {@code ComGiftId}).
 */
@Component
public class SuiviCadeauxRapport implements Rapport {

    static final String CODE = "ComTdbGift";
    static final String CADEAU_NON_PRECISE = "Cadeau non précisé";

    private static final String LIBELLE_TRANCHE = "Tranche";
    private static final String LIBELLE_CADEAU = "Cadeau";
    private static final String CADEAU_LIVRE = "cadeauLivre";
    private static final String BA_LIVRE = "baLivre";
    private static final String TOUS = "tous";
    private static final String OUI = "oui";
    private static final String TRANCHE = "tranche";
    private static final String CADEAU = "cadeau";
    private static final String ARTICLE = "article";
    private static final String NOMBRE = "nombre";
    private static final List<ParametreRapport.Option> LIVRE = List.of(new ParametreRapport.Option(TOUS, "Tous"),
            new ParametreRapport.Option(OUI, "Oui"), new ParametreRapport.Option("non", "Non"));

    // Finalisé = StrStatus.BoolField014 (non mappé) ; annulé = annulation ou désistement (BoolField002 / 003).
    private static final String CADEAUX = """
            SELECT t.Description AS Tranche, i.Code AS Article, s.Description AS Statut,
                   c.SocialReason, c.Description AS ClientDescription, c.FirstName, c.LastName, c.IDNumber,
                   co.SocialReason AS CoRaison, co.Description AS CoDescription, co.FirstName AS CoPrenom,
                   co.LastName AS CoNom, ca.Description AS Cadeau, r.Description AS Raison,
                   CAST(CASE WHEN g.DeliveryDate IS NULL THEN 0 ELSE 1 END AS bit) AS CadeauLivre,
                   CAST(CASE WHEN g.VoucherDate IS NULL THEN 0 ELSE 1 END AS bit) AS BaLivre,
                   CAST(CASE WHEN s.BoolField002 = 1 OR s.BoolField003 = 1 THEN 1 ELSE 0 END AS bit) AS Annule,
                   CAST(CASE WHEN s.BoolField014 = 1 THEN 1 ELSE 0 END AS bit) AS Finalise
            FROM ComFolderGift g
            JOIN ComFolder f ON f.PKey = g.ComFolderId
            JOIN StkItem i ON i.PKey = f.StkItemId
            JOIN CfgTranche t ON t.PKey = i.StkHierarchyId
            LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId
            LEFT JOIN CfgTier c ON c.PKey = f.CfgCustomerId
            LEFT JOIN ComProspection p ON p.PKey = ISNULL(g.ComProspectionId, f.ComProspectionId)
            LEFT JOIN CfgTier co ON co.PKey = p.CfgCommercialId
            LEFT JOIN ComGift ca ON ca.PKey = g.ComGiftId
            LEFT JOIN ComGiftReason r ON r.PKey = g.ComGiftReasonId
            WHERE i.StkHierarchyId IN (:tranches)""";

    private final NamedParameterJdbcTemplate jdbc;

    public SuiviCadeauxRapport(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private record Cadeau(Map<String, Object> ligne, boolean livre, boolean bonAchat, boolean finalise) {
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.tranches(), choix(CADEAU_LIVRE, "Cadeau livré"), choix(BA_LIVRE, "B.A. livré"));
    }

    private static ParametreRapport choix(String nom, String libelle) {
        return new ParametreRapport(nom, libelle, TypeParametre.CHOIX, false, false, TOUS, LIVRE, null, null);
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Predicate<Cadeau> retenu = filtre(contexte.texte(CADEAU_LIVRE), Cadeau::livre)
                .and(filtre(contexte.texte(BA_LIVRE), Cadeau::bonAchat));
        List<Cadeau> cadeaux = LotsTranches.lire(contexte.tranchesVisibles(), lot -> jdbc.query(CADEAUX,
                        Map.of("tranches", LotsTranches.textes(lot)), (rs, n) -> cadeau(rs)))
                .stream().filter(retenu)
                .sorted(Comparator.comparing((Cadeau c) -> String.valueOf(c.ligne().get(TRANCHE)))
                        .thenComparing(c -> String.valueOf(c.ligne().get(ARTICLE))))
                .toList();
        return new ResultatRapport(List.of(
                new SectionRapport("cadeaux", "Cadeaux", colonnes(), cadeaux.stream().map(Cadeau::ligne).toList(),
                        TRANCHE, null, List.of()),
                nonLivres(cadeaux)), List.of());
    }

    private static Predicate<Cadeau> filtre(String valeur, Predicate<Cadeau> vrai) {
        if (valeur == null || TOUS.equals(valeur)) {
            return c -> true;
        }
        return OUI.equals(valeur) ? vrai : vrai.negate();
    }

    private static Cadeau cadeau(ResultSet rs) throws SQLException {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(TRANCHE, rs.getString(LIBELLE_TRANCHE));
        ligne.put(ARTICLE, rs.getString("Article"));
        ligne.put("statut", rs.getString("Statut"));
        ligne.put("client", TierNomFormatter.nom(rs.getString("SocialReason"), rs.getString("ClientDescription"),
                rs.getString("FirstName"), rs.getString("LastName")));
        ligne.put("cin", rs.getString("IDNumber"));
        ligne.put("parrain", TierNomFormatter.nom(rs.getString("CoRaison"), rs.getString("CoDescription"),
                rs.getString("CoPrenom"), rs.getString("CoNom")));
        String cadeau = rs.getString(LIBELLE_CADEAU);
        ligne.put(CADEAU, cadeau == null ? CADEAU_NON_PRECISE : cadeau);
        boolean livre = rs.getBoolean("CadeauLivre");
        boolean bonAchat = rs.getBoolean("BaLivre");
        ligne.put(CADEAU_LIVRE, livre);
        ligne.put("raison", rs.getString("Raison"));
        ligne.put(BA_LIVRE, bonAchat);
        ligne.put("annule", rs.getBoolean("Annule"));
        return new Cadeau(ligne, livre, bonAchat, rs.getBoolean("Finalise"));
    }

    private static List<ColonneRapport> colonnes() {
        return List.of(ColonneRapport.texte(TRANCHE, LIBELLE_TRANCHE), ColonneRapport.texte(ARTICLE, "Article"),
                ColonneRapport.texte("statut", "Statut"), ColonneRapport.texte("client", "Client"),
                ColonneRapport.texte("cin", "CIN"), ColonneRapport.texte("parrain", "Parrain"),
                ColonneRapport.texte(CADEAU, LIBELLE_CADEAU), booleen(CADEAU_LIVRE, "Cadeau livré"),
                ColonneRapport.texte("raison", "Raison"), booleen(BA_LIVRE, "B.A. livré"),
                booleen("annule", "Annulé"));
    }

    private static ColonneRapport booleen(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.BOOLEEN, null, FonctionTotal.AUCUNE, true, 0);
    }

    /** Cadeaux sans bon d'achat des dossiers à statut finalisé, par (cadeau, tranche). */
    private static SectionRapport nonLivres(List<Cadeau> cadeaux) {
        Map<List<String>, Long> parCle = new LinkedHashMap<>();
        cadeaux.stream().filter(c -> c.finalise() && !c.bonAchat())
                .forEach(c -> parCle.merge(List.of(String.valueOf(c.ligne().get(CADEAU)),
                        String.valueOf(c.ligne().get(TRANCHE))), 1L, Long::sum));
        List<Map<String, Object>> lignes = new ArrayList<>();
        parCle.forEach((cle, n) -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(CADEAU, cle.get(0));
            ligne.put(TRANCHE, cle.get(1));
            ligne.put(NOMBRE, n);
            lignes.add(ligne);
        });
        return new SectionRapport("non-livres", "Cadeaux non livrés", List.of(ColonneRapport.texte(CADEAU, LIBELLE_CADEAU),
                ColonneRapport.texte(TRANCHE, LIBELLE_TRANCHE), ColonneRapport.effectif(NOMBRE, "Nombre", null)),
                lignes, null, null, List.of());
    }
}
