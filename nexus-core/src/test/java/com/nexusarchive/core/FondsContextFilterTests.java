// Input: JUnit + Spring Mock MVC
// Output: FondsContextFilter 覆盖测试
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FondsContextFilterTests {
    private final FondsContextFilter filter = new FondsContextFilter();

    @Test
    void shouldInjectFondsNoFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Fonds-No", "F001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("F001", chain.fondsNoInChain);
        assertFalse(FondsContext.isActive());
    }

    @Test
    void shouldInjectFondsNoFromAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("fonds_no", "F002");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("F002", chain.fondsNoInChain);
        assertFalse(FondsContext.isActive());
    }

    @Test
    void shouldIgnoreWhenMissingFondsNo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain(false);

        filter.doFilter(request, response, chain);

        assertFalse(chain.contextWasActive);
        assertFalse(FondsContext.isActive());
    }

    @Test
    void shouldRejectInvalidFondsNo() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Fonds-No", "F001;DROP");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(FondsIsolationException.class, () -> filter.doFilter(
                request,
                response,
                new CapturingFilterChain()));
        assertFalse(FondsContext.isActive());
    }

    private static final class CapturingFilterChain implements FilterChain {
        private final boolean requireContext;
        private String fondsNoInChain;
        private boolean contextWasActive;

        private CapturingFilterChain() {
            this(true);
        }

        private CapturingFilterChain(boolean requireContext) {
            this.requireContext = requireContext;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            contextWasActive = FondsContext.isActive();
            if (requireContext) {
                fondsNoInChain = FondsContext.requireFondsNo();
            }
        }
    }
}
