package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.dto.projet.reunions.ChoixReunionsDto;
import com.tsix.immocrm.dto.projet.reunions.FiltresReunions;
import com.tsix.immocrm.dto.projet.reunions.ReunionCreationDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionLigneDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionModificationDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.bul.BulDelegate;
import com.tsix.immocrm.entities.bul.BulDelegateContact;
import com.tsix.immocrm.entities.bul.BulMeeting;
import com.tsix.immocrm.entities.bul.BulMeetingContact;
import com.tsix.immocrm.entities.bul.BulMeetingLine;
import com.tsix.immocrm.entities.bul.BulMeetingPrjProjectProgressCriteriaLine;
import com.tsix.immocrm.entities.bul.BulMeetingType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Réunions de chantier (FEATURE-n1-reunions-chantier.md) : liste D4, fiche, création R1/D2/R4/R5, suppression R7. */
@Service
@Transactional(readOnly = true)
public class ReunionsService {

    static final String LIRE = "Yes.Prj.Front.Bul.MeetingList";
    static final String MODIFIER = LIRE + ".toolNew";
    static final String SUPPRIMER = LIRE + ".toolDelete";
    private static final String PROCHAINE = "La date de la prochaine réunion doit être supérieure à la date de cette réunion.";
    private static final String LIBELLE = "Réunion";
    private static final String REUNION = "reunion";

    private final EntityManager em;
    private final DroitsOmran droits;
    private final PerimetreProjetService perimetre;
    private final LectureReunion lecture;
    private final EcritureReunion ecriture;
    private final ReportReunionPrecedente report;
    private final Clock clock;

    public ReunionsService(EntityManager em, DroitsOmran droits, PerimetreProjetService perimetre, LectureReunion lecture,
                           EcritureReunion ecriture, ReportReunionPrecedente report, Clock clock) {
        this.em = em;
        this.droits = droits;
        this.perimetre = perimetre;
        this.lecture = lecture;
        this.ecriture = ecriture;
        this.report = report;
        this.clock = clock;
    }

