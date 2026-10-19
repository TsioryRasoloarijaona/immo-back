package com.tsix.immocrm.services.note;

import com.tsix.immocrm.dto.note.CreerNoteRequest;
import com.tsix.immocrm.dto.note.NoteDto;
import com.tsix.immocrm.entities.adm.AdmMessage;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmMessageRepository;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine.Porteur;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Notes ({@code AdmMessage}) d'un objet du patrimoine — FEATURE-patrimoine-audit-actions.md §4, §11.8. */
@Service
public class NoteService {

    private static final DateTimeFormatter DESCRIPTION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdmMessageRepository messageRepository;
    private final PorteursPatrimoine porteurs;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public NoteService(AdmMessageRepository messageRepository, PorteursPatrimoine porteurs,
                       UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.messageRepository = messageRepository;
        this.porteurs = porteurs;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NoteDto> lister(String porteur, UUID id) {
        porteurs.exigerVisible(Porteur.depuis(porteur), id);
        return messageRepository.findByCategoryIdOrderByDocDateDesc(id).stream().map(NoteService::versDto).toList();
    }

    @Transactional
    public NoteDto creer(CreerNoteRequest saisie) {
        Porteur porteur = Porteur.depuis(saisie.porteur());
        if (saisie.id() == null) {
            throw new InvalidRequestException("id est requis");
        }
        porteurs.exigerVisible(porteur, saisie.id());
        if (!StringUtils.hasText(saisie.texte())) {
            throw new InvalidRequestException("Veuillez saisir une note !");
        }
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        AdmMessage note = new AdmMessage();
        note.setId(UUID.randomUUID());
        note.setDocDate(maintenant);
        note.setNotes(TexteNote.versHtml(saisie.texte()));
        note.setCategoryId(saisie.id());
        note.setDescription(maintenant.format(DESCRIPTION) + " " + auteur);
        note.setUserName(auteur);
        note.setOrigin("");
        note.setReminderDate(saisie.rappel());
        return versDto(messageRepository.save(note));
    }

    /** Seules les notes d'un objet du patrimoine visible se suppriment ici (celles d'un tiers : 404). */
    @Transactional
    public void supprimer(UUID id) {
        AdmMessage note = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note introuvable: " + id));
        if (note.getCategoryId() == null) {
            throw new ResourceNotFoundException("Note introuvable: " + id);
        }
        porteurs.exigerVisible(note.getCategoryId());
        messageRepository.delete(note);
    }

    private static NoteDto versDto(AdmMessage m) {
        return new NoteDto(m.getId(), m.getDocDate(), m.getUserName(), m.getDescription(),
                TexteNote.versTexte(m.getNotes()), m.getReminderDate());
    }
}
