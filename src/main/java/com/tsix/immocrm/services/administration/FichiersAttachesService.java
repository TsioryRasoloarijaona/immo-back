package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.domain.administration.CategorieFichierAttache;
import com.tsix.immocrm.dto.administration.FiltresFichiersAttachesDto;
import com.tsix.immocrm.dto.administration.FiltresFichiersAttachesDto.ChoixFiltreFichier;
import com.tsix.immocrm.dto.administration.FiltresFichiersAttachesDto.TypePieceFiltre;
import com.tsix.immocrm.dto.administration.LigneFichierAttacheDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.export.LignesAplaties;
import com.tsix.immocrm.web.export.LignesExport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Fichiers attachés de toute l'application, en lecture seule. FEATURE-admin-fichiers-attaches.md. */
@Service
@Transactional(readOnly = true)
public class FichiersAttachesService {

    private static final Sort TRI = Sort.by(Sort.Order.desc("userCreateDateTime"), Sort.Order.asc("id"));

    private final AdmAttachedFileSummaryRepository repository;
    private final AdmAttachedFileTypeRepository types;

    public FichiersAttachesService(AdmAttachedFileSummaryRepository repository, AdmAttachedFileTypeRepository types) {
        this.repository = repository;
        this.types = types;
    }

    public PageDto<LigneFichierAttacheDto> lister(FiltresFichiersAttaches filtres, int page, int taille) {
        return PageDto.of(repository.findAll(filtres.specification(), PageRequest.of(page, taille, TRI))
                .map(FichiersAttachesService::ligne));
    }

    public List<Map<String, Object>> lignesExport(FiltresFichiersAttaches filtres) {
        return LignesAplaties.toutes(lister(filtres, 0, LignesExport.LIGNES_EXPORT_MAX).content());
    }

    public FiltresFichiersAttachesDto filtres() {
        return new FiltresFichiersAttachesDto(
                types.findAll(Sort.by("description")).stream()
                        .map(t -> new TypePieceFiltre(t.getId(), t.getDescription())).toList(),
                Arrays.stream(CategorieFichierAttache.values())
                        .map(c -> new ChoixFiltreFichier(c.name(), c.libelle())).toList(),
                repository.groupes(),
                repository.nombreSansGroupe() > 0
                        ? new ChoixFiltreFichier(FiltresFichiersAttaches.NON_DEFINI, "Non défini") : null);
    }

    private static LigneFichierAttacheDto ligne(AdmAttachedFileSummary piece) {
        CategorieFichierAttache categorie = CategorieFichierAttache.de(piece.getCategory());
        return new LigneFichierAttacheDto(piece.getId(), piece.getDescription(), categorie.name(),
                piece.getCategoryDescription(), piece.getCategoryId(), categorie.objetCle(),
                Champs.de(piece.getType(), AdmAttachedFileType::getDescription), texte(piece.getExtension()),
                texte(piece.getGroupe()), Boolean.TRUE.equals(piece.getHasFile()), piece.getUserCreate(),
                piece.getUserCreateDateTime(), piece.getUserUpdate(), piece.getUserUpdateDateTime());
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }
}
