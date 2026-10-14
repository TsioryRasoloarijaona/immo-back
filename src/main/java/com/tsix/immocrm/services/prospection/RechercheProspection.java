package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionCategory;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.com.ComProspectionProject;
import com.tsix.immocrm.entities.com.ComProspectionType;
import com.tsix.immocrm.entities.stk.StkItem;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Recherche multi-critère des listes, champs d'OMRAN ({@code ProspectionList.GetList},
 * {@code PreProspectionList.GetList}) : espaces retirés des deux côtés, casse ignorée. Une
 * sous-requête par table liée, jamais une jointure : la pagination et les groupes comptent juste.
 * Champs vides en production sautés — voir FEATURE-prospection-filtres.md §1.
 */
final class RechercheProspection {

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String COM_PROSPECTION = "comProspection";
    private static final String STK_ITEM = "stkItem";
    private static final String STK_HIERARCHY = "stkHierarchy";
    private static final String STK_ITEM_CATEGORY = "stkItemCategory";
    private static final String STK_ITEM_TYPE = "stkItemType";
    private static final String CFG_PROSPECT = "cfgProspect";
    private static final String KIND = "comProspectionKind";
    private static final String ORIGIN = "comProspectionOrigin";
    private static final String SUPPORT = "comProspectionOriginSupport";
    private static final String TYPE = "comActionMessageType";
    private static final String MOBILE = "mobile";
    private static final String INDICATIF = "mobileIndicatif";

    private RechercheProspection() {
    }

    static Predicate prospections(Root<ComProspection> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                  String texte) {
        Motif m = new Motif(cb, texte);
        List<Predicate> criteres = new ArrayList<>(m.champs(root, CODE, "tempDescription", "tempTel",
                "tempEMail", "notes"));
        criteres.add(root.get(CFG_PROSPECT).get(ID).in(tiers(query, m)));
        criteres.add(root.get(STK_ITEM).get(ID).in(articlesDetailles(query, m)));
        criteres.add(reference(root, query, m, ORIGIN, ComProspectionOrigin.class, CODE));
        criteres.add(reference(root, query, m, TYPE, ComActionMessageType.class, CODE));
        criteres.add(reference(root, query, m, SUPPORT, ComProspectionOriginSupport.class, DESCRIPTION));
        criteres.add(reference(root, query, m, KIND, ComProspectionKind.class, DESCRIPTION));
        criteres.add(root.get(ID).in(dossiers(query, m)));
        criteres.add(root.get(ID).in(lignesLiees(query, m, ComProspectionProject.class, STK_HIERARCHY)));
        criteres.add(root.get(ID).in(lignesLiees(query, m, ComProspectionCategory.class, STK_ITEM_CATEGORY)));
        criteres.add(root.get(ID).in(lignesLiees(query, m, ComProspectionType.class, STK_ITEM_TYPE)));
        return cb.or(criteres.toArray(new Predicate[0]));
    }

    static Predicate preProspections(Root<ComProspection> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                     String texte) {
        Motif m = new Motif(cb, texte);
        // TempField3 à 9, parrain, intermédiaire et mode de paiement : vides sur toutes les pré-prospections.
        List<Predicate> criteres = new ArrayList<>(m.champs(root, CODE, "tempDescription", "tempTel",
                "tempEMail", "tempField1", "tempField2", "tempField10", "notes"));
        criteres.add(reference(root, query, m, CFG_PROSPECT, CfgTier.class, DESCRIPTION));
        criteres.add(reference(root, query, m, STK_HIERARCHY, CfgTranche.class, DESCRIPTION));
        criteres.add(reference(root, query, m, STK_ITEM, StkItem.class, DESCRIPTION));
        criteres.add(reference(root, query, m, KIND, ComProspectionKind.class, DESCRIPTION));
        criteres.add(reference(root, query, m, TYPE, ComActionMessageType.class, DESCRIPTION));
        criteres.add(reference(root, query, m, "comAction", ComAction.class, DESCRIPTION));
        criteres.add(reference(root, query, m, ORIGIN, ComProspectionOrigin.class, DESCRIPTION));
        criteres.add(reference(root, query, m, SUPPORT, ComProspectionOriginSupport.class, DESCRIPTION));
        return cb.or(criteres.toArray(new Predicate[0]));
    }

