package com.tsix.immocrm.services.notification;

import com.tsix.immocrm.domain.TypeNotification;
import com.tsix.immocrm.services.notification.LeadsAttribues.Lead;
import com.tsix.immocrm.services.notification.NotificationService.NouvelleNotification;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/** Textes des notifications (FEATURE-repartition-notifications.md §3.3) ; aucune lecture en base. */
final class MessagesNotification {

    private static final int NOMS_MAX = 3;
    private static final String SEPARATEUR = " · ";

    private MessagesNotification() {
    }

    /** Un lead : « Nouveau lead : nom · projet » vers sa fiche ; plusieurs : « N nouveaux leads à trier » vers /tri. */
    static NouvelleNotification leadsAttribues(UUID admUserId, List<Lead> leads) {
        boolean prospections = leads.stream().anyMatch(l -> !l.preProspection());
        if (leads.size() == 1) {
            Lead lead = leads.getFirst();
            String libelle = lead.nom() + (StringUtils.hasText(lead.projetCode()) ? SEPARATEUR + lead.projetCode() : "");
            return prospections
                    ? new NouvelleNotification(admUserId, TypeNotification.LEADS_ATTRIBUES,
                            "Prospection attribuée : " + libelle, "Elle vous est désormais attribuée.",
                            "/prospections/" + lead.id())
                    : new NouvelleNotification(admUserId, TypeNotification.LEADS_ATTRIBUES,
                            "Nouveau lead : " + libelle, "À trier dans votre Boîte de tri.",
                            "/pre-prospections/" + lead.id());
        }
        String titre = prospections
                ? leads.size() + " prospections vous ont été attribuées"
                : leads.size() + " nouveaux leads à trier";
        return new NouvelleNotification(admUserId, TypeNotification.LEADS_ATTRIBUES, titre, noms(leads),
                prospections ? "/prospections" : "/tri");
    }

    static NouvelleNotification leadsADistribuer(UUID admUserId, int nombre) {
        String titre = nombre == 1 ? "1 lead importé à distribuer" : nombre + " leads importés à distribuer";
        return new NouvelleNotification(admUserId, TypeNotification.LEADS_A_DISTRIBUER, titre,
                "Sans projet ou sans commercial habilité : la répartition automatique ne les a pas attribués.",
                "/equipe");
    }

    /** « 4 leads à trier · 3 appels · 1 visite · 2 en retard », zéros omis ; null si tout est à zéro. */
    static String resume(long leads, long appels, long visites, long retards) {
        StringJoiner parties = new StringJoiner(SEPARATEUR);
        ajouter(parties, leads, " lead à trier", " leads à trier");
        ajouter(parties, appels, " appel", " appels");
        ajouter(parties, visites, " visite", " visites");
        ajouter(parties, retards, " en retard", " en retard");
        return parties.length() == 0 ? null : parties.toString();
    }

    private static void ajouter(StringJoiner parties, long nombre, String singulier, String pluriel) {
        if (nombre > 0) {
            parties.add(nombre + (nombre == 1 ? singulier : pluriel));
        }
    }

    private static String noms(List<Lead> leads) {
        StringJoiner noms = new StringJoiner(", ");
        leads.stream().limit(NOMS_MAX).forEach(l -> noms.add(l.nom()));
        int reste = leads.size() - NOMS_MAX;
        if (reste <= 0) {
            return noms + ".";
        }
        return noms + " et " + reste + (reste == 1 ? " autre." : " autres.");
    }
}
