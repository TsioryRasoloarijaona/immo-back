package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.dto.projet.reunions.AvancementSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.ContactSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.DecisionSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.IntervenantSaisieDto;
import com.tsix.immocrm.dto.projet.reunions.MaitresOuvrageAjoutDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.bul.BulDelegate;
import com.tsix.immocrm.entities.bul.BulDelegateContact;
import com.tsix.immocrm.entities.bul.BulMeeting;
import com.tsix.immocrm.entities.bul.BulMeetingContact;
import com.tsix.immocrm.entities.bul.BulMeetingLine;
import com.tsix.immocrm.entities.bul.BulMeetingPrjProjectProgressCriteriaLine;
import com.tsix.immocrm.entities.bul.BulObservationStatus;
import com.tsix.immocrm.entities.bul.ContactReunionOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProjectProgressCriteriaLine;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntSupplier;

/** Onglets d'une réunion : maîtres d'ouvrage, intervenants (+ contacts), décisions, avancements ; renvoient la fiche. */
@Service
@Transactional
public class OngletsReunionService {

    private static final String REUNION = "bulMeetingId";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final EntityManager em;
    private final DroitsOmran droits;
    private final LectureReunion lecture;
    private final EcritureReunion ecriture;

    public OngletsReunionService(EntityManager em, DroitsOmran droits, LectureReunion lecture, EcritureReunion ecriture) {
        this.em = em;
        this.droits = droits;
        this.lecture = lecture;
        this.ecriture = ecriture;
    }

    // ------------------------------------------------------------------ maîtres d'ouvrage

    /** R6 : un contact déjà présent est ignoré ; O.D.T. = nombre de lignes + 1. */
    public ReunionFicheDto ajouterMaitresOuvrage(UUID id, MaitresOuvrageAjoutDto corps) {
        BulMeeting m = reunion(id);
        List<UUID> contacts = corps.contactIds() == null ? List.of()
                : corps.contactIds().stream().filter(Objects::nonNull).distinct().toList();
        contacts.forEach(this::tiers);
        List<BulMeetingContact> presents = lecture.lignes(BulMeetingContact.class, REUNION, List.of(id));
        int ordre = presents.size();
        for (UUID contact : contacts) {
            if (presents.stream().noneMatch(c -> contact.equals(c.getContactId()))) {
                BulMeetingContact ligne = new BulMeetingContact();
                ligne.setBulMeetingId(id);
                ligne.setIsPresent(false);
                ligne.setNotify(false);
                ecriture.persister(ligne, contact, ++ordre);
            }
        }
        return fiche(m);
    }

    public ReunionFicheDto modifierMaitreOuvrage(UUID id, UUID ligneId, ContactSaisieDto corps) {
        BulMeeting m = reunion(id);
        BulMeetingContact ligne = ligne(BulMeetingContact.class, ligneId, BulMeetingContact::getBulMeetingId, id);
        if (corps.contactId() != null && !corps.contactId().equals(ligne.getContactId())) {
            tiers(corps.contactId());
            if (lecture.lignes(BulMeetingContact.class, REUNION, List.of(id)).stream()
                    .anyMatch(c -> corps.contactId().equals(c.getContactId()))) {
                throw new ConflictException("Ce maître d'ouvrage est déjà dans la réunion.");
            }
        }
        saisirContact(ligne, corps);
        return fiche(m);
    }

    public ReunionFicheDto supprimerMaitreOuvrage(UUID id, UUID ligneId) {
        BulMeeting m = reunion(id);
        ecriture.supprimer(List.of(ligne(BulMeetingContact.class, ligneId, BulMeetingContact::getBulMeetingId, id)));
        return fiche(m);
    }

    // ------------------------------------------------------------------ intervenants

    public ReunionFicheDto creerIntervenant(UUID id, IntervenantSaisieDto corps) {
        BulMeeting m = reunion(id);
        BulDelegate ligne = new BulDelegate();
        ligne.setBulMeetingId(id);
        saisirIntervenant(m, ligne, corps);
        ecriture.persister(ligne);
        return fiche(m);
    }

    public ReunionFicheDto modifierIntervenant(UUID id, UUID ligneId, IntervenantSaisieDto corps) {
        BulMeeting m = reunion(id);
        BulDelegate ligne = ligne(BulDelegate.class, ligneId, BulDelegate::getBulMeetingId, id);
        saisirIntervenant(m, ligne, corps);
        ecriture.modifier(ligne);
        return fiche(m);
    }

    public ReunionFicheDto supprimerIntervenant(UUID id, UUID ligneId) {
        BulMeeting m = reunion(id);
        BulDelegate ligne = ligne(BulDelegate.class, ligneId, BulDelegate::getBulMeetingId, id);
        ecriture.supprimer(lecture.lignes(BulDelegateContact.class, "bulDelegateId", List.of(ligneId)));
        ecriture.supprimer(List.of(ligne));
        return fiche(m);
    }