    public List<ReunionLigneDto> liste(FiltresReunions f) {
        droits.exiger(LIRE);
        Set<UUID> projets = f.projetIds().isEmpty() ? perimetre.projetsVisibles() : perimetre.restreindre(f.projetIds());
        if (projets.isEmpty()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder("""
                SELECT m, p, t, e FROM BulMeeting m JOIN PrjProject p ON p.id = m.prjProjectId
                LEFT JOIN BulMeetingType t ON t.id = m.bulMeetingTypeId LEFT JOIN CfgTier e ON e.id = m.cfgEmployeeId
                WHERE m.prjProjectId IN :projets""");
        Map<String, Object> parametres = new HashMap<>(Map.of("projets", projets));
        filtre(jpql, parametres, " AND p.cfgCompany.id = :societe", "societe", f.societeId());
        filtre(jpql, parametres, " AND m.bulMeetingTypeId = :type", "type", f.typeId());
        filtre(jpql, parametres, " AND m.meetingDate >= :du", "du", f.du() == null ? null : f.du().atStartOfDay());
        filtre(jpql, parametres, " AND m.meetingDate < :au", "au", f.au() == null ? null : f.au().plusDays(1).atStartOfDay());
        String recherche = f.recherche() == null ? "" : f.recherche().replace(" ", "").toLowerCase(Locale.ROOT);
        filtre(jpql, parametres, """
                 AND (lower(replace(m.code, ' ', '')) LIKE :recherche OR lower(replace(m.note, ' ', '')) LIKE :recherche
                 OR lower(replace(e.description, ' ', '')) LIKE :recherche)""",
                "recherche", recherche.isEmpty() ? null : "%" + recherche + "%");
        TypedQuery<Object[]> requete = em.createQuery(jpql.append(" ORDER BY m.meetingDate DESC, m.sorting DESC").toString(),
                Object[].class);
        parametres.forEach(requete::setParameter);
        return requete.getResultList().stream().map(r -> {
            BulMeeting m = (BulMeeting) r[0];
            return new ReunionLigneDto(m.getId(), LectureReunion.libelle((PrjProject) r[1]),
                    LectureReunion.libelle((BulMeetingType) r[2]), m.getCode(), LectureReunion.jour(m.getMeetingDate()),
                    LectureReunion.jour(m.getNextDate()), LectureReunion.libelle((CfgTier) r[3]), m.getNote(), m.getSorting());
        }).toList();
    }

    public ChoixReunionsDto choix(UUID projetId) {
        droits.exiger(LIRE);
        List<ReferentielItemDto> types = em.createQuery("SELECT t FROM BulMeetingType t ORDER BY t.sorting ASC, t.code ASC",
                        BulMeetingType.class).getResultList().stream()
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), LectureReunion.libelle(t), t.getSorting())).toList();
        List<ReferentielItemDto> lots = projetId == null || !perimetre.estVisible(projetId) ? List.of()
                : em.createQuery("SELECT l FROM PrjMarket l WHERE l.prjProject.id = :projet ORDER BY l.code ASC",
                        PrjMarket.class).setParameter("projet", projetId).getResultList().stream()
                .map(l -> new ReferentielItemDto(l.getId(), l.getCode(), LectureReunion.libelle(l), null)).toList();
        return new ChoixReunionsDto(types, lecture.observations(), lots);
    }

    public ReunionFicheDto fiche(UUID id) {
        droits.exiger(LIRE);
        return lecture.fiche(lecture.visible(id));
    }

    @Transactional
    public ReunionFicheDto creer(ReunionCreationDto corps) {
        droits.exiger(MODIFIER);
        if (corps.projetId() == null || corps.typeId() == null || corps.date() == null) {
            throw new InvalidRequestException("Le projet, le type et la date de la réunion sont obligatoires.");
        }
        if (!perimetre.estVisible(corps.projetId())) {
            throw new ResourceNotFoundException("Projet introuvable : " + corps.projetId());
        }
        if (em.find(BulMeetingType.class, corps.typeId()) == null) {
            throw new InvalidRequestException("Type de réunion inconnu : " + corps.typeId());
        }
        BulMeeting m = new BulMeeting();
        m.setPrjProjectId(corps.projetId());
        m.setBulMeetingTypeId(corps.typeId());
        m.setSorting(em.createQuery("""
                        SELECT COALESCE(MAX(m.sorting), 0) + 1 FROM BulMeeting m
                        WHERE m.prjProjectId = :projet AND m.bulMeetingTypeId = :type""", Integer.class)
                .setParameter("projet", corps.projetId()).setParameter("type", corps.typeId()).getSingleResult());
        appliquer(m, corps.enTete());
        ecriture.persister(m);
        report.reporter(m);
        em.flush();
        return lecture.fiche(m);
    }

    @Transactional
    public ReunionFicheDto modifier(UUID id, ReunionModificationDto corps) {
        droits.exiger(MODIFIER);
        BulMeeting m = lecture.visible(id);
        appliquer(m, corps);
        ecriture.modifier(m);
        em.flush();
        return lecture.fiche(m);
    }

    @Transactional
    public void supprimer(UUID id) {
        droits.exiger(SUPPRIMER);
        BulMeeting m = lecture.visible(id);
        List<BulMeetingLine> decisions = lecture.lignes(BulMeetingLine.class, LectureReunion.CHAMP_REUNION, List.of(id));
        lecture.refuserSiReprises(decisions, id);
        List<BulDelegate> delegues = lecture.lignes(BulDelegate.class, LectureReunion.CHAMP_REUNION, List.of(id));
        if (!delegues.isEmpty()) {
            ecriture.supprimer(lecture.lignes(BulDelegateContact.class, "bulDelegateId",
                    delegues.stream().map(BulDelegate::getId).toList()));
        }
        ecriture.supprimer(delegues);
        ecriture.supprimer(lecture.lignes(BulMeetingContact.class, LectureReunion.CHAMP_REUNION, List.of(id)));
        ecriture.supprimer(decisions);
        ecriture.supprimer(em.createQuery(
                        "SELECT a FROM BulMeetingPrjProjectProgressCriteriaLine a WHERE a.bulMeetingId = :reunion",
                        BulMeetingPrjProjectProgressCriteriaLine.class)
                .setParameter(REUNION, id).getResultList());
        ecriture.supprimer(List.of(m));
    }

    /** En-tête commun à la création et au PUT : toutes les vérifications avant le premier setter. */
    private void appliquer(BulMeeting m, ReunionModificationDto e) {
        if (e.date() == null) {
            throw new InvalidRequestException("La date de la réunion est obligatoire.");
        }
        if (e.prochaineDate() != null && !e.prochaineDate().isAfter(e.date())) {
            throw new InvalidRequestException(PROCHAINE);
        }
        if (e.surveillantId() != null && em.find(CfgTier.class, e.surveillantId()) == null) {
            throw new InvalidRequestException("Surveillant inconnu : " + e.surveillantId());
        }
        String code = reference(m, e.reference());
        m.setCode(code);
        m.setMeetingDate(e.date().atStartOfDay());
        m.setNextDate(e.prochaineDate() == null ? null : e.prochaineDate().atStartOfDay());
        m.setCfgEmployeeId(e.surveillantId());
        m.setNote(e.note());
    }

    /** D2 : Réf. saisie gardée, vide = {@code yy/} + plus grand numéro de l'année + 1 ; unique sans casse (409). */
    private String reference(BulMeeting m, String saisie) {
        String code = saisie == null ? "" : saisie.strip();
        if (code.isEmpty() && m.getCode() != null) {
            code = m.getCode();
        } else if (code.isEmpty()) {
            String prefixe = "%02d/".formatted(LocalDate.now(clock).getYear() % 100);
            ReferentielAdminService.verrouiller(em, BulMeeting.class, prefixe, LIBELLE);
            Number dernier = (Number) em.createNativeQuery("""
                            SELECT MAX(CAST(SUBSTRING(Code, 4, 5) AS int)) FROM BulMeeting
                            WHERE Code LIKE :motif AND LEN(Code) = 8""")
                    .setParameter("motif", prefixe + "[0-9][0-9][0-9][0-9][0-9]").getSingleResult();
            code = prefixe + "%05d".formatted(dernier == null ? 1 : dernier.intValue() + 1);
        }
        ReferentielAdminService.verrouiller(em, BulMeeting.class, code, LIBELLE);
        Long doublons = em.createQuery("SELECT COUNT(m) FROM BulMeeting m WHERE lower(m.code) = lower(:code) AND m.id <> :id",
                        Long.class)
                .setParameter("code", code).setParameter("id", m.getId() == null ? UUID.randomUUID() : m.getId())
                .getSingleResult();
        if (doublons > 0) {
            throw new ConflictException("La référence P.V. %s est déjà prise.".formatted(code));
        }
        return code;
    }

    private static void filtre(StringBuilder jpql, Map<String, Object> parametres, String condition, String nom,
                               Object valeur) {
        if (valeur != null) {
            jpql.append(condition);
            parametres.put(nom, valeur);
        }
    }
}
