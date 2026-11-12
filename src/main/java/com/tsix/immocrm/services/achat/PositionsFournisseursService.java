package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.PositionFournisseur;
import com.tsix.immocrm.dto.achat.FiltresPositionsFournisseurs;
import com.tsix.immocrm.dto.achat.LignePositionDto;
import com.tsix.immocrm.dto.achat.PositionFournisseurDto;
import com.tsix.immocrm.dto.achat.PositionsFournisseursDto;
import com.tsix.immocrm.services.achat.RequetesPositionFournisseur.Mouvement;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Soldes fournisseurs et lignes de solde : mouvements visibles regroupés ici, additionnés par {@link PositionFournisseur}. */
@Service
@Transactional(readOnly = true)
public class PositionsFournisseursService {

    private static final Comparator<PositionFournisseurDto> TRI = Comparator
            .comparing(PositionFournisseurDto::fournisseurNom, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PositionFournisseurDto::societeCode, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<LignePositionDto> TRI_LIGNES = Comparator
            .comparing(LignePositionDto::dateDocument, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(LignePositionDto::code, Comparator.nullsLast(Comparator.naturalOrder()));

    private record Cle(UUID fournisseurId, String fournisseurNom, UUID societeId, String societeCode) {

        PositionFournisseurDto dto(PositionFournisseur p) {
            return new PositionFournisseurDto(fournisseurId, fournisseurNom, societeId, societeCode, p.soldeDebut(),
                    p.netAPayer(), p.regle(), p.soldeFin());
        }
    }

    private final PerimetreProjetService perimetre;
    private final RequetesPositionFournisseur requetes;

    public PositionsFournisseursService(PerimetreProjetService perimetre, RequetesPositionFournisseur requetes) {
        this.perimetre = perimetre;
        this.requetes = requetes;
    }

    public PositionsFournisseursDto positions(FiltresPositionsFournisseurs f) {
        String motif = f.search() == null ? "" : SoldesFournisseursService.sansEspaces(f.search());
        Map<Cle, PositionFournisseur> retenues = new LinkedHashMap<>();
        mouvements(f, f.societeIds(), null).stream()
                .filter(m -> motif.isEmpty()
                        || SoldesFournisseursService.sansEspaces(Objects.toString(m.fournisseurNom(), "")).contains(motif))
                .collect(Collectors.groupingBy(m -> new Cle(m.fournisseurId(), m.fournisseurNom(), m.societeId(),
                        m.societeCode()), LinkedHashMap::new, Collectors.mapping(Mouvement::position, Collectors.toList())))
                .forEach((cle, mouvements) -> {
                    PositionFournisseur p = PositionFournisseur.de(mouvements, f.du(), f.au());
                    if (!Boolean.TRUE.equals(f.masquerResteZero()) || !p.resteZero()) {
                        retenues.put(cle, p);
                    }
                });
        List<PositionFournisseurDto> lignes = retenues.entrySet().stream()
                .map(e -> e.getKey().dto(e.getValue())).sorted(TRI).toList();
        return new PositionsFournisseursDto(lignes,
                new Cle(null, null, null, null).dto(PositionFournisseur.total(retenues.values())));
    }

    public List<LignePositionDto> lignes(UUID fournisseurId, UUID societeId, FiltresPositionsFournisseurs f) {
        return mouvements(f, List.of(societeId), fournisseurId).stream().map(Mouvement::ligne)
                .filter(l -> PositionFournisseur.dansPeriode(l.dateDocument(), f.du(), f.au()))
                .sorted(TRI_LIGNES).toList();
    }

    private List<Mouvement> mouvements(FiltresPositionsFournisseurs f, List<UUID> societes, UUID fournisseur) {
        PerimetreAchat visibles = perimetre.achat(f.projetIds()).deSocietes(societes);
        return visibles.vide() ? List.of() : requetes.mouvements(visibles, fournisseur);
    }
}
