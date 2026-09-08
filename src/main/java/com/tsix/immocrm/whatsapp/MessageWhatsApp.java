package com.tsix.immocrm.whatsapp;

/**
 * Un message WhatsApp à envoyer.
 *
 * <p>Volontairement plus pauvre que {@code MessageEmail} : pas de template, pas de variables,
 * pas de pièce jointe. WhatsApp n'a pas de rendu HTML à produire, et l'envoi d'un document par
 * UltraMsg passe par un <i>autre</i> endpoint qui exige une <b>URL publique</b> — ce que nos URL
 * de stockage ne sont pas, puisqu'elles expirent en quinze minutes. Les plans restent donc au
 * mail tant que cette question n'est pas tranchée.
 *
 * @param destinataire numéro au format international, {@code +212661224857} — produit par
 *                     {@link NumeroWhatsApp}, jamais lu tel quel depuis la base
 * @param texte        corps du message, en clair
 */
public record MessageWhatsApp(String destinataire, String texte) {
}
