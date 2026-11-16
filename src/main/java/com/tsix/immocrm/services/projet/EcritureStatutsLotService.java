package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto;
import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto.CaseStatutLotDto;
import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto.ChoixStatutLotDto;
import com.tsix.immocrm.dto.projet.StatutLotDto;
import com.tsix.immocrm.dto.projet.StatutLotEcritureDto;
import com.tsix.immocrm.dto.projet.TransitionLotEcritureDto;
import com.tsix.immocrm.dto.projet.TransitionStatutLotDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatusEcriture;
import com.tsix.immocrm.entities.str.StrStatusMandatoryField;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.entities.str.StrWorkFlowIntervener;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.config.CatalogueStatutAchat;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Écriture des statuts et transitions de lot (FEATURE-restes-projet-achat.md §6) : 404 (id inconnu ou hors des 5 genres),
 * puis droit (403), puis toute la saisie (400), puis unicité (409), avant le premier setter.
 */
@Service
@Transactional
public class EcritureStatutsLotService {

    static final String DROIT_STATUTS = "Yes.Prj.Front.Config.MarketStatusList";
    static final String DROIT_TRANSITIONS = "Yes.Prj.Front.Config.MarketWorkFlowList";
    private static final String STATUT = "Statut de lot";
    private static final String TRANSITION = "Transition de lot";
    private static final String ID = "id";
    private static final String GENRE = "genre";
    private static final int NOTES = 1000;
    private static final String USAGES = """
            SELECT (SELECT COUNT(*) FROM PrjMarket WHERE StrStatusId = :id),
                   (SELECT COUNT(*) FROM StrWorkFlow WHERE StatusFromId = :id OR StatusToId = :id),
                   (SELECT COUNT(*) FROM StrHistory WHERE StrStatusId = :id),
                   (SELECT COUNT(*) FROM StrStatusIntervener WHERE StrStatusId = :id)
                   + (SELECT COUNT(*) FROM StrStatusAttachment WHERE StrStatusId = :id)""";

    private final EntityManager em;
    private final DroitsOmran droits;
    private final LectureStatutsLot lecture;
    private final TracePatrimoine trace;

