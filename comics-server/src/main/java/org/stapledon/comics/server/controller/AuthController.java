package org.stapledon.comics.server.controller;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for authentication queries.
 */
@Controller
public class AuthController {

    /**
     * Validates the current JWT token. Returns false if no token is provided or if the token is invalid.
     */
    @QueryMapping
    public boolean validateToken() {
        // TODO: Implement actual token validation
        return false;
    }
}
