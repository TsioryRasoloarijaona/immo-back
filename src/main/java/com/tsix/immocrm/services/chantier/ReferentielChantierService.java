package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.dto.chantier.ReferentielChantierDto;
import com.tsix.immocrm.dto.chantier.ReferentielsChantierDto;
import com.tsix.immocrm.dto.chantier.StatutLotDto;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.repositories.buy.BuyDocumentStatusRepository;
import com.tsix.immocrm.repositories.prj.PrjMarketRepository;
import com.tsix.immocrm.repositories.prj.PrjMarketTypeRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReferentielChantierService {

    // Codes de type « 10 », « 20 », … « 100 » : tri numérique, puis alphabétique pour un code non numérique.
    private static final Comparator<ReferentielChantierDto> PAR_CODE_NUMERIQUE = Comparator
            .comparingInt((ReferentielChantierDto r) -> Objects.toString(r.code(), "").length())
            .thenComparing(r -> Objects.toString(r.code(), ""));

    private final PrjMarketTypeRepository typeRepository;
    private final StrStatusRepository statutRepository;
    private final BuyDocumentStatusRepository statutFactureRepository;
    private final PrjMarketRepository lotRepository;
    private final PerimetreChantierService perimetreService;

    public ReferentielChantierService(PrjMarketTypeRepository typeRepository, StrStatusRepository statutRepository,
                                      BuyDocumentStatusRepository statutFactureRepository,
                                      PrjMarketRepository lotRepository, PerimetreChantierService perimetreService) {
        this.typeRepository = typeRepository;
        this.statutRepository = statutRepository;
        this.statutFactureRepository = statutFactureRepository;
        this.lotRepository = lotRepository;
        this.perimetreService = perimetreService;
    }

    public ReferentielsChantierDto referentiels() {
        List<ReferentielChantierDto> types = typeRepository.findAll().stream()
                .map(t -> new ReferentielChantierDto(t.getId(), t.getCode(), t.getDescription(), null))
                .sorted(PAR_CODE_NUMERIQUE)
                .toList();
        List<StatutLotDto> statuts = statutRepository.findStatutsDesLots().stream()
                .map(ReferentielChantierService::statutLot)
                .toList();
        List<ReferentielChantierDto> statutsFacture = statutFactureRepository.findAll(Sort.by("sorting", "code"))
                .stream()
                .map(s -> new ReferentielChantierDto(s.getId(), s.getCode(), s.getDescription(), s.getSorting()))
                .toList();
        return new ReferentielsChantierDto(types, statuts, statutsFacture, categoriesDuPerimetre());
    }

    // SQL natif : le pilote peut rendre un uniqueidentifier en UUID ou en texte selon la version.
    private static StatutLotDto statutLot(Object[] r) {
        UUID id = r[0] instanceof UUID uuid ? uuid : UUID.fromString(r[0].toString());
        Integer ordre = r[3] == null ? null : ((Number) r[3]).intValue();
        return new StatutLotDto(id, (String) r[1], (String) r[2], ordre, (String) r[4], (String) r[5]);
    }

    private List<ReferentielChantierDto> categoriesDuPerimetre() {
        PerimetreChantier perimetre = perimetreService.perimetreChantierCourant();
        if (perimetre.estVide()) {
            return List.of();
        }
        List<StlCategory> categories = perimetre.toutVoir()
                ? lotRepository.findCategoriesReferencees()
                : lotRepository.findCategoriesReferencees(perimetre.projetIds());
        return categories.stream()
                .map(c -> new ReferentielChantierDto(c.getId(), c.getCode(), c.getDescription(), c.getSorting()))
                .sorted(Comparator.comparing(ReferentielChantierDto::code, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
