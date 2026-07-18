package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.kid.Kid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class KidsCollectionController {

    private final KidVisibilityStrategyResolver visibilityResolver;

    @ResponseBody
    @GetMapping("/kids")
    PagedModel<PersistentEntityResource> findAll(
            final Pageable pageable,
            final PersistentEntityResourceAssembler assembler,
            final PagedResourcesAssembler<Kid> pagedAssembler
    ) {
        final Page<Kid> page = this.visibilityResolver.resolve(pageable);
        log.debug("Returning {} of {} kids visible to the authenticated principal", page.getNumberOfElements(),
                  page.getTotalElements());
        return pagedAssembler.toModel(page, assembler::toModel);
    }

}
