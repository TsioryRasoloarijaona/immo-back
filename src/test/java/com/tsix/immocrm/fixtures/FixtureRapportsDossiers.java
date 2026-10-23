package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrStatusIntervener;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Rapports dossiers, clients, notaires (FEATURE-tdb-vente-rapports-dossiers.md §9) : paramètres {@code AdmParamsGuid},
 * types de règlement, module COM, règlements datés, pièces attachées, statuts ordonnés. Tout est annulé avec le test.
 */
@Component
public class FixtureRapportsDossiers {

    public static final String LISTE_DOSSIERS = "ComTdbFolderWithClientInfos";
    public static final String NOTAIRES_CREDITS = "ComSaleDashboardSuiviNotairesCredits";
    public static final String DELAIS_LIVRAISON = "ComSaleDashboardDeliveryDelay";
    public static final String REMISE_PIECES = "ComTdbRemisePiecesBancaires";
    public static final String VERIF_ATTACHEMENTS = "ComVerifAttachment";

    public static final String PARAM_CREDIT = "DefaultCreditDocumentType";
    public static final String PARAM_STATUT_NOTAIRE = "DefaultComFolderStatusLawyer";
    public static final String PARAM_SYNDIC = "DefaultSyndicFeeDocumentType";
    public static final String CHEQUE_NOTAIRE = "CHEQUE NOTAIRE";

    private static final UUID MODULE_COM = UUID.fromString("00002C2D-BDFA-4541-94F7-C708A0E00001");

    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final FixtureEtatVentes ev;

    public FixtureRapportsDossiers(JdbcTemplate jdbc, EntityManager em, FixtureEtatVentes ev) {
        this.jdbc = jdbc;
        this.em = em;
        this.ev = ev;
    }

    // ------------------------------------------------------------------ paramètres AdmParamsGuid

    /** Pose ou remplace le paramètre (clé = Code). */
    public void parametre(String code, UUID valeur) {
        em.flush();
        jdbc.update("""
                IF EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = ?)
                    UPDATE AdmParamsGuid SET CodeValue = ? WHERE Code = ?
                ELSE
                    INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, NEWID())""",
                code, valeur.toString(), code, code, valeur.toString());
    }

    public void retirerParametre(String code) {
        em.flush();
        jdbc.update("DELETE FROM AdmParamsGuid WHERE Code = ?", code);
    }

    // ------------------------------------------------------------------ statuts, notaires, échéances

    /** Statut de dossier propre au test, de {@code Sorting} donné, ni annulé ni désisté. */
    public StrStatus statut(String code, int ordre) {
        StrStatus s = ev.statutEnCours(code);
        s.setSorting(ordre);
        em.flush();
        return s;
    }

    /** Statut annulé ({@code BoolField002}) de {@code Sorting} élevé, comme ANNULATION en prod. */
    public StrStatus statutAnnule(String code) {
        StrStatus s = ev.statutSortie(code, true);
        s.setSorting(30);
        em.flush();
        return s;
    }

    public CfgTier notaire(String nom) {
        return ev.client(nom);
    }

    /** Catégorie d'échéance « crédit » ({@code IsSaleCredit}, au crédit) posée en {@code DefaultCreditDocumentType}. */
    public ComSettlementCategory categorieCredit() {
        ComSettlementCategory c = ev.categorieEcheance(FixtureEtatVentes.Nature.CREDIT, true);
        parametre(PARAM_CREDIT, c.getId());
        return c;
    }

    /** Catégorie « frais syndic » ({@code IsSaleExpense}, au crédit) posée en {@code DefaultSyndicFeeDocumentType}. */
    public ComSettlementCategory categorieSyndic() {
        ComSettlementCategory c = ev.categorieEcheance(FixtureEtatVentes.Nature.FRAIS, true);
        parametre(PARAM_SYNDIC, c.getId());
        return c;
    }

    public ComDocument echeance(ComFolder dossier, ComSettlementCategory categorie, BigDecimal montant) {
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setComSettlementCategory(categorie);
        d.setAmount(montant);
        d.setDocDate(LocalDateTime.of(2022, 1, 1, 0, 0));
        d.setExcludeFromPrice(Boolean.TRUE.equals(categorie.getIsSaleExpense()));
        em.persist(d);
        return d;
    }

    public void coAcquereur(ComFolder dossier, CfgTier client) {
        ComFolderCustomer co = new ComFolderCustomer();
        co.setId(UUID.randomUUID());
        co.setComFolder(dossier);
        co.setCfgTier(client);
        em.persist(co);
    }

