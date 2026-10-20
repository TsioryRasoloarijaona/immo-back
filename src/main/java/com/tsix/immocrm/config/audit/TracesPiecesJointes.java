package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;

import java.util.List;

/** Tous porteurs, sans parent : absente des types, l'onglet Audit d'un porteur ne la lit pas (P2b §8.3). */
public class TracesPiecesJointes implements TracesAudit {

    @Override
    public List<Class<?>> classes() {
        return List.of(AdmAttachedFile.class);
    }

    @Override
    public Trace tracer(Object entite) {
        return entite instanceof AdmAttachedFile f
                ? new Trace("Yes.Adm.Attachment.AttachmentWithData", "Pièce jointe", f.getLibelleAudit(), null, null)
                : null;
    }
}
