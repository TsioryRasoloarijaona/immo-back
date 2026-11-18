package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.entities.bul.BulDelegate;
import com.tsix.immocrm.entities.bul.BulDelegateContact;
import com.tsix.immocrm.entities.bul.BulMeeting;
import com.tsix.immocrm.entities.bul.BulMeetingContact;
import com.tsix.immocrm.entities.bul.BulMeetingLine;
import com.tsix.immocrm.entities.bul.BulMeetingPrjProjectProgressCriteriaLine;
import com.tsix.immocrm.entities.bul.ContactReunionOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** R5 : copie dans une nouvelle réunion le P.V. de la précédente (plus grand O.D.T., même projet et même type). */
@Component
class ReportReunionPrecedente {

    private static final String REUNION = "bulMeetingId";

    private final EntityManager em;
    private final LectureReunion lecture;
    private final EcritureReunion ecriture;

    ReportReunionPrecedente(EntityManager em, LectureReunion lecture, EcritureReunion ecriture) {
        this.em = em;
        this.lecture = lecture;
        this.ecriture = ecriture;
    }

    void reporter(BulMeeting nouvelle) {
        List<UUID> precedente = em.createQuery("""
                        SELECT m.id FROM BulMeeting m
                        WHERE m.prjProjectId = :projet AND m.bulMeetingTypeId = :type AND m.id <> :id
                        ORDER BY m.sorting DESC, m.meetingDate DESC""", UUID.class)
                .setParameter("projet", nouvelle.getPrjProjectId()).setParameter("type", nouvelle.getBulMeetingTypeId())
                .setParameter("id", nouvelle.getId()).setMaxResults(1).getResultList();
        if (precedente.isEmpty()) {
            return;
        }
        List<UUID> source = List.of(precedente.getFirst());
        UUID id = nouvelle.getId();
        lecture.lignes(BulMeetingContact.class, REUNION, source).forEach(c -> {
            BulMeetingContact copie = new BulMeetingContact();
            copie.setBulMeetingId(id);
            copie(c, copie);
        });
        lecture.lignes(BulDelegate.class, REUNION, source).forEach(d -> copieIntervenant(d, id));
        // HashSet : Set.copyOf refuserait contains(null) d'une décision sans statut.
        Set<UUID> fermes = new HashSet<>(em.createQuery(
                "SELECT s.id FROM BulObservationStatus s WHERE s.isClosed = true", UUID.class).getResultList());
        lecture.lignes(BulMeetingLine.class, REUNION, source).stream()
                .filter(l -> !fermes.contains(l.getBulObservationStatusId()))
                .forEach(l -> copieDecision(l, id));
        em.createQuery("SELECT a FROM BulMeetingPrjProjectProgressCriteriaLine a WHERE a.bulMeetingId IN :source",
                        BulMeetingPrjProjectProgressCriteriaLine.class)
                .setParameter("source", source).getResultList().forEach(a -> {
                    BulMeetingPrjProjectProgressCriteriaLine copie = new BulMeetingPrjProjectProgressCriteriaLine();
                    copie.setBulMeetingId(id);
                    copie.setPrjProjectProgressCriteriaLineId(a.getPrjProjectProgressCriteriaLineId());
                    copie.setCfgTrancheLevelingDefinitionId(a.getCfgTrancheLevelingDefinitionId());
                    copie.setAchievement(a.getAchievement());
                    ecriture.persister(copie);
                });
    }

    private void copieIntervenant(BulDelegate d, UUID reunion) {
        BulDelegate copie = new BulDelegate();
        copie.setBulMeetingId(reunion);
        copie.setSorting(d.getSorting());
        copie.setPrjMarketId(d.getPrjMarketId());
        copie.setCfgSupplierId(d.getCfgSupplierId());
        copie.setEffectif(d.getEffectif());
        copie.setNotes(d.getNotes());
        ecriture.persister(copie);
        lecture.lignes(BulDelegateContact.class, "bulDelegateId", List.of(d.getId())).forEach(c -> {
            BulDelegateContact contact = new BulDelegateContact();
            contact.setBulDelegateId(copie.getId());
            copie(c, contact);
        });
    }

    private void copieDecision(BulMeetingLine l, UUID reunion) {
        BulMeetingLine copie = new BulMeetingLine();
        copie.setBulMeetingId(reunion);
        copie.setBulMeetingLineId(l.getId());
        copie.setSorting(l.getSorting());
        copie.setDueDate(l.getDueDate());
        copie.setDescription(l.getDescription());
        copie.setNote(l.getNote());
        copie.setIsInternal(l.getIsInternal());
        copie.setCfgSupplierId(l.getCfgSupplierId());
        copie.setPrjMarketId(l.getPrjMarketId());
        copie.setBulObservationStatusId(l.getBulObservationStatusId());
        ecriture.persister(copie);
    }

    private void copie(ContactReunionOmran source, ContactReunionOmran cible) {
        cible.setIsPresent(source.getIsPresent());
        cible.setNotify(source.getNotify());
        ecriture.persister(cible, source.getContactId(), source.getSorting());
    }
}
