package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lignes lues par les onglets de FEATURE-admin-onglets-tiers.md, en JDBC dans la transaction du test (rollback). Parents
 * d'abord (FK de la CI) ; dossiers sans article (aucun trigger de dossier vivant). {@link #preparer()} exige un test
 * {@code @Transactional} : le DDL posé sur yesRemsTest est annulé au rollback.
 */
@Component
public class FixtureOngletsTiers {

    public static final String ONGLET = "/api/administration/tiers/{famille}/{id}/onglets/{onglet}";
    public static final String EXPORT = "/api/administration/tiers/{famille}/export";

    public static final String PROSPECTIONS = "prospections";
    public static final String DOSSIERS = "dossiers";
    public static final String RECETTES = "recettes";
    public static final String DEPENSES = "depenses";
    public static final String REGLEMENTS = "reglements";
    public static final String CONGES = "conges";
    public static final String HISTORIQUE = "historique";

    /** Onglets des fournisseurs, contacts et personnels ; {@link #CONGES} en plus pour les personnels. */
    public static final List<String> ONGLETS_COMMUNS = List.of(PROSPECTIONS, DOSSIERS, RECETTES, DEPENSES, REGLEMENTS,
            HISTORIQUE);
    public static final List<String> TOUS_ONGLETS = List.of(PROSPECTIONS, DOSSIERS, RECETTES, DEPENSES, REGLEMENTS,
            CONGES, HISTORIQUE);

    // GrhVacation : absente de yesRemsTest et de la CI ; définition de yesRemsProd.
    private static final String TABLE_CONGES = """
            IF OBJECT_ID('dbo.GrhVacation', 'U') IS NULL
            CREATE TABLE dbo.GrhVacation (
                PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL,
                DocDate datetime NULL, DateFrom datetime NULL, DateTo datetime NULL, DaysNumber decimal(26, 6) NULL,
                Notes nvarchar(500) NULL, GrhVacationTypeId uniqueidentifier NULL, GrhEmployeeId uniqueidentifier NULL,
                CfgCompanyId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
                UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, StrEntityId uniqueidentifier NULL,
                StrStatusId uniqueidentifier NULL, GrhVacationGroupGrhEmployeeId uniqueidentifier NULL)""";

    // StlDocument de yesRemsTest est réduite (PKey, Code, Description, DocType, DocDate) ; la CI a la table complète.
    private static final List<String> COLONNES_RECETTES = List.of("""
            IF COL_LENGTH('dbo.StlDocument', 'CfgTierId') IS NULL
            ALTER TABLE dbo.StlDocument ADD CfgTierId uniqueidentifier NULL""", """
            IF COL_LENGTH('dbo.StlDocument', 'MaturityDate') IS NULL
            ALTER TABLE dbo.StlDocument ADD MaturityDate datetime NULL""");

    private final JdbcTemplate jdbc;

    public FixtureOngletsTiers(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public static MockHttpServletRequestBuilder onglet(String famille, UUID id, String onglet, RequestPostProcessor qui) {
        return get(ONGLET, famille, id, onglet).with(qui);
    }

    public static List<String> ids(String page) {
        return JsonPath.read(page, "$.content[*].id");
    }

    public static long total(String page) {
        return ((Number) JsonPath.read(page, "$.totalElements")).longValue();
    }

    /** Tables et colonnes absentes de yesRemsTest ; à appeler dans le {@code setUp} d'un test {@code @Transactional}. */
    public void preparer() {
        jdbc.execute(TABLE_CONGES);
        COLONNES_RECETTES.forEach(jdbc::execute);
    }

    /** Une ligne de l'onglet pour ce tiers, datée {@code date} sur toutes les colonnes de date candidates. */
    public UUID ligne(Jeu j, String onglet, UUID tiers, LocalDate date) {
        return switch (onglet) {
            case PROSPECTIONS -> prospection(j, tiers, date);
            case DOSSIERS -> dossierVente(j, tiers, date);
            case RECETTES -> recette(j, tiers, date);
            case DEPENSES -> depense(j, tiers, date);
            case REGLEMENTS -> reglement(j, tiers, null, date);
            case CONGES -> conge(j, tiers, date);
            case HISTORIQUE -> historique(tiers, date);
            default -> throw new IllegalArgumentException(onglet);
        };
    }

    public UUID prospection(Jeu j, UUID tiers, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ComProspection (PKey, Code, DocDate, CfgProspectId) VALUES (?, ?, ?, ?)", id.toString(),
                j.code("PRO"), jour(date), tiers.toString());
        return id;
    }

