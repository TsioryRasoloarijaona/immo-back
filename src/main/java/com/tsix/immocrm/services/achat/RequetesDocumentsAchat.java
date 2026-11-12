package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.AlerteClotureCommande;
import com.tsix.immocrm.domain.achat.ChampDateAchat;
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
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.SocieteAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.StatutAchatDto;
import com.tsix.immocrm.dto.achat.ReferentielsAchatDto.TypeLotAchatDto;
import com.tsix.immocrm.services.administration.HistoriqueService;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
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
    private static final String PKEY = "PKey";
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String DOC_DATE = "DocDate";
    private static final UUID AUCUN = new UUID(0, 0);

    /** Seul prédicat de visibilité d'une pièce {@code d} (R3, R4, R5), lié par {@link #parametres}. */
    static final String VISIBLE = "d.CfgCompanyId IN (:societes) AND (d.PrjProjectId IN (:projets) "
            + "OR (:sansProjet = 1 AND d.PrjProjectId IS NULL)) AND (:moduleProjet = 0 OR d.AdmModuleId IN "
            + "(SELECT am.PKey FROM AdmModule am WHERE am.Code = N'PRJ')) ";

    private static final String CATEGORIES = "(SELECT STRING_AGG(sc.Description, ', ') WITHIN GROUP "
            + "(ORDER BY sc.Description ASC) FROM BuyVentilationCategory v JOIN StlCategory sc ON sc.PKey = v.StlCategoryId "
            + "WHERE v.BuyDocumentId = d.PKey)";

    private static final String PIECES = "SELECT d.PKey, d.Code, d.DocType, d.DocRef, d.Description, d.DocDate, "
            + "d.MaturityDate, d.CfgTierId, t.Description AS FournisseurNom, d.PrjProjectId, p.Code AS ProjetCode, "
            + "d.PrjMarketId, m.Code AS LotCode, c.Code AS SocieteCode, d.BuyDocumentStatusId, bs.Code AS StatutCode, "
            + "bs.Description AS StatutDescription, bs.IsOkForPayement, bs.IsValidatedOnAccounting, d.AmountHT, "
            + "d.AmountVat, d.AmountTTC, d.AmountToPay, (" + SoldeDocumentAchat.SQL_REGLE + ") AS Regle, "
            + "d.Notes, d.AmountRegul, d.FiscalStamp, "
            + "e.Code AS EntiteCode, e.Description AS EntiteDescription, d.UserCreateDateTime, d.UserCreate, "
            + "d.UserUpdateDateTime, d.UserUpdate, dc.DecompteId, dc.DecompteCode, dc.DecompteNumero, "
            + "c.Description AS SocieteNom, mo.Code AS ModuleCode, mo.Description AS ModuleLibelle, d.PrjMarketTypeId, "
            + "mt.Description AS TypeLotLibelle, d.IsClosed, d.IsAccounted, d.OfficeOrderRef, d.PersonToContact, "
            + "bf.Description AS DossierGroupement, cur.Code AS DeviseCode, bs.ColorName, bs.AdmWorkFlowStateTypeId, "
            + CATEGORIES + " AS Categories, "
            + "CASE WHEN d.DocType = 'ORDER' THEN " + AlerteClotureCommande.SQL_HT_FACTURE + " END AS HtFacture "
            + "FROM BuyDocument d LEFT JOIN CfgTier t ON t.PKey = d.CfgTierId "
            + "LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId LEFT JOIN PrjMarket m ON m.PKey = d.PrjMarketId "
            + "LEFT JOIN CfgCompany c ON c.PKey = d.CfgCompanyId LEFT JOIN StrEntity e ON e.PKey = d.StrEntityId "
            + "LEFT JOIN BuyDocumentStatus bs ON bs.PKey = d.BuyDocumentStatusId "
            + "LEFT JOIN AdmModule mo ON mo.PKey = d.AdmModuleId LEFT JOIN PrjMarketType mt ON mt.PKey = d.PrjMarketTypeId "
            + "LEFT JOIN BuyFolder bf ON bf.Pkey = d.BuyFolderId LEFT JOIN StlCurrency cur ON cur.PKey = d.StlCurrencyId "
            + RequetesReglementsProjet.DECOMPTE + "WHERE " + VISIBLE;

    private static final String DATE_FILTREE = "CAST(CASE WHEN :echeance = 1 THEN d.MaturityDate ELSE d.DocDate END AS date)";

    private static final String LISTE = PIECES
            + "AND (:toutesNatures = 1 OR d.DocType IN (:natures)) "
            + "AND (:tousStatuts = 1 OR d.BuyDocumentStatusId IN (:statuts)) "
            + "AND (:lot IS NULL OR d.PrjMarketId = :lot) AND (:fournisseur IS NULL OR d.CfgTierId = :fournisseur) "
            + "AND (:tousModules = 1 OR mo.Code IN (:modules)) "
            + "AND (:tousTypes = 1 OR d.PrjMarketTypeId IN (:typesLot)) "
            + "AND (:archive IS NULL OR COALESCE(d.IsClosed, 0) = :archive) "
            + "AND (:comptabilise IS NULL OR COALESCE(d.IsAccounted, 0) = :comptabilise) "
            + "AND (:montantDe IS NULL OR d.AmountToPay >= :montantDe) "
            + "AND (:montantA IS NULL OR d.AmountToPay <= :montantA) "
            + "AND (:declencheur IS NULL OR d.CfgTierTriggerId = :declencheur) "
            + "AND (:renseignee = 0 OR " + DATE_FILTREE + " IS NOT NULL) "
            + "AND (:du IS NULL OR " + DATE_FILTREE + " >= :du) AND (:au IS NULL OR " + DATE_FILTREE + " <= :au) "
            + "AND (:motif IS NULL OR LOWER(d.Code) LIKE :motif OR LOWER(d.DocRef) LIKE :motif "
            + "OR LOWER(d.OfficeOrderRef) LIKE :motif OR LOWER(d.Description) LIKE :motif "
            + "OR LOWER(d.Notes) LIKE :motif OR LOWER(t.Description) LIKE :motif OR LOWER(p.Code) LIKE :motif "
            + "OR LOWER(p.Description) LIKE :motif OR LOWER(m.Code) LIKE :motif OR LOWER(m.Description) LIKE :motif "
            + "OR LOWER(bf.Description) LIKE :motif)";

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
            + "JOIN CfgTier t ON t.PKey = d.CfgTierId WHERE " + VISIBLE + "ORDER BY t.Description";

    private static final String SOCIETES = "SELECT PKey, Code, Description FROM CfgCompany "
            + "WHERE PKey IN (:societes) ORDER BY Code";

    private static final String TYPES_LOT = "SELECT PKey, Code, Description FROM PrjMarketType ORDER BY Description, Code";

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

    /** Paramètres de {@link #VISIBLE} ; une liste vide devient un UUID qui ne désigne rien (SQL Server refuse IN ()). */
    static MapSqlParameterSource parametres(PerimetreAchat perimetre) {
        return new MapSqlParameterSource("societes", nonVide(perimetre.societes()))
                .addValue(PROJETS, nonVide(perimetre.projets()))
                .addValue("sansProjet", perimetre.sansProjet() ? 1 : 0)
                .addValue("moduleProjet", perimetre.moduleProjet() ? 1 : 0);
    }

    private static List<String> nonVide(Collection<UUID> ids) {
        return textes(ids.isEmpty() ? List.of(AUCUN) : ids);
    }

    /**
     * Pièces du {@code perimetre} retenues par les filtres, solde calculé, sans tri ; {@code declencheur} non nul
     * restreint au déclencheur ({@code CfgTierTriggerId}).
     */
    List<DocumentAchatListItemDto> pieces(PerimetreAchat perimetre, FiltresDocumentsAchat f, UUID declencheur) {
        boolean renseignee = Boolean.TRUE.equals(f.dateRenseignee());
        MapSqlParameterSource p = parametres(perimetre)
                .addValue("toutesNatures", f.natures().isEmpty() ? 1 : 0)
                .addValue("natures", codes(f.natures()))
                .addValue("tousStatuts", f.statutIds().isEmpty() ? 1 : 0)
                // SQL Server convertit la liste même quand :tousStatuts = 1 : un UUID valide, jamais ''.
                .addValue("statuts", nonVide(f.statutIds()))
                .addValue("tousModules", f.modules().isEmpty() ? 1 : 0)
                .addValue("modules", codes(f.modules()))
                .addValue("tousTypes", f.typeLotIds().isEmpty() ? 1 : 0)
                .addValue("typesLot", nonVide(f.typeLotIds()))
                .addValue("lot", texte(f.lotId()), Types.VARCHAR)
                .addValue("fournisseur", texte(f.fournisseurId()), Types.VARCHAR)
                .addValue("archive", f.archive(), Types.BIT)
                .addValue("comptabilise", f.comptabilise(), Types.BIT)
                .addValue("montantDe", f.montantDe(), Types.DECIMAL)
                .addValue("montantA", f.montantA(), Types.DECIMAL)
                .addValue("declencheur", texte(declencheur), Types.VARCHAR)
                .addValue("echeance", f.champDate() == ChampDateAchat.ECHEANCE ? 1 : 0)
                .addValue("renseignee", renseignee ? 1 : 0)
                .addValue("du", renseignee ? null : f.du(), Types.DATE)
                .addValue("au", renseignee ? null : f.au(), Types.DATE)
                .addValue("motif", f.search() == null ? null
                        : HistoriqueService.motif(f.search().trim().toLowerCase(Locale.ROOT)), Types.NVARCHAR);
        return jdbc.query(LISTE, p, (rs, n) -> piece(rs));
    }

    private static List<String> codes(List<? extends Enum<?>> valeurs) {
        return valeurs.isEmpty() ? List.of("") : valeurs.stream().map(Enum::name).toList();
    }

    Optional<Entete> entete(PerimetreAchat perimetre, UUID id) {
        return jdbc.query(FICHE, parametres(perimetre).addValue(ID, id.toString()), (rs, n) -> new Entete(piece(rs),
                        chaine(rs, "Notes"), rs.getBigDecimal("AmountRegul"), rs.getBigDecimal("FiscalStamp"),
                        rs.getBoolean("IsValidatedOnAccounting"), chaine(rs, "EntiteCode"),
                        chaine(rs, "EntiteDescription"), horodatage(rs, "UserCreateDateTime"), chaine(rs, "UserCreate"),
                        horodatage(rs, "UserUpdateDateTime"), chaine(rs, "UserUpdate")))
                .stream().findFirst();
    }

    List<LigneDocumentAchatDto> lignes(UUID id) {
        return jdbc.query(LIGNES, Map.of(ID, id.toString()), (rs, n) -> new LigneDocumentAchatDto(uuid(rs, "Pkey"),
                (Integer) rs.getObject("Sorting"), chaine(rs, DESCRIPTION), chaine(rs, "Unity"),
                rs.getBigDecimal("Quantity"), rs.getBigDecimal("UnitPriceHT"), pourcent(rs.getBigDecimal("VatRatio")),
                pourcent(rs.getBigDecimal("DiscountRatio"))));
    }

    List<VentilationDocumentAchatDto> ventilation(UUID id) {
        return jdbc.query(VENTILATION, Map.of(ID, id.toString()), (rs, n) -> new VentilationDocumentAchatDto(
                chaine(rs, CODE), chaine(rs, DESCRIPTION), rs.getBigDecimal("VentilationRate")));
    }

    List<HistoriqueDocumentAchatDto> historique(UUID id) {
        return jdbc.query(HISTORIQUE, Map.of(ID, id.toString()), (rs, n) -> new HistoriqueDocumentAchatDto(
                horodatage(rs, DOC_DATE), chaine(rs, CODE), chaine(rs, DESCRIPTION), chaine(rs, "Utilisateur"),
                chaine(rs, "Notes")));
    }

    List<ReglementDocumentAchatDto> reglements(UUID id) {
        return jdbc.query(REGLEMENTS, Map.of(ID, id.toString()), (rs, n) -> new ReglementDocumentAchatDto(
                uuid(rs, PKEY), chaine(rs, CODE), jour(rs, DOC_DATE), chaine(rs, "TypeCode"),
                chaine(rs, "TypeDescription"), rs.getBigDecimal("Amount"), rs.getInt("Annule") == 1));
    }

    List<StatutAchatDto> statuts() {
        return jdbc.query(STATUTS, (rs, n) -> new StatutAchatDto(uuid(rs, PKEY), chaine(rs, CODE),
                chaine(rs, DESCRIPTION), rs.getBoolean("IsOkForPayement"), rs.getBoolean("IsValidatedOnAccounting"),
                (Integer) rs.getObject("Sorting")));
    }

    List<ProjetAchatDto> projets(Collection<UUID> projets) {
        return jdbc.query(PROJETS_VISIBLES, Map.of(PROJETS, textes(projets)), (rs, n) -> new ProjetAchatDto(
                uuid(rs, PKEY), chaine(rs, CODE), chaine(rs, DESCRIPTION)));
    }

    List<FournisseurAchatDto> fournisseurs(PerimetreAchat perimetre) {
        return jdbc.query(FOURNISSEURS, parametres(perimetre), (rs, n) -> new FournisseurAchatDto(
                uuid(rs, PKEY), chaine(rs, DESCRIPTION)));
    }

    List<SocieteAchatDto> societes(Collection<UUID> societes) {
        return jdbc.query(SOCIETES, Map.of("societes", textes(societes)), (rs, n) -> new SocieteAchatDto(
                uuid(rs, PKEY), chaine(rs, CODE), chaine(rs, DESCRIPTION)));
    }

    List<TypeLotAchatDto> typesLot() {
        return jdbc.query(TYPES_LOT, (rs, n) -> new TypeLotAchatDto(uuid(rs, PKEY), chaine(rs, CODE),
                chaine(rs, DESCRIPTION)));
    }

    private static DocumentAchatListItemDto piece(ResultSet rs) throws SQLException {
        SoldeDocumentAchat solde = SoldeDocumentAchat.de(rs.getBigDecimal("AmountToPay"), rs.getBigDecimal("Regle"));
        String nature = chaine(rs, "DocType");
        BigDecimal ht = rs.getBigDecimal("AmountHT");
        boolean aCloturer = AlerteClotureCommande.aCloturer(nature, ht, rs.getBigDecimal("HtFacture"),
                uuid(rs, "AdmWorkFlowStateTypeId"));
        return new DocumentAchatListItemDto(uuid(rs, PKEY), chaine(rs, CODE), nature,
                NatureDocumentAchat.libelleDe(nature), chaine(rs, "DocRef"), chaine(rs, DESCRIPTION),
                jour(rs, DOC_DATE), jour(rs, "MaturityDate"), uuid(rs, "CfgTierId"), chaine(rs, "FournisseurNom"),
                uuid(rs, "PrjProjectId"), chaine(rs, "ProjetCode"), uuid(rs, "PrjMarketId"), chaine(rs, "LotCode"),
                chaine(rs, "SocieteCode"), uuid(rs, "BuyDocumentStatusId"), chaine(rs, "StatutCode"),
                chaine(rs, "StatutDescription"), rs.getBoolean("IsOkForPayement"), ht,
                rs.getBigDecimal("AmountVat"), rs.getBigDecimal("AmountTTC"), solde.montantAPayer(),
                solde.montantRegle(), solde.resteARegler(), solde.solde(), solde.tropRegle(), solde.aDeduire(),
                uuid(rs, "DecompteId"), chaine(rs, "DecompteCode"), (Integer) rs.getObject("DecompteNumero"),
                chaine(rs, "SocieteNom"), chaine(rs, "ModuleCode"), chaine(rs, "ModuleLibelle"),
                uuid(rs, "PrjMarketTypeId"), chaine(rs, "TypeLotLibelle"), rs.getBoolean("IsClosed"),
                rs.getBoolean("IsAccounted"), chaine(rs, "OfficeOrderRef"), chaine(rs, "PersonToContact"),
                chaine(rs, "DossierGroupement"), chaine(rs, "Categories"), chaine(rs, "DeviseCode"), null, aCloturer,
                chaine(rs, "ColorName"));
    }

    static List<String> textes(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).toList();
    }

    static String texte(UUID id) {
        return id == null ? null : id.toString();
    }

    static String chaine(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null || valeur.isBlank() ? null : valeur;
    }

    static UUID uuid(ResultSet rs, String colonne) throws SQLException {
        String valeur = rs.getString(colonne);
        return valeur == null ? null : UUID.fromString(valeur);
    }

    private static LocalDateTime horodatage(ResultSet rs, String colonne) throws SQLException {
        return rs.getObject(colonne, LocalDateTime.class);
    }

    static LocalDate jour(ResultSet rs, String colonne) throws SQLException {
        LocalDateTime valeur = horodatage(rs, colonne);
        return valeur == null ? null : valeur.toLocalDate();
    }

    private static BigDecimal pourcent(BigDecimal fraction) {
        return fraction == null ? null : fraction.multiply(CENT).stripTrailingZeros();
    }
}
