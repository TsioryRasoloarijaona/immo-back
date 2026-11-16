package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.CatalogueChampsStatutDto.ChampStatutAchatDto;
import com.tsix.immocrm.dto.achat.config.DocumentAJoindreAchatDto;
import com.tsix.immocrm.dto.achat.config.DocumentTransitionAchatDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatEcritureDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatEcritureDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusDocumentToAttach;
import com.tsix.immocrm.entities.buy.BuyDocumentWorkFlow;
import com.tsix.immocrm.entities.buy.BuyDocumentWorkFlowAdmRole;
import com.tsix.immocrm.entities.buy.BuyDocumentWorkFlowDocumentToAttach;
import com.tsix.immocrm.entities.prj.PrjMarketType;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private static final String CHAMPS_PIECE = "Champs obligatoires";
    private static final String CHAMPS_FOURNISSEUR = "Champs obligatoires fournisseur";
    private static final UUID SANS_GESTION = new UUID(0, 0);
    private static final String TYPE_DOCUMENT = "Type de document";
    private static final String GESTION_DECOMPTES = "PrjInvoiceDebitWorkingOut";

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
        FicheStatut f = FicheStatut.lire(em, d, "SELECT COUNT(*) FROM AdmWorkFlowStateType WHERE PKey = :id");
        String code = f.code();
        String champs = champs(d.champsObligatoires(), CatalogueStatutAchat.CHAMPS.piece(), CHAMPS_PIECE, LONG);
        String champsFournisseur = champs(d.champsObligatoiresFournisseur(), CatalogueStatutAchat.CHAMPS.fournisseur(),
                CHAMPS_FOURNISSEUR, LONG);
        verifierDocuments(d.documentsAJoindre());
        ReferentielAdminService.verrouiller(em, BuyDocumentStatus.class, code, STATUT);
        long pris = em.createQuery("SELECT COUNT(s) FROM BuyDocumentStatus s WHERE LOWER(TRIM(s.code)) = LOWER(:code) "
                + "AND s.id <> :id", Long.class).setParameter("code", code).setParameter(ID, s.getId()).getSingleResult();
        if (pris > 0) {
            throw new ConflictException("Le code %s est déjà pris par un autre statut d'achat.".formatted(code));
        }
        s.setSorting(f.ordre());
        s.setCode(code);
        s.setDescription(f.description());
        s.setAdmWorkFlowStateTypeId(f.type());
        s.setDelay(d.dureeVieJours());
        s.setColorName(f.couleur());
        s.setIsOkForPayement(d.okPourPaiement());
        s.setIsValidatedOnAccounting(d.valideComptabilite());
        s.setIsReadOnly(d.lectureSeule());
        s.setCfgImageId(d.imageId());
        s.setNotes(f.notes());
        s.setMandatoryFields(champs);
        s.setSupplierMandatoryFields(champsFournisseur);
        trace.horodater(s);
    }

    private static String champs(List<String> noms, List<ChampStatutAchatDto> catalogue, String libelle, int max) {
        List<String> inconnus = noms.stream().filter(n -> catalogue.stream().noneMatch(c -> c.nom().equals(n))).toList();
        if (!inconnus.isEmpty()) {
            throw new InvalidRequestException("%s : champ(s) inconnu(s) %s".formatted(libelle, inconnus));
        }
        String stocke = CatalogueStatutAchat.stocker(noms.stream().distinct().toList());
        if (stocke.length() > max) {
            throw new InvalidRequestException("%s : %d caractères au plus une fois stockés.".formatted(libelle, max));
        }
        return stocke;
    }

    // Absent du corps = valeur gardée (false à la création) : FEATURE-restes-projet-achat.md §5.
    private static String champsGardes(List<String> noms, List<ChampStatutAchatDto> catalogue, String libelle, int max,
                                       String actuel) {
        return noms == null ? Objects.requireNonNullElse(actuel, "") : champs(noms, catalogue, libelle, max);
    }

    private static Boolean garde(Boolean saisie, Boolean actuel) {
        return saisie != null ? saisie : Boolean.TRUE.equals(actuel);
    }

    private void verifierDocuments(List<DocumentAJoindreAchatDto> documents) {
        Set<UUID> vus = new HashSet<>();
        for (DocumentAJoindreAchatDto doc : documents) {
            UUID type = FicheStatut.exiger(doc == null ? null : doc.typeDocumentId(), TYPE_DOCUMENT);
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
            trace.horodater(ligne);
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
        ecrireDocumentsTransition(w.getId(), d.documentsAJoindre());
        return ecrireRoles(w.getId(), d.roleIds());
    }

    public TransitionAchatDto modifierTransition(UUID id, TransitionAchatEcritureDto d) {
        BuyDocumentWorkFlow w = trouver(BuyDocumentWorkFlow.class, id, TRANSITION);
        droits.exiger(DROIT);
        appliquer(w, d);
        roles(id).forEach(em::remove);
        if (d.documentsAJoindre() != null) {
            documentsTransition(id).forEach(em::remove);
            ecrireDocumentsTransition(id, d.documentsAJoindre());
        }
        return ecrireRoles(id, d.roleIds());
    }

    // Liens de rôles et documents d'abord : FK NO ACTION vers la transition ; l'historique n'a pas de FK, il reste.
    public void supprimerTransition(UUID id) {
        BuyDocumentWorkFlow w = trouver(BuyDocumentWorkFlow.class, id, TRANSITION);
        droits.exiger(DROIT);
        roles(id).forEach(em::remove);
        documentsTransition(id).forEach(em::remove);
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
        StrEntity entite = d.gestionId() == null ? null : em.find(StrEntity.class, d.gestionId());
        if (d.gestionId() != null && entite == null) {
            throw new InvalidRequestException("Gestion inconnue : " + d.gestionId());
        }
        verifierDocuments(d.documentsAJoindre(), entite != null && GESTION_DECOMPTES.equals(entite.getCode()));
        d.roleIds().stream().filter(r -> r == null || em.find(AdmRole.class, r) == null).findFirst().ifPresent(r -> {
            throw new InvalidRequestException("Rôle inconnu : " + r);
        });
        String champs = champsGardes(d.champsObligatoires(), CatalogueStatutAchat.CHAMPS.piece(), CHAMPS_PIECE,
                SaisiePatrimoine.COURT, w.getMandatoryFields());
        String champsFournisseur = champsGardes(d.champsObligatoiresFournisseur(),
                CatalogueStatutAchat.CHAMPS.fournisseur(), CHAMPS_FOURNISSEUR, LONG, w.getSupplierMandatoryFields());
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
        w.setMandatoryFields(champs);
        w.setSupplierMandatoryFields(champsFournisseur);
        w.setWithNotification(garde(d.avecNotification(), w.getWithNotification()));
        w.setSendEmailToUsers(garde(d.envoiEmails(), w.getSendEmailToUsers()));
        w.setNotifyCfgTierTrigger(garde(d.notifierDeclencheur(), w.getNotifyCfgTierTrigger()));
        w.setSendEMailToCfgTierTrigger(garde(d.emailDeclencheur(), w.getSendEMailToCfgTierTrigger()));
        trace.horodater(w);
    }

    private UUID statut(UUID id, String libelle) {
        if (em.find(BuyDocumentStatus.class, FicheStatut.exiger(id, libelle)) == null) {
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
            trace.horodater(lien);
            em.persist(lien);
        });
        em.flush();
        return lecture.transition(transition).orElseThrow();
    }

    // Grille OMRAN (BuyDocumentWorkFlowAttachedFile) : un document par type de pièce × type de marché × décompte.
    private void verifierDocuments(List<DocumentTransitionAchatDto> documents, boolean decomptes) {
        if (documents == null) {
            return;
        }
        Set<List<Object>> vus = new HashSet<>();
        for (DocumentTransitionAchatDto brut : documents) {
            DocumentTransitionAchatDto doc = FicheStatut.exiger(brut, "Document à joindre");
            UUID type = FicheStatut.exiger(doc.typeDocumentId(), TYPE_DOCUMENT);
            UUID marche = FicheStatut.exiger(doc.typeMarcheId(), "Type de marché");
            if (em.find(AdmAttachedFileType.class, type) == null) {
                throw new InvalidRequestException("Type de document inconnu : " + type);
            }
            if (em.find(PrjMarketType.class, marche) == null) {
                throw new InvalidRequestException("Type de marché inconnu : " + marche);
            }
            String decompte = doc.decompte();
            boolean valide = decompte == null ? !decomptes
                    : decomptes && ConfigurationAchatService.DECOMPTES.contains(decompte);
            if (!valide) {
                throw new InvalidRequestException("Décompte (%s) obligatoire pour la gestion %s, absent pour les autres."
                        .formatted(String.join(", ", ConfigurationAchatService.DECOMPTES), GESTION_DECOMPTES));
            }
            if (!vus.add(Arrays.asList(type, marche, decompte))) {
                throw new InvalidRequestException("Document à joindre en double : " + type + " / " + marche);
            }
        }
    }

    private void ecrireDocumentsTransition(UUID transition, List<DocumentTransitionAchatDto> documents) {
        if (documents == null) {
            return;
        }
        documents.forEach(doc -> {
            BuyDocumentWorkFlowDocumentToAttach ligne = new BuyDocumentWorkFlowDocumentToAttach();
            ligne.setId(UUID.randomUUID());
            ligne.setPorteurId(transition);
            ligne.setAdmAttachedFileTypeId(doc.typeDocumentId());
            ligne.setPrjMarketTypeId(doc.typeMarcheId());
            ligne.setIsMandatory(true);
            ligne.setWithTitle(false);
            ligne.setForFirstWorkingOut(ConfigurationAchatService.DECOMPTES.get(0).equals(doc.decompte()));
            ligne.setForIntermediateWorkingOut(ConfigurationAchatService.DECOMPTES.get(1).equals(doc.decompte()));
            ligne.setForFinalWorkingOut(ConfigurationAchatService.DECOMPTES.get(2).equals(doc.decompte()));
            trace.horodater(ligne);
            em.persist(ligne);
        });
    }

    private List<BuyDocumentWorkFlowDocumentToAttach> documentsTransition(UUID transition) {
        return em.createQuery("SELECT d FROM BuyDocumentWorkFlowDocumentToAttach d WHERE d.buyDocumentWorkFlowId = :id",
                BuyDocumentWorkFlowDocumentToAttach.class).setParameter(ID, transition).getResultList();
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
}
