package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import com.tsix.immocrm.entities.com.ComFolderStatusPaymentType;
import com.tsix.immocrm.entities.str.StrAttachment;
import com.tsix.immocrm.entities.str.StrEntityLinkedEntity;
import com.tsix.immocrm.entities.str.StrField;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrStatusAttachment;
import com.tsix.immocrm.entities.str.StrStatusMandatoryField;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.entities.str.StrWorkFlowField;
import com.tsix.immocrm.entities.str.StrWorkFlowIntervener;
import com.tsix.immocrm.services.dossier.workflow.SeuilVersement;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Paramétrage du workflow des dossiers de vente posé par un test (tables {@code Str*} vides en base de test et en CI),
 * avec les valeurs de production quand le test les reproduit. Tout est annulé avec la transaction du test.
 */
@Component
public class FixtureWorkflow {

    /** {@code StrEntity} « Tier » de production : entité des champs obligatoires du client et des acquéreurs. */
    public static final UUID ENTITE_TIERS = UUID.fromString("48A6E82C-0EC5-43A4-B376-C7C6D8663F40");

    private final EntityManager em;

    public FixtureWorkflow(EntityManager em) {
        this.em = em;
    }

    public StrWorkFlow transition(StrStatus depuis, StrStatus vers, boolean automatique) {
        StrWorkFlow w = new StrWorkFlow();
        w.setId(UUID.randomUUID());
        w.setStatusFromId(depuis.getId());
        w.setStatusToId(vers.getId());
        w.setCode(depuis.getCode() + " => " + vers.getCode());
        w.setDescription(vers.getCode());
        w.setSorting(10);
        w.setWithAutomaticTransition(automatique);
        w.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        em.persist(w);
        return w;
    }

    /** Intervenant « rôle » de la transition. */
    public void intervenantRole(StrWorkFlow transition, UUID admRoleId) {
        StrIntervener i = new StrIntervener();
        i.setId(UUID.randomUUID());
        i.setCode("TST-ROLE");
        i.setAdmRoleId(admRoleId);
        i.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        em.persist(i);
        StrWorkFlowIntervener wi = new StrWorkFlowIntervener();
        wi.setId(UUID.randomUUID());
        wi.setStrIntervenerId(i.getId());
        wi.setStrWorkFlowId(transition.getId());
        wi.setWithPassword(false);
        em.persist(wi);
    }

    public StrField champ(UUID entite, String code, String libelle) {
        StrField f = new StrField();
        f.setId(UUID.randomUUID());
        f.setCode(code);
        f.setDescription(libelle);
        f.setStrConfigEntityId(entite);
        em.persist(f);
        return f;
    }

    /** Lien « Client » du dossier ({@code CfgCustomerId}, LINKED_IS_PARENT), comme en production. */
    public StrEntityLinkedEntity lienClient() {
        entiteTiers();
        StrEntityLinkedEntity l = new StrEntityLinkedEntity();
        l.setId(UUID.randomUUID());
        l.setCode("Client");
        l.setStrLinkedEntityId(ENTITE_TIERS);
        l.setLinkPropertyName("CfgCustomerId");
        l.setLinkType("LINKED_IS_PARENT");
        l.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        em.persist(l);
        return l;
    }

    public void champObligatoireDuStatut(StrStatus statut, StrField champ, StrEntityLinkedEntity lien) {
        StrStatusMandatoryField m = new StrStatusMandatoryField();
        m.setId(UUID.randomUUID());
        m.setStrStatusId(statut.getId());
        m.setStrFieldId(champ.getId());
        m.setStrEntityLinkedEntityId(lien != null ? lien.getId() : null);
        em.persist(m);
    }

    public void champObligatoireDeTransition(StrWorkFlow transition, StrField champ) {
        StrWorkFlowField m = new StrWorkFlowField();
        m.setId(UUID.randomUUID());
        m.setStrWorkFlowId(transition.getId());
        m.setStrFieldId(champ.getId());
        em.persist(m);
    }

    public AdmAttachedFileType typePiece(String libelle) {
        AdmAttachedFileType t = new AdmAttachedFileType();
        t.setId(UUID.randomUUID());
        t.setCode(libelle);
        t.setDescription(libelle);
        em.persist(t);
        return t;
    }

    public void pieceDuStatut(StrStatus statut, AdmAttachedFileType type, boolean justeACocher) {
        StrAttachment a = new StrAttachment();
        a.setId(UUID.randomUUID());
        a.setAdmAttachedFileTypeId(type.getId());
        a.setIsMandatory(false);
        a.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        em.persist(a);
        StrStatusAttachment sa = new StrStatusAttachment();
        sa.setId(UUID.randomUUID());
        sa.setStrAttachmentId(a.getId());
        sa.setStrStatusId(statut.getId());
        sa.setIsJustToRequiredToBeChecked(justeACocher);
        sa.setIsWithTitle(false);
        em.persist(sa);
    }

    public void pieceJointe(UUID dossierId, AdmAttachedFileType type) {
        AdmAttachedFile f = new AdmAttachedFile();
        f.setId(UUID.randomUUID());
        f.setCategoryId(dossierId);
        f.setCategory("Dossier de vente");
        f.setDescription("TST-PIECE");
        f.setAdmAttachedFileTypeId(type.getId());
        em.persist(f);
    }

    /** Seuil global ({@code CfgTrancheId} nul), « Toujours », comme toutes les lignes de production. */
    public void seuilGlobal(StrStatus statut, int operateur, BigDecimal ratio) {
        CfgTrancheStatus ts = new CfgTrancheStatus();
        ts.setId(UUID.randomUUID());
        ts.setStrStatusId(statut.getId());
        ts.setStrFolderEntityId(StatutDossier.CONFIG_ENTITY_ID);
        ts.setThresholdOperator(operateur);
        ts.setThresholdRatio(ratio);
        ts.setComFolderStatusPaymentType(typeToujours());
        em.persist(ts);
    }

    private ComFolderStatusPaymentType typeToujours() {
        ComFolderStatusPaymentType t = em.find(ComFolderStatusPaymentType.class, SeuilVersement.TOUJOURS);
        if (t != null) {
            return t;
        }
        t = new ComFolderStatusPaymentType();
        t.setId(SeuilVersement.TOUJOURS);
        t.setCode("Toujours");
        t.setDescription("Toujours");
        em.persist(t);
        return t;
    }

    /** Personne physique : seuls ses champs de workflow sont contrôlés (colonne non mappée, SQL natif). */
    public void personnePhysique(UUID tierId) {
        UUID forme = UUID.randomUUID();
        em.flush();
        em.createNativeQuery("INSERT INTO AdmSocialForm (PKey, Code, Description, IsPerson) VALUES (?1, 'TST-PP', 'TST-PP', 1)")
                .setParameter(1, forme).executeUpdate();
        em.createNativeQuery("UPDATE CfgTier SET AdmSocialFormId = ?1 WHERE PKey = ?2")
                .setParameter(1, forme).setParameter(2, tierId).executeUpdate();
    }

    private void entiteTiers() {
        em.flush();
        em.createNativeQuery("IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = ?1) "
                        + "INSERT INTO StrEntity (PKey, Code, Description) VALUES (?1, 'Tier', 'Tiers')")
                .setParameter(1, ENTITE_TIERS).executeUpdate();
    }
}