    // ------------------------------------------------------------------ règlements

    /** Type de règlement par code, créé s'il manque (yesRemsTest n'en a aucun). */
    public StlSettlementType typeReglement(String code, boolean nonComptabilise) {
        em.flush();
        List<String> ids = jdbc.queryForList(
                "SELECT CAST(PKey AS varchar(36)) FROM StlSettlementType WHERE Code = ? ORDER BY PKey ASC",
                String.class, code);
        if (!ids.isEmpty()) {
            return em.find(StlSettlementType.class, UUID.fromString(ids.get(0)));
        }
        StlSettlementType t = new StlSettlementType();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setDescription(code);
        t.setIsNotContabilized(nonComptabilise);
        em.persist(t);
        return t;
    }

    /** Id du module de code {@code COM}, inséré s'il manque. */
    public UUID moduleVente() {
        em.flush();
        jdbc.update("""
                IF NOT EXISTS (SELECT 1 FROM AdmModule WHERE Code = N'COM')
                    INSERT INTO AdmModule (PKey, Code, Description) VALUES (?, N'COM', N'Vente')""",
                MODULE_COM.toString());
        return UUID.fromString(jdbc.queryForObject(
                "SELECT TOP 1 CAST(PKey AS varchar(36)) FROM AdmModule WHERE Code = N'COM' ORDER BY PKey ASC",
                String.class));
    }

    /** Module propre au test, de code quelconque (ni COM ni STL). */
    public UUID autreModule(String code) {
        em.flush();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmModule (PKey, Code, Description) VALUES (?, ?, ?)", id.toString(), code, code);
        return id;
    }

    /** Règlement d'en-tête seul ; les lignes s'ajoutent par {@link #ligne}. */
    public StlSettlement reglement(BigDecimal montant, boolean encaissement, StlSettlementType type,
                                   LocalDateTime dateDocument, LocalDateTime dateEcheance) {
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("RG-" + UUID.randomUUID().toString().substring(0, 8));
        s.setAmount(montant);
        s.setIsCredit(encaissement);
        s.setStlSettlementType(type);
        s.setDocDate(dateDocument);
        s.setEcheanceDate(dateEcheance);
        em.persist(s);
        return s;
    }

    public void ligne(StlSettlement reglement, ComDocument echeance, BigDecimal montant) {
        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(reglement);
        l.setComDocument(echeance);
        l.setAmount(montant);
        em.persist(l);
    }

    /** Règlement d'une seule ligne, du montant entier, sur {@code echeance}. */
    public StlSettlement regler(ComDocument echeance, BigDecimal montant, boolean encaissement,
                                StlSettlementType type, LocalDateTime dateDocument, LocalDateTime dateEcheance) {
        StlSettlement s = reglement(montant, encaissement, type, dateDocument, dateEcheance);
        ligne(s, echeance, montant);
        return s;
    }

    // ------------------------------------------------------------------ pièces attachées

    public AdmAttachedFileType typePiece(String libelle) {
        AdmAttachedFileType t = new AdmAttachedFileType();
        t.setId(UUID.randomUUID());
        t.setCode(libelle);
        t.setDescription(libelle);
        em.persist(t);
        return t;
    }

    /** Ligne {@code AdmAttachedFile} rattachée par {@code CategoryId} ; {@code type} peut être nul. */
    public void piece(UUID porteurId, AdmAttachedFileType type, String category, boolean cocheeNonJointe) {
        em.flush();
        jdbc.update("""
                INSERT INTO AdmAttachedFile (PKey, Category, CategoryId, AdmAttachedFileTypeId, Description,
                    IsCheckedButNotAttached)
                VALUES (?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID().toString(), category, porteurId.toString(),
                type == null ? null : type.getId().toString(), "TST-PIECE", cocheeNonJointe);
    }

    // ------------------------------------------------------------------ statuts visibles (StatutsVisiblesListe)

    /** Intervenant de statut : {@code statut} visible des comptes du rôle {@code role} dans la liste des dossiers. */
    public void statutVisiblePar(StrStatus statut, AdmRole role) {
        StrIntervener i = new StrIntervener();
        i.setId(UUID.randomUUID());
        i.setAdmRoleId(role.getId());
        i.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        em.persist(i);
        StrStatusIntervener si = new StrStatusIntervener();
        si.setId(UUID.randomUUID());
        si.setStrIntervenerId(i.getId());
        si.setStrStatusId(statut.getId());
        em.persist(si);
    }
}
