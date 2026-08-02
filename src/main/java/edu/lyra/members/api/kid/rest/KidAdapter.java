package edu.lyra.members.api.kid.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.security.access.AccessDeniedException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

class KidAdapter
        implements RepresentationModelAssembler<Kid, KidModel> {

    private final KidRepository                 kidRepository;
    private final ParentRepository              parentRepository;
    private final ClassroomRepository           classroomRepository;
    private final KidVisibilityStrategyResolver visibilityResolver;
    private final KidMapper                     mapper;
    private final KidPolicy                     policy;

    KidAdapter(
            final KidRepository kidRepository,
            final ParentRepository parentRepository,
            final ClassroomRepository classroomRepository,
            final KidVisibilityStrategyResolver visibilityResolver,
            final KidMapper mapper,
            final KidPolicy policy
    ) {
        this.kidRepository       = kidRepository;
        this.parentRepository    = parentRepository;
        this.classroomRepository = classroomRepository;
        this.visibilityResolver  = visibilityResolver;
        this.mapper              = mapper;
        this.policy              = policy;
    }

    @Override
    public KidModel toModel(final Kid kid) {
        final KidModel model = this.mapper.toModel(kid);
        model.add(linkTo(methodOn(KidController.class).get(kid.getId())).withSelfRel());
        return model;
    }

    Optional<KidModel> findById(final UUID id) {
        return this.kidRepository.findById(id).map(this::toModel);
    }

    PagedModel<KidModel> findAll(final Pageable pageable, final PagedResourcesAssembler<Kid> pagedAssembler) {
        final Page<Kid> page = this.visibilityResolver.resolve(pageable);
        return pagedAssembler.toModel(page, this);
    }

    Optional<PagedModel<KidModel>> findByParent(
            final UUID parentId,
            final Pageable pageable,
            final PagedResourcesAssembler<Kid> pagedAssembler
    ) {
        if(! this.parentRepository.existsById(parentId)) {
            return Optional.empty();
        }
        final Page<Kid> page = this.kidRepository.findByParentIdOrderByNameAsc(parentId, pageable);
        return Optional.of(pagedAssembler.toModel(page, this));
    }

    // Mirrors the original KidAuthorizationEventHandler: a kid can only be registered under the
    // authenticated caller's own parent account.
    KidModel create(final KidRequest request) {
        final Kid    kid     = this.mapper.toEntity(request);
        final UUID   subject = AuthenticatedPrincipal.requireCurrentId();
        final Parent parent  = this.parentRepository.findById(subject).orElseThrow(
                () -> new AccessDeniedException("Authenticated user cannot register this kid"));
        kid.setParent(parent);
        return this.toModel(this.kidRepository.save(kid));
    }

    Optional<KidModel> update(final UUID id, final KidPatchRequest request) {
        return this.kidRepository.findById(id).map(kid -> {
            final Parent    newParent    = this.resolveParent(request.parent(), kid.getParent());
            final Classroom newClassroom = this.resolveClassroom(request.classroom(), kid.getClassroom());
            this.policy.authorizeUpdate(kid, newParent, newClassroom);
            this.mapper.update(request, kid, newParent, newClassroom);
            return this.toModel(this.kidRepository.save(kid));
        });
    }

    private Parent resolveParent(final UUID requested, final Parent current) {
        if(requested == null) {
            return current;
        }
        return this.parentRepository.findById(requested).orElseThrow(
                () -> new UnresolvableReferenceException("No parent found with id " + requested));
    }

    private Classroom resolveClassroom(final UUID requested, final Classroom current) {
        if(requested == null) {
            return current;
        }
        return this.classroomRepository.findById(requested).orElseThrow(
                () -> new UnresolvableReferenceException("No classroom found with id " + requested));
    }

    boolean delete(final UUID id) {
        final Optional<Kid> found = this.kidRepository.findById(id);
        if(found.isEmpty()) {
            return false;
        }
        final Kid kid = found.get();
        this.policy.authorizeDelete(kid);
        this.kidRepository.delete(kid);
        return true;
    }

}
