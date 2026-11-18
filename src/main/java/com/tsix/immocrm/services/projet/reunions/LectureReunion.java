package com.tsix.immocrm.services.projet.reunions;

import com.tsix.immocrm.dto.projet.reunions.AvancementsDto;
import com.tsix.immocrm.dto.projet.reunions.ContactReunionDto;
import com.tsix.immocrm.dto.projet.reunions.DecisionDto;
import com.tsix.immocrm.dto.projet.reunions.IntervenantDto;
import com.tsix.immocrm.dto.projet.reunions.ObservationDto;
import com.tsix.immocrm.dto.projet.reunions.ReunionFicheDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.bul.BulDelegate;
import com.tsix.immocrm.entities.bul.BulDelegateContact;
import com.tsix.immocrm.entities.bul.BulMeeting;
import com.tsix.immocrm.entities.bul.BulMeetingContact;
import com.tsix.immocrm.entities.bul.BulMeetingLine;
import com.tsix.immocrm.entities.bul.BulMeetingPrjProjectProgressCriteriaLine;
import com.tsix.immocrm.entities.bul.BulMeetingType;
import com.tsix.immocrm.entities.bul.BulObservationStatus;
import com.tsix.immocrm.entities.bul.ContactReunionOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Réunion visible (D4) et sa fiche ; joint la transaction de l'appelant. */
@Component
public class LectureReunion {

    private static final String REUNION = "reunion";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final EntityManager em;
    private final PerimetreProjetService perimetre;

    public LectureReunion(EntityManager em, PerimetreProjetService perimetre) {
        this.em = em;
        this.perimetre = perimetre;
    }

    /** 404 si la réunion n'existe pas ou si son projet n'est pas habilité. */
    public BulMeeting visible(UUID id) {
        BulMeeting m = em.find(BulMeeting.class, id);
        if (m == null || !perimetre.estVisible(m.getPrjProjectId())) {
            throw new ResourceNotFoundException("Réunion introuvable : " + id);
        }
        return m;
    }

    public ReunionFicheDto fiche(BulMeeting m) {
        UUID id = m.getId();
        List<BulMeetingContact> maitres = lignes(BulMeetingContact.class, "bulMeetingId", List.of(id));
        List<BulDelegate> delegues = lignes(BulDelegate.class, "bulMeetingId", List.of(id));
        List<BulDelegateContact> contacts = delegues.isEmpty() ? List.of()
                : lignes(BulDelegateContact.class, "bulDelegateId", delegues.stream().map(BulDelegate::getId).toList());
        List<BulMeetingLine> decisions = lignes(BulMeetingLine.class, "bulMeetingId", List.of(id));
        Map<UUID, String> tiers = libelles(CfgTier.class, Stream.of(
                Stream.of(m.getCfgEmployeeId()),
                maitres.stream().map(ContactReunionOmran::getContactId),
                contacts.stream().map(ContactReunionOmran::getContactId),
                delegues.stream().map(BulDelegate::getCfgSupplierId),
                decisions.stream().map(BulMeetingLine::getCfgSupplierId)).flatMap(Function.identity()),
                CfgTier::getId, LectureReunion::libelle);
        Map<UUID, String> lots = libelles(PrjMarket.class, Stream.concat(delegues.stream().map(BulDelegate::getPrjMarketId),
                decisions.stream().map(BulMeetingLine::getPrjMarketId)), PrjMarket::getId, LectureReunion::libelle);
        Map<UUID, ObservationDto> observations = observations().stream()
                .collect(Collectors.toMap(ObservationDto::id, Function.identity()));
        return new ReunionFicheDto(id, m.getSorting(), m.getCode(), m.getPrjProjectId(),
                libelle(em.find(PrjProject.class, m.getPrjProjectId())), m.getBulMeetingTypeId(),
                libelle(m.getBulMeetingTypeId() == null ? null : em.find(BulMeetingType.class, m.getBulMeetingTypeId())),
                jour(m.getMeetingDate()), jour(m.getNextDate()), m.getCfgEmployeeId(), tiers.get(m.getCfgEmployeeId()),
                m.getNote(),
                maitres.stream().map(c -> contact(c, tiers)).toList(),
                delegues.stream().map(d -> new IntervenantDto(d.getId(), d.getSorting(), d.getEffectif(), d.getPrjMarketId(),
                        lots.get(d.getPrjMarketId()), d.getCfgSupplierId(), tiers.get(d.getCfgSupplierId()), d.getNotes(),
                        contacts.stream().filter(c -> d.getId().equals(c.getBulDelegateId())).map(c -> contact(c, tiers))
                                .toList())).toList(),
                decisions.stream().map(l -> decision(l, lots, tiers, observations)).toList(),
                avancements(m));
    }

    /** Statuts de réserve par O.D.T. */
    public List<ObservationDto> observations() {
        return em.createQuery("SELECT s FROM BulObservationStatus s ORDER BY s.sorting ASC, s.code ASC",
                BulObservationStatus.class).getResultList().stream().map(LectureReunion::observation).toList();
    }

    static ObservationDto observation(BulObservationStatus s) {
        return new ObservationDto(s.getId(), libelle(s), s.getColorName(), Boolean.TRUE.equals(s.getIsClosed()));
    }

