package com.tsix.immocrm.services.projet.travaux;

import com.tsix.immocrm.dto.projet.travaux.ExecutionTransitionJourTravailleDto;
import com.tsix.immocrm.dto.projet.travaux.FiltresTravaux;
import com.tsix.immocrm.dto.projet.travaux.HistoriqueJourTravailleDto;
import com.tsix.immocrm.dto.projet.travaux.TransitionJourTravailleDto;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleEcritureDto;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleFicheDto;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleLigneDto;
import com.tsix.immocrm.dto.projet.travaux.ReferentielsTravauxDto;
import com.tsix.immocrm.dto.projet.travaux.ReferentielsTravauxDto.TypeTravauxDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.buy.BuyItem;
import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import com.tsix.immocrm.entities.grh.GrhWorkedDay;
import com.tsix.immocrm.entities.grh.GrhWorkedDayType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.ReferencesPatrimoine;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Travaux chantier (FEATURE-n2-travaux-chantier.md) : liste D2/D3, fiche, création D1/R1, modification R3/R4, suppression R5. */
@Service
@Transactional(readOnly = true)
public class TravauxChantierService {

    static final String LIRE = "Yes.Prj.Front.Bul.GrhWorkedDayView";
    static final String MODIFIER = LIRE + ".toolNew";
    static final String SUPPRIMER = LIRE + ".toolDelete";
    static final String SANS_DEPART = "Vous n'avez pas les droits sur les états de création de l'entité 'Jour travaillé' !";

    private final EntityManager em;
    private final DroitsOmran droits;
    private final PerimetreProjetService perimetre;
    private final StatutsJourTravaille statuts;
    private final TransitionsJourTravaille workflow;
    private final LectureTravaux lecture;
    private final SaisieJourTravaille saisie;
    private final ReferencesPatrimoine references;

    public TravauxChantierService(EntityManager em, DroitsOmran droits, PerimetreProjetService perimetre,
                                  TransitionsJourTravaille workflow, LectureTravaux lecture, SaisieJourTravaille saisie,
                                  ReferencesPatrimoine references) {
        this.em = em;
        this.droits = droits;
        this.perimetre = perimetre;
        this.statuts = workflow.statuts();
        this.workflow = workflow;
        this.lecture = lecture;
        this.saisie = saisie;
        this.references = references;
    }

    public List<JourTravailleLigneDto> liste(FiltresTravaux filtres) {
        droits.exiger(LIRE);
        return lecture.lignes(filtres);
    }

    public ReferentielsTravauxDto referentiels(UUID projetId) {
        droits.exiger(LIRE);
        List<TypeTravauxDto> types = em.createQuery("SELECT t FROM GrhWorkedDayType t ORDER BY t.code ASC",
                GrhWorkedDayType.class).getResultList().stream().map(t -> new TypeTravauxDto(t.getId(), t.getCode(),
                t.getDescription(), t.getGroupDescription(), t.getUnity(), t.getUnitPriceHT(), t.getVatRatio(),
                t.getUnity1(), t.getUnitPriceHT1(), t.getVatRatio1())).toList();
        return new ReferentielsTravauxDto(types, statuts.tous().stream().map(StatutsJourTravaille::dto).toList(),
                statuts.departVisible().isPresent(), items(CfgProjectZone.class), niveaux(projetId),
                items(BuyItem.class), references.personnels());
    }

    public JourTravailleFicheDto fiche(UUID id) {
        droits.exiger(LIRE);
        return fiche(lecture.visible(id));
    }

    @Transactional
    public JourTravailleFicheDto creer(JourTravailleEcritureDto corps) {
        droits.exiger(MODIFIER);
        saisie.verifier(corps);
        StrStatus depart = statuts.departVisible().orElseThrow(() -> new ForbiddenException(SANS_DEPART));
        GrhWorkedDay jour = new GrhWorkedDay();
        jour.setStrEntityId(statuts.entite());
        jour.setStrStatusId(depart.getId());
        saisie.appliquer(jour, corps);
        saisie.persister(jour);
        workflow.creation(jour);
        return fiche(jour);
    }

