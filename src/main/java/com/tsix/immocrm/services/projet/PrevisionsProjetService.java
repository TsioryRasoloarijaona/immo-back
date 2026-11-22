package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.AgregationPrevisionsProjet;
import com.tsix.immocrm.domain.projet.AgregationPrevisionsProjet.Montant;
import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.dto.projet.LotPrevisionProjetDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.PointAvancementDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.TotalNetAPayerDto;
import com.tsix.immocrm.dto.projet.ProjetPrevisionOptionDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.utils.TierNomFormatter;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.TypeExport.BOOLEEN;
import static com.tsix.immocrm.domain.TypeExport.DATE;
import static com.tsix.immocrm.domain.TypeExport.MONTANT;
import static com.tsix.immocrm.domain.TypeExport.NOMBRE;

/** Prévisions des lots d'un projet (FEATURE-n4-previsions-chantier.md N4a) : lecture seule, séries de la fiche lot sommées. */
@Service
@Transactional(readOnly = true)
public class PrevisionsProjetService {

    private static final String DROIT = "Yes.Prj.Front.Tdb.ProjectPrevision";
    private static final String AVANCEMENT = "avancement";

    private static final List<ColonneFixe> GENERAL = List.of(ColonneFixe.texte("netAPayer", "Net à payer"),
            ColonneFixe.de("total", "Total", MONTANT), ColonneFixe.de("due", "Due à ce jour", MONTANT),
            ColonneFixe.de("reste", "Reste", MONTANT), ColonneFixe.de(AVANCEMENT, "Avancement", NOMBRE));
    private static final List<ColonneFixe> LOTS = List.of(ColonneFixe.texte("code", "Code"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.texte("fournisseur", "Fournisseur"),
            ColonneFixe.de("multiFournisseur", "Multi-fournisseurs", BOOLEEN), ColonneFixe.texte("notes", "Notes"),
            ColonneFixe.de("cloture", "Clôturé", BOOLEEN), ColonneFixe.de("resilie", "Résilié", BOOLEEN),
            ColonneFixe.de("previsionNetAPayer", "Prévision net à payer", MONTANT),
            ColonneFixe.de("previsionNetAPayerCalculee", "Prévision calculée", MONTANT),
            ColonneFixe.de("previsionNetAPayerDifference", "Écart prévision", MONTANT),
            ColonneFixe.de("coutPrevisionnelNetAPayer", "Coût prévisionnel net à payer", MONTANT),
            ColonneFixe.de("coutPrevisionnelNetAPayerCalcule", "Coût prévisionnel calculé", MONTANT),
            ColonneFixe.de("coutPrevisionnelNetAPayerDifference", "Écart coût prévisionnel", MONTANT),
            ColonneFixe.de("positionNetAPayer", "Position net à payer", MONTANT),
            ColonneFixe.de("positionNetAPayerCalculee", "Position calculée", MONTANT),
            ColonneFixe.de("positionNetAPayerDifference", "Écart position", MONTANT));
    private static final List<ColonneFixe> DECAISSEMENT = List.of(ColonneFixe.de("date", "Date", DATE),
            ColonneFixe.texte("lot", "Lot"), ColonneFixe.texte("description", "Description"),
            ColonneFixe.de("montant", "Montant", MONTANT));
    private static final List<ColonneFixe> DECAISSEMENT_AJUSTE = Stream.concat(DECAISSEMENT.stream(),
            Stream.of(ColonneFixe.texte("nature", "Nature"))).toList();
    private static final List<ColonneFixe> AVANCEMENTS = List.of(ColonneFixe.de("mois", "Mois", DATE),
            ColonneFixe.de(AVANCEMENT, "Avancement", NOMBRE));

    private final DroitsOmran droits;
    private final PerimetreProjetService perimetre;
    private final LotsProjetService lotsProjet;
    private final SeriesPrevisionsLots series;
    private final ExportTableur tableur;
    private final EntityManager em;
    private final Clock clock;

    PrevisionsProjetService(DroitsOmran droits, PerimetreProjetService perimetre, LotsProjetService lotsProjet,
                            SeriesPrevisionsLots series, ExportTableur tableur, EntityManager em, Clock clock) {
        this.droits = droits;
        this.perimetre = perimetre;
        this.lotsProjet = lotsProjet;
        this.series = series;
        this.tableur = tableur;
        this.em = em;
        this.clock = clock;
    }

    public List<ProjetPrevisionOptionDto> projets() {
        droits.exiger(DROIT);
        Set<UUID> visibles = perimetre.projetsVisibles();
        if (visibles.isEmpty()) {
            return List.of();
        }
        return em.createQuery("select new com.tsix.immocrm.dto.projet.ProjetPrevisionOptionDto(p.id, p.code, p.description)"
                        + " from PrjProject p where p.id in :ids order by p.description", ProjetPrevisionOptionDto.class)
                .setParameter("ids", visibles).getResultList();
    }

    public PrevisionsProjetDto previsions(UUID projetId) {
        return lire(projetId);
    }

    private PrevisionsProjetDto lire(UUID projetId) {
        droits.exiger(DROIT);
        PrjProject projet = perimetre.estVisible(projetId) ? em.find(PrjProject.class, projetId) : null;
        if (projet == null) {
            throw new ResourceNotFoundException("Projet introuvable");
        }
        List<SeriesPrevisionsLots.Lot> lus = series.lire(lotsProjet.lotsDesPrevisions(projetId));
        Map<UUID, BigDecimal> ht = new HashMap<>();
        lus.stream().filter(l -> !Boolean.TRUE.equals(l.marche().getIsMultiSupplier()))
                .forEach(l -> ht.put(l.marche().getId(), l.calcul().previsionHT()));
        AgregationPrevisionsProjet agregation = new AgregationPrevisionsProjet(
                lus.stream().map(SeriesPrevisionsLots.Lot::seriesProjet).toList(), AgregationPrevisionsProjet.poids(ht), true);
        LocalDate aujourdhui = LocalDate.now(clock);
        return new PrevisionsProjetDto(projetId, projet.getDescription(), aujourdhui, agregation.totaux(aujourdhui),
                agregation.mois(), lus.stream().map(PrevisionsProjetService::ligne).toList(), agregation.decaissements(),
                agregation.avancements());
    }

    public ResponseEntity<byte[]> exporter(UUID projetId) {
        PrevisionsProjetDto p = lire(projetId);
        return tableur.reponseEnMemoire("previsions-projet", "Prévisions - " + p.projetDescription(), List.of(
                new FeuilleExport("GENERAL", GENERAL, List.of(generale("Prévision", p.totaux().prevision()),
                        generale("Prévision ajustée", p.totaux().previsionAjustee()), generale("Réel", p.totaux().reel())), null),
                new FeuilleExport("Lots", LOTS, p.lots(), null),
                new FeuilleExport("Prévision - Décaissement", DECAISSEMENT, p.decaissements().prevision(), null),
                avancements("Prévision - Avancement", p.avancements().prevision()),
                new FeuilleExport("Prévision ajustée - Décaissement", DECAISSEMENT_AJUSTE,
                        p.decaissements().previsionAjustee(), null),
                avancements("Prévision ajustée - Avancement", p.avancements().previsionAjustee()),
                new FeuilleExport("Réel - Décaissement", DECAISSEMENT, p.decaissements().reel(), null),
                avancements("Réel - Avancement", p.avancements().reel())));
    }

    private static FeuilleExport avancements(String nom, List<PointAvancementDto> points) {
        return new FeuilleExport(nom, AVANCEMENTS, points, null);
    }

    private static Map<String, Object> generale(String libelle, TotalNetAPayerDto t) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put("netAPayer", libelle);
        ligne.put("total", t.total());
        ligne.put("due", t.due());
        ligne.put("reste", t.reste());
        ligne.put(AVANCEMENT, t.avancement());
        return ligne;
    }

    /** R9 : montants de base du calcul du lot ; « calculé » = Σ de ses séries ; écart = base − calculé. */
    private static LotPrevisionProjetDto ligne(SeriesPrevisionsLots.Lot l) {
        PrjMarket m = l.marche();
        CalculLot c = l.calcul();
        BigDecimal estimation = somme(l.series().estimation());
        BigDecimal reel = somme(l.series().reel());
        BigDecimal ajustee = somme(l.series().previsions()).add(reel);
        BigDecimal prevision = c.previsionNetAPayer();
        BigDecimal cout = c.coutPrevisionnel().netAPayer();
        BigDecimal position = c.coutReel().netAPayer();
        return new LotPrevisionProjetDto(m.getId(), m.getCode(), m.getDescription(), TierNomFormatter.nom(m.getCfgSupplier()),
                Boolean.TRUE.equals(m.getIsMultiSupplier()), m.getNotes(), Boolean.TRUE.equals(m.getIsClosed()),
                c.drapeaux().annule(), prevision, estimation, prevision.subtract(estimation), cout, ajustee,
                cout.subtract(ajustee), position, reel, position.subtract(reel));
    }

    private static BigDecimal somme(List<Montant> lignes) {
        return lignes.stream().map(Montant::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
