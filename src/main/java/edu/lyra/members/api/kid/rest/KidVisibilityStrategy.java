package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.kid.Kid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface KidVisibilityStrategy {

    boolean supports();

    Page<Kid> findVisible(final Pageable pageable);

}
