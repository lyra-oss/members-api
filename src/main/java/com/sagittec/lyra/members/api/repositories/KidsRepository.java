package com.sagittec.lyra.members.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository("kids")
public interface KidsRepository
        extends JpaRepository<Kid, Integer> {}
