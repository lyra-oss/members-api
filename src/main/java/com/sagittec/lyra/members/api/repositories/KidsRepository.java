package com.sagittec.lyra.members.api.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface KidsRepository
        extends CrudRepository<Kid, Integer> {}
