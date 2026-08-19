package com.tsix.immocrm.dto.prospect;

import java.util.UUID;

public record CreerContactProspectRequest(UUID contactProspectId, UUID relationTypeId) {
}
