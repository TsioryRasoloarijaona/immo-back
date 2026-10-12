package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.dto.chantier.ProjetChantierDetailDto;
import com.tsix.immocrm.dto.chantier.ProjetChantierListItemDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjMarketRepository;
import com.tsix.immocrm.repositories.prj.PrjProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.chantier.ChantierMapper.jour;
import static com.tsix.immocrm.services.chantier.ChantierMapper.lire;
import static com.tsix.immocrm.services.chantier.ChantierMapper.videEnNull;

@Service
@Transactional(readOnly = true)
public class ProjetChantierService {

    static final String PROJET_INTROUVABLE = "Projet introuvable: ";

    // Centres de coûts annuels du légataire : « 2024-GHRIZA », « 2023--AJFR »…
    private static final Pattern CODE_ANNUEL = Pattern.compile("^\\d{4}-.*");

    private final PrjProjectRepository projetRepository;
    private final PrjMarketRepository lotRepository;
    private final ChiffrageLots chiffrage;
    private final PerimetreChantierService perimetreService;

    public ProjetChantierService(PrjProjectRepository projetRepository, PrjMarketRepository lotRepository,
                                 ChiffrageLots chiffrage, PerimetreChantierService perimetreService) {
        this.projetRepository = projetRepository;
        this.lotRepository = lotRepository;
        this.chiffrage = chiffrage;
        this.perimetreService = perimetreService;
    }

    public List<ProjetChantierListItemDto> liste() {
        PerimetreChantier perimetre = perimetreService.perimetreChantierCourant();
        if (perimetre.estVide()) {
            return List.of();
        }
        List<PrjProject> projets = perimetre.toutVoir()
                ? projetRepository.findAllAvecReferentiels()
                : projetRepository.findAllAvecReferentiels(perimetre.projetIds());
        List<PrjMarket> lots = perimetre.toutVoir()
                ? lotRepository.findAvecProjet()
                : ParBlocs.executer(perimetre.projetIds(), lotRepository::findByProjetIds);
        Map<UUID, LotChiffre> chiffres = chiffrage.chiffrer(lots);
        Map<UUID, List<LotChiffre>> parProjet = lots.stream().collect(Collectors.groupingBy(
                ChantierMapper::projetId, Collectors.mapping(l -> chiffres.get(l.getId()), Collectors.toList())));
        return projets.stream()
                .sorted(Comparator.comparing(p -> Objects.toString(p.getCode(), "")))
                .map(p -> listItem(p, TotauxProjet.de(parProjet.getOrDefault(p.getId(), List.of()))))
                .toList();
    }

    public ProjetChantierDetailDto detail(UUID id) {
        PrjProject p = projetVisible(id);
        TotauxProjet t = TotauxProjet.de(chiffrage.chiffrer(lotRepository.findByProjetIds(List.of(id))).values());
        ProjetChantierListItemDto l = listItem(p, t);
        CfgTier responsable = p.getCfgTierManager();
        return new ProjetChantierDetailDto(l.id(), l.code(), l.description(), l.societeCode(),
                l.societeDescription(), l.statutCode(), l.statutDescription(), l.nbLots(), l.totalMarchesHT(),
                l.totalMarchesTTC(), l.nbDecomptes(), l.totalDecomptesTTC(), l.totalFacture(), l.totalRegle(),
                l.totalResteARegler(), l.estCentreDeCouts(),
                videEnNull(p.getDescriptionLong()), videEnNull(p.getNotes()), videEnNull(p.getAddress()),
                lire(responsable, CfgTier::getId), lire(responsable, CfgTier::getDescription),
                jour(p.getStartDate()), p.getDelay(), jour(p.getEndDate()),
                jour(p.getProvisionalWorkStartDate()), jour(p.getProvisionalCompletingWorkDate()),
                jour(p.getRealWorkStartDate()), jour(p.getRealCompletingWorkDate()),
                p.getUserCreateDateTime(), p.getUserCreate());
    }

    /** Inconnu ou hors périmètre : même 404, pour ne pas révéler l'existence du projet. */
    PrjProject projetVisible(UUID id) {
        PerimetreChantier perimetre = perimetreService.perimetreChantierCourant();
        return projetRepository.findAvecReferentiels(id)
                .filter(p -> perimetre.autorise(p.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(PROJET_INTROUVABLE + id));
    }

    private static ProjetChantierListItemDto listItem(PrjProject p, TotauxProjet t) {
        CfgCompany societe = p.getCfgCompany();
        StrStatus statut = p.getStrStatus();
        boolean centreDeCouts = t.nbLots() == 0 && p.getCode() != null && CODE_ANNUEL.matcher(p.getCode()).matches();
        return new ProjetChantierListItemDto(p.getId(), p.getCode(), p.getDescription(),
                lire(societe, CfgCompany::getCode), lire(societe, CfgCompany::getDescription),
                lire(statut, StrStatus::getCode), lire(statut, StrStatus::getDescription),
                t.nbLots(), t.marchesHT(), t.marchesTTC(), t.nbDecomptes(), t.decomptesTTC(),
                t.solde().totalFacture(), t.solde().totalRegle(), t.solde().resteARegler(), centreDeCouts);
    }
}
