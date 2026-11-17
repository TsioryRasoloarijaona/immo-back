package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ReceptionDetailDto;
import com.tsix.immocrm.dto.projet.ReceptionDetailDto.ReserveDto;
import com.tsix.immocrm.dto.projet.ReceptionDetailDto.TiersInviteDto;
import com.tsix.immocrm.dto.projet.ReceptionSaisieDto;
import com.tsix.immocrm.dto.projet.ReceptionSaisieDto.ReserveSaisieDto;
import com.tsix.immocrm.dto.projet.ReceptionSaisieDto.TiersInviteSaisieDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketReception;
import com.tsix.immocrm.entities.prj.PrjMarketReceptionCfgTier;
import com.tsix.immocrm.entities.prj.PrjMarketReceptionReserve;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.projet.GrillesLot.Grille;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tsix.immocrm.services.projet.ValidationBordereau.TEXTE;
import static com.tsix.immocrm.services.projet.ValidationBordereau.exiger;
import static com.tsix.immocrm.services.projet.ValidationBordereau.longueur;
import static com.tsix.immocrm.services.projet.ValidationBordereau.sansNul;

/** Fiche réception du lot (FEATURE-lot-avenants-avances.md §4.3, R13-R16) : aucun droit propre, pas de recalcul des prévisions. */
@Service
public class EcritureReceptionLot {

    private static final String ENREGISTRE = "Réception enregistrée";
    private static final String RESERVES = "reserves[].id";
    private static final String TIERS = "tiersInvites[].id";
    private static final String TYPE_EXISTE = "SELECT COUNT(*) FROM PrjMarketReceptionType WHERE PKey = :id";
    private static final String GENRE_EXISTE = "SELECT COUNT(*) FROM PrjMarketReceptionKind WHERE PKey = :id";

    private final LotsProjetService lots;
    private final GrillesLot grilles;
    private final EntityManager em;

