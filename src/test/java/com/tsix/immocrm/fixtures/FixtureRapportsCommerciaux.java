package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderGift;
import com.tsix.immocrm.entities.com.ComGift;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComFolderGiftRepository;
import com.tsix.immocrm.repositories.com.ComGiftRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rapports commerciaux R3a (FEATURE-tdb-vente-rapports-commercial.md §10) : prospections qualifiées (type, source,
 * nature, action), interactions, tiers prospects, cadeaux de dossier, statuts de vente à seuil. Codes = libellés.
 */
@Component
public class FixtureRapportsCommerciaux {

    private final FixtureRapports rapports;
    private final FixtureSuiviCommerciaux suivi;
    private final FixtureEtatVentes etatVentes;
    private final DepotsRapportsCommerciaux depots;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    /** Dépôts écrits par la fixture (S107). */
    @Component
    public record DepotsRapportsCommerciaux(ComProspectionRepository prospections,
                                            ComProspectionOriginRepository origines,
                                            ComProspectionKindRepository natures, ComActionRepository actions,
                                            ComGiftRepository cadeaux, ComFolderGiftRepository cadeauxDossier,
                                            CfgTierRepository tiers, CfgTierTypeRepository typesTiers,
                                            StrStatusRepository statuts) {
    }

    public FixtureRapportsCommerciaux(FixtureRapports rapports, FixtureSuiviCommerciaux suivi,
                                      FixtureEtatVentes etatVentes, DepotsRapportsCommerciaux depots,
                                      JdbcTemplate jdbc, EntityManager entityManager) {
        this.rapports = rapports;
        this.suivi = suivi;
        this.etatVentes = etatVentes;
        this.depots = depots;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------ patrimoine et comptes

    public CfgProject projet(String code, boolean entiteActive) {
        return etatVentes.projet(code, entiteActive);
    }

    public CfgTranche tranche(String code, CfgProject projet) {
        return etatVentes.tranche(code, projet, true);
    }

    /** Compte de {@link FixtureRapports#compte} affecté à {@code projets} (sans affectation = sans restriction). */
    public AdmUser compte(String code, CfgProject[] projets, AdmRole... roles) {
        AdmUser user = rapports.compte(code, roles);
        // Le nom affiché d'un commercial (compte ou employé, description ou nom) contient toujours {@code code}.
        CfgTier employe = user.getCfgEmployee();
        employe.setDescription("EMP " + code);
        employe.setSocialReason("EMP " + code);
        depots.tiers().save(employe);
        for (CfgProject p : projets) {
            etatVentes.affecter(user, p);
        }
        return user;
    }

    /** Clé du commercial sur le fil ({@code CommerciauxVisibles.cle}) : son employé. */
    public static String cle(AdmUser commercial) {
        return commercial.getCfgEmployee().getId().toString();
    }

    // ------------------------------------------------------------------ prospections

    public ComProspectionOrigin origine(String code) {
        ComProspectionOrigin o = new ComProspectionOrigin();
        o.setId(UUID.randomUUID());
        o.setCode(code);
        o.setDescription(code);
        return depots.origines().save(o);
    }

    public ComProspectionKind nature(String code) {
        ComProspectionKind k = new ComProspectionKind();
        k.setId(UUID.randomUUID());
        k.setCode(code);
        k.setDescription(code);
        return depots.natures().save(k);
    }

    public ComActionMessageType typeAction(String code) {
        return suivi.type(code);
    }

    public ComActionMessageSubCategory resultat(String code) {
        return suivi.resultat(code);
    }

    public ComAction action(String code, LocalDateTime debut, LocalDateTime fin) {
        ComAction a = new ComAction();
        a.setId(UUID.randomUUID());
        a.setCode(code);
        a.setDescription(code);
        a.setStartDate(debut);
        a.setEndDate(fin);
        a.setIsActive(true);
        return depots.actions().save(a);
    }

    /** Prospection simple : seuls tranche (peut être nulle), commercial (peut être nul) et date. */
    public ComProspection prospection(CfgTranche tranche, AdmUser commercial, LocalDateTime date) {
        return suivi.prospection(tranche, suivi.personne("PRS-" + UUID.randomUUID().toString().substring(0, 8)),
                commercial, date);
    }

    /** Prospection qualifiée ; chaque qualification peut être nulle. */
    public ComProspection prospection(CfgTranche tranche, LocalDateTime date, ComActionMessageType type,
                                      ComProspectionOrigin origine, ComProspectionKind nature, ComAction action) {
        ComProspection p = prospection(tranche, null, date);
        p.setComActionMessageType(type);
        p.setComProspectionOrigin(origine);
        p.setComProspectionKind(nature);
        p.setComAction(action);
        return depots.prospections().save(p);
    }

    // ------------------------------------------------------------------ interactions

    /** Interaction de catégorie Prospection ; {@code UserName} = description du compte (même libellé partout). */
    public ComActionMessage interaction(AdmUser auteur, ComProspection prospection, ComActionMessageType type,
                                        ComActionMessageSubCategory resultat, LocalDateTime date) {
        ComActionMessage m = suivi.interaction(auteur, prospection, type, resultat, date);
        m.setUserName(auteur.getDescription());
        return suivi.enregistrer(m);
    }

    /** Interaction de catégorie Prospection sans compte auteur ({@code AdmUserId} et {@code UserName} nuls). */
    public ComActionMessage interactionSansAuteur(ComProspection prospection, ComActionMessageType type,
                                                  LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(prospection);
        m.setCfgTier(prospection.getCfgProspect());
        m.setComActionMessageType(type);
        m.setComActionMessageCategory(suivi.categorieProspection());
        m.setDocDate(date);
        m.setUserCreateDateTime(date);
        return suivi.enregistrer(m);
    }

    public ComActionMessage visiteVente(AdmUser auteur, ComFolder dossier, ComActionMessageType visite,
                                        LocalDateTime date) {
        return suivi.visiteVente(auteur, dossier, visite, FixtureSuiviCommerciaux.CATEGORIE_VENTE_2222, date);
    }

    public ComFolder vente(String code, AdmUser commercial, CfgTranche tranche, LocalDateTime date) {
        return suivi.vente(code, commercial, tranche, null, date, date, new java.math.BigDecimal("1000000"));
    }

    // ------------------------------------------------------------------ tiers prospects (n° 25)

    public CfgTierType typeTiers(String code) {
        return depots.typesTiers().findByCode(code).orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode(code);
            t.setDescription(code);
            return depots.typesTiers().save(t);
        });
    }

    /** Tiers du type {@code codeType}, commercial = employé de {@code commercial} (nul : non affecté). */
    public CfgTier tiers(String codeType, AdmUser commercial, LocalDateTime creeLe) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("TRS-" + UUID.randomUUID().toString().substring(0, 8));
        t.setFirstName("Prenom");
        t.setLastName(t.getCode());
        t.setCfgTierType(typeTiers(codeType));
        t.setCfgCommercial(commercial != null ? commercial.getCfgEmployee() : null);
        t.setUserCreateDateTime(creeLe);
        return depots.tiers().save(t);
    }

    // ------------------------------------------------------------------ dossiers et cadeaux (n° 16)

    /** Statut de vente propre au test, {@code Sorting} donné et {@code BoolField014} (finalisé) posé en SQL. */
    public StrStatus statutVente(String code, int ordre, boolean finalise) {
        StrStatus s = etatVentes.statutEnCours(code);
        s.setSorting(ordre);
        StrStatus sauve = depots.statuts().save(s);
        entityManager.flush();
        jdbc.update("UPDATE StrStatus SET BoolField014 = ? WHERE PKey = ?", finalise, sauve.getId().toString());
        return sauve;
    }

    /** {@code AdmParamsGuid} (clé = Code) posé ou remplacé dans la transaction du test. */
    public void parametreStatut(String code, StrStatus statut) {
        entityManager.flush();
        String valeur = statut.getId().toString();
        jdbc.update("""
                IF EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = ?)
                    UPDATE AdmParamsGuid SET CodeValue = ? WHERE Code = ?
                ELSE
                    INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (?, ?, NEWID())""",
                code, valeur, code, code, valeur);
    }

    /** Dossier de vente sur un nouvel article {@code codeArticle} de {@code tranche}, client avec CIN. */
    public ComFolder dossier(String codeArticle, CfgTranche tranche, StrStatus statut) {
        StkItem article = etatVentes.article(codeArticle, tranche, etatVentes.categorie("CAT-" + codeArticle),
                etatVentes.type("TYP-" + codeArticle));
        CfgTier client = etatVentes.client(codeArticle);
        client.setIDNumber("CIN-" + codeArticle);
        depots.tiers().save(client);
        return etatVentes.vendre(article, "DOS-" + codeArticle, statut, client, LocalDateTime.of(2025, 3, 3, 10, 0));
    }

    public ComGift cadeau(String code) {
        ComGift g = new ComGift();
        g.setId(UUID.randomUUID());
        g.setCode(code);
        g.setDescription(code);
        return depots.cadeaux().save(g);
    }

    /** Cadeau d'un dossier ; {@code cadeau} nul = {@code ComGiftId} NULL ; dates nulles = non livré / sans bon. */
    public ComFolderGift cadeauDossier(ComFolder dossier, ComGift cadeau, LocalDateTime livraison,
                                       LocalDateTime bonAchat) {
        ComFolderGift g = new ComFolderGift();
        g.setId(UUID.randomUUID());
        g.setComFolder(dossier);
        g.setComGift(cadeau);
        g.setDelivered(livraison != null);
        g.setDeliveryDate(livraison);
        g.setVoucherDate(bonAchat);
        return depots.cadeauxDossier().save(g);
    }

    public void relire() {
        rapports.relire();
    }
}
