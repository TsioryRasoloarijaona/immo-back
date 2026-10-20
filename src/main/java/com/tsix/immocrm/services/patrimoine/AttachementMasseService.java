package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.BilanAttachementDto;
import com.tsix.immocrm.dto.patrimoine.BilanAttachementDto.Resultat;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.SaisiePieceJointe;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Attachement en masse d'OMRAN ({@code ItemList.AttachFile}). Sans transaction : chaque fichier a la sienne
 * ({@link AttachementMasseFichier}), un refus n'annule pas les autres.
 */
@Service
public class AttachementMasseService {

    private final TrancheVisibleRepository trancheRepository;
    private final StkItemRepository itemRepository;
    private final AdmAttachedFileTypeRepository typeRepository;
    private final AttachementMasseFichier fichierMasse;

    public AttachementMasseService(TrancheVisibleRepository trancheRepository, StkItemRepository itemRepository,
                                   AdmAttachedFileTypeRepository typeRepository, AttachementMasseFichier fichierMasse) {
        this.trancheRepository = trancheRepository;
        this.itemRepository = itemRepository;
        this.typeRepository = typeRepository;
        this.fichierMasse = fichierMasse;
    }

    public List<BilanAttachementDto> importerPiecesEnMasse(UUID trancheId, List<MultipartFile> fichiers,
                                                           SaisiePieceJointe saisie, AuthenticatedUser principal) {
        trancheRepository.exigerVisible(trancheId);
        if (!typeRepository.existsById(saisie.typeId())) {
            throw new ResourceNotFoundException("Type de pièce jointe introuvable: " + saisie.typeId());
        }
        // Clé = code exact : OMRAN compare par Equals, sensible à la casse (la collation SQL ne l'est pas).
        Map<String, StkItem> parCode = itemRepository.findPrincipauxDeNature(trancheId, ArticleService.NATURE_BATIMENT)
                .stream().collect(Collectors.toMap(StkItem::getCode, Function.identity(), (a, b) -> a));
        return fichiers.stream().map(f -> attacher(f, parCode, saisie, principal)).toList();
    }

    private BilanAttachementDto attacher(MultipartFile fichier, Map<String, StkItem> parCode, SaisiePieceJointe saisie,
                                         AuthenticatedUser principal) {
        String nom = Objects.toString(fichier.getOriginalFilename(), "");
        int point = nom.lastIndexOf('.');
        StkItem article = parCode.get(point < 0 ? nom : nom.substring(0, point));
        if (article == null) {
            return new BilanAttachementDto(nom, Resultat.CODE_INTROUVABLE, "Code article introuvable", null);
        }
        try {
            return fichierMasse.ajouterOuRemplacer(article, nom, fichier, saisie, principal);
        } catch (InvalidRequestException | MaxUploadSizeExceededException e) {
            return new BilanAttachementDto(nom, Resultat.REFUSE, e.getMessage(), null);
        }
    }
}
