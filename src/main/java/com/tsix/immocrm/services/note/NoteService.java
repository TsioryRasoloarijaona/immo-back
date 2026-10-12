package com.tsix.immocrm.services.note;

import com.tsix.immocrm.domain.note.HtmlNote;
import com.tsix.immocrm.domain.note.PorteurNote;
import com.tsix.immocrm.dto.note.CreerNoteRequest;
import com.tsix.immocrm.dto.note.NoteDto;
import com.tsix.immocrm.entities.adm.AdmMessage;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmMessageRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Bouton « Notes » des fiches programme, tranche et article (FEATURE-patrimoine-audit-actions.md §4). */
@Service
@Transactional(readOnly = true)
public class NoteService {

    static final String CHAMP_TEXTE = "texteHtml";
    static final String CHAMP_ID = "id";
    /** Message exact d'OMRAN. */
    static final String NOTE_VIDE = "veuillez saisir une note !";

    private static final DateTimeFormatter FORMAT_LIBELLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdmMessageRepository repository;
    private final CfgProjectRepository projetRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository articleRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public NoteService(AdmMessageRepository repository, CfgProjectRepository projetRepository,
                       CfgTrancheRepository trancheRepository, StkItemRepository articleRepository,
                       UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.repository = repository;
        this.projetRepository = projetRepository;
        this.trancheRepository = trancheRepository;
        this.articleRepository = articleRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public List<NoteDto> lister(String porteur, UUID id) {
        verifierPorteur(PorteurNote.depuis(porteur), id);
        return repository.findByCategoryIdOrderByDocDateDescIdDesc(id).stream().map(NoteService::versDto).toList();
    }

    @Transactional
    public NoteDto creer(CreerNoteRequest demande) {
        PorteurNote porteur = PorteurNote.depuis(demande.porteur());
        if (demande.id() == null) {
            throw new ChampInvalideException(CHAMP_ID, "id de la fiche porteuse obligatoire");
        }
        String html = HtmlNote.assainir(demande.texteHtml());
        if (HtmlNote.estVide(html)) {
            throw new ChampInvalideException(CHAMP_TEXTE, NOTE_VIDE);
        }
        verifierPorteur(porteur, demande.id());

        LocalDateTime maintenant = LocalDateTime.now(clock);
        String auteur = utilisateurAffiche.courant().nom();
        AdmMessage note = new AdmMessage();
        note.setId(UUID.randomUUID());
        note.setDocDate(maintenant);
        note.setNotes(html);
        note.setCategoryId(demande.id());
        note.setDescription(StringUtils.hasText(auteur)
                ? maintenant.format(FORMAT_LIBELLE) + " " + auteur : maintenant.format(FORMAT_LIBELLE));
        note.setUserName(auteur);
        note.setOrigin("");
        note.setReminderDate(demande.rappel());
        return versDto(repository.save(note));
    }

    @Transactional
    public void supprimer(UUID id) {
        AdmMessage note = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note introuvable: " + id));
        repository.delete(note);
    }

    private void verifierPorteur(PorteurNote porteur, UUID id) {
        boolean existe = switch (porteur) {
            case PROJET -> projetRepository.existsById(id);
            case TRANCHE -> trancheRepository.existsById(id);
            case ARTICLE -> articleRepository.existsById(id);
        };
        if (!existe) {
            throw new ResourceNotFoundException("Fiche " + porteur.code() + " introuvable: " + id);
        }
    }

    // Assaini aussi à la lecture : OMRAN écrit AdmMessage.Notes sans filtre, et le front injecte ce HTML.
    private static NoteDto versDto(AdmMessage m) {
        return new NoteDto(m.getId(), m.getDocDate(), m.getUserName(), m.getDescription(),
                HtmlNote.assainir(m.getNotes()), m.getReminderDate());
    }
}
