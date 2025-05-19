package org.stapledon.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.StapledonAccountGivens;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.user.User;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration test to debug JWT token handling issues
 */
@Slf4j
public class JwtTokenDebuggerIT extends AbstractIntegrationTest {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Test
    @DisplayName("Verify JWT Token Generation and Validation - Step by Step")
    void verifyJwtTokenFlow() throws Exception {
        try {
            // STEP 1: Create a test user with a unique name to avoid conflicts
            String username = "jwt_test_user_" + System.currentTimeMillis();
            System.out.println("\n==== STEP 1: Creating test user ====");
            StapledonAccountGivens.GivenAccountContext userContext = createTestUser(username);
            System.out.println("✓ User created: " + userContext.getUsername());
            
            // STEP 2: Generate token manually using User object
            System.out.println("\n==== STEP 2: Generating token manually ====");
            User testUser = User.builder()
                    .username(username)
                    .roles(Collections.singletonList("USER"))
                    .build();
            String manualToken = jwtTokenUtil.generateToken(testUser);
            System.out.println("✓ Manual token generated (length: " + manualToken.length() + ")");
            
            // STEP 3: Verify manual token
            System.out.println("\n==== STEP 3: Validating manual token ====");
            String manualTokenUsername = jwtTokenUtil.extractUsername(manualToken);
            System.out.println("✓ Username extracted: " + manualTokenUsername);
            
            var manualTokenRoles = jwtTokenUtil.extractRoles(manualToken);
            System.out.println("✓ Roles extracted: " + manualTokenRoles);
            
            // STEP 4: Try authentication via API
            System.out.println("\n==== STEP 4: Authenticating via API ====");
            AuthRequest authRequest = AuthRequest.builder()
                    .username(username)
                    .password(userContext.getPassword())
                    .build();
                    
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authRequest)))
                    .andDo(print())
                    .andReturn();
                    
            // Verify successful login
            System.out.println("✓ Login response status: " + loginResult.getResponse().getStatus());
            String responseContent = loginResult.getResponse().getContentAsString();
            String apiToken = extractFromResponse(responseContent, "data.token", String.class);
            System.out.println("✓ API token: " + (apiToken != null ? apiToken.substring(0, Math.min(20, apiToken.length())) + "..." : "null"));
            
            // STEP 5: Compare tokens
            System.out.println("\n==== STEP 5: Comparing tokens ====");
            System.out.println("✓ Manual token: " + manualToken.substring(0, Math.min(20, manualToken.length())) + "...");
            System.out.println("✓ API token: " + (apiToken != null ? apiToken.substring(0, Math.min(20, apiToken.length())) + "..." : "null"));
            
            // STEP 6: Verify UserDetailsService
            System.out.println("\n==== STEP 6: Verifying UserDetailsService ====");
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            System.out.println("✓ UserDetails loaded: " + userDetails.getUsername());
            System.out.println("✓ Authorities: " + userDetails.getAuthorities());
            
            // STEP 7: Test protected endpoint using manually generated token
            System.out.println("\n==== STEP 7: Testing protected endpoint with manual token ====");
            MvcResult manualTokenResult = mockMvc.perform(get("/api/v1/preferences")
                    .header("Authorization", "Bearer " + manualToken))
                    .andDo(print())
                    .andReturn();
                    
            System.out.println("✓ Protected endpoint response status with manual token: " + manualTokenResult.getResponse().getStatus());
            
            // STEP 8: Test protected endpoint using API token
            System.out.println("\n==== STEP 8: Testing protected endpoint with API token ====");
            MvcResult apiTokenResult = mockMvc.perform(get("/api/v1/preferences")
                    .header("Authorization", "Bearer " + apiToken))
                    .andDo(print())
                    .andReturn();
                    
            System.out.println("✓ Protected endpoint response status with API token: " + apiTokenResult.getResponse().getStatus());
            
            // STEP 9: Direct check of token validation from JwtTokenFilter
            System.out.println("\n==== STEP 9: Direct token validation check ====");
            boolean isManualTokenValid = jwtTokenUtil.validateToken(manualToken, userDetails);
            System.out.println("✓ Manual token valid: " + isManualTokenValid);
            
            boolean isApiTokenValid = jwtTokenUtil.validateToken(apiToken, userDetails);
            System.out.println("✓ API token valid: " + isApiTokenValid);
            
            // If we get this far without exceptions, consider the test a success
            System.out.println("\n==== TEST COMPLETED ====");
        } catch (Exception e) {
            System.out.println("\n==== TEST FAILED ====");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}