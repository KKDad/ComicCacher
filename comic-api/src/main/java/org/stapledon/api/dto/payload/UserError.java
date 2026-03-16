package org.stapledon.api.dto.payload;

import org.stapledon.api.dto.ErrorCode;

/**
 * A user-facing error from a mutation.
 */
public record UserError(String message, String field, ErrorCode code) {

    /**
     * Creates a UserError with a message and code, no field path.
     */
    public static UserError of(String message, ErrorCode code) {
        return new UserError(message, null, code);
    }
}