    EcritureReceptionLot(LotsProjetService lots, GrillesLot grilles, EntityManager em) {
        this.lots = lots;
        this.grilles = grilles;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public ReceptionDetailDto lire(UUID lot, UUID reception) {
        lots.visible(lot);
        return detail(receptionDuLot(lot, reception));
    }

    @Transactional
    public ReceptionDetailDto creer(UUID lotId, ReceptionSaisieDto corps) {
        PrjMarket lot = lots.visible(lotId);
        PrjMarketReception r = new PrjMarketReception();
        r.setId(UUID.randomUUID());
        r.setPrjMarketId(lotId);
        ecrire(lot, r, corps, true);
        return detail(r);
    }

    @Transactional
    public ReceptionDetailDto modifier(UUID lotId, UUID reception, ReceptionSaisieDto corps) {
        PrjMarket lot = lots.visible(lotId);
        PrjMarketReception r = receptionDuLot(lotId, reception);
        ecrire(lot, r, corps, false);
        return detail(r);
    }

    @Transactional
    public void supprimer(UUID lotId, UUID reception) {
        PrjMarket lot = lots.visible(lotId);
        PrjMarketReception r = receptionDuLot(lotId, reception);
        grilles.enBase(reserves(reception), reception).values().forEach(em::remove);
        grilles.enBase(tiers(reception), reception).values().forEach(em::remove);
        em.flush();
        em.remove(r);
        grilles.tracer(lot, ENREGISTRE);
    }

    private void ecrire(PrjMarket lot, PrjMarketReception r, ReceptionSaisieDto corps, boolean nouvelle) {
        valider(corps);
        Grille<ReserveSaisieDto, PrjMarketReceptionReserve> gr = reserves(r.getId());
        Grille<TiersInviteSaisieDto, PrjMarketReceptionCfgTier> gt = tiers(r.getId());
        Map<UUID, PrjMarketReceptionReserve> reservesEnBase = nouvelle ? new HashMap<>() : grilles.enBase(gr, r.getId());
        Map<UUID, PrjMarketReceptionCfgTier> tiersEnBase = nouvelle ? new HashMap<>() : grilles.enBase(gt, r.getId());
        grilles.refuserIds(gr, corps.reserves(), reservesEnBase, RESERVES);
        grilles.refuserIds(gt, corps.tiersInvites(), tiersEnBase, TIERS);
        GrillesLot.Ecriture e = grilles.ecriture();
        if (corps.ordre() != null) {
            r.setSorting(corps.ordre());
        } else if (nouvelle) {
            r.setSorting(rangSuivant(r.getPrjMarketId()));
        }
        r.setCode(corps.code());
        r.setDescription(corps.description());
        r.setDocDate(corps.dateReception().atStartOfDay());
        r.setMeetingPlace(corps.lieu());
        r.setRequiredBySupplier(corps.demandeeParFournisseur());
        r.setPrjMarketReceptionTypeId(corps.typeId());
        r.setPrjMarketReceptionKindId(corps.genreId());
        if (nouvelle) {
            e.cree(r);
            em.persist(r);
        } else {
            e.modifie(r);
        }
        grilles.enregistrer(gr, rangees(corps.reserves(), ReserveSaisieDto::ordre,
                (s, o) -> new ReserveSaisieDto(s.id(), o, s.description(), s.levee())), reservesEnBase, e);
        grilles.enregistrer(gt, rangees(corps.tiersInvites(), TiersInviteSaisieDto::ordre,
                (s, o) -> new TiersInviteSaisieDto(s.id(), o, s.tiersId())), tiersEnBase, e);
        grilles.tracer(lot, ENREGISTRE);
    }

    private int rangSuivant(UUID lot) {
        return Math.toIntExact(em.createQuery("select count(r) from PrjMarketReception r where r.prjMarketId = :lot", Long.class)
                .setParameter("lot", lot).getSingleResult()) + 1;
    }

    /** Ordre absent = rang dans l'envoi. */
    private static <S> List<S> rangees(List<S> envoi, Function<S, Integer> ordre, BiFunction<S, Integer, S> avec) {
        return IntStream.range(0, envoi.size())
                .mapToObj(i -> ordre.apply(envoi.get(i)) == null ? avec.apply(envoi.get(i), i + 1) : envoi.get(i)).toList();
    }

    private void valider(ReceptionSaisieDto corps) {
        exiger(StringUtils.hasText(corps.code()), "code : obligatoire");
        exiger(corps.dateReception() != null, "dateReception : obligatoire");
        exiger(corps.typeId() != null && existe(TYPE_EXISTE, corps.typeId()), "typeId : type de réception inconnu");
        exiger(corps.genreId() != null && existe(GENRE_EXISTE, corps.genreId()), "genreId : genre de réception inconnu");
        longueur(corps.code(), TEXTE, "code");
        longueur(corps.description(), TEXTE, "description");
        longueur(corps.lieu(), TEXTE, "lieu");
        exiger(sansNul(corps.reserves()) && sansNul(corps.tiersInvites()), "reserves, tiersInvites : listes obligatoires, sans élément nul");
        corps.reserves().forEach(s -> {
            exiger(StringUtils.hasText(s.description()), "reserves[].description : obligatoire");
            longueur(s.description(), TEXTE, "reserves[].description");
        });
        List<UUID> cites = corps.tiersInvites().stream().map(TiersInviteSaisieDto::tiersId).toList();
        Set<UUID> uniques = new HashSet<>(cites);
        exiger(!uniques.contains(null) && uniques.size() == cites.size(), "tiersInvites[].tiersId : obligatoire, un tiers une seule fois");
        exiger(uniques.isEmpty() || em.createQuery("select count(t) from CfgTier t where t.id in :ids", Long.class)
                .setParameter("ids", uniques).getSingleResult() == uniques.size(), "tiersInvites[].tiersId : tiers inconnu");
    }

    private boolean existe(String requete, UUID id) {
        return ((Number) em.createNativeQuery(requete).setParameter("id", id).getSingleResult()).intValue() > 0;
    }

    private PrjMarketReception receptionDuLot(UUID lot, UUID reception) {
        PrjMarketReception r = em.find(PrjMarketReception.class, reception);
        if (r == null || !lot.equals(r.getPrjMarketId())) {
            throw new ResourceNotFoundException("Réception introuvable");
        }
        return r;
    }

    private static Grille<ReserveSaisieDto, PrjMarketReceptionReserve> reserves(UUID reception) {
        return new Grille<>(PrjMarketReceptionReserve.class, "prjMarketReceptionId", ReserveSaisieDto::id,
                PrjMarketReceptionReserve::getId, id -> {
            PrjMarketReceptionReserve l = new PrjMarketReceptionReserve();
            l.setId(id);
            l.setPrjMarketReceptionId(reception);
            return l;
        }, (l, s) -> {
            l.setSorting(s.ordre());
            l.setDescription(s.description());
            l.setIsFixed(s.levee());
        }, l -> List.of(Objects.toString(l.getSorting()), l.getDescription(), Objects.toString(l.getIsFixed())));
    }

    private static Grille<TiersInviteSaisieDto, PrjMarketReceptionCfgTier> tiers(UUID reception) {
        return new Grille<>(PrjMarketReceptionCfgTier.class, "prjMarketReceptionId", TiersInviteSaisieDto::id,
                PrjMarketReceptionCfgTier::getId, id -> {
            PrjMarketReceptionCfgTier l = new PrjMarketReceptionCfgTier();
            l.setId(id);
            l.setPrjMarketReceptionId(reception);
            return l;
        }, (l, s) -> {
            l.setSorting(s.ordre());
            l.setCfgTierId(s.tiersId());
        }, l -> List.of(Objects.toString(l.getSorting()), Objects.toString(l.getCfgTierId())));
    }

    // ------------------------------------------------------------------ lecture

    private ReceptionDetailDto detail(PrjMarketReception r) {
        Object[] libelles = (Object[]) em.createNativeQuery("""
                        SELECT (SELECT t.Description FROM PrjMarketReceptionType t WHERE t.PKey = :t),
                               (SELECT k.Description FROM PrjMarketReceptionKind k WHERE k.PKey = :k),
                               (SELECT k.IsFinal FROM PrjMarketReceptionKind k WHERE k.PKey = :k)""")
                .setParameter("t", r.getPrjMarketReceptionTypeId()).setParameter("k", r.getPrjMarketReceptionKindId())
                .getSingleResult();
        List<ReserveDto> reserves = grilles.enBase(reserves(r.getId()), r.getId()).values().stream()
                .sorted(Comparator.comparing(PrjMarketReceptionReserve::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(l -> new ReserveDto(l.getId(), l.getSorting(), l.getDescription(), Boolean.TRUE.equals(l.getIsFixed())))
                .toList();
        List<PrjMarketReceptionCfgTier> invites = grilles.enBase(tiers(r.getId()), r.getId()).values().stream()
                .sorted(Comparator.comparing(PrjMarketReceptionCfgTier::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Set<UUID> ids = invites.stream().map(PrjMarketReceptionCfgTier::getCfgTierId).collect(Collectors.toSet());
        Map<UUID, String> noms = ids.isEmpty() ? Map.of() : em.createQuery("select t from CfgTier t where t.id in :ids", CfgTier.class)
                .setParameter("ids", ids).getResultList().stream()
                .collect(Collectors.toMap(CfgTier::getId, t -> Objects.toString(TierNomFormatter.nom(t), "")));
        return new ReceptionDetailDto(r.getId(), r.getPrjMarketId(), r.getSorting(), r.getCode(), r.getDescription(),
                r.getDocDate() == null ? null : r.getDocDate().toLocalDate(), r.getMeetingPlace(),
                Boolean.TRUE.equals(r.getRequiredBySupplier()), r.getPrjMarketReceptionTypeId(), (String) libelles[0],
                r.getPrjMarketReceptionKindId(), (String) libelles[1], Boolean.TRUE.equals(libelles[2]) || Integer.valueOf(1).equals(libelles[2]),
                reserves, invites.stream().map(t -> new TiersInviteDto(t.getId(), t.getSorting(), t.getCfgTierId(),
                        noms.get(t.getCfgTierId()))).toList());
    }
}
