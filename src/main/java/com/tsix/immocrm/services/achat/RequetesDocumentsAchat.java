package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.HistoriqueDocumentAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.LigneDocumentAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.ReglementDocumentAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.VentilationDocumentAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.FiltresDocumentsAchat;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.FournisseurAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.ProjetAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.StatutAchatDto;
import com.tsix.immocrm.services.administration.HistoriqueService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** SQL natif des pièces d'achat : aucune entité {@code Prj*} dans ce lot (FEATURE-achat-lecture-factures.md §4). */
@Component
class RequetesDocumentsAchat {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final String ID = "id";
    private static final String PROJETS = "projets";

    private static final String PIECES = "SELECT d.PKey, d.Code, d.DocType, d.DocRef, d.Description, d.DocDate, "
            + "d.MaturityDate, d.CfgTierId, t.Description AS FournisseurNom, d.PrjProjectId, p.Code AS ProjetCode, "
            + "d.PrjMarketId, m.Code AS LotCode, c.Code AS SocieteCode, d.BuyDocumentStatusId, bs.Code AS StatutCode, "
            + "bs.Description AS StatutDescription, bs.IsOkForPayement, bs.IsValidatedOnAccounting, d.AmountHT, "
            + "d.AmountVat, d.AmountTTC, d.AmountToPay, (" + SoldeDocumentAchat.SQL_REGLE + ") AS Regle, "
            + "d.Notes, d.AmountRegul, d.FiscalStamp, "
            + "e.Code AS EntiteCode, e.Description AS EntiteDescription, d.UserCreateDateTime, d.UserCreate, "
            + "d.UserUpdateDateTime, d.UserUpdate "
            + "FROM BuyDocument d LEFT JOIN CfgTier t ON t.PKey = d.CfgTierId "
            + "LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId LEFT JOIN PrjMarket m ON m.PKey = d.PrjMarketId "
            + "LEFT JOIN CfgCompany c ON c.PKey = d.CfgCompanyId LEFT JOIN StrEntity e ON e.PKey = d.StrEntityId "
            + "LEFT JOIN BuyDocumentStatus bs ON bs.PKey = d.BuyDocumentStatusId WHERE d.PrjProjectId IN (:projets) ";

    private static final String LISTE = PIECES
            + "AND (:toutesNatures = 1 OR d.DocType IN (:natures)) "
            + "AND (:tousStatuts = 1 OR d.BuyDocumentStatusId IN (:statuts)) "
            + "AND (:lot IS NULL OR d.PrjMarketId = :lot) AND (:fournisseur IS NULL OR d.CfgTierId = :fournisseur) "
            + "AND (:du IS NULL OR CAST(d.DocDate AS date) >= :du) AND (:au IS NULL OR CAST(d.DocDate AS date) <= :au) "
            + "AND (:motif IS NULL OR LOWER(d.Code) LIKE :motif OR LOWER(d.DocRef) LIKE :motif "
            + "OR LOWER(d.Description) LIKE :motif OR LOWER(t.Description) LIKE :motif)";

    private static final String FICHE = PIECES + "AND d.PKey = :id";

    private static final String LIGNES = "SELECT Pkey, Sorting, Description, Unity, Quantity, UnitPriceHT, VatRatio, "
            + "DiscountRatio FROM BuyDocumentLine WHERE BuyDocumentId = :id ORDER BY Sorting, Code";

    private static final String VENTILATION = "SELECT c.Code, c.Description, v.VentilationRate "
            + "FROM BuyVentilationCategory v LEFT JOIN StlCategory c ON c.PKey = v.StlCategoryId "
            + "WHERE v.BuyDocumentId = :id ORDER BY c.Code";

    private static final String HISTORIQUE = "SELECT h.DocDate, st.Code, st.Description, "
            // Piège : Description et Code n'ont pas la même collation en CI.
            + "COALESCE(NULLIF(u.Description, '') COLLATE DATABASE_DEFAULT, u.Code COLLATE DATABASE_DEFAULT) AS Utilisateur, h.Notes FROM BuyDocumentStatusHistory h "
            + "LEFT JOIN BuyDocumentStatus st ON st.PKey = h.BuyDocumentStatusId "
            + "LEFT JOIN AdmUser u ON u.PKey = h.AdmUserId WHERE h.BuyDocumentId = :id ORDER BY h.DocDate";

