package org.stapledon.engine.source;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.stapledon.common.config.CacheLayout;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.management.ManagementFacade;

/**
 * Checks a comic before it is created or saved: the rules the API and the Sources page share. Returns every problem found, each tied to the input field
 * it concerns, so the caller can report them together.
 */
@Component
public class ComicValidator {

    private final SourceRegistry sources;
    private final ManagementFacade comics;
    private final Clock clock;

    public ComicValidator(SourceRegistry sources, ManagementFacade comics, Clock clock) {
        this.sources = sources;
        this.comics = comics;
        this.clock = clock;
    }

    /**
     * One problem with a comic, for the input field it concerns.
     */
    public record Problem(String field, String message) {
    }

    /**
     * Problems with a comic about to be created.
     */
    public List<Problem> validateNew(ComicItem comic) {
        List<Problem> problems = validate(comic, null);
        Optional<ComicSource> source = sources.find(comic.getSource());
        if (source.isPresent() && source.get().indexed() && comic.getFirstStripNumber() == null) {
            problems.add(new Problem("firstStripNumber", source.get().displayName() + " comics are numbered: set the first strip number"));
        }
        return problems;
    }

    /**
     * Problems with an existing comic's new values. Only fields that changed are checked, so a value saved before these rules existed never blocks an
     * unrelated change (such as switching the comic off).
     */
    public List<Problem> validateUpdate(ComicItem existing, ComicItem updated) {
        Set<String> changed = new HashSet<>();
        if (!Objects.equals(existing.getName(), updated.getName())) {
            changed.add("name");
        }
        if (!Objects.equals(existing.getSource(), updated.getSource()) || !Objects.equals(existing.getSourceIdentifier(), updated.getSourceIdentifier())) {
            changed.add("source");
            changed.add("sourceIdentifier");
        }
        if (!Objects.equals(existing.getFirstStripNumber(), updated.getFirstStripNumber())) {
            changed.add("firstStripNumber");
        }
        if (!Objects.equals(existing.getSourceStartDate(), updated.getSourceStartDate())) {
            changed.add("sourceStartDate");
        }
        return validate(updated, existing.getId()).stream()
                .filter(problem -> changed.contains(problem.field()))
                .toList();
    }

    private List<Problem> validate(ComicItem comic, Integer selfId) {
        List<Problem> problems = new ArrayList<>();
        List<ComicItem> others = comics.getAllComics().stream()
                .filter(other -> selfId == null || other.getId() != selfId)
                .toList();

        String name = comic.getName();
        if (name == null || name.isBlank()) {
            problems.add(new Problem("name", "Name is required"));
        } else {
            String directory = new ComicIdentifier(comic.getId(), name).getDirectoryName();
            if (!CacheLayout.isComicDirectory(directory)) {
                problems.add(new Problem("name", "\"" + name + "\" is reserved for the cache's own files"));
            }
            if (others.stream().anyMatch(other -> other.getName() != null && other.getName().equalsIgnoreCase(name))) {
                problems.add(new Problem("name", "Another comic is already called \"" + name + "\""));
            } else if (others.stream().anyMatch(other -> other.getName() != null
                    && new ComicIdentifier(other.getId(), other.getName()).getDirectoryName().equalsIgnoreCase(directory))) {
                problems.add(new Problem("name", "\"" + name + "\" would share another comic's storage folder"));
            }
        }

        String identifier = comic.getSourceIdentifier();
        if (identifier != null && !identifier.isEmpty() && !ComicSource.isValidIdentifier(identifier)) {
            problems.add(new Problem("sourceIdentifier", "Source identifier must be lower-case letters, digits, '-' or '_'"));
        }

        Optional<ComicSource> source = sources.find(comic.getSource());
        if (comic.getSource() != null && !comic.getSource().isEmpty() && source.isEmpty()) {
            problems.add(new Problem("source", "Unknown source \"" + comic.getSource() + "\""));
        }
        if (source.isPresent() && (identifier == null || identifier.isEmpty() || ComicSource.isValidIdentifier(identifier))) {
            String key = source.get().identifierFor(comic);
            others.stream()
                    .filter(other -> source.get().id().equals(other.getSource()))
                    .filter(other -> source.get().identifierFor(other).equals(key))
                    .findFirst()
                    .ifPresent(other -> problems.add(new Problem("sourceIdentifier",
                            "\"" + other.getName() + "\" is already this " + source.get().displayName() + " comic")));
        }

        Integer first = comic.getFirstStripNumber();
        if (first != null && first < 1) {
            problems.add(new Problem("firstStripNumber", "First strip number must be 1 or more"));
        }
        if (first != null && comic.getLastStripNumber() != null && first > comic.getLastStripNumber()) {
            problems.add(new Problem("firstStripNumber", "First strip number is after the last strip (#" + comic.getLastStripNumber() + ")"));
        }
        if (comic.getSourceStartDate() != null && comic.getSourceStartDate().isAfter(LocalDate.now(clock))) {
            problems.add(new Problem("sourceStartDate", "Start date is in the future"));
        }
        return problems;
    }
}
