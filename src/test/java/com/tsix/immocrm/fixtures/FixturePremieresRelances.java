package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Premières relances d'une tranche (FEATURE-patrimoine-import-export.md §4.1, lot P9d) : tranche datée
 * ({@code Revival1Date}, {@code AgreementFirstDelay}), dossiers portés par un article racine ({@code StkItem.ComFolderId}),
 * statuts propres au test (annulation {@code BoolField002}, désistement {@code BoolField003}), prospection, client,
 * commercial. Parents avant enfants (FK de la CI), un dossier vivant par article (trigger de la CI).
 */
@Component
public class FixturePremieresRelances {

    /** {@code ComActionMessageType} « Note » (PKey de production), absent de yesRemsTest. */
    public static final UUID TYPE_NOTE = UUID.fromString("8EE7C999-F031-445A-B92F-0F75B090B7E6");
    public static final String PARAMETRE_RESERVATION = "DefaultComFolderStatusReservation";
    public static final LocalDateTime DATE_RELANCE = LocalDateTime.of(2026, 3, 1, 0, 0);
    public static final int PREMIER_DELAI = 45;
    /** {@link #DATE_RELANCE} + {@link #PREMIER_DELAI} jours. */
    public static final LocalDateTime ECHEANCE = LocalDateTime.of(2026, 4, 15, 0, 0);

    private final FixtureEtatVentes ev;
    private final FixtureFluxVente flux;
    private final EntityManager em;

    public FixturePremieresRelances(FixtureEtatVentes ev, FixtureFluxVente flux, EntityManager em) {
        this.ev = ev;
        this.flux = flux;
        this.em = em;
    }

    /** Tranche d'un test et son référentiel d'article. */
    public record Jeu(String jeton, CfgProject projet, CfgTranche tranche, StkItemCategory categorie, StkItemType type) {
    }

    public static String jeton() {
        return "P9D" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    /** Tranche visible (entité active) datée : 1re relance {@link #DATE_RELANCE}, 1er délai {@link #PREMIER_DELAI}. */
    public Jeu jeu() {
        return jeu(true);
    }

    public Jeu jeu(boolean entiteActive) {
        String jeton = jeton();
        CfgProject projet = ev.projet("PRJ-" + jeton, entiteActive);
        CfgTranche tranche = ev.tranche("TR-" + jeton, projet, true);
        tranche.setRevival1Date(DATE_RELANCE);
        tranche.setAgreementFirstDelay(PREMIER_DELAI);
        CfgTranche sauvee = em.merge(tranche);
        em.flush();
        return new Jeu(jeton, projet, sauvee, ev.categorie("CAT-" + jeton), ev.type("TYP-" + jeton));
    }

    public CfgTranche sansDateDeRelance(CfgTranche tranche) {
        tranche.setRevival1Date(null);
        CfgTranche sauvee = em.merge(tranche);
        em.flush();
        return sauvee;
    }

    // ------------------------------------------------------------------ statuts

    public StrStatus enCours(Jeu j, String code) {
        return ev.statutEnCours(code + "-" + j.jeton());
    }

    public StrStatus annulation(Jeu j) {
        return ev.statutSortie("ANN-" + j.jeton(), true);
    }

    public StrStatus desistement(Jeu j) {
        return ev.statutSortie("DES-" + j.jeton(), false);
    }

    /** {@code AdmParamsGuid.DefaultComFolderStatusReservation} = {@code statut} (posé dans la transaction du test). */
    public void statutReservation(StrStatus statut) {
        flux.parametre(PARAMETRE_RESERVATION, statut.getId());
    }

    // ------------------------------------------------------------------ dossiers

    /** Dossier sur un article racine de la tranche ({@code IsUnderItem = 0}), client et commercial ; prospection si demandée. */
    public ComFolder dossier(Jeu j, String code, StrStatus statut, boolean avecProspection) {
        StkItem article = ev.article("ART-" + code, j.tranche(), j.categorie(), j.type());
        return completer(ev.vendre(article, code, statut, ev.client("NOM" + code), null), code, avecProspection);
    }

    /** Dossier porté par un sous-article ({@code IsUnderItem = 1}) d'un article racine libre de la tranche. */
    public ComFolder dossierSousArticle(Jeu j, String code, StrStatus statut) {
        StkItem parent = ev.article("ARTP-" + code, j.tranche(), j.categorie(), j.type());
        StkItem sous = ev.sousArticle("SOUS-" + code, parent, j.categorie(), j.type());
        return completer(ev.vendre(sous, code, statut, ev.client("NOM" + code), null), code, true);
    }

    /** Dossier dont l'article de la tranche ne pointe pas vers lui ({@code StkItem.ComFolderId} NULL). */
    public ComFolder dossierSansLienRetour(Jeu j, String code, StrStatus statut) {
        ComFolder dossier = dossier(j, code, statut, true);
        StkItem article = dossier.getStkItem();
        article.setComFolderId(null);
        em.merge(article);
        em.flush();
        return dossier;
    }

    private ComFolder completer(ComFolder dossier, String code, boolean avecProspection) {
        dossier.setCfgCommercial(commercial(code));
        if (avecProspection) {
            dossier.setComProspection(prospection("PSN-" + code));
        }
        ComFolder sauve = ev.enregistrer(dossier);
        em.flush();
        return sauve;
    }

    /** Dates de 1re relance déjà portées par le dossier ; nulles permises. */
    public ComFolder relance(ComFolder dossier, LocalDateTime revival1, LocalDateTime validite) {
        dossier.setRevival1(revival1);
        dossier.setRevival1ValidityDate(validite);
        ComFolder sauve = ev.enregistrer(dossier);
        em.flush();
        return sauve;
    }

    private CfgTier commercial(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("COM-" + code);
        tier.setFirstName("Commercial");
        tier.setLastName("COM" + code);
        em.persist(tier);
        return tier;
    }

    private ComProspection prospection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        em.persist(p);
        return p;
    }

