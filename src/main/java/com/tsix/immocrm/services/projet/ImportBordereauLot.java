package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.TypeBordereau;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ImportBordereauLot {

    public Object importer(UUID id, TypeBordereau type, MultipartFile fichier) {
        throw new UnsupportedOperationException();
    }
}
