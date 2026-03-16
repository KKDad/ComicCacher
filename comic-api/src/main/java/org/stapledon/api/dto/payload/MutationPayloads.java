package org.stapledon.api.dto.payload;

import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.batch.BatchJobDto;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageCacheStats;

import java.util.List;

/**
 * Relay-style mutation payload records for GraphQL.
 * Each payload contains the result object and a list of user-facing errors.
 */
public final class MutationPayloads {

    private MutationPayloads() {
    }

    // Comic payloads
    public record CreateComicPayload(ComicItem comic, List<UserError> errors) {
    }

    public record UpdateComicPayload(ComicItem comic, List<UserError> errors) {
    }

    public record DeleteComicPayload(boolean success, List<UserError> errors) {
    }

    // User payloads
    public record UpdateProfilePayload(User user, List<UserError> errors) {
    }

    public record UpdatePasswordPayload(boolean success, List<UserError> errors) {
    }

    public record DeleteAccountPayload(boolean success, List<UserError> errors) {
    }

    // Preference payloads
    public record FavoritePayload(UserPreference preference, List<UserError> errors) {
    }

    public record UpdateLastReadPayload(UserPreference preference, List<UserError> errors) {
    }

    public record UpdateDisplaySettingsPayload(UserPreference preference, List<UserError> errors) {
    }

    // Metrics payloads
    public record RefreshStorageMetricsPayload(ImageCacheStats storageMetrics, List<UserError> errors) {
    }

    public record RefreshAllMetricsPayload(boolean success, List<UserError> errors) {
    }

    // Retrieval payloads
    public record DeleteRetrievalRecordPayload(boolean success, List<UserError> errors) {
    }

    public record PurgeRetrievalRecordsPayload(int purgedCount, List<UserError> errors) {
    }

    // Batch job payloads
    public record TriggerBatchJobPayload(BatchJobDto batchJob, List<UserError> errors) {
    }
}
