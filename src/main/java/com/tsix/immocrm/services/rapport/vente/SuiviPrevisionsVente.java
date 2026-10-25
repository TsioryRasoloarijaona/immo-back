package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.dto.rapport.ParametreRapportDto;
import com.tsix.immocrm.dto.rapport.ResultatRapportDto;
import com.tsix.immocrm.services.rapport.ServiceRapports;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Écran « Suivi des prévisions de vente » (FEATURE-tdb-vente-previsions.md §4). */
@Service
public class SuiviPrevisionsVente {

    public List<ParametreRapportDto> parametres() {
        throw new UnsupportedOperationException();
    }

    public ResultatRapportDto resultat(Map<String, List<String>> recus) {
        throw new UnsupportedOperationException();
    }

    public ResultatRapportDto detail(Map<String, List<String>> recus) {
        throw new UnsupportedOperationException();
    }

    public ServiceRapports.Export exporter(String format, Map<String, List<String>> recus) {
        throw new UnsupportedOperationException();
    }
}
