package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lignes du lot P7a (FEATURE-patrimoine-terrains.md §6.1) en SQL natif : les tests ne dépendent pas des entités que le
 * lot crée. Configuration semée si absente, à ses PKey de production ; flush avant chaque ordre (la CI porte les FK).
 * Les routes et noms de champs ci-dessous sont le contrat du fil.
 */
@Component
public class FixtureTerrains {

    // ------------------------------------------------------------------ routes
    public static final String TERRAINS = "/api/terrains";
    public static final String TERRAIN = "/api/terrains/{id}";
    public static final String FRAIS = "/api/terrains/{id}/frais";
    public static final String VALIDER = "/api/terrains/{id}/frais/{fraisId}/valider";
    public static final String SOUS_TERRAINS = "/api/terrains/{id}/sous-terrains";
    public static final String LOTISSEMENTS = "/api/lotissements";

    // ------------------------------------------------------------------ champs du corps (terrain)
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String ENTITE_ID = "entiteId";
    public static final String PAYS_ID = "paysId";
    public static final String VILLE_ID = "villeId";
    public static final String NATURE_ID = "natureId";
    public static final String VOCATION_ID = "vocationId";
    public static final String CATEGORIE_ID = "categorieId";
    public static final String TYPE_ID = "typeId";
    public static final String SURFACE_PROVISOIRE = "surfaceProvisoire";
    public static final String SURFACE_DEFINITIVE = "surfaceDefinitive";
    public static final String PRIX_M2_PROVISOIRE_HT = "prixM2ProvisoireHT";
    public static final String PRIX_M2_DEFINITIF_HT = "prixM2DefinitifHT";
    public static final String PRIX_M2_DEFINITIF_TTC = "prixM2DefinitifTTC";
    public static final String TAUX_TVA = "tauxTva";
    public static final String TITRE_FONCIER = "titreFoncier";
    public static final String FOURNISSEUR_ID = "fournisseurId";
    public static final String PRIX_ACHAT_M2 = "prixAchatM2";
    public static final String PRIX_ACHAT = "prixAchat";
    public static final String COUT_TOTAL = "coutTotal";
    public static final String COUT_UNITAIRE_FINAL = "coutUnitaireFinal";

    // ------------------------------------------------------------------ champs d'une ligne de frais
    public static final String ID = "id";
    public static final String DATE = "date";
    public static final String MONTANT = "montant";
    public static final String NOTES = "notes";
    public static final String TYPE_FRAIS_ID = "typeFraisId";

    // ------------------------------------------------------------------ PKey de production
    public static final UUID ENTITE_LAND_PROJECT = UUID.fromString("41512D17-461E-43B8-8735-E03E69E9226C");
    public static final UUID STATUT_ACQUIS = UUID.fromString("75C1FE20-2711-4C49-8D9D-531955E74460");
    public static final UUID NATURE_TER = FixtureReferentielsAdmin.NATURE_TER;
    public static final UUID MODULE_STL = UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00004");
    public static final UUID ENTITE_FACTURE_TERRAIN = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B605");
    public static final UUID STATUT_EN_COURS = UUID.fromString("364DCDC4-A9AA-4CAD-A1B0-60D3310FDE69");
    public static final UUID STATUT_REGLE = UUID.fromString("B0750D33-6F60-4D4D-B446-35E5B996FD7C");
    public static final UUID TRANSITION_EN_COURS_REGLE = UUID.fromString("6C8FEF18-6F36-44C3-AB86-0B46C4230AC9");
    public static final UUID SEQUENCE_FACTURE = UUID.fromString("50C4F768-BE0C-49AA-A6FD-F784D28F2F3A");
    public static final String CODE_SEQUENCE = "Facture fournisseur";
    public static final String MESSAGE_DEJA_VALIDE =
            "Vous ne pouvez pas ajouter une facture pour un frais qui est déjà validé !";

    private static final String P_ID = "id";
    private static final String P_CODE = "code";
    private static final String P_DESCRIPTION = "description";
    private static final String P_ENTITE = "entite";
    private static final String TABLE_WORKFLOW = "BuyDocumentWorkFlow";

    private final EntityManager entityManager;
    private final FixturePatrimoine fixture;

