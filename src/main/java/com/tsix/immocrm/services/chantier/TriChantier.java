package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

/** Les clés de tri exposées sont les noms des DTO ; on les traduit en chemins d'entité, liste blanche stricte. */
final class TriChantier {

    static final Map<String, String> LOTS = Map.of(
            "code", "code",
            "description", "description",
            "dateDebut", "startDateManual",
            "dateFin", "endDateManual",
            "statutOrdre", "strStatus.sorting",
            "typeCode", "prjMarketType.code",
            "fournisseurNom", "cfgSupplier.description");

    static final Map<String, String> DECOMPTES = Map.of(
            "dateDocument", "docDate",
            "code", "code",
            "numero", "sorting",
            "montantHT", "amountHT",
            "montantTTC", "amountTTC",
            "netAPayer", "amountToPay",
            "lotCode", "prjMarket.code",
            "projetCode", "prjMarket.prjProject.code",
            "fournisseurNom", "cfgTier.description");

    private TriChantier() {
    }

    static Pageable traduire(Pageable pageable, Map<String, String> correspondances) {
        List<Sort.Order> ordres = pageable.getSort().stream()
                .map(ordre -> ordre.withProperty(chemin(ordre.getProperty(), correspondances)))
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(ordres));
    }

    private static String chemin(String cle, Map<String, String> correspondances) {
        String chemin = correspondances.get(cle);
        if (chemin == null) {
            throw new InvalidRequestException("Tri inconnu : « %s ». Valeurs admises : %s"
                    .formatted(cle, correspondances.keySet().stream().sorted().toList()));
        }
        return chemin;
    }
}
