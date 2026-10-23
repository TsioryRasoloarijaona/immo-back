package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.importation.BilanImportDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ImportPatrimoineService {

    public BilanImportDto importer(String type, UUID trancheId, MultipartFile fichier, String feuille, boolean simulation) {
        throw new UnsupportedOperationException();
    }
}
