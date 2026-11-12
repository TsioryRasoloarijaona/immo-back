package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.CatalogueChampsStatutDto.ChampStatutAchatDto;
import com.tsix.immocrm.dto.achat.config.DocumentAJoindreAchatDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatEcritureDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatEcritureDto;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusDocumentToAttach;
import com.tsix.immocrm.entities.buy.BuyDocumentWorkFlow;
import com.tsix.immocrm.entities.buy.BuyDocumentWorkFlowAdmRole;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Écriture des statuts et transitions d'achat (FEATURE-statuts-achat-parite.md) : 404, puis droit (403), puis toute la
 * saisie (400), puis unicité (409), avant le premier setter.
 */
@Service
@Transactional
public class EcritureConfigurationAchatService {

    static final String DROIT = "Yes.Buy.Front.Config.BuyDocumentStatus";
    private static final String ID = "id";
    private static final String STATUT = "Statut d'achat";
    private static final String TRANSITION = "Transition d'achat";
    private static final int LONG = 1000;
    private static final UUID SANS_GESTION = new UUID(0, 0);

    private final EntityManager em;
    private final DroitsOmran droits;
    private final ConfigurationAchatService lecture;
    private final TracePatrimoine trace;

    public EcritureConfigurationAchatService(EntityManager em, DroitsOmran droits, ConfigurationAchatService lecture,
                                             TracePatrimoine trace) {
        this.em = em;
        this.droits = droits;
        this.lecture = lecture;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ statuts

    public StatutAchatConfigDto creerStatut(StatutAchatEcritureDto d) {
        droits.exiger(DROIT);
        BuyDocumentStatus s = new BuyDocumentStatus();
        s.setId(UUID.randomUUID());
        appliquer(s, d);
        em.persist(s);
        return ecrireDocuments(s.getId(), d.documentsAJoindre());
    }

    public StatutAchatConfigDto modifierStatut(UUID id, StatutAchatEcritureDto d) {
        BuyDocumentStatus s = trouver(BuyDocumentStatus.class, id, STATUT);
        droits.exiger(DROIT);
        appliquer(s, d);
        documents(id).forEach(em::remove);
        return ecrireDocuments(id, d.documentsAJoindre());
    }

    public void supprimerStatut(UUID id) {
        BuyDocumentStatus s = trouver(BuyDocumentStatus.class, id, STATUT);
        droits.exiger(DROIT);
        StatutAchatConfigDto usage = lecture.statut(id).orElseThrow();
        if (usage.nombrePieces() + usage.nombreTransitions() + usage.nombreHistoriques() > 0) {
            throw new ConflictException("Le statut %s est utilisé par %d pièce(s), %d transition(s) et %d ligne(s) "
                    .formatted(s.getCode(), usage.nombrePieces(), usage.nombreTransitions(), usage.nombreHistoriques())
                    + "d'historique : suppression impossible.");
        }
        documents(id).forEach(em::remove);
        em.flush();
        em.remove(s);
    }

    private void appliquer(BuyDocumentStatus s, StatutAchatEcritureDto d) {
        Integer ordre = exiger(d.ordre(), "Ordre");
        String code = SaisiePatrimoine.obligatoire(d.code(), null, "Code");
        String description = SaisiePatrimoine.obligatoire(d.description(), null, "Description");
        UUID type = exiger(d.typeId(), "Type");
        verifierExiste("SELECT COUNT(*) FROM AdmWorkFlowStateType WHERE PKey = :id", type, "Type d'état inconnu.");
        if (d.dureeVieJours() < 0) {
            throw new InvalidRequestException("Durée de vie : un nombre de jours positif ou nul.");
        }
        String couleur = couleur(d.couleur());
        if (d.imageId() != null) {
            verifierExiste("SELECT COUNT(*) FROM CfgImage WHERE PKey = :id", d.imageId(), "Image inconnue.");
        }
        String notes = SaisiePatrimoine.texte(d.notes(), null, LONG, "Notes");
        String champs = champs(d.champsObligatoires(), CatalogueStatutAchat.CHAMPS.piece(), "Champs obligatoires");
        String champsFournisseur = champs(d.champsObligatoiresFournisseur(), CatalogueStatutAchat.CHAMPS.fournisseur(),
                "Champs obligatoires fournisseur");
        verifierDocuments(d.documentsAJoindre());
        ReferentielAdminService.verrouiller(em, BuyDocumentStatus.class, code, STATUT);
        long pris = em.createQuery("SELECT COUNT(s) FROM BuyDocumentStatus s WHERE LOWER(TRIM(s.code)) = LOWER(:code) "
                + "AND s.id <> :id", Long.class).setParameter("code", code).setParameter(ID, s.getId()).getSingleResult();
        if (pris > 0) {
            throw new ConflictException("Le code %s est déjà pris par un autre statut d'achat.".formatted(code));
        }
        s.setSorting(ordre);
        s.setCode(code);
        s.setDescription(description);
        s.setAdmWorkFlowStateTypeId(type);
        s.setDelay(d.dureeVieJours());
        s.setColorName(couleur);
        s.setIsOkForPayement(d.okPourPaiement());
        s.setIsValidatedOnAccounting(d.valideComptabilite());
        s.setIsReadOnly(d.lectureSeule());
        s.setCfgImageId(d.imageId());
        s.setNotes(notes);
        s.setMandatoryFields(champs);
        s.setSupplierMandatoryFields(champsFournisseur);
        horodater(s);
    }

    private static String couleur(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return "";
        }
        return CatalogueStatutAchat.couleur(saisie.trim())
                .orElseThrow(() -> new InvalidRequestException("Couleur inconnue : " + saisie));
    }

