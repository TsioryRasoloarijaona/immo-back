package com.tsix.immocrm.whatsapp;

/**
 * Sans pièce jointe : UltraMsg exige une URL publique pour un document, et nos URL de stockage expirent.
 *
 * @param destinataire format international, produit par {@link NumeroWhatsApp}, jamais lu tel quel en base
 */
public record MessageWhatsApp(String destinataire, String texte) {
}