    public EcritureStatutsLotService(EntityManager em, DroitsOmran droits, LectureStatutsLot lecture,
                                     TracePatrimoine trace) {
        this.em = em;
        this.droits = droits;
        this.lecture = lecture;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ statuts

    public StatutLotDto creerStatut(StatutLotEcritureDto d) {
        droits.exiger(DROIT_STATUTS);
        StrStatusEcriture s = new StrStatusEcriture();
        s.setId(UUID.randomUUID());
        IntStream.rangeClosed(1, StrStatusEcriture.NOMBRE_CASES).forEach(n -> s.cocher(n, false));
        List<UUID> champs = appliquer(s, d);
        em.persist(s);
        return ecrireChamps(s.getId(), champs);
    }

    public StatutLotDto modifierStatut(UUID id, StatutLotEcritureDto d) {
        StrStatusEcriture s = trouver(StrStatusEcriture.class, id, StrStatusEcriture::getStrConfigEntityId, STATUT);
        droits.exiger(DROIT_STATUTS);
        return ecrireChamps(id, appliquer(s, d));
    }

    public void supprimerStatut(UUID id) {
        StrStatusEcriture s = trouver(StrStatusEcriture.class, id, StrStatusEcriture::getStrConfigEntityId, STATUT);
        droits.exiger(DROIT_STATUTS);
        Object[] n = (Object[]) em.createNativeQuery(USAGES).setParameter(ID, id).getSingleResult();
        long lots = nombre(n[0]);
        long transitions = nombre(n[1]);
        long historiques = nombre(n[2]);
        long parametrages = nombre(n[3]);
        if (lots + transitions + historiques + parametrages > 0) {
            throw new ConflictException(("Le statut %s est cité par %d lot(s), %d transition(s), %d ligne(s) d'historique "
                    + "et %d paramétrage(s) : suppression impossible.")
                    .formatted(s.getCode(), lots, transitions, historiques, parametrages));
        }
        champs(id).forEach(em::remove);
        em.flush();
        em.remove(s);
    }

    private List<UUID> appliquer(StrStatusEcriture s, StatutLotEcritureDto d) {
        UUID genre = genre(s.getStrConfigEntityId(), d.genreId());
        Integer ordre = exiger(d.ordre(), "Ordre");
        String code = SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(d.code()), null, "Code");
        String description = SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(d.description()), null,
                "Description");
        UUID type = exiger(d.typeId(), "Type");
        verifierExiste("SELECT COUNT(*) FROM StrStatusType WHERE PKey = :id", type, "Type de statut inconnu.");
        if (d.dureeVieJours() < 0) {
            throw new InvalidRequestException("Durée de vie : un nombre de jours positif ou nul.");
        }
        String couleur = CatalogueStatutAchat.couleur(d.couleur());
        if (d.imageId() != null) {
            verifierExiste("SELECT COUNT(*) FROM CfgImage WHERE PKey = :id", d.imageId(), "Image inconnue.");
        }
        String notes = SaisiePatrimoine.texte(d.notes(), null, NOTES, "Notes");
        CatalogueStatutLotDto catalogue = lecture.catalogue(genre);
        List<Integer> libellees = catalogue.cases().stream().map(CaseStatutLotDto::numero).toList();
        d.cases().stream().filter(n -> !libellees.contains(n)).findFirst().ifPresent(n -> {
            throw new InvalidRequestException("Case %s hors du catalogue du genre.".formatted(n));
        });
        List<UUID> champsDuGenre = catalogue.champs().stream().map(ChoixStatutLotDto::id).toList();
        d.champsObligatoires().stream().filter(c -> !champsDuGenre.contains(c)).findFirst().ifPresent(c -> {
            throw new InvalidRequestException("Champ obligatoire hors du genre : " + c);
        });
        verifierCodeLibre(s.getId(), genre, code);
        s.setStrConfigEntityId(genre);
        s.setSorting(ordre);
        s.setCode(code);
        s.setDescription(description);
        s.setStrStatusTypeId(type);
        s.setDelay(d.dureeVieJours());
        s.setColorName(couleur);
        s.setCfgImageId(d.imageId());
        s.setNotes(notes);
        libellees.forEach(n -> s.cocher(n, d.cases().contains(n)));
        trace.horodater(s);
        return d.champsObligatoires().stream().distinct().toList();
    }

    private void verifierCodeLibre(UUID id, UUID genre, String code) {
        ReferentielAdminService.verrouiller(em, StrStatusEcriture.class, genre + "/" + code.toLowerCase(Locale.ROOT), STATUT);
        long pris = em.createQuery("""
                        SELECT COUNT(s) FROM StrStatusEcriture s WHERE s.strConfigEntityId = :genre
                        AND LOWER(TRIM(s.code)) = LOWER(:code) AND s.id <> :id""", Long.class)
                .setParameter(GENRE, genre).setParameter("code", code).setParameter(ID, id).getSingleResult();
        if (pris > 0) {
            throw new ConflictException("Le code %s est déjà pris par un autre statut de ce genre.".formatted(code));
        }
    }

    private StatutLotDto ecrireChamps(UUID statut, List<UUID> champs) {
        champs(statut).forEach(em::remove);
        em.flush();
        champs.forEach(champ -> {
            StrStatusMandatoryField m = new StrStatusMandatoryField();
            m.setId(UUID.randomUUID());
            m.setStrStatusId(statut);
            m.setStrFieldId(champ);
            trace.horodater(m);
            em.persist(m);
        });
        em.flush();
        return lecture.statut(statut);
    }

    private List<StrStatusMandatoryField> champs(UUID statut) {
        return em.createQuery("SELECT m FROM StrStatusMandatoryField m WHERE m.strStatusId = :id",
                StrStatusMandatoryField.class).setParameter(ID, statut).getResultList();
    }

    // ------------------------------------------------------------------ transitions

    public TransitionStatutLotDto creerTransition(TransitionLotEcritureDto d) {
        droits.exiger(DROIT_TRANSITIONS);
        StrWorkFlow w = new StrWorkFlow();
        w.setId(UUID.randomUUID());
        appliquer(w, d);
        em.persist(w);
        return ecrireRoles(w, d.roleIds());
    }

    public TransitionStatutLotDto modifierTransition(UUID id, TransitionLotEcritureDto d) {
        StrWorkFlow w = trouver(StrWorkFlow.class, id, StrWorkFlow::getStrConfigEntityId, TRANSITION);
        droits.exiger(DROIT_TRANSITIONS);
        appliquer(w, d);
        return ecrireRoles(w, d.roleIds());
    }

    // Liens d'abord : FK vers la transition ; StrHistory garde l'identifiant de la transition, sans FK.
    public void supprimerTransition(UUID id) {
        StrWorkFlow w = trouver(StrWorkFlow.class, id, StrWorkFlow::getStrConfigEntityId, TRANSITION);
        droits.exiger(DROIT_TRANSITIONS);
        liens(id).forEach(em::remove);
        em.flush();
        em.remove(w);
    }

    private void appliquer(StrWorkFlow w, TransitionLotEcritureDto d) {
        UUID genre = genre(w.getStrConfigEntityId(), d.genreId());
        String code = SaisiePatrimoine.texte(SaisiePatrimoine.sansBlancsAuxBords(d.code()), null, SaisiePatrimoine.COURT,
                "Code");
        String description = SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(d.description()), null,
                "Description");
        UUID depart = statutDuGenre(d.statutDepartId(), genre, "Statut de départ");
        UUID arrivee = statutDuGenre(d.statutArriveeId(), genre, "Statut d'arrivée");
        if (depart.equals(arrivee)) {
            throw new InvalidRequestException("Le statut de départ et le statut d'arrivée doivent être différents.");
        }
        d.roleIds().stream().filter(r -> r == null || em.find(AdmRole.class, r) == null).findFirst().ifPresent(r -> {
            throw new InvalidRequestException("Rôle inconnu : " + r);
        });
        ReferentielAdminService.verrouiller(em, StrWorkFlow.class, depart + ">" + arrivee, TRANSITION);
        long doublon = em.createQuery("""
                        SELECT COUNT(w) FROM StrWorkFlow w WHERE w.strConfigEntityId = :genre
                        AND w.statusFromId = :depart AND w.statusToId = :arrivee AND w.id <> :id""", Long.class)
                .setParameter(GENRE, genre).setParameter("depart", depart).setParameter("arrivee", arrivee)
                .setParameter(ID, w.getId()).getSingleResult();
        if (doublon > 0) {
            throw new ConflictException("Une transition relie déjà ces deux statuts dans ce genre.");
        }
        w.setStrConfigEntityId(genre);
        w.setSorting(Objects.requireNonNullElse(d.ordre(), 1));
        w.setCode(code);
        w.setDescription(description);
        w.setStatusFromId(depart);
        w.setStatusToId(arrivee);
        w.setWithComment(d.avecCommentaire());
        w.setWithPassword(d.avecMotDePasse());
        w.setWithAutomaticTransition(d.transitionAutomatique());
        w.setWithUserDate(Boolean.TRUE.equals(w.getWithUserDate()));
        trace.horodater(w);
    }

    private UUID statutDuGenre(UUID id, UUID genre, String libelle) {
        StrStatusEcriture s = em.find(StrStatusEcriture.class, exiger(id, libelle));
        if (s == null || !genre.equals(s.getStrConfigEntityId())) {
            throw new InvalidRequestException(libelle + " inconnu dans ce genre : " + id);
        }
        return id;
    }

    private TransitionStatutLotDto ecrireRoles(StrWorkFlow w, List<UUID> roleIds) {
        liens(w.getId()).forEach(em::remove);
        em.flush();
        roleIds.stream().distinct().forEach(role -> {
            StrWorkFlowIntervener lien = new StrWorkFlowIntervener();
            lien.setId(UUID.randomUUID());
            lien.setStrWorkFlowId(w.getId());
            lien.setStrIntervenerId(intervenant(role, w.getStrConfigEntityId()));
            lien.setWithPassword(false);
            trace.horodater(lien);
            em.persist(lien);
        });
        em.flush();
        return lecture.transition(w.getId());
    }

    /** L'intervenant « rôle » du genre, créé au premier usage comme le fait OMRAN. */
    private UUID intervenant(UUID role, UUID genre) {
        return em.createQuery("""
                        SELECT i.id FROM StrIntervener i WHERE i.admRoleId = :role AND i.strConfigEntityId = :genre
                        AND i.admUserId IS NULL ORDER BY i.code""", UUID.class)
                .setParameter("role", role).setParameter(GENRE, genre).setMaxResults(1).getResultStream().findFirst()
                .orElseGet(() -> {
                    AdmRole r = em.find(AdmRole.class, role);
                    StrIntervener i = new StrIntervener();
                    i.setId(UUID.randomUUID());
                    i.setCode(r.getCode());
                    i.setDescription(r.getDescription());
                    i.setAdmRoleId(role);
                    i.setStrConfigEntityId(genre);
                    trace.horodater(i);
                    em.persist(i);
                    return i.getId();
                });
    }

    private List<StrWorkFlowIntervener> liens(UUID transition) {
        return em.createQuery("SELECT l FROM StrWorkFlowIntervener l WHERE l.strWorkFlowId = :id",
                StrWorkFlowIntervener.class).setParameter(ID, transition).getResultList();
    }

    // ------------------------------------------------------------------ commun

    /** Un genre saisi n'est accepté qu'à la création ; ensuite, une valeur différente est refusée. */
    private UUID genre(UUID actuel, UUID saisi) {
        if (actuel != null) {
            if (saisi != null && !saisi.equals(actuel)) {
                throw new InvalidRequestException("Le genre ne change pas : " + saisi);
            }
            return actuel;
        }
        UUID genre = exiger(saisi, "Genre");
        return lecture.genre(genre).orElseThrow(() -> new InvalidRequestException("Genre de lot inconnu : " + genre))
                .getId();
    }

    private <T> T trouver(Class<T> classe, UUID id, Function<T, UUID> genre, String objet) {
        T trouve = em.find(classe, id);
        if (trouve == null || lecture.genre(genre.apply(trouve)).isEmpty()) {
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

    private static long nombre(Object valeur) {
        return ((Number) valeur).longValue();
    }
}