    /** Association à un. L'id de clé étrangère ne déclenche aucune jointure : une ligne sans lien reste listée. */
    private static <T> Predicate reference(Root<ComProspection> root, CriteriaQuery<?> query, Motif m,
                                           String association, Class<T> entite, String... attributs) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<T> ligne = ids.from(entite);
        ids.select(ligne.get(ID)).where(m.cb.or(m.champs(ligne, attributs).toArray(new Predicate[0])));
        return root.get(association).get(ID).in(ids);
    }

    /** Prospect ou client lié : nom affiché, prénom + nom dans les deux ordres, mobile avec indicatif, CIN, ville. */
    private static Subquery<UUID> tiers(CriteriaQuery<?> query, Motif m) {
        CriteriaBuilder cb = m.cb;
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<CfgTier> t = ids.from(CfgTier.class);
        Expression<String> prenom = t.get("firstName");
        Expression<String> nom = t.get("lastName");        Expression<String> indicatifMobile = cb.concat(t.<String>get(INDICATIF), t.<String>get(MOBILE));
        List<Predicate> criteres = new ArrayList<>(m.champs(t, DESCRIPTION, MOBILE, "phone", "eMail",
                "iDNumber", "city"));
        criteres.add(m.contient(cb.concat(prenom, nom)));
        criteres.add(m.contient(cb.concat(nom, prenom)));
        criteres.add(m.contient(indicatifMobile));
        criteres.add(m.contient(cb.concat(cb.literal("+"), indicatifMobile)));
        criteres.add(m.contient(cb.concat(cb.literal("00"), indicatifMobile)));
        criteres.add(t.get("admCityId").in(villes(query, m)));
        ids.select(t.get(ID)).where(cb.or(criteres.toArray(new Predicate[0])));
        return ids;
    }

    private static Subquery<UUID> villes(CriteriaQuery<?> query, Motif m) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<AdmCity> ville = ids.from(AdmCity.class);
        return ids.select(ville.get(ID)).where(m.contient(ville, DESCRIPTION));
    }

    /** Article : code, libellé, catégorie, type, présentation. */
    private static Subquery<UUID> articlesDetailles(CriteriaQuery<?> query, Motif m) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<StkItem> a = ids.from(StkItem.class);
        List<Predicate> criteres = new ArrayList<>(m.champs(a, CODE, DESCRIPTION));
        criteres.add(m.contient(a.join(STK_ITEM_CATEGORY, JoinType.LEFT), DESCRIPTION));
        criteres.add(m.contient(a.join(STK_ITEM_TYPE, JoinType.LEFT), DESCRIPTION));
        criteres.add(m.contient(a.join("stkPresentationType", JoinType.LEFT), DESCRIPTION));
        return ids.select(a.get(ID)).where(m.cb.or(criteres.toArray(new Predicate[0])));
    }

    /** Client et statut du dossier issu de la prospection (dossiers de vente ; la location est hors liste). */
    private static Subquery<UUID> dossiers(CriteriaQuery<?> query, Motif m) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<ComFolder> f = ids.from(ComFolder.class);
        return ids.select(f.get(COM_PROSPECTION).get(ID)).where(m.cb.or(
                m.contient(f.join("cfgCustomer", JoinType.LEFT), DESCRIPTION),
                m.contient(f.join("strStatus", JoinType.LEFT), DESCRIPTION)));
    }

    /** Lignes filles (autres tranches, catégories, types du profil) dont le libellé correspond. */
    private static <T> Subquery<UUID> lignesLiees(CriteriaQuery<?> query, Motif m, Class<T> entite,
                                                  String reference) {
        Subquery<UUID> ids = query.subquery(UUID.class);
        Root<T> ligne = ids.from(entite);
        return ids.select(ligne.get(COM_PROSPECTION).get(ID))
                .where(m.contient(ligne.join(reference), DESCRIPTION));
    }

    /** Le texte cherché, compacté comme les colonnes : {@code REPLACE(x, ' ', '')}, en minuscules. */
    private static final class Motif {
        private final CriteriaBuilder cb;
        private final String motif;

        Motif(CriteriaBuilder cb, String texte) {
            this.cb = cb;
            this.motif = "%" + texte.replace(" ", "").toLowerCase(Locale.ROOT) + "%";
        }

        Predicate contient(Expression<String> colonne) {
            return cb.like(cb.lower(cb.function("replace", String.class, colonne, cb.literal(" "),
                    cb.literal(""))), motif);
        }

        Predicate contient(From<?, ?> source, String attribut) {
            return contient(source.<String>get(attribut));
        }

        List<Predicate> champs(From<?, ?> source, String... attributs) {
            return Arrays.stream(attributs).map(a -> contient(source, a)).toList();
        }
    }
}