    private static final String REGLEMENTS = "SELECT s.PKey, s.Code, s.DocDate, ty.Code AS TypeCode, "
            + "ty.Description AS TypeDescription, l.Amount, CASE WHEN " + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX
            + " THEN 0 ELSE 1 END AS Annule FROM StlSettlementLine l JOIN StlSettlement s ON s.PKey = l.StlSettlementId "
            + "LEFT JOIN StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId WHERE l.BuyDocumentId = :id "
            + "ORDER BY s.DocDate, s.Code";

    private static final String STATUTS = "SELECT PKey, Code, Description, IsOkForPayement, IsValidatedOnAccounting, "
            + "Sorting FROM BuyDocumentStatus ORDER BY Sorting, Code";

    private static final String PROJETS_VISIBLES = "SELECT PKey, Code, Description FROM PrjProject "
            + "WHERE PKey IN (:projets) ORDER BY Code";

    private static final String FOURNISSEURS = "SELECT DISTINCT t.PKey, t.Description FROM BuyDocument d "
            + "JOIN CfgTier t ON t.PKey = d.CfgTierId WHERE d.PrjProjectId IN (:projets) ORDER BY t.Description";

    /** En-tête de la fiche : la ligne de liste et les colonnes propres à la fiche. */
    record Entete(DocumentAchatListItemDto document, String notes, BigDecimal regularisation, BigDecimal timbreFiscal,
                  boolean valideComptabilite, String entiteCode, String entiteDescription, LocalDateTime dateCreation,
                  String creePar, LocalDateTime dateModification, String modifiePar) {

        DocumentAchatDetailDto avec(List<LigneDocumentAchatDto> lignes, List<VentilationDocumentAchatDto> ventilation,
                                    List<HistoriqueDocumentAchatDto> historique, List<ReglementDocumentAchatDto> reglements) {
            return new DocumentAchatDetailDto(document, notes, regularisation, timbreFiscal, valideComptabilite,
                    entiteCode, entiteDescription, dateCreation, creePar, dateModification, modifiePar, lignes,
                    ventilation, historique, reglements);
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    RequetesDocumentsAchat(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Pièces des {@code projets} (non vides) retenues par les filtres, solde calculé, sans tri. */
    List<DocumentAchatListItemDto> pieces(Collection<UUID> projets, FiltresDocumentsAchat f) {
        MapSqlParameterSource p = new MapSqlParameterSource(PROJETS, textes(projets))
                .addValue("toutesNatures", f.natures().isEmpty() ? 1 : 0)
                .addValue("natures", f.natures().isEmpty() ? List.of("") : f.natures().stream().map(Enum::name).toList())
                .addValue("tousStatuts", f.statutIds().isEmpty() ? 1 : 0)
                // SQL Server convertit la liste même quand :tousStatuts = 1 : un UUID valide, jamais ''.
                .addValue("statuts", textes(f.statutIds().isEmpty() ? List.of(new UUID(0, 0)) : f.statutIds()))
                .addValue("lot", texte(f.lotId()), Types.VARCHAR)
                .addValue("fournisseur", texte(f.fournisseurId()), Types.VARCHAR)
                .addValue("du", f.du(), Types.DATE)
                .addValue("au", f.au(), Types.DATE)
                .addValue("motif", f.search() == null ? null
                        : HistoriqueService.motif(f.search().trim().toLowerCase(Locale.ROOT)), Types.NVARCHAR);
        return jdbc.query(LISTE, p, (rs, n) -> piece(rs));
    }

    Optional<Entete> entete(Collection<UUID> projets, UUID id) {
        return jdbc.query(FICHE, Map.of(PROJETS, textes(projets), ID, id.toString()), (rs, n) -> new Entete(piece(rs),
                        chaine(rs, "Notes"), rs.getBigDecimal("AmountRegul"), rs.getBigDecimal("FiscalStamp"),
                        rs.getBoolean("IsValidatedOnAccounting"), chaine(rs, "EntiteCode"),
                        chaine(rs, "EntiteDescription"), horodatage(rs, "UserCreateDateTime"), chaine(rs, "UserCreate"),
                        horodatage(rs, "UserUpdateDateTime"), chaine(rs, "UserUpdate")))
                .stream().findFirst();
    }

    List<LigneDocumentAchatDto> lignes(UUID id) {
        return jdbc.query(LIGNES, Map.of(ID, id.toString()), (rs, n) -> new LigneDocumentAchatDto(uuid(rs, "Pkey"),
                (Integer) rs.getObject("Sorting"), chaine(rs, "Description"), chaine(rs, "Unity"),
                rs.getBigDecimal("Quantity"), rs.getBigDecimal("UnitPriceHT"), pourcent(rs.getBigDecimal("VatRatio")),
                pourcent(rs.getBigDecimal("DiscountRatio"))));
    }

    List<VentilationDocumentAchatDto> ventilation(UUID id) {
        return jdbc.query(VENTILATION, Map.of(ID, id.toString()), (rs, n) -> new VentilationDocumentAchatDto(
                chaine(rs, "Code"), chaine(rs, "Description"), rs.getBigDecimal("VentilationRate")));
    }

    List<HistoriqueDocumentAchatDto> historique(UUID id) {
        return jdbc.query(HISTORIQUE, Map.of(ID, id.toString()), (rs, n) -> new HistoriqueDocumentAchatDto(
                horodatage(rs, "DocDate"), chaine(rs, "Code"), chaine(rs, "Description"), chaine(rs, "Utilisateur"),
                chaine(rs, "Notes")));
    }

    List<ReglementDocumentAchatDto> reglements(UUID id) {
        return jdbc.query(REGLEMENTS, Map.of(ID, id.toString()), (rs, n) -> new ReglementDocumentAchatDto(
                uuid(rs, "PKey"), chaine(rs, "Code"), jour(rs, "DocDate"), chaine(rs, "TypeCode"),
                chaine(rs, "TypeDescription"), rs.getBigDecimal("Amount"), rs.getInt("Annule") == 1));
    }

    List<StatutAchatDto> statuts() {
        return jdbc.query(STATUTS, (rs, n) -> new StatutAchatDto(uuid(rs, "PKey"), chaine(rs, "Code"),
                chaine(rs, "Description"), rs.getBoolean("IsOkForPayement"), rs.getBoolean("IsValidatedOnAccounting"),
                (Integer) rs.getObject("Sorting")));
    }

    List<ProjetAchatDto> projets(Collection<UUID> projets) {
        return jdbc.query(PROJETS_VISIBLES, Map.of(PROJETS, textes(projets)), (rs, n) -> new ProjetAchatDto(
                uuid(rs, "PKey"), chaine(rs, "Code"), chaine(rs, "Description")));
    }

    List<FournisseurAchatDto> fournisseurs(Collection<UUID> projets) {
        return jdbc.query(FOURNISSEURS, Map.of(PROJETS, textes(projets)), (rs, n) -> new FournisseurAchatDto(
                uuid(rs, "PKey"), chaine(rs, "Description")));
    }

    private static DocumentAchatListItemDto piece(ResultSet rs) throws SQLException {
        SoldeDocumentAchat solde = SoldeDocumentAchat.de(rs.getBigDecimal("AmountToPay"), rs.getBigDecimal("Regle"));
        String nature = chaine(rs, "DocType");
        return new DocumentAchatListItemDto(uuid(rs, "PKey"), chaine(rs, "Code"), nature,
                NatureDocumentAchat.libelleDe(nature), chaine(rs, "DocRef"), chaine(rs, "Description"),
                jour(rs, "DocDate"), jour(rs, "MaturityDate"), uuid(rs, "CfgTierId"), chaine(rs, "FournisseurNom"),
                uuid(rs, "PrjProjectId"), chaine(rs, "ProjetCode"), uuid(rs, "PrjMarketId"), chaine(rs, "LotCode"),
                chaine(rs, "SocieteCode"), uuid(rs, "BuyDocumentStatusId"), chaine(rs, "StatutCode"),
                chaine(rs, "StatutDescription"), rs.getBoolean("IsOkForPayement"), rs.getBigDecimal("AmountHT"),
                rs.getBigDecimal("AmountVat"), rs.getBigDecimal("AmountTTC"), solde.montantAPayer(),
                solde.montantRegle(), solde.resteARegler(), solde.solde(), solde.tropRegle(), solde.aDeduire());
    }

    private static List<String> textes(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).toList();
    }

    private static String texte(UUID id) {
        return id == null ? null : id.toString();
    }

    private static String chaine(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null || valeur.isBlank() ? null : valeur;
    }

    private static UUID uuid(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null ? null : UUID.fromString(valeur);
    }

    private static LocalDateTime horodatage(ResultSet rs, String colonne) throws SQLException {
        return rs.getObject(colonne, LocalDateTime.class);
    }

    private static LocalDate jour(ResultSet rs, String colonne) throws SQLException {
        LocalDateTime valeur = horodatage(rs, colonne);
        return valeur == null ? null : valeur.toLocalDate();
    }

    private static BigDecimal pourcent(BigDecimal fraction) {
        return fraction == null ? null : fraction.multiply(CENT).stripTrailingZeros();
    }
}
