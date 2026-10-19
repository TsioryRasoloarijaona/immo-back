package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleDossierDto;
import com.tsix.immocrm.dto.patrimoine.ArticleHistoriqueDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ArticleOngletsService {

    public List<ProspectionListItemDto> prospections(UUID articleId) {
        throw new UnsupportedOperationException();
    }

    public List<ArticleDossierDto> dossiers(UUID articleId) {
        throw new UnsupportedOperationException();
    }

    public List<ArticleHistoriqueDto> historique(UUID articleId) {
        throw new UnsupportedOperationException();
    }
}
