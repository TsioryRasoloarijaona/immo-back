package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.DroitsRoleEcritureDto;
import com.tsix.immocrm.dto.administration.FiltresRoles;
import com.tsix.immocrm.dto.administration.NoeudDroitDto;
import com.tsix.immocrm.dto.administration.RoleDto;
import com.tsix.immocrm.dto.administration.RoleEcritureDto;
import com.tsix.immocrm.dto.administration.RoleLigneDto;
import com.tsix.immocrm.dto.common.PageDto;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RolesService {

    public PageDto<RoleLigneDto> lister(FiltresRoles filtres, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public List<Map<String, Object>> lignesExport(FiltresRoles filtres) {
        throw new UnsupportedOperationException();
    }

    public List<NoeudDroitDto> arbreDroits() {
        throw new UnsupportedOperationException();
    }

    public RoleDto detail(UUID id) {
        throw new UnsupportedOperationException();
    }

    public RoleDto creer(RoleEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public RoleDto modifier(UUID id, RoleEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public RoleDto remplacerDroits(UUID id, DroitsRoleEcritureDto corps) {
        throw new UnsupportedOperationException();
    }

    public void supprimer(UUID id) {
        throw new UnsupportedOperationException();
    }
}