    private static String champs(List<String> noms, List<ChampStatutAchatDto> catalogue, String libelle) {
        List<String> inconnus = noms.stream().filter(n -> catalogue.stream().noneMatch(c -> c.nom().equals(n))).toList();
        if (!inconnus.isEmpty()) {
            throw new InvalidRequestException("%s : champ(s) inconnu(s) %s".formatted(libelle, inconnus));
        }
        String stocke = CatalogueStatutAchat.stocker(noms.stream().distinct().toList());
        if (stocke != null && stocke.length() > LONG) {
            throw new InvalidRequestException("%s : %d caractères au plus une fois stockés.".formatted(libelle, LONG));
        }
        return stocke;
    }

    private void verifierDocuments(List<DocumentAJoindreAchatDto> documents) {
        HashSet<UUID> vus = new HashSet<>();
        for (DocumentAJoindreAchatDto doc : documents) {
            UUID type = exiger(doc == null ? null : doc.typeDocumentId(), "Type de document");
            if (em.find(AdmAttachedFileType.class, type) == null) {
                throw new InvalidRequestException("Type de document inconnu : " + type);
            }
            if (!vus.add(type)) {
                throw new InvalidRequestException("Type de document en double : " + type);
            }
        }
    }

    private StatutAchatConfigDto ecrireDocuments(UUID statut, List<DocumentAJoindreAchatDto> documents) {
        em.flush();
        documents.forEach(doc -> {
            BuyDocumentStatusDocumentToAttach ligne = new BuyDocumentStatusDocumentToAttach();
            ligne.setId(UUID.randomUUID());
            ligne.setPorteurId(statut);
            ligne.setAdmAttachedFileTypeId(doc.typeDocumentId());
            ligne.setIsMandatory(doc.obligatoire());
            ligne.setWithTitle(doc.avecTitre());
            horodater(ligne);
            em.persist(ligne);
        });
        em.flush();
        return lecture.statut(statut).orElseThrow();
    }

    private List<BuyDocumentStatusDocumentToAttach> documents(UUID statut) {
        return em.createQuery("SELECT d FROM BuyDocumentStatusDocumentToAttach d WHERE d.buyDocumentStatusId = :id",
                BuyDocumentStatusDocumentToAttach.class).setParameter(ID, statut).getResultList();
    }

    // ------------------------------------------------------------------ transitions

    public TransitionAchatDto creerTransition(TransitionAchatEcritureDto d) {
        droits.exiger(DROIT);
        BuyDocumentWorkFlow w = new BuyDocumentWorkFlow();
        w.setId(UUID.randomUUID());
        appliquer(w, d);
        em.persist(w);
        return ecrireRoles(w.getId(), d.roleIds());
    }

    public TransitionAchatDto modifierTransition(UUID id, TransitionAchatEcritureDto d) {
        BuyDocumentWorkFlow w = trouver(BuyDocumentWorkFlow.class, id, TRANSITION);
        droits.exiger(DROIT);
        appliquer(w, d);
        roles(id).forEach(em::remove);
        return ecrireRoles(id, d.roleIds());
    }

    // Liens de rôles d'abord : FK NO ACTION vers la transition ; l'historique n'a pas de FK, il reste.
    public void supprimerTransition(UUID id) {
        BuyDocumentWorkFlow w = trouver(BuyDocumentWorkFlow.class, id, TRANSITION);
        droits.exiger(DROIT);
        roles(id).forEach(em::remove);
        em.flush();
        em.remove(w);
    }

