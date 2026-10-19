package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.BilanAttachementDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class AttachementMasseService {

    public List<BilanAttachementDto> importerPiecesEnMasse(UUID trancheId, List<MultipartFile> fichiers,
                                                           PiecesJointesPatrimoineService.Saisie saisie,
                                                           AuthenticatedUser principal) {
        throw new UnsupportedOperationException();
    }
}