    @Transactional
    public JourTravailleFicheDto modifier(UUID id, JourTravailleEcritureDto corps) {
        droits.exiger(MODIFIER);
        GrhWorkedDay jour = lecture.visible(id);
        saisie.verifier(corps);
        saisie.appliquer(jour, corps);
        saisie.marquer(jour);
        return fiche(jour);
    }

    @Transactional
    public void supprimer(UUID id) {
        droits.exiger(SUPPRIMER);
        GrhWorkedDay jour = lecture.visible(id);
        workflow.verifierSuppression(jour);
        em.remove(jour);
    }

    /** Aucun droit propre aux transitions (R-N2b-1) : la lecture suffit. */
    public List<TransitionJourTravailleDto> transitions(UUID id) {
        return workflow.permises(lisible(id));
    }

    @Transactional
    public JourTravailleFicheDto executer(UUID id, ExecutionTransitionJourTravailleDto execution) {
        GrhWorkedDay jour = lisible(id);
        workflow.executer(jour, execution);
        saisie.marquer(jour);
        return fiche(jour);
    }

    public List<HistoriqueJourTravailleDto> historique(UUID id) {
        return workflow.historique(lisible(id).getId());
    }

    /** R-N2b-1 : 404 (jour invisible) avant le 403 (droit de lecture). */
    private GrhWorkedDay lisible(UUID id) {
        GrhWorkedDay jour = lecture.visible(id);
        droits.exiger(LIRE);
        return jour;
    }

    private JourTravailleFicheDto fiche(GrhWorkedDay j) {
        JourTravailleEcritureDto valeurs = new JourTravailleEcritureDto(j.getCfgCompanyId(), j.getPrjProjectId(),
                j.getGrhWorkedDayTypeId(), j.getDocDate() == null ? null : j.getDocDate().toLocalDate(),
                LectureTravaux.heure(j.getTimeStart()), LectureTravaux.heure(j.getTimeEnd()), j.getCfgEmployeeId(),
                j.getCfgCustomerId(), j.getCfgTrancheLevelingDefinitionId(), j.getCfgProjectZoneId(), j.getBuyItemId(),
                j.getQuantity(), j.getQuantity1(), j.getUnity(), j.getUnitPriceHT(), j.getVatRatio(), j.getUnity1(),
                j.getUnitPriceHT1(), j.getVatRatio1(), j.getCode(), j.getDescription(), j.getNotes());
        StrStatus statut = j.getStrStatusId() == null ? null : em.find(StrStatus.class, j.getStrStatusId());
        return new JourTravailleFicheDto(j.getId(), StatutsJourTravaille.dto(statut), valeurs,
                LectureTravaux.total(j.getQuantity(), j.getUnitPriceHT()),
                LectureTravaux.total(j.getQuantity1(), j.getUnitPriceHT1()), j.getStlInvoiceCode(),
                j.getStlInvoiceProformaCode());
    }

    private <T extends CodeLibelleOmran> List<ReferentielItemDto> items(Class<T> classe) {
        return em.createQuery("SELECT o FROM " + classe.getSimpleName() + " o ORDER BY o.code ASC", classe)
                .getResultList().stream().map(o -> new ReferentielItemDto(o.getId(), o.getCode(), o.getDescription(), null))
                .toList();
    }

    private List<ReferentielItemDto> niveaux(UUID projetId) {
        if (projetId == null || !perimetre.estVisible(projetId)) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> lignes = em.createNativeQuery("""
                        SELECT PKey, Code, Sorting FROM CfgTrancheLevelingDefinition WHERE CfgTrancheId = :projet
                        ORDER BY Sorting ASC, Code ASC""")
                .setParameter("projet", projetId).getResultList();
        return lignes.stream().map(r -> new ReferentielItemDto(ValeursNatives.uuid(r[0]), (String) r[1], (String) r[1],
                ValeursNatives.entier(r[2]))).toList();
    }
}
