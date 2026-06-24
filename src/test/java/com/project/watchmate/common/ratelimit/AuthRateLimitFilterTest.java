package com.project.watchmate.common.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.project.watchmate.common.error.ApiError;

import tools.jackson.databind.ObjectMapper;

class AuthRateLimitFilterTest {

    private RateLimitProperties properties;
    private ObjectMapper objectMapper;
    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setLoginCapacity(2);
        properties.setLoginRefillSeconds(60);
        properties.setRegisterCapacity(2);
        properties.setRegisterRefillSeconds(60);
        properties.setResendCapacity(2);
        properties.setResendRefillSeconds(60);
        properties.setRefreshCapacity(2);
        properties.setRefreshRefillSeconds(60);
        properties.setBucketTtlSeconds(3600);
        properties.setBucketCleanupIntervalSeconds(300);

        objectMapper = new ObjectMapper();
        filter = new AuthRateLimitFilter(properties, objectMapper);
    }

    @Nested
    @DisplayName("Rate limiting disabled")
    class DisabledTests {

        @Test
        void whenDisabled_alwaysPassesThrough() throws Exception {
            properties.setEnabled(false);
            filter = new AuthRateLimitFilter(properties, objectMapper);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");

            for (int i = 0; i < 10; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, new MockFilterChain());
                assertEquals(200, response.getStatus());
            }
        }
    }

    @Nested
    @DisplayName("Non rate-limited paths")
    class NonRateLimitedPathTests {

        @Test
        void nonAuthPath_alwaysPassesThrough() throws Exception {
            MockHttpServletRequest request = postRequest("/api/v1/reviews");
            MockFilterChain chain = mock(MockFilterChain.class);

            for (int i = 0; i < 5; i++) {
                filter.doFilter(request, new MockHttpServletResponse(), chain);
            }
            verify(chain, times(5)).doFilter(any(), any());
        }

        @Test
        void getRequest_onRateLimitedPath_alwaysPassesThrough() throws Exception {
            properties.setLoginCapacity(1);
            filter = new AuthRateLimitFilter(properties, objectMapper);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
            request.setRequestURI("/api/v1/auth/login");
            MockFilterChain chain = mock(MockFilterChain.class);

            // Even though login is rate-limited, GET requests must bypass the limiter.
            for (int i = 0; i < 5; i++) {
                filter.doFilter(request, new MockHttpServletResponse(), chain);
            }
            verify(chain, times(5)).doFilter(any(), any());
        }

        @Test
        void optionsRequest_onRateLimitedPath_alwaysPassesThrough() throws Exception {
            properties.setLoginCapacity(1);
            filter = new AuthRateLimitFilter(properties, objectMapper);
            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
            request.setRequestURI("/api/v1/auth/login");
            MockFilterChain chain = mock(MockFilterChain.class);

            // OPTIONS/preflight must never be rate-limited.
            for (int i = 0; i < 5; i++) {
                filter.doFilter(request, new MockHttpServletResponse(), chain);
            }
            verify(chain, times(5)).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Login rate limiting")
    class LoginRateLimitTests {

        @Test
        void underLimit_requestsPassThrough() throws Exception {
            properties.setLoginCapacity(3);
            filter = new AuthRateLimitFilter(properties, objectMapper);

            for (int i = 0; i < 3; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(postRequest("/api/v1/auth/login"), response, new MockFilterChain());
                assertEquals(200, response.getStatus());
            }
        }

        @Test
        void overLimit_returns429WithApiError() throws Exception {
            properties.setLoginCapacity(2);
            properties.setLoginRefillSeconds(90);
            filter = new AuthRateLimitFilter(properties, objectMapper);
            MockHttpServletRequest request = postRequest("/api/v1/auth/login");

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(429, response.getStatus());
            // Retry-After must reflect the configured refill window for the endpoint.
            assertEquals("90", response.getHeader("Retry-After"));
            assertEquals("application/json", response.getContentType());

            ApiError error = objectMapper.readValue(response.getContentAsString(), ApiError.class);
            assertEquals("TOO_MANY_REQUESTS", error.code());
        }

        @Test
        void retryAfter_reflectsEndpointSpecificRefillWindow() throws Exception {
            properties.setResendCapacity(1);
            properties.setResendRefillSeconds(120);
            filter = new AuthRateLimitFilter(properties, objectMapper);
            MockHttpServletRequest request = postRequest("/api/v1/auth/verify/resend");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(429, response.getStatus());
            assertEquals("120", response.getHeader("Retry-After"));
        }
    }

    @Nested
    @DisplayName("Resend verification rate limiting")
    class ResendRateLimitTests {

        @Test
        void underLimit_requestsPassThrough() throws Exception {
            properties.setResendCapacity(2);
            filter = new AuthRateLimitFilter(properties, objectMapper);

            for (int i = 0; i < 2; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(postRequest("/api/v1/auth/verify/resend"), response, new MockFilterChain());
                assertEquals(200, response.getStatus());
            }
        }

        @Test
        void overLimit_returns429() throws Exception {
            properties.setResendCapacity(1);
            filter = new AuthRateLimitFilter(properties, objectMapper);
            MockHttpServletRequest request = postRequest("/api/v1/auth/verify/resend");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(429, response.getStatus());
        }
    }

    @Nested
    @DisplayName("IP isolation")
    class IpIsolationTests {

        @Test
        void differentIps_haveSeparateBuckets() throws Exception {
            properties.setLoginCapacity(1);
            filter = new AuthRateLimitFilter(properties, objectMapper);

            MockHttpServletRequest ip1Request = postRequest("/api/v1/auth/login");
            ip1Request.setRemoteAddr("1.2.3.4");
            MockHttpServletRequest ip2Request = postRequest("/api/v1/auth/login");
            ip2Request.setRemoteAddr("5.6.7.8");

            filter.doFilter(ip1Request, new MockHttpServletResponse(), new MockFilterChain());

            MockHttpServletResponse ip1Response = new MockHttpServletResponse();
            filter.doFilter(ip1Request, ip1Response, new MockFilterChain());
            assertEquals(429, ip1Response.getStatus());

            MockHttpServletResponse ip2Response = new MockHttpServletResponse();
            filter.doFilter(ip2Request, ip2Response, new MockFilterChain());
            assertEquals(200, ip2Response.getStatus());
        }

        @Test
        void xForwardedForHeader_usedAsClientIp() throws Exception {
            properties.setLoginCapacity(1);
            filter = new AuthRateLimitFilter(properties, objectMapper);

            MockHttpServletRequest request = postRequest("/api/v1/auth/login");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(429, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Stale bucket cleanup")
    class CleanupTests {

        /**
         * A stale entry (last accessed longer ago than TTL) must be evicted so the
         * next request starts with a fresh full-capacity bucket.
         * removeStaleEntries(now) is called with a synthetic future timestamp so no
         * real time needs to pass.
         */
        @Test
        void staleEntry_isRemovedAfterTtl_andNextRequestGetsFreshBucket() throws Exception {
            properties.setLoginCapacity(1);
            properties.setBucketTtlSeconds(10); // 10-second TTL for this test
            filter = new AuthRateLimitFilter(properties, objectMapper);

            MockHttpServletRequest request = postRequest("/api/v1/auth/login");

            // Exhaust the single-token bucket.
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            MockHttpServletResponse blocked = new MockHttpServletResponse();
            filter.doFilter(request, blocked, new MockFilterChain());
            assertEquals(429, blocked.getStatus());

            // Simulate 11 seconds passing — entry is now beyond the 10 s TTL.
            filter.removeStaleEntries(System.currentTimeMillis() + 11_000L);

            // The stale entry was evicted; the next request creates a fresh bucket and passes.
            MockHttpServletResponse fresh = new MockHttpServletResponse();
            filter.doFilter(request, fresh, new MockFilterChain());
            assertEquals(200, fresh.getStatus());
        }

        /**
         * An entry accessed within the TTL window must not be evicted, so an
         * exhausted bucket remains exhausted and continues rate-limiting the client.
         */
        @Test
        void recentlyUsedEntry_isNotRemovedBeforeTtl() throws Exception {
            properties.setLoginCapacity(1);
            properties.setBucketTtlSeconds(10); // 10-second TTL
            filter = new AuthRateLimitFilter(properties, objectMapper);

            MockHttpServletRequest request = postRequest("/api/v1/auth/login");

            // Exhaust the bucket.
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            // Simulate only 5 seconds passing — well within the 10 s TTL.
            filter.removeStaleEntries(System.currentTimeMillis() + 5_000L);

            // Entry was NOT evicted; bucket is still exhausted → still 429.
            MockHttpServletResponse stillBlocked = new MockHttpServletResponse();
            filter.doFilter(request, stillBlocked, new MockFilterChain());
            assertEquals(429, stillBlocked.getStatus());
        }

        /**
         * Cleanup must not disturb buckets that still have remaining capacity.
         * Normal rate limiting continues to work before and after a cleanup pass.
         */
        @Test
        void cleanupDoesNotBreakNormalRateLimiting() throws Exception {
            properties.setLoginCapacity(3);
            properties.setBucketTtlSeconds(3600);
            filter = new AuthRateLimitFilter(properties, objectMapper);

            MockHttpServletRequest request = postRequest("/api/v1/auth/login");

            // Consume 2 of 3 tokens.
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            // Cleanup with synthetic time 1 second in the future — well under the 3600 s TTL.
            // The entry must be preserved with its remaining token intact.
            filter.removeStaleEntries(System.currentTimeMillis() + 1_000L);

            // 3rd request still passes (1 token remaining).
            MockHttpServletResponse third = new MockHttpServletResponse();
            filter.doFilter(request, third, new MockFilterChain());
            assertEquals(200, third.getStatus());

            // 4th request is rate-limited (bucket now empty).
            MockHttpServletResponse fourth = new MockHttpServletResponse();
            filter.doFilter(request, fourth, new MockFilterChain());
            assertEquals(429, fourth.getStatus());
        }
    }

    private MockHttpServletRequest postRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
