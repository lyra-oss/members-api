package com.sagittec.lyra.members.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ParentsRepository
        extends JpaRepository<Parent, Integer> {}
