package rszabadi.swiss;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "app.admin.user=tester",
        "app.admin.password=correct-horse-battery" })
class SecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void vehiclesRequireLogin() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writingWithoutLoginIsRejected() throws Exception {
        mockMvc.perform(post("/vehicles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writingWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/vehicles").with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void correctPasswordLogsIn() throws Exception {
        mockMvc.perform(formLogin().user("tester").password("correct-horse-battery"))
                .andExpect(authenticated().withUsername("tester"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin().user("tester").password("wrong-password-here"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }
    
    @Test
    void loginPageContainsFormAndCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @Test
    void loginPageShowsErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Invalid username or password")));
    }
    private static RequestPostProcessor fromAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private MockHttpServletRequestBuilder login(String password, String address) {
        return post("/login").with(csrf()).with(fromAddress(address))
                .param("username", "tester").param("password", password);
    }

    @Test
    void tooManyWrongPasswordsLockLoginForThatAddress() throws Exception {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            mockMvc.perform(login("wrong-password-here", "10.20.30.40"))
                    .andExpect(redirectedUrl("/login?error"));
        }
        mockMvc.perform(login("correct-horse-battery", "10.20.30.40"))
                .andExpect(redirectedUrl("/login?locked"))
                .andExpect(unauthenticated());
    }

    @Test
    void aLockDoesNotAffectOtherAddresses() throws Exception {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            mockMvc.perform(login("wrong-password-here", "10.20.30.41"));
        }
        mockMvc.perform(login("correct-horse-battery", "10.20.30.42"))
                .andExpect(authenticated().withUsername("tester"));
    }

    @Test
    void loginPageShowsLockedMessage() throws Exception {
        mockMvc.perform(get("/login").param("locked", ""))
                .andExpect(content().string(containsString("Too many failed attempts")));
    }
}