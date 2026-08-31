package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

public record AjouterAutreTrancheRequest(UUID trancheId, UUID articleId) {
}
