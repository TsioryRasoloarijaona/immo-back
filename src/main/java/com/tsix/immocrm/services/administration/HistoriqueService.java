package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.FiltresHistorique;
import com.tsix.immocrm.dto.administration.LigneHistoriqueDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.web.export.LignesAplaties;
import com.tsix.immocrm.web.export.LignesExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Journal des actions en lecture seule. FEATURE-admin-historique.md. */
@Service
public class HistoriqueService {

    private static final Sort TRI = Sort.by(Sort.Direction.DESC, "actionDateTime", "id");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdmWebHistoricsRepository repository;
    private final Clock clock;

    public HistoriqueService(AdmWebHistoricsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneHistoriqueDto> lister(FiltresHistorique filtres, int page, int size) {
        return PageDto.of(lire(filtres, PageRequest.of(page, size, TRI)));
    }

    /** Date en texte : le style de date du classeur masquerait l'heure, utile dans un journal. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> lignesExport(FiltresHistorique filtres) {
        return lire(filtres, PageRequest.of(0, LignesExport.LIGNES_EXPORT_MAX, TRI)).stream()
                .map(HistoriqueService::ligneExport)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> utilisateurs(LocalDate dateDebut, LocalDate dateFin) {
        FiltresHistorique.Periode periode =
                new FiltresHistorique(dateDebut, dateFin, null, null, null).periode(clock);
        return repository.utilisateurs(periode.debut(), periode.finExclue());
    }

    private Page<LigneHistoriqueDto> lire(FiltresHistorique filtres, Pageable pageable) {
        FiltresHistorique.Periode periode = filtres.periode(clock);
        return repository.lignes(periode.debut(), periode.finExclue(), motif(filtres.texte()),
                StringUtils.hasLength(filtres.utilisateur()) ? filtres.utilisateur() : null,
                filtres.actionValidee(), pageable);
    }

    /** Texte saisi pris littéralement dans le {@code LIKE} : {@code %}, {@code _} et {@code [} échappés. */
    private static String motif(String texte) {
        if (!StringUtils.hasLength(texte)) {
            return null;
        }
        return "%" + texte.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
    }

    private static Map<String, Object> ligneExport(LigneHistoriqueDto ligne) {
        Map<String, Object> valeurs = LignesAplaties.de(ligne);
        valeurs.put("dateAction", DATE_HEURE.format(ligne.dateAction()));
        return valeurs;
    }
}