    static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }

    static String libelle(CodeLibelleOmran o) {
        return o == null ? null : libelle(o.getCode(), o.getDescription());
    }

    static String libelle(CfgTier t) {
        return t == null ? null : libelle(t.getCode(), t.getDescription());
    }

    private static String libelle(String code, String description) {
        return description == null || description.isBlank() ? code : description;
    }

    /** Lignes de {@code classe} dont {@code propriete} est dans {@code parents}, par O.D.T. */
    <T> List<T> lignes(Class<T> classe, String propriete, Collection<UUID> parents) {
        return em.createQuery("SELECT l FROM %s l WHERE l.%s IN :parents ORDER BY l.sorting ASC"
                        .formatted(classe.getSimpleName(), propriete), classe)
                .setParameter("parents", parents).getResultList();
    }

    private <T> Map<UUID, String> libelles(Class<T> classe, Stream<UUID> ids, Function<T, UUID> cle,
                                           Function<T, String> libelle) {
        List<UUID> cles = ids.filter(Objects::nonNull).distinct().toList();
        if (cles.isEmpty()) {
            return Collections.emptyMap(); // pas Map.of() : get(null) y lève une NPE (lot ou tiers absent)
        }
        return em.createQuery("SELECT o FROM %s o WHERE o.id IN :ids".formatted(classe.getSimpleName()), classe)
                .setParameter("ids", cles).getResultList().stream()
                .collect(Collectors.toMap(cle, o -> Objects.toString(libelle.apply(o), "")));
    }

    private static ContactReunionDto contact(ContactReunionOmran c, Map<UUID, String> tiers) {
        return new ContactReunionDto(c.getId(), c.getSorting(), c.getContactId(), tiers.get(c.getContactId()),
                Boolean.TRUE.equals(c.getIsPresent()), Boolean.TRUE.equals(c.getNotify()));
    }

    private static DecisionDto decision(BulMeetingLine l, Map<UUID, String> lots, Map<UUID, String> tiers,
                                        Map<UUID, ObservationDto> observations) {
        ObservationDto observation = l.getBulObservationStatusId() == null ? null
                : observations.get(l.getBulObservationStatusId());
        return new DecisionDto(l.getId(), l.getSorting(), l.getPrjMarketId(), lots.get(l.getPrjMarketId()),
                l.getCfgSupplierId(), tiers.get(l.getCfgSupplierId()), l.getDescription(), observation, jour(l.getDueDate()),
                l.getNote(), Boolean.TRUE.equals(l.getIsInternal()), observation != null && observation.ferme());
    }

    private AvancementsDto avancements(BulMeeting m) {
        List<ReferentielItemDto> criteres = em.createQuery("""
                        SELECT new com.tsix.immocrm.dto.referentiel.ReferentielItemDto(c.id, c.description, c.description,
                            c.sorting)
                        FROM PrjProjectProgressCriteriaLine c WHERE c.prjProjectId = :projet
                        ORDER BY c.sorting ASC, c.description ASC""", ReferentielItemDto.class)
                .setParameter("projet", m.getPrjProjectId()).getResultList();
        List<ReferentielItemDto> niveaux = niveaux(m.getPrjProjectId());
        List<AvancementsDto.Valeur> valeurs = em.createQuery(
                        "SELECT a FROM BulMeetingPrjProjectProgressCriteriaLine a WHERE a.bulMeetingId = :reunion",
                        BulMeetingPrjProjectProgressCriteriaLine.class)
                .setParameter(REUNION, m.getId()).getResultList().stream()
                .filter(a -> a.getAchievement() != null)
                .map(a -> new AvancementsDto.Valeur(a.getPrjProjectProgressCriteriaLineId(),
                        a.getCfgTrancheLevelingDefinitionId(), a.getAchievement().multiply(CENT).stripTrailingZeros()))
                .toList();
        return new AvancementsDto(criteres, niveaux, valeurs);
    }

    /** Niveaux de nivellement du projet ({@code CfgTrancheLevelingDefinition.CfgTrancheId} cite {@code PrjProject}). */
    // SELECT * : Code et Sorting manquent tant que ci/schema/195 n'est pas passé (yesRemsTest n'a que PKey, CfgTrancheId).
    List<ReferentielItemDto> niveaux(UUID projet) {
        List<Tuple> lignes = em.createNativeQuery("SELECT * FROM CfgTrancheLevelingDefinition WHERE CfgTrancheId = :projet",
                Tuple.class).setParameter("projet", projet).getResultList();
        return lignes.stream().map(t -> {
            String code = (String) colonne(t, "Code");
            Number ordre = (Number) colonne(t, "Sorting");
            return new ReferentielItemDto(uuid(colonne(t, "PKey")), code, code, ordre == null ? null : ordre.intValue());
        }).sorted(Comparator.comparing(ReferentielItemDto::sorting, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ReferentielItemDto::code, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    private static Object colonne(Tuple t, String nom) {
        return t.getElements().stream().map(TupleElement::getAlias).filter(nom::equalsIgnoreCase).findFirst()
                .map(t::get).orElse(null);
    }

    static UUID uuid(Object valeur) {
        return valeur instanceof UUID u ? u : UUID.fromString(valeur.toString());
    }
}
