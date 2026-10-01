package org.stapledon.api.resolver;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ComicItem;

import java.util.List;
import java.util.Optional;

/**
 * Who may see a comic. A hidden comic ({@code enabled=false}) doesn't exist for readers and operators; admins see every comic.
 */
@Component
public class ComicVisibility {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    /**
     * True when the current caller is an admin.
     */
    public boolean callerIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_AUTHORITY::equals);
    }

    /**
     * True when the current caller may see this comic.
     */
    public boolean canSee(ComicItem comic) {
        return comic.isEnabled() || callerIsAdmin();
    }

    /**
     * The comic, if the current caller may see it.
     */
    public Optional<ComicItem> visible(Optional<ComicItem> comic) {
        return comic.filter(this::canSee);
    }

    /**
     * The comics the current caller may see.
     */
    public List<ComicItem> visible(List<ComicItem> comics) {
        if (callerIsAdmin()) {
            return comics;
        }
        return comics.stream().filter(ComicItem::isEnabled).toList();
    }
}
