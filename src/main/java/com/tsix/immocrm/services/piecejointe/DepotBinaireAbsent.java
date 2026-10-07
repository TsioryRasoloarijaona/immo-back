package com.tsix.immocrm.services.piecejointe;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Mode mono-base : la colonne {@code AttachedFile} n'existe pas, S3 seul porte le binaire. */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() == ''")
public class DepotBinaireAbsent implements DepotBinaireSource {

    @Override
    public void deposer(UUID pieceId, byte[] contenu) {
        // Rien, volontairement.
    }
}
