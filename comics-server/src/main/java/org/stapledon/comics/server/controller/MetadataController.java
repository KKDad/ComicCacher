package org.stapledon.comics.server.controller;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL controller for metadata queries.
 */
@Controller
public class MetadataController {

    /**
     * Returns a list of known error codes.
     */
    @QueryMapping
    public List<ApplicationErrorCode> errorCodes() {
        return List.of(new ApplicationErrorCode("AUTH_001", "Invalid credentials"), new ApplicationErrorCode("AUTH_002", "Token expired"),
                new ApplicationErrorCode("AUTH_003", "Token invalid"), new ApplicationErrorCode("COMIC_001", "Comic not found"), new ApplicationErrorCode("COMIC_002", "Strip not found"));
    }

    /**
     * Represents an application error code.
     */
    public record ApplicationErrorCode(String code, String message) {
    }
}
