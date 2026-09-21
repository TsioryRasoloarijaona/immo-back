package com.tsix.immocrm.services.piecejointe;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mode mono-base : la colonne {@code AttachedFile} n'existe pas (supprimée par
 * {@code 004_drop_attachedfile_filestream.sql}), il n'y a donc nulle part où écrire — S3 porte
 * le binaire, comme avant la feature source-de-vérité. Ne rien faire est le comportement exact
 * d'avant, celui que la suite de tests verrouille.
 */
@Service
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() == ''")
public class DepotBinaireAbsent implements DepotBinaireSource {

    @Override
    public void deposer(UUID pieceId, byte[] contenu) {
        // Rien, volontairement — voir le javadoc de classe.
    }
}
