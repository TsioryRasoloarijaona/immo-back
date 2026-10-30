package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.EmailDetailDto;
import com.tsix.immocrm.dto.administration.LigneEmailDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmEMailHistory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmEMailHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/** Journal des e-mails en lecture seule. FEATURE-admin-email-historique.md. */
@Service
public class EmailHistoriqueService {

    private static final Sort TRI = Sort.by(Sort.Order.desc("mailDate"), Sort.Order.asc("id"));

    private final AdmEMailHistoryRepository repository;

    public EmailHistoriqueService(AdmEMailHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneEmailDto> lister(FiltresEmail filtres, int page, int size) {
        return PageDto.of(repository.findAll(filtres.specification(), PageRequest.of(page, size, TRI))
                .map(EmailHistoriqueService::ligne));
    }

    @Transactional(readOnly = true)
    public EmailDetailDto detail(UUID id) {
        AdmEMailHistory email = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-mail introuvable : " + id));
        return new EmailDetailDto(ligne(email), email.getEmailsCc(), email.getEmailsBcc(), email.getBody(),
                email.getBodyHtml());
    }

    @Transactional(readOnly = true)
    public List<String> utilisateurs() {
        return repository.utilisateurs();
    }

    private static LigneEmailDto ligne(AdmEMailHistory email) {
        String erreur = email.getErrorDescription();
        return new LigneEmailDto(email.getId(), email.getMailDate(), email.getAdmUserDescription(),
                email.getEmailFrom(), email.getEmailsTo(), email.getSubject(), email.getAutoMatic(),
                email.getEmailsSent(), StringUtils.hasText(erreur) ? erreur : null, email.getObjectLabel());
    }
}
