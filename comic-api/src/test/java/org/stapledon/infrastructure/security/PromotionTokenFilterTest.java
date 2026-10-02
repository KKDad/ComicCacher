package org.stapledon.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.stream.Stream;

class PromotionTokenFilterTest {

    private static final String TOKEN = "s3cret";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    record Case(String label, boolean serve, String configured, String path, String presented, boolean authenticated) {

        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Case> cases() {
        return Stream.of(
                new Case("right token on dev", true, TOKEN, "/api/v1/promotion/manifest", TOKEN, true),
                new Case("wrong token", true, TOKEN, "/api/v1/promotion/manifest", "nope", false),
                new Case("no token", true, TOKEN, "/api/v1/promotion/manifest", null, false),
                new Case("not served here (prod)", false, TOKEN, "/api/v1/promotion/manifest", TOKEN, false),
                new Case("served but no token configured", true, "", "/api/v1/promotion/manifest", "", false),
                new Case("other paths are left alone", true, TOKEN, "/api/v1/comics/1/avatar", TOKEN, false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void grantsThePromotionAuthorityOnlyForTheSharedToken(Case tc) throws Exception {
        PromotionTokenFilter filter = new PromotionTokenFilter(tc.serve(), tc.configured());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", tc.path());
        if (tc.presented() != null) {
            request.addHeader("X-Promotion-Token", tc.presented());
        }
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (tc.authenticated()) {
            assertThat(authentication).isNotNull();
            assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(PromotionTokenFilter.AUTHORITY);
        } else {
            assertThat(authentication).isNull();
        }
    }
}
