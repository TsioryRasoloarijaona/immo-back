package com.tsix.immocrm.services.patrimoine.plans;

import com.tsix.immocrm.dto.patrimoine.plans.BilanImportPlanDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ImportPlansSvg {

    @Transactional
    public BilanImportPlanDto importer(UUID trancheId, UUID etageId, MultipartFile fichier, boolean apercu) {
        throw new UnsupportedOperationException();
    }
}
