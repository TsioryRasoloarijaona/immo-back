package com.tsix.immocrm.services.parcours;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ImportPreProspectionService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Prédicats et libellés communs à « Ma journée » et à la Boîte de tri : une pré-prospection « à trier » n'a qu'une
 * définition (FEATURE-parcours-commercial.md §3).
 */
public final class LignesParcours {

    static final String COM_PROSPECTION = "comProspection";
    static final String CFG_COMMERCIAL = "cfgCommercial";
    static final String IS_CLOSED = "isClosed";
    static final String DOC_DATE = "docDate";
    private static final String COM_PROSPECTION_KIND = "comProspectionKind";
    private static final String ID = "id";
    /** Codes {@code ComProspectionKind} d'un lead encore « de départ » ; une étape nulle l'est aussi. */
    static final List<String> ETAPES_DE_DEPART = List.of(ImportPreProspectionService.CODE_ETAPE_DEFAUT);
    /** Fenêtre par défaut des leads : au-delà, la ligne relève du nettoyage du stock (hors périmètre). */
    static final int MOIS_RECENTS = 6;

    private LignesParcours() {
    }

    /**
     * Pré-prospections ouvertes du commercial ; {@code depuis} nul = sans borne d'âge. Un commercial nul ne retient
     * rien : un compte sans employé n'a pas de lignes.
     */
    static Specification<ComProspection> aTrier(@Nullable UUID commercialId, @Nullable LocalDate depuis) {
        Specification<ComProspection> spec = DiscriminantPreProspection.estPreProspection()
                .and(ouvertesDu(commercialId));
        if (depuis == null) {
            return spec;
        }
        return spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DOC_DATE), depuis.atStartOfDay()));
    }

    /**
     * « Non traitée » : aucune interaction saisie <b>et</b> toujours à l'étape de départ. Seule définition de la Boîte
     * de tri, des nouveaux leads, de « À distribuer » et de {@code jamaisTraite} (FEATURE-tri-a-jour.md §Règle 1).
     */
    public static Specification<ComProspection> nonTraitee() {
        return sansInteraction().and(aLEtapeDeDepart());
    }

    /** Même règle sur une ligne déjà agrégée (nombre d'actions non système, code d'étape). */
    public static boolean nonTraitee(long nombreActions, @Nullable String etapeCode) {
        return nombreActions == 0 && estEtapeDeDepart(etapeCode);
    }

    /** Étape posée à la création (formulaire, import, n8n) ou aucune étape. */
    public static boolean estEtapeDeDepart(@Nullable String etapeCode) {
        return etapeCode == null || ETAPES_DE_DEPART.stream().anyMatch(e -> e.equalsIgnoreCase(etapeCode.trim()));
    }

    private static Specification<ComProspection> aLEtapeDeDepart() {
        return (root, query, cb) -> {
            Subquery<UUID> depart = query.subquery(UUID.class);
            var kind = depart.from(ComProspectionKind.class);
            depart.select(kind.get(ID)).where(kind.get("code").in(ETAPES_DE_DEPART));
            return cb.or(cb.isNull(root.get(COM_PROSPECTION_KIND)), root.get(COM_PROSPECTION_KIND).get(ID).in(depart));
        };
    }

    /** Aucune interaction saisie ; celles qu'OMRAN écrit seul ({@code IsSystem = 1}) ne comptent pas. */
    private static Specification<ComProspection> sansInteraction() {
        return (root, query, cb) -> {
            Subquery<UUID> messages = query.subquery(UUID.class);
            var m = messages.from(ComActionMessage.class);
            messages.select(m.get(ID)).where(
                    cb.equal(m.get(COM_PROSPECTION), root),
                    cb.or(cb.isNull(m.get("isSystem")), cb.isFalse(m.get("isSystem"))));
            return cb.not(cb.exists(messages));
        };
    }

    private static Specification<ComProspection> ouvertesDu(@Nullable UUID commercialId) {
        return (root, query, cb) -> {
            if (commercialId == null) {
                return cb.disjunction();
            }
            Join<ComProspection, CfgTier> commercial = root.join(CFG_COMMERCIAL, JoinType.INNER);
            return cb.and(cb.equal(commercial.get(ID), commercialId),
                    cb.or(cb.isNull(root.get(IS_CLOSED)), cb.isFalse(root.get(IS_CLOSED))));
        };
    }

    static String nom(ComProspection p) {
        return ProspectionService.nomProspect(p.getCfgProspect(), p);
    }

    static String telephone(ComProspection p) {
        return ProspectionService.telephoneProspect(p.getCfgProspect(), p);
    }

    /** L'indicatif n'existe que pour le mobile de la fiche ; le numéro du formulaire n'en porte pas. */
    static @Nullable String indicatif(ComProspection p) {
        CfgTier prospect = p.getCfgProspect();
        if (prospect == null || !StringUtils.hasText(prospect.getMobile())) {
            return null;
        }
        return prospect.getMobileIndicatif();
    }

    static @Nullable String trancheCode(ComProspection p) {
        return Champs.de(p.getStkHierarchy(), CfgTranche::getCode);
    }

    static @Nullable String projetCode(ComProspection p) {
        return Champs.de(p.getStkHierarchy(), CfgTranche::getCfgProject, CfgProject::getCode);
    }
}
