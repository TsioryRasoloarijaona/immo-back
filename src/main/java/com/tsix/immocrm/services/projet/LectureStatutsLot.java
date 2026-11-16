package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.achat.config.RoleTransitionDto;
import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto;
import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto.CaseStatutLotDto;
import com.tsix.immocrm.dto.projet.CatalogueStatutLotDto.ChoixStatutLotDto;
import com.tsix.immocrm.dto.projet.StatutLotDto;
import com.tsix.immocrm.dto.projet.TransitionStatutLotDto;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatusEcriture;
import com.tsix.immocrm.entities.str.StrStatusMandatoryField;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.config.CatalogueStatutAchat;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.uuid;

/** Statuts et transitions des 5 genres de lot (FEATURE-restes-projet-achat.md §6), filtrés par code de genre. */
@Service
@Transactional(readOnly = true)
public class LectureStatutsLot {

    static final List<String> GENRES = List.of("PrjMarket", "PrjMarketMultiSupplier", "PrjMarketAdministrative",
            "PrjMarketStudies", "PrjMarketDealers");
    private static final String GENRES_PARAM = "genres";
    private static final String TYPES = "SELECT PKey, Code, Description FROM StrStatusType ORDER BY Sorting ASC, Code ASC";
    private static final String CHAMPS = """
            SELECT PKey, Code, Description FROM StrField WHERE StrConfigEntityId = :genre ORDER BY Sorting ASC, Code ASC""";
    private static final String LIBELLES_CASES = """
            SELECT BoolField001Label, BoolField002Label, BoolField003Label, BoolField004Label, BoolField005Label,
                   BoolField006Label, BoolField007Label, BoolField008Label, BoolField009Label, BoolField010Label,
                   BoolField011Label, BoolField012Label, BoolField013Label, BoolField014Label, BoolField015Label,
                   BoolField016Label, BoolField017Label, BoolField018Label, BoolField019Label, BoolField020Label
            FROM StrEntity WHERE PKey = :genre""";
    private static final String ROLES = """
            SELECT l.StrWorkFlowId, r.PKey, r.Code FROM StrWorkFlowIntervener l
            JOIN StrIntervener i ON i.PKey = l.StrIntervenerId JOIN AdmRole r ON r.PKey = i.AdmRoleId
            JOIN StrWorkFlow w ON w.PKey = l.StrWorkFlowId JOIN StrEntity e ON e.PKey = w.StrConfigEntityId
            WHERE e.Code IN (:genres) ORDER BY r.Code ASC""";

    private final EntityManager em;

    public LectureStatutsLot(EntityManager em) {
        this.em = em;
    }

    /** Un des 5 genres de lot, vide pour tout autre {@code StrEntity}. */
    public Optional<StrEntity> genre(UUID id) {
        return Optional.ofNullable(id == null ? null : genres().get(id));
    }

