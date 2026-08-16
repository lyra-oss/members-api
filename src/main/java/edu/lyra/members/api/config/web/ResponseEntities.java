package edu.lyra.members.api.config.web;

import java.util.Optional;

import lombok.experimental.UtilityClass;
import org.springframework.http.ResponseEntity;

/**
 * The two response shapes every controller in this codebase falls back to once its adapter reports "no such
 * resource": a found value becomes {@code 200 OK}, and a missing one becomes {@code 404 Not Found}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@UtilityClass
public class ResponseEntities {

    /**
     * Builds a {@code 200 OK} carrying {@code body}'s value, or a bodyless {@code 404 Not Found} when it is empty.
     *
     * @param body the adapter's lookup result
     * @param <T>  the response body's type
     *
     * @return {@code 200 OK} with {@code body}'s value, or {@code 404 Not Found}
     */
    public <T> ResponseEntity<T> okOrNotFound(final Optional<T> body) {
        return body.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Builds a bodyless {@code 204 No Content} when {@code found} is {@code true}, or a bodyless
     * {@code 404 Not Found} otherwise.
     *
     * @param found whether the adapter located (and acted on) the resource
     *
     * @return {@code 204 No Content} if {@code found}, otherwise {@code 404 Not Found}
     */
    public ResponseEntity<Void> noContentOrNotFound(final boolean found) {
        return found ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

}
