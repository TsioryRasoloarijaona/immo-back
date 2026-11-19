package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatLigne;
import com.tsix.immocrm.dto.achat.demandes.ElementDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ExecutionTransitionDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.FiltresDemandesAchat;
import com.tsix.immocrm.dto.achat.demandes.HistoriqueDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.StatutCibleDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.TransitionDemandeAchat;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.services.workflow.MoteurWorkflowStr;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Demandes internes d'achat (FEATURE-n6-demandes-achat.md) : visibilité D5 (404) avant droits D7 (403). */
@Service
public class DemandesAchatService {

    private static final String LECTURE = "Yes.Buy.Front.CallOfTender.BuySelectionList";
    private static final String NOUVEAU = LECTURE + ".toolNew";
    private static final String SUPPRIMER = LECTURE + ".toolDelete";
    private static final String EXPORTER = LECTURE + ".toolExport";
    private static final String CONFIDENTIEL = LECTURE + ".IsConfidential";

    private final LectureDemandesAchat lecture;
    private final EcrituresDemandeAchat ecritures;
    private final DroitsOmran droits;
    private final PerimetreProjetService perimetres;
    private final IntervenantCourant intervenant;
    private final MoteurWorkflowStr moteur;
    private final StrHistoryRepository historiques;

    public DemandesAchatService(LectureDemandesAchat lecture, EcrituresDemandeAchat ecritures, DroitsOmran droits,
                                PerimetreProjetService perimetres, IntervenantCourant intervenant, MoteurWorkflowStr moteur,
                                StrHistoryRepository historiques) {
        this.lecture = lecture;
        this.ecritures = ecritures;
        this.droits = droits;
        this.perimetres = perimetres;
        this.intervenant = intervenant;
        this.moteur = moteur;
        this.historiques = historiques;
    }

    @Transactional(readOnly = true)
    public List<DemandeAchatLigne> liste(FiltresDemandesAchat filtres) {
        droits.exiger(LECTURE);
        return lignes(filtres);
    }

    @Transactional(readOnly = true)
    public List<DemandeAchatLigne> export(FiltresDemandesAchat filtres) {
        droits.exiger(LECTURE, EXPORTER);
        return lignes(filtres);
    }

    @Transactional(readOnly = true)
    public DemandeAchatFiche fiche(UUID id) {
        DemandeAchatFiche f = visible(id);
        droits.exiger(LECTURE);
        return f;
    }

    @Transactional
    public DemandeAchatFiche creer(SaisieDemandeAchat saisie) {
        droits.exiger(NOUVEAU);
        return relire(ecritures.creer(saisie));
    }

    @Transactional
    public DemandeAchatFiche modifier(UUID id, SaisieDemandeAchat saisie) {
        visible(id);
        droits.exiger(NOUVEAU);
        ecritures.modifier(id, saisie);
        return relire(id);
    }

    @Transactional
    public void supprimer(UUID id) {
        visible(id);
        droits.exiger(SUPPRIMER);
        ecritures.supprimer(id);
    }

    /** Aucun droit propre aux transitions (D7), mais la lecture : la réponse porte la fiche. */
    @Transactional(readOnly = true)
    public List<TransitionDemandeAchat> transitions(UUID id) {
        visible(id);
        droits.exiger(LECTURE);
        List<StrWorkFlow> permises = moteur.permises(ecritures.demande(id));
        Map<UUID, StrStatus> cibles = moteur.cibles(permises);
        return permises.stream().map(w -> transition(w, cibles.get(w.getStatusToId()))).toList();
    }

    @Transactional
    public DemandeAchatFiche executer(UUID id, ExecutionTransitionDemandeAchat execution) {
        visible(id);
        droits.exiger(LECTURE);
        ecritures.executer(id, execution);
        return relire(id);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueDemandeAchat> historique(UUID id) {
        visible(id);
        droits.exiger(LECTURE);
        return historiques.findByObjectIdInOrderByDocDateDesc(List.of(id)).stream()
                .map(h -> new HistoriqueDemandeAchat(h.getDocDate(), h.getAdmUserDescription(), h.getDescription(), h.getNotes(),
                        h.getUserDate()))
                .toList();
    }

    private List<DemandeAchatLigne> lignes(FiltresDemandesAchat f) {
        boolean confidentiel = droits.parmi(CONFIDENTIEL).contains(CONFIDENTIEL);
        PerimetreAchat base = perimetres.achat(f.projetIds()).deSocietes(f.societeIds());
        PerimetreAchat retenu = Boolean.TRUE.equals(f.sansProjet())
                ? new PerimetreAchat(base.societes(), base.projets(), true, false) : base;
        return lecture.liste(retenu, f).stream()
                .filter(statutVisible())
                .filter(d -> confidentiel || !d.confidentielle())
                .map(DemandesAchatService::ligne)
                .toList();
    }

    /** D5 : société, projet et statut visibles ; sinon 404, avant tout contrôle de droit. */
    DemandeAchatFiche visible(UUID id) {
        return lecture.fiche(id, perimetres.achat(List.of())).filter(statutVisible())
                .orElseThrow(() -> new ResourceNotFoundException("Demande interne d'achat introuvable : " + id));
    }

    DemandeAchatFiche relire(UUID id) {
        return lecture.fiche(id, perimetres.achat(List.of())).orElseThrow();
    }

    private Predicate<DemandeAchatFiche> statutVisible() {
        Optional<Set<UUID>> visibles = intervenant.statutsDeLotsEtProjets();
        return d -> visibles.map(v -> d.statut() != null && v.contains(d.statut().id())).orElse(true);
    }

    private static TransitionDemandeAchat transition(StrWorkFlow w, StrStatus cible) {
        return new TransitionDemandeAchat(w.getId(), w.getDescription(),
                cible == null ? null : new StatutCibleDemandeAchat(cible.getId(), cible.getCode(), MoteurWorkflowStr.libelle(cible),
                        cible.getColorName()),
                Boolean.TRUE.equals(w.getWithComment()), Boolean.TRUE.equals(w.getWithUserDate()),
                Boolean.TRUE.equals(w.getWithPassword()));
    }

    private static DemandeAchatLigne ligne(DemandeAchatFiche d) {
        return new DemandeAchatLigne(d.id(), libelle(d.procedure()), d.statut(), d.code(), d.date(), d.description(),
                libelle(d.societe()), libelle(d.projet()), libelle(d.lot()), libelle(d.module()), libelle(d.fournisseur()),
                libelle(d.categorie()), libelle(d.declencheur()), d.notes(), d.prixEstimeTTC(), d.estDerniere());
    }

    private static String libelle(ElementDemandeAchat e) {
        return Champs.de(e, ElementDemandeAchat::libelle);
    }
}
