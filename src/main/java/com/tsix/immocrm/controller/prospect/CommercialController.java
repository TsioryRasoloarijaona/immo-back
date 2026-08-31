package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.prospect.CommercialListItemDto;
import com.tsix.immocrm.services.prospect.CommercialService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/commerciaux")
public class CommercialController {

    private final CommercialService commercialService;

    public CommercialController(CommercialService commercialService) {
        this.commercialService = commercialService;
    }

    @GetMapping
    public PageDto<CommercialListItemDto> findAll(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(commercialService.findAll(pageable));
    }
}