    /** Type d'action « Note » à sa PKey de production, créé s'il manque. */
    public ComActionMessageType typeNote() {
        ComActionMessageType existant = em.find(ComActionMessageType.class, TYPE_NOTE);
        if (existant != null) {
            return existant;
        }
        ComActionMessageType type = new ComActionMessageType();
        type.setId(TYPE_NOTE);
        type.setCode("Note");
        type.setDescription("Note");
        type.setIsSystem(false);
        em.persist(type);
        em.flush();
        return type;
    }

    // ------------------------------------------------------------------ lecture

    public void relire() {
        em.flush();
        em.clear();
    }

    public ComFolder relu(ComFolder dossier) {
        return em.find(ComFolder.class, dossier.getId());
    }

    public List<ComActionMessage> alertes(ComFolder dossier) {
        em.flush();
        return em.createQuery("SELECT m FROM ComActionMessage m WHERE m.comFolder.id = :id", ComActionMessage.class)
                .setParameter("id", dossier.getId()).getResultList();
    }

    public long alertesDeLaTranche(Jeu j) {
        em.flush();
        return em.createQuery("""
                        SELECT COUNT(m) FROM ComActionMessage m
                        WHERE m.comFolder.id IN (SELECT f.id FROM ComFolder f WHERE f.code LIKE :jeton)""", Long.class)
                .setParameter("jeton", "%" + j.jeton() + "%").getSingleResult();
    }

    public static String date(LocalDate jour) {
        return "%02d/%02d/%04d".formatted(jour.getDayOfMonth(), jour.getMonthValue(), jour.getYear());
    }

    // ------------------------------------------------------------------ montants (lettre)

    /** Échéance d'une nature au crédit, réglée de {@code regle} (encaissement) puis d'un règlement annulé {@code annule}. */
    public void echeance(ComFolder dossier, FixtureEtatVentes.Nature nature, String montant, String regle,
                         String annule) {
        ComDocument e = ev.echeance(dossier, nature, true, new BigDecimal(montant));
        if (regle != null) {
            ev.regler(e, new BigDecimal(regle), true, FixtureFluxVente.IN_WORK);
        }
        if (annule != null) {
            ev.regler(e, new BigDecimal(annule), true, FixtureFluxVente.CANCELED);
        }
    }

    public ComFolder prix(ComFolder dossier, String prix) {
        return ev.prix(dossier, new BigDecimal(prix));
    }
}