    public FixtureTerrains(EntityManager entityManager, FixturePatrimoine fixture) {
        this.entityManager = entityManager;
        this.fixture = fixture;
    }

    /** Références d'un terrain de test ; {@code jeton} suffixe tous les codes (yesRemsTest est partagée). */
    public record Contexte(String jeton, CfgCompany entite, AdmCountry pays, UUID ville, StkKind natureTer,
                           StkItemCategory categorie, StkItemType type) {
    }

    public static String jeton(String prefixe) {
        return prefixe + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public Contexte contexte(String prefixe) {
        fixture.referentielArticle();
        configurationTerrain();
        String jeton = jeton(prefixe);
        StkKind ter = natureTer();
        AdmCountry pays = fixture.pays("PAYS-" + jeton);
        return new Contexte(jeton, fixture.entite("ENT-" + jeton, true), pays, ville("VIL-" + jeton, pays.getId()), ter,
                fixture.categorie("CAT-" + jeton, ter, false), fixture.type("TYP-" + jeton, ter, false));
    }

    /** Nature Terrain (code TER) à sa PKey de production, créée si absente. */
    public StkKind natureTer() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StkKind WHERE PKey = :id)
                            INSERT INTO StkKind (PKey, Code, Description) VALUES (:id, N'TER', N'Terrain')""")
                .setParameter(P_ID, NATURE_TER));
        return entityManager.find(StkKind.class, NATURE_TER);
    }

    // ------------------------------------------------------------------ entité LandProject et ses statuts

    /** Entité {@code LandProject} et un statut « Acquis » (BoolField001 NULL) si l'entité n'en a aucun. */
    public void configurationTerrain() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'LandProject')
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'LandProject', N'Terrains')""")
                .setParameter(P_ID, ENTITE_LAND_PROJECT));
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrStatus WHERE StrConfigEntityId = :entite)
                            INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, BoolField001)
                            VALUES (:id, N'Acquis', N'Acquis', 10, :entite, NULL)""")
                .setParameter(P_ID, STATUT_ACQUIS).setParameter(P_ENTITE, entiteLandProject()));
    }

    public UUID entiteLandProject() {
        return premierGuid("SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'LandProject' ORDER BY PKey ASC");
    }

    /** Premier statut de l'entité LandProject par {@code Sorting}, tel que la base le donne. */
    public UUID statutInitial() {
        return premierGuid("""
                SELECT TOP 1 s.PKey FROM StrStatus s JOIN StrEntity e ON e.PKey = s.StrConfigEntityId
                WHERE e.Code = N'LandProject' ORDER BY s.Sorting ASC, s.PKey ASC""");
    }

    public UUID statutTerrain(String code, int ordre, Boolean boolField001) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, BoolField001)
                        VALUES (:id, :code, :description, :ordre, :entite, :bool)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Statut " + code)
                .setParameter("ordre", ordre).setParameter(P_ENTITE, entiteLandProject())
                .setParameter("bool", boolField001));
        return id;
    }

    // ------------------------------------------------------------------ terrains

    /** Terrain complet (nature TER, vocation PROJET, sans tranche) au premier statut lu en base ; 1 000 m². */
    public UUID terrain(Contexte c, String code) {
        return terrain(c, code, null, fixture.vocationProjet().getId());
    }

    /** Terrain rattaché à une tranche (lot de terrain) ou non ({@code trancheId} nul). */
    public UUID terrain(Contexte c, String code, UUID trancheId, UUID vocationId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkItem (PKey, Code, Description, StkKindId, StkVocationId, StkItemCategoryId,
                            StkItemTypeId, StkHierarchyId, Area, AreaFloor, Vat, IsFinished, IsUnderItem, IsUnderItemOrigin,
                            IsTitled, AlwaysAffected, CfgCompanyId, AdmCountryId, AdmCityId, StrStatusId, StrEntityId)
                        VALUES (:id, :code, :description, :nature, :vocation, :categorie, :type, :tranche, 1000, 1000, 0.2,
                            1, 0, 0, 1, 0, :entite, :pays, :ville, :statut, :strEntite)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Terrain " + code)
                .setParameter("nature", c.natureTer().getId()).setParameter("vocation", vocationId)
                .setParameter("categorie", c.categorie().getId()).setParameter("type", c.type().getId())
                .setParameter("tranche", trancheId).setParameter(P_ENTITE, c.entite().getId())
                .setParameter("pays", c.pays().getId()).setParameter("ville", c.ville())
                .setParameter("statut", statutInitial()).setParameter("strEntite", entiteLandProject()));
        return id;
    }

    /** {@code colonne} : un nom de colonne écrit par le test, jamais une saisie. */
    public void definir(UUID articleId, String colonne, Object valeur) {
        executer(entityManager.createNativeQuery("UPDATE StkItem SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter("valeur", valeur).setParameter(P_ID, articleId));
    }

    public UUID ville(String code, UUID paysId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmCity (PKey, Code, Description, AdmCountryId) VALUES (:id, :code, :description, :pays)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Ville " + code)
                .setParameter("pays", paysId));
        return id;
    }

    /** {@code table} : LndLevel ou LndVocation (Code, Description). */
    public UUID referenceFonciere(String table, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO " + table + " (PKey, Code, Description) VALUES (:id, :code, :description)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Libelle " + code));
        return id;
    }

    // ------------------------------------------------------------------ fournisseurs, types de frais, frais

    /** Tiers fournisseur ; {@code delai} = CfgTier.PaymentDelay (jours), nul = aucun. */
    public UUID fournisseur(String code, Integer delai) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO CfgTier (PKey, Code, Description, PaymentDelay) VALUES (:id, :code, :description, :delai)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Fournisseur " + code)
                .setParameter("delai", delai));
        return id;
    }

    public record TypeFrais(UUID id, String code, String description, UUID categorieComptable, UUID typeMarche) {
    }

    /** Type de frais avec ({@code complet}) ou sans catégorie comptable et type de marché. */
    public TypeFrais typeFrais(String jeton, boolean avecCategorie, boolean avecMarche) {
        UUID categorie = avecCategorie ? codeDescription("StlCategory", "CC-" + jeton) : null;
        UUID marche = avecMarche ? codeDescription("PrjMarketType", "MT-" + jeton) : null;
        UUID id = UUID.randomUUID();
        String code = "TF-" + jeton;
        String description = "Frais notaire " + jeton;
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkExpenseType (Pkey, Code, Description, StlCategoryId, PrjMarketTypeId)
                        VALUES (:id, :code, :description, :categorie, :marche)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, description)
                .setParameter("categorie", categorie).setParameter("marche", marche));
        return new TypeFrais(id, code, description, categorie, marche);
    }

    public UUID frais(UUID terrainId, UUID typeId, UUID fournisseurId, BigDecimal montant, boolean valide) {
        return frais(terrainId, typeId, fournisseurId, montant, valide, "");
    }

    public UUID frais(UUID terrainId, UUID typeId, UUID fournisseurId, BigDecimal montant, boolean valide,
                      String notes) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkExpense (Pkey, [Date], Amount, IsValidated, Notes, CfgSupplierId, StkExpenseTypeId,
                            StkItemId)
                        VALUES (:id, :date, :montant, :valide, :notes, :fournisseur, :type, :terrain)""")
                .setParameter(P_ID, id).setParameter("date", LocalDate.of(2026, 3, 15)).setParameter("montant", montant)
                .setParameter("valide", valide).setParameter("notes", notes).setParameter("fournisseur", fournisseurId)
                .setParameter("type", typeId).setParameter("terrain", terrainId));
        return id;
    }

    /** Facture minimale portant ce frais (cas OMRAN : facture sans IsValidated, ou frais déjà facturé). */
    public UUID factureExistante(UUID fraisId, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO BuyDocument (PKey, Code, DocType, ReferenceType, StkExpenseId)
                        VALUES (:id, :code, N'INVOICE', N'LAND_EXPENSE', :frais)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("frais", fraisId));
        return id;
    }

    // ------------------------------------------------------------------ configuration de la facture (§3.1.5)

    /** Module STL, entité StkInvoiceDebitLand, statuts EN COURS / REGLÉ et leur transition, séquence, devise. */
    public void configurationFacture() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmModule WHERE Code = N'STL')
                            INSERT INTO AdmModule (PKey, Code, Description) VALUES (:id, N'STL', N'Trésorerie')""")
                .setParameter(P_ID, MODULE_STL));
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'StkInvoiceDebitLand')
                            INSERT INTO StrEntity (PKey, Code, Description)
                            VALUES (:id, N'StkInvoiceDebitLand', N'Facture frais sur terrain')""")
                .setParameter(P_ID, ENTITE_FACTURE_TERRAIN));
        statutFacture(STATUT_EN_COURS, "EN COURS", 10);
        statutFacture(STATUT_REGLE, "REGLÉ", 20);
        if (tableExiste(TABLE_WORKFLOW) && compter(
                "SELECT COUNT(*) FROM BuyDocumentWorkFlow WHERE StrEntityId = :id", entiteFactureTerrain()) == 0) {
            transitionFacture(TRANSITION_EN_COURS_REGLE, STATUT_EN_COURS, STATUT_REGLE);
        }
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmSequence WHERE Code = N'Facture fournisseur')
                            INSERT INTO AdmSequence (PKey, Code, SeqYear, SeqYearNumber, SeqValue, SeqValueNumber, Separator,
                                BasedOnSystemYear, CfgCompanyId, SeqPrefix, SeqSuffix)
                            VALUES (:id, N'Facture fournisseur', 2021, 2, 3013, 5, N'/', 1, NULL, N'', N'')""")
                .setParameter(P_ID, SEQUENCE_FACTURE));
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = N'DefaultCurrency')
                            INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (N'DefaultCurrency', :devise, :id)""")
                .setParameter("devise", FixtureEntites.DEVISE_MAD).setParameter(P_ID, UUID.randomUUID()));
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StlCurrency WHERE PKey = :id)
                            INSERT INTO StlCurrency (PKey, Code, Description) VALUES (:id, N'MAD', N'MAD')""")
                .setParameter(P_ID, deviseParDefaut()));
    }

    private void statutFacture(UUID id, String code, int ordre) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM BuyDocumentStatus WHERE PKey = :id)
                            INSERT INTO BuyDocumentStatus (PKey, Code, Description, Sorting, IsOkForPayement)
                            VALUES (:id, :code, :code, :ordre, 0)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("ordre", ordre));
    }

    public UUID statutFacture(String code) {
        UUID id = UUID.randomUUID();
        statutFacture(id, code, 1);
        return id;
    }

    /** Sans effet tant que 120_terrains_tables.sql n'a pas créé la table (yesRemsTest avant le lot). */
    public void transitionFacture(UUID id, UUID depuis, UUID vers) {
        if (!tableExiste(TABLE_WORKFLOW)) {
            return;
        }
        executer(entityManager.createNativeQuery("""
                        INSERT INTO BuyDocumentWorkFlow (PKey, Sorting, StartStatusId, EndStatusId, StrEntityId, Description)
                        VALUES (:id, 10, :depuis, :vers, :entite, N'En cours => Validé(e)')""")
                .setParameter(P_ID, id).setParameter("depuis", depuis).setParameter("vers", vers)
                .setParameter(P_ENTITE, entiteFactureTerrain()));
    }

    public UUID moduleStl() {
        return premierGuid("SELECT TOP 1 PKey FROM AdmModule WHERE Code = N'STL' ORDER BY PKey ASC");
    }

    public UUID entiteFactureTerrain() {
        return premierGuid("SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'StkInvoiceDebitLand' ORDER BY PKey ASC");
    }

    public UUID deviseParDefaut() {
        return premierGuid("SELECT TOP 1 CodeValue FROM AdmParamsGuid WHERE Code = N'DefaultCurrency'");
    }

    /** Réécrit la ligne « Facture fournisseur » (transaction de test annulée ensuite). */
    public void sequence(Sequence s) {
        executer(entityManager.createNativeQuery("""
                        UPDATE AdmSequence SET SeqValue = :valeur, SeqPrefix = :prefixe, Separator = :separateur,
                            SeqSuffix = :suffixe, SeqValueNumber = :largeurNumero, SeqYearNumber = :largeurAnnee,
                            BasedOnSystemYear = :anneeSysteme, SeqYear = :annee
                        WHERE Code = N'Facture fournisseur'""")
                .setParameter("valeur", s.valeur()).setParameter("prefixe", s.prefixe())
                .setParameter("separateur", s.separateur()).setParameter("suffixe", s.suffixe())
                .setParameter("largeurNumero", s.largeurNumero()).setParameter("largeurAnnee", s.largeurAnnee())
                .setParameter("anneeSysteme", s.anneeSysteme()).setParameter("annee", s.annee()));
    }

    public record Sequence(int valeur, String prefixe, String separateur, String suffixe, int largeurNumero,
                           int largeurAnnee, boolean anneeSysteme, int annee) {

        /** Valeurs mesurées en production le 20/10 (§3.3). */
        public static Sequence commeEnProduction(int valeur) {
            return new Sequence(valeur, "", "/", "", 5, 2, true, 2021);
        }
    }

    public int valeurSequence() {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(
                        "SELECT TOP 1 SeqValue FROM AdmSequence WHERE Code = N'Facture fournisseur' ORDER BY PKey ASC")
                .getSingleResult();
        return ((Number) n).intValue();
    }

    /** AdmUser.CfgEmployeeId : l'employé (CfgTier) du compte, source de BuyDocument.CfgTierTriggerId. */
    public void employeDuCompte(UUID admUserId, UUID tierId) {
        executer(entityManager.createNativeQuery("UPDATE AdmUser SET CfgEmployeeId = :tier WHERE PKey = :id")
                .setParameter("tier", tierId).setParameter(P_ID, admUserId));
    }

    // ------------------------------------------------------------------ outils

    /** Ordre SQL écrit par le test (masquer une configuration, etc.), sans paramètre. */
    public void executer(String sql) {
        executer(entityManager.createNativeQuery(sql));
    }

    public boolean tableExiste(String table) {
        entityManager.flush();
        Object id = entityManager.createNativeQuery("SELECT OBJECT_ID(:table, 'U')")
                .setParameter("table", "dbo." + table).getSingleResult();
        return id != null;
    }

    public int articlesDeCode(String code) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery("SELECT COUNT(*) FROM StkItem WHERE Code = :code")
                .setParameter(P_CODE, code).getSingleResult();
        return ((Number) n).intValue();
    }

    public int compter(String sql, UUID id) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery(sql).setParameter(P_ID, id).getSingleResult();
        return ((Number) n).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Tuple> lignes(String sql, UUID id) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.createNativeQuery(sql, Tuple.class).setParameter(P_ID, id).getResultList();
    }

    public Tuple ligne(String sql, UUID id) {
        List<Tuple> lignes = lignes(sql, id);
        if (lignes.size() != 1) {
            throw new IllegalStateException(lignes.size() + " ligne(s) pour " + sql);
        }
        return lignes.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<String> guids(String sql, UUID id) {
        entityManager.flush();
        List<Object> valeurs = entityManager.createNativeQuery(sql).setParameter(P_ID, id).getResultList();
        return valeurs.stream().map(FixturePatrimoine::guid).toList();
    }

    /** Une colonne datetime peut revenir en Timestamp ou en LocalDateTime selon le pilote. */
    public static LocalDateTime horodatage(Object valeur) {
        return switch (valeur) {
            case null -> null;
            case LocalDateTime l -> l;
            case Timestamp t -> t.toLocalDateTime();
            default -> LocalDateTime.parse(valeur.toString().replace(' ', 'T'));
        };
    }

    private UUID premierGuid(String sql) {
        entityManager.flush();
        List<?> valeurs = entityManager.createNativeQuery(sql).getResultList();
        return valeurs.isEmpty() || valeurs.get(0) == null ? null : UUID.fromString(valeurs.get(0).toString());
    }

    private UUID codeDescription(String table, String code) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery(
                        "INSERT INTO " + table + " (PKey, Code, Description) VALUES (:id, :code, :description)")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter(P_DESCRIPTION, "Libelle " + code));
        return id;
    }

    /** Sans clear : l'écouteur d'audit relit les références des entités en attente à la validation. */
    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
    }
}
