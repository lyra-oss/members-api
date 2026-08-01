package edu.lyra.members.api.parent.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

class ParentAdapter
        implements RepresentationModelAssembler<Parent, ParentModel> {

    private final ParentRepository parentRepository;
    private final KidRepository    kidRepository;
    private final PersonRepository personRepository;
    private final ParentMapper     mapper;
    private final ParentPolicy     policy;
    private final ApiBasePath      apiBasePath;

    ParentAdapter(
            final ParentRepository parentRepository,
            final KidRepository kidRepository,
            final PersonRepository personRepository,
            final ParentMapper mapper,
            final ParentPolicy policy,
            final ApiBasePath apiBasePath
    ) {
        this.parentRepository = parentRepository;
        this.kidRepository    = kidRepository;
        this.personRepository = personRepository;
        this.mapper           = mapper;
        this.policy           = policy;
        this.apiBasePath      = apiBasePath;
    }

    @Override
    public ParentModel toModel(final Parent parent) {
        final ParentModel model = this.mapper.toModel(parent);
        model.add(this.selfLink(parent));
        return model;
    }

    private Link selfLink(final Parent parent) {
        //@formatter:off
        final String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                                                        .path(this.apiBasePath.basePath())
                                                        .path("/parents/{id}")
                                                        .buildAndExpand(parent.getId())
                                                        .toUriString();
        //@formatter:on
        return Link.of(href).withSelfRel();
    }

    Optional<ParentModel> findById(final UUID id) {
        return this.parentRepository.findById(id).map(this::toModel);
    }

    PagedModel<ParentModel> findAll(final Pageable pageable, final PagedResourcesAssembler<Parent> pagedAssembler) {
        final Page<Parent> page = this.parentRepository.findAll(pageable);
        return pagedAssembler.toModel(page, this);
    }

    Optional<ParentModel> findByKid(final UUID kidId) {
        return this.kidRepository.findById(kidId).map(Kid::getParent).map(this::toModel);
    }

    // Mirrors the original ParentRegistrationHandler: if the authenticated subject already has a
    // Person record (e.g. they registered as a teacher first), that existing identity wins over
    // whatever name/surname/mail this request supplied; otherwise a new Person is created from the
    // request under the subject's id.
    ParentModel create(final ParentRequest request) {
        final Parent parent  = this.mapper.toEntity(request);
        final UUID   subject = AuthenticatedPrincipal.requireCurrentId();
        final Person person = this.personRepository.findById(subject).orElseGet(() -> {
            final Person newPerson = parent.getPerson();
            newPerson.setId(subject);
            return newPerson;
        });
        parent.setPerson(person);
        return this.toModel(this.parentRepository.save(parent));
    }

    Optional<ParentModel> update(final UUID id, final ParentPatchRequest request) {
        return this.parentRepository.findById(id).map(parent -> {
            this.policy.authorizeUpdate(parent);
            this.mapper.update(request, parent);
            return this.toModel(this.parentRepository.save(parent));
        });
    }

    boolean delete(final UUID id) {
        final Optional<Parent> found = this.parentRepository.findById(id);
        if(found.isEmpty()) {
            return false;
        }
        final Parent parent = found.get();
        this.policy.authorizeDelete(parent);
        this.parentRepository.delete(parent);
        return true;
    }

    boolean bindKid(final UUID parentId, final UUID kidId) {
        final Optional<Parent> parent = this.parentRepository.findById(parentId);
        final Optional<Kid>    kid    = this.kidRepository.findById(kidId);
        if(parent.isEmpty() || kid.isEmpty()) {
            return false;
        }
        this.policy.authorizeKidBinding(parent.get(), kid.get());
        kid.get().setParent(parent.get());
        this.kidRepository.save(kid.get());
        return true;
    }

}