    public ReunionFicheDto creerContactIntervenant(UUID id, UUID intervenantId, ContactSaisieDto corps) {
        BulMeeting m = reunion(id);
        ligne(BulDelegate.class, intervenantId, BulDelegate::getBulMeetingId, id);
        if (corps.contactId() == null) {
            throw new InvalidRequestException("Le contact est obligatoire.");
        }
        tiers(corps.contactId());
        BulDelegateContact ligne = new BulDelegateContact();
        ligne.setBulDelegateId(intervenantId);
        ligne.setIsPresent(Boolean.TRUE.equals(corps.present()));
        ligne.setNotify(Boolean.TRUE.equals(corps.aNotifier()));
        ecriture.persister(ligne, corps.contactId(),
                lecture.lignes(BulDelegateContact.class, "bulDelegateId", List.of(intervenantId)).size() + 1);
        return fiche(m);
    }

    public ReunionFicheDto modifierContactIntervenant(UUID id, UUID intervenantId, UUID ligneId, ContactSaisieDto corps) {
        BulMeeting m = reunion(id);
        ligne(BulDelegate.class, intervenantId, BulDelegate::getBulMeetingId, id);
        BulDelegateContact ligne = ligne(BulDelegateContact.class, ligneId, BulDelegateContact::getBulDelegateId, intervenantId);
        if (corps.contactId() != null) {
            tiers(corps.contactId());
        }
        saisirContact(ligne, corps);
        return fiche(m);
    }

    public ReunionFicheDto supprimerContactIntervenant(UUID id, UUID intervenantId, UUID ligneId) {
        BulMeeting m = reunion(id);
        ligne(BulDelegate.class, intervenantId, BulDelegate::getBulMeetingId, id);
        ecriture.supprimer(List.of(ligne(BulDelegateContact.class, ligneId, BulDelegateContact::getBulDelegateId,
                intervenantId)));
        return fiche(m);
    }

    // ------------------------------------------------------------------ décisions

    public ReunionFicheDto creerDecision(UUID id, DecisionSaisieDto corps) {
        BulMeeting m = reunion(id);
        BulMeetingLine ligne = new BulMeetingLine();
        ligne.setBulMeetingId(id);
        saisirDecision(m, ligne, corps);
        ecriture.persister(ligne);
        return fiche(m);
    }

    public ReunionFicheDto modifierDecision(UUID id, UUID ligneId, DecisionSaisieDto corps) {
        BulMeeting m = reunion(id);
        BulMeetingLine ligne = ligne(BulMeetingLine.class, ligneId, BulMeetingLine::getBulMeetingId, id);
        saisirDecision(m, ligne, corps);
        ecriture.modifier(ligne);
        return fiche(m);
    }

    public ReunionFicheDto supprimerDecision(UUID id, UUID ligneId) {
        BulMeeting m = reunion(id);
        ecriture.supprimer(List.of(ligne(BulMeetingLine.class, ligneId, BulMeetingLine::getBulMeetingId, id)));
        return fiche(m);
    }

    // ------------------------------------------------------------------ avancements

    /** Crée la cellule si absente ; 0 ou vide la supprime (valeur absente). Pourcentage 0-100, stocké en fraction. */
    public ReunionFicheDto modifierAvancement(UUID id, AvancementSaisieDto corps) {
        BulMeeting m = reunion(id);
        if (corps.critereId() == null || corps.niveauId() == null) {
            throw new InvalidRequestException("Le critère et le niveau sont obligatoires.");
        }
        PrjProjectProgressCriteriaLine critere = em.find(PrjProjectProgressCriteriaLine.class, corps.critereId());
        if (critere == null || !m.getPrjProjectId().equals(critere.getPrjProjectId())
                || lecture.niveaux(m.getPrjProjectId()).stream().map(ReferentielItemDto::id).noneMatch(corps.niveauId()::equals)) {
            throw new InvalidRequestException("Critère ou niveau étranger au projet de la réunion.");
        }
        BigDecimal pourcentage = corps.pourcentage() == null ? BigDecimal.ZERO : corps.pourcentage();
        if (pourcentage.signum() < 0 || pourcentage.compareTo(CENT) > 0) {
            throw new InvalidRequestException("Le pourcentage doit être compris entre 0 et 100.");
        }
        List<BulMeetingPrjProjectProgressCriteriaLine> cellules = em.createQuery("""
                        SELECT a FROM BulMeetingPrjProjectProgressCriteriaLine a WHERE a.bulMeetingId = :reunion
                        AND a.prjProjectProgressCriteriaLineId = :critere AND a.cfgTrancheLevelingDefinitionId = :niveau""",
                        BulMeetingPrjProjectProgressCriteriaLine.class)
                .setParameter("reunion", id).setParameter("critere", corps.critereId())
                .setParameter("niveau", corps.niveauId()).getResultList();
        if (pourcentage.signum() == 0) {
            ecriture.supprimer(cellules);
        } else if (cellules.isEmpty()) {
            BulMeetingPrjProjectProgressCriteriaLine cellule = new BulMeetingPrjProjectProgressCriteriaLine();
            cellule.setBulMeetingId(id);
            cellule.setPrjProjectProgressCriteriaLineId(corps.critereId());
            cellule.setCfgTrancheLevelingDefinitionId(corps.niveauId());
            cellule.setAchievement(pourcentage.divide(CENT, 6, RoundingMode.HALF_UP));
            ecriture.persister(cellule);
        } else {
            cellules.forEach(c -> {
                c.setAchievement(pourcentage.divide(CENT, 6, RoundingMode.HALF_UP));
                ecriture.modifier(c);
            });
        }
        return fiche(m);
    }

