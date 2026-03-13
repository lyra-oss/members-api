package edu.lyra.members.api.repositories.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ParentsRepository
        extends CrudRepository<Parent, Integer>, ListPagingAndSortingRepository<Parent, Integer> {}