    private void appliquer(BuyDocumentWorkFlow w, TransitionAchatEcritureDto d) {
        String description = SaisiePatrimoine.texte(d.description(), null, SaisiePatrimoine.COURT, "Description");
        UUID depart = statut(d.statutDepartId(), "Statut de départ");
        UUID arrivee = statut(d.statutArriveeId(), "Statut d'arrivée");
        if (depart.equals(arrivee)) {
            throw new InvalidRequestException("Le statut de départ et le statut d'arrivée doivent être différents.");
        }
        if (d.gestionId() != null && em.find(StrEntity.class, d.gestionId()) == null) {
            throw new InvalidRequestException("Gestion inconnue : " + d.gestionId());
        }
        d.roleIds().stream().filter(r -> r == null || em.find(AdmRole.class, r) == null).findFirst().ifPresent(r -> {
            throw new InvalidRequestException("Rôle inconnu : " + r);
        });
        UUID gestion = Objects.requireNonNullElse(d.gestionId(), SANS_GESTION);
        ReferentielAdminService.verrouiller(em, BuyDocumentWorkFlow.class, depart + ">" + arrivee + "@" + gestion,
                TRANSITION);
        long doublon = em.createQuery("SELECT COUNT(w) FROM BuyDocumentWorkFlow w WHERE w.startStatusId = :depart "
                        + "AND w.endStatusId = :arrivee AND COALESCE(w.strEntityId, :aucune) = :gestion AND w.id <> :id",
                        Long.class).setParameter("depart", depart).setParameter("arrivee", arrivee)
                .setParameter("aucune", SANS_GESTION).setParameter("gestion", gestion).setParameter(ID, w.getId())
                .getSingleResult();
        if (doublon > 0) {
            throw new ConflictException("Une transition relie déjà ces deux statuts dans cette gestion.");
        }
        w.setSorting(Objects.requireNonNullElse(d.ordre(), 1));
        w.setDescription(description);
        w.setStartStatusId(depart);
        w.setEndStatusId(arrivee);
        w.setStrEntityId(d.gestionId());
        w.setNotesMandatory(d.noteObligatoire());
        w.setIsValidatedByCfgTierTrigger(d.valideeParDeclencheur());
        horodater(w);
    }

    private UUID statut(UUID id, String libelle) {
        if (em.find(BuyDocumentStatus.class, exiger(id, libelle)) == null) {
            throw new InvalidRequestException(libelle + " inconnu : " + id);
        }
        return id;
    }

    private TransitionAchatDto ecrireRoles(UUID transition, List<UUID> roleIds) {
        em.flush();
        roleIds.stream().distinct().forEach(role -> {
            BuyDocumentWorkFlowAdmRole lien = new BuyDocumentWorkFlowAdmRole();
            lien.setId(UUID.randomUUID());
            lien.setTransition(transition);
            lien.setRole(role);
            horodater(lien);
            em.persist(lien);
        });
        em.flush();
        return lecture.transition(transition).orElseThrow();
    }

    private List<BuyDocumentWorkFlowAdmRole> roles(UUID transition) {
        return em.createQuery("SELECT r FROM BuyDocumentWorkFlowAdmRole r WHERE r.transition = :id",
                BuyDocumentWorkFlowAdmRole.class).setParameter(ID, transition).getResultList();
    }

    // ------------------------------------------------------------------ commun

    private <T> T trouver(Class<T> classe, UUID id, String objet) {
        T trouve = em.find(classe, id);
        if (trouve == null) {
            throw new ResourceNotFoundException(objet + " introuvable : " + id);
        }
        return trouve;
    }

    private static <T> T exiger(T valeur, String libelle) {
        if (valeur == null) {
            throw new InvalidRequestException(libelle + " est obligatoire.");
        }
        return valeur;
    }

    private void verifierExiste(String requete, UUID id, String message) {
        if (((Number) em.createNativeQuery(requete).setParameter(ID, id).getSingleResult()).intValue() == 0) {
            throw new InvalidRequestException(message);
        }
    }

    private void horodater(AuditOmran s) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (s.getUserCreate() == null) {
            s.setUserCreate(t.auteur());
            s.setUserCreateDateTime(t.le());
        }
        s.setUserUpdate(t.auteur());
        s.setUserUpdateDateTime(t.le());
    }

    private void horodater(LigneOmran l) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (l.getUserCreate() == null) {
            l.setUserCreate(t.auteur());
            l.setUserCreateDateTime(t.le());
        }
        l.setUserUpdate(t.auteur());
        l.setUserUpdateDateTime(t.le());
    }
}
