package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class NfsFileOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    void atomicWriteOfBytesCreatesParentsAndLeavesNoTempFile() throws IOException {
        Path target = tempDir.resolve("garfield/2026/2026-09-24.png");
        byte[] image = {1, 2, 3, 4};

        NfsFileOperations.atomicWrite(target, image);

        assertThat(Files.readAllBytes(target)).isEqualTo(image);
        try (var files = Files.list(target.getParent())) {
            assertThat(files).containsExactly(target);
        }
    }

    @Test
    void atomicWriteReplacesExistingContent() throws IOException {
        Path target = tempDir.resolve("users.json");
        NfsFileOperations.atomicWrite(target, "old");

        NfsFileOperations.atomicWrite(target, "new");

        assertThat(NfsFileOperations.readAsString(target)).isEqualTo("new");
    }

    @Test
    void writeIntoAFileInsteadOfADirectoryThrows() throws IOException {
        Path blocked = tempDir.resolve("blocked");
        Files.writeString(blocked, "not a directory");

        assertThatThrownBy(() -> NfsFileOperations.atomicWrite(blocked.resolve("child.png"), new byte[] {1}))
                .isInstanceOf(IOException.class);
    }
}