    /** Dossier de vente sans article ni statut, {@code CfgCustomerId} = client. */
    public UUID dossierVente(Jeu j, UUID client, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ComFolder (PKey, Code, Description, DocDate, StkVocationId, CfgCustomerId)
                VALUES (?, ?, ?, ?, ?, ?)""", id.toString(), j.code("DV"), "Dossier " + j.code("DV"), jour(date),
                j.parent("StkVocation").toString(), client.toString());
        return id;
    }

    public UUID dossierLocation(Jeu j, UUID client, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO RntFolder (PKey, Code, Description, DocDate, CfgCustomerId) VALUES (?, ?, ?, ?, ?)",
                id.toString(), j.code("DL"), "Location " + j.code("DL"), jour(date), client.toString());
        return id;
    }

    /** Ligne {@code ComFolderCustomer} : {@code tiers} co-acquéreur du dossier. */
    public UUID coAcquereur(UUID dossier, UUID tiers) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ComFolderCustomer (PKey, ComFolderId, CfgTierId) VALUES (?, ?, ?)", id.toString(),
                dossier.toString(), tiers.toString());
        return id;
    }

    public UUID recette(Jeu j, UUID tiers, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO StlDocument (PKey, Code, Description, DocDate, MaturityDate, CfgTierId) VALUES (?, ?, ?, ?, ?, ?)",
                id.toString(), j.code("REC"), "Recette " + j.code("REC"), jour(date), jour(date), tiers.toString());
        return id;
    }

    /** Facture payable ({@code DocType = 'INVOICE'}, {@code CanBePayed = 1}) : le filtre de la vue OMRAN ne l'écarte pas. */
    public UUID depense(Jeu j, UUID tiers, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO BuyDocument (PKey, Code, Description, DocType, CanBePayed, IsClosed, DocDate, MaturityDate,
                                         CfgTierId, AmountHT, AmountTTC, AmountToPay)
                VALUES (?, ?, ?, 'INVOICE', 1, 0, ?, ?, ?, ?, ?, ?)""", id.toString(), j.code("DEP"),
                "Depense " + j.code("DEP"), jour(date), jour(date), tiers.toString(), new BigDecimal("1000"),
                new BigDecimal("1200"), new BigDecimal("1200"));
        return id;
    }

    /** Règlement dont le tiers est {@code TierId} et/ou {@code TierPayerId} ({@code null} = colonne vide). */
    public UUID reglement(Jeu j, UUID tiers, UUID payeur, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO StlSettlement (PKey, Code, Description, DocDate, SettlementDate, EcheanceDate, Amount, TierId,
                                           TierPayerId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", id.toString(), j.code("REG"), "Reglement " + j.code("REG"),
                jour(date), jour(date), jour(date), new BigDecimal("500"), texte(tiers), texte(payeur));
        return id;
    }

    public UUID conge(Jeu j, UUID employe, LocalDate du) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO GrhVacation (PKey, Code, Description, DocDate, DateFrom, DateTo, DaysNumber, Notes, GrhEmployeeId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", id.toString(), j.code("CNG"), "Conge " + j.code("CNG"), jour(du),
                jour(du), jour(du.plusDays(2)), new BigDecimal("3"), "Note conge", employe.toString());
        return id;
    }

    public UUID historique(UUID tiers, LocalDate date) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO StrFolderStatusHistory (PKey, DocDate, Description, CfgTierId) VALUES (?, ?, ?, ?)",
                id.toString(), jour(date), "Historique", tiers.toString());
        return id;
    }

    private static Timestamp jour(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private static String texte(UUID id) {
        return id == null ? null : id.toString();
    }
}