    public List<StatutLotDto> statutsLot() {
        Map<UUID, StrEntity> genres = genres();
        if (genres.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> types = lignes(TYPES, null).stream()
                .filter(r -> r[2] != null).collect(Collectors.toMap(r -> uuid(r[0]), r -> (String) r[2]));
        Map<UUID, List<UUID>> champs = em.createQuery("""
                        SELECT m FROM StrStatusMandatoryField m, StrStatusEcriture s
                        WHERE s.id = m.strStatusId AND s.strConfigEntityId IN :genres""", StrStatusMandatoryField.class)
                .setParameter(GENRES_PARAM, genres.keySet()).getResultStream()
                .collect(Collectors.groupingBy(StrStatusMandatoryField::getStrStatusId,
                        Collectors.mapping(StrStatusMandatoryField::getStrFieldId, Collectors.toList())));
        return statuts(genres).stream().map(s -> {
            StrEntity g = genres.get(s.getStrConfigEntityId());
            return new StatutLotDto(s.getId(), s.getCode(), s.getDescription(), s.getSorting(), s.getColorName(),
                    g.getId(), g.getCode(), g.getDescription(), types.get(s.getStrStatusTypeId()), s.getStrStatusTypeId(),
                    s.getDelay(), s.getCfgImageId(), s.getNotes(), IntStream.rangeClosed(1, StrStatusEcriture.NOMBRE_CASES)
                    .filter(s::coche).boxed().toList(), champs.getOrDefault(s.getId(), List.of()));
        }).toList();
    }

    public StatutLotDto statut(UUID id) {
        return statutsLot().stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Statut de lot introuvable : " + id));
    }

    public CatalogueStatutLotDto catalogue(UUID genreId) {
        StrEntity genre = genre(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre de lot introuvable : " + genreId));
        Object[] libelles = lignes(LIBELLES_CASES, genre.getId()).get(0);
        List<CaseStatutLotDto> cases = IntStream.rangeClosed(1, StrStatusEcriture.NOMBRE_CASES)
                .filter(n -> libelles[n - 1] instanceof String l && !l.isBlank())
                .mapToObj(n -> new CaseStatutLotDto(n, ((String) libelles[n - 1]).trim())).toList();
        return new CatalogueStatutLotDto(choix(TYPES, null), CatalogueStatutAchat.couleurs(), cases,
                choix(CHAMPS, genre.getId()));
    }

    public List<TransitionStatutLotDto> transitions() {
        Map<UUID, StrEntity> genres = genres();
        if (genres.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> codes = statuts(genres).stream()
                .collect(Collectors.toMap(StrStatusEcriture::getId, s -> String.valueOf(s.getCode())));
        Map<UUID, List<RoleTransitionDto>> roles = lignes(ROLES, GENRES_PARAM, GENRES).stream()
                .collect(Collectors.groupingBy(r -> uuid(r[0]), Collectors.mapping(
                        r -> new RoleTransitionDto(uuid(r[1]), (String) r[2]), Collectors.toList())));
        return em.createQuery("SELECT w FROM StrWorkFlow w WHERE w.strConfigEntityId IN :genres", StrWorkFlow.class)
                .setParameter(GENRES_PARAM, genres.keySet()).getResultStream()
                .sorted(parGenre(genres, StrWorkFlow::getStrConfigEntityId, StrWorkFlow::getSorting))
                .map(w -> new TransitionStatutLotDto(w.getId(), w.getStrConfigEntityId(),
                        genres.get(w.getStrConfigEntityId()).getDescription(), w.getSorting(), w.getCode(),
                        w.getDescription(), w.getStatusFromId(), codes.get(w.getStatusFromId()), w.getStatusToId(),
                        codes.get(w.getStatusToId()), Boolean.TRUE.equals(w.getWithComment()),
                        Boolean.TRUE.equals(w.getWithPassword()), Boolean.TRUE.equals(w.getWithAutomaticTransition()),
                        roles.getOrDefault(w.getId(), List.of()).stream().distinct().toList()))
                .toList();
    }

    public TransitionStatutLotDto transition(UUID id) {
        return transitions().stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transition de lot introuvable : " + id));
    }

    private Map<UUID, StrEntity> genres() {
        return em.createQuery("SELECT e FROM StrEntity e WHERE e.code IN :genres ORDER BY e.code", StrEntity.class)
                .setParameter(GENRES_PARAM, GENRES).getResultStream()
                .collect(Collectors.toMap(StrEntity::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private List<StrStatusEcriture> statuts(Map<UUID, StrEntity> genres) {
        return em.createQuery("SELECT s FROM StrStatusEcriture s WHERE s.strConfigEntityId IN :genres",
                        StrStatusEcriture.class).setParameter(GENRES_PARAM, genres.keySet()).getResultStream()
                .sorted(parGenre(genres, StrStatusEcriture::getStrConfigEntityId, StrStatusEcriture::getSorting))
                .toList();
    }

    private static <T> Comparator<T> parGenre(Map<UUID, StrEntity> genres, Function<T, UUID> genre,
                                              Function<T, Integer> ordre) {
        return Comparator.<T, String>comparing(t -> genres.get(genre.apply(t)).getCode())
                .thenComparing(ordre, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private List<ChoixStatutLotDto> choix(String requete, UUID genre) {
        return lignes(requete, genre).stream().map(r -> new ChoixStatutLotDto(uuid(r[0]), (String) r[1],
                r[2] == null ? (String) r[1] : (String) r[2])).toList();
    }

    private List<Object[]> lignes(String requete, UUID genre) {
        return lignes(requete, "genre", genre);
    }

    private List<Object[]> lignes(String requete, String parametre, Object valeur) {
        Query q = em.createNativeQuery(requete);
        if (requete.contains(":" + parametre)) {
            q.setParameter(parametre, valeur);
        }
        return ((List<?>) q.getResultList()).stream().map(Object[].class::cast).toList();
    }
}