    // ------------------------------------------------------------------ communs

    private BulMeeting reunion(UUID id) {
        droits.exiger(ReunionsService.MODIFIER);
        return lecture.visible(id);
    }

    private ReunionFicheDto fiche(BulMeeting m) {
        em.flush();
        return lecture.fiche(m);
    }

    /** R9 : ligne fille inconnue ou d'un autre parent → 404. */
    private <T> T ligne(Class<T> classe, UUID ligneId, Function<T, UUID> parent, UUID attendu) {
        T ligne = em.find(classe, ligneId);
        if (ligne == null || !attendu.equals(parent.apply(ligne))) {
            throw new ResourceNotFoundException("Ligne introuvable : " + ligneId);
        }
        return ligne;
    }

    private void tiers(UUID id) {
        if (id != null && em.find(CfgTier.class, id) == null) {
            throw new InvalidRequestException("Tiers inconnu : " + id);
        }
    }

    private void lot(BulMeeting m, UUID lotId) {
        if (lotId == null) {
            return;
        }
        PrjMarket lot = em.find(PrjMarket.class, lotId);
        if (lot == null || lot.getPrjProject() == null || !m.getPrjProjectId().equals(lot.getPrjProject().getId())) {
            throw new InvalidRequestException("Lot étranger au projet de la réunion : " + lotId);
        }
    }

    private static void saisirContact(ContactReunionOmran ligne, ContactSaisieDto corps) {
        if (corps.contactId() != null) {
            ligne.setContactId(corps.contactId());
        }
        if (corps.present() != null) {
            ligne.setIsPresent(corps.present());
        }
        if (corps.aNotifier() != null) {
            ligne.setNotify(corps.aNotifier());
        }
    }

    /** R6 : un intervenant par (lot, fournisseur) dans la réunion → 409. */
    private void saisirIntervenant(BulMeeting m, BulDelegate ligne, IntervenantSaisieDto corps) {
        lot(m, corps.lotId());
        tiers(corps.fournisseurId());
        List<BulDelegate> autres = lecture.lignes(BulDelegate.class, REUNION, List.of(m.getId())).stream()
                .filter(d -> !d.getId().equals(ligne.getId())).toList();
        if (autres.stream().anyMatch(d -> Objects.equals(d.getPrjMarketId(), corps.lotId())
                && Objects.equals(d.getCfgSupplierId(), corps.fournisseurId()))) {
            throw new ConflictException("Cet intervenant (lot, fournisseur) est déjà dans la réunion.");
        }
        ligne.setSorting(ordre(corps.ordre(), ligne.getSorting(), autres::size));
        ligne.setPrjMarketId(corps.lotId());
        ligne.setCfgSupplierId(corps.fournisseurId());
        ligne.setEffectif(corps.effectif());
        ligne.setNotes(corps.note());
    }

    /** R4 : O.D.T. saisi, sinon celui de la ligne, sinon nombre de lignes + 1. */
    private static Integer ordre(Integer saisi, Integer actuel, IntSupplier lignes) {
        if (saisi != null) {
            return saisi;
        }
        return actuel != null ? actuel : lignes.getAsInt() + 1;
    }

    /** R2 : description et observation obligatoires. */
    private void saisirDecision(BulMeeting m, BulMeetingLine ligne, DecisionSaisieDto corps) {
        if (corps.description() == null || corps.description().isBlank() || corps.observationId() == null) {
            throw new InvalidRequestException("La description et l'observation de la décision sont obligatoires.");
        }
        if (em.find(BulObservationStatus.class, corps.observationId()) == null) {
            throw new InvalidRequestException("Observation inconnue : " + corps.observationId());
        }
        lot(m, corps.lotId());
        tiers(corps.fournisseurId());
        ligne.setSorting(ordre(corps.ordre(), ligne.getSorting(),
                () -> lecture.lignes(BulMeetingLine.class, REUNION, List.of(m.getId())).size()));
        ligne.setPrjMarketId(corps.lotId());
        ligne.setCfgSupplierId(corps.fournisseurId());
        ligne.setDescription(corps.description().strip());
        ligne.setBulObservationStatusId(corps.observationId());
        ligne.setDueDate(corps.objectif() == null ? null : corps.objectif().atStartOfDay());
        ligne.setNote(corps.note());
        ligne.setIsInternal(Boolean.TRUE.equals(corps.nousMemes()));
    }
}
