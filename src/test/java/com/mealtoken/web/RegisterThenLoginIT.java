package com.mealtoken.web;

import com.mealtoken.repository.UserRepository;
import com.mealtoken.support.AbstractIntegrationTest;
import com.mealtoken.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The real browser path: submit the registration form, then sign in with each identifier. */
@AutoConfigureMockMvc
class RegisterThenLoginIT extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private static String mobile() { return "8" + String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L)); }

    private MvcResult submit(String username, String email, String phone, String password) throws Exception {
        return mvc.perform(post("/register").with(csrf())
                .param("fullName", "Test User").param("username", username).param("email", email)
                .param("phone", phone).param("password", password).param("accountType", "CUSTOMER")).andReturn();
    }

    private String signIn(String identifier, String password) throws Exception {
        return mvc.perform(post("/login").with(csrf()).param("username", identifier).param("password", password))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }

    @Test
    void registerThenSignInWithUsernameEmailAndMobile() throws Exception {
        String name = Fixtures.unique("user").replace("-", "");
        String email = name + "@test.local", phone = mobile(), pw = "Str0ng#Passw0rd";

        MvcResult reg = submit(name, email, phone, pw);
        assertThat(reg.getResponse().getStatus()).isEqualTo(302);
        assertThat(reg.getResponse().getRedirectedUrl()).endsWith("/login");
        assertThat(reg.getFlashMap().get("registeredUsername")).isEqualTo(name);

        var saved = users.findByUsernameIgnoreCase(name).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getPhone()).isEqualTo(phone);
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getPasswordHash()).startsWith("$2");

        for (String id : new String[]{name, name.toUpperCase(), email, email.toUpperCase(), phone, " " + name + " "})
            assertThat(signIn(id, pw)).as("sign in with '%s'", id).doesNotContain("error").doesNotContain("failure").doesNotContain("locked");
        assertThat(signIn(name, pw + "x")).endsWith("/login?error");
    }

    @Test
    void registrationErrorsRedisplayTheFormInsteadOfFailing() throws Exception {
        String name = Fixtures.unique("dup").replace("-", "");
        String email = name + "@test.local";
        submit(name, email, mobile(), "Str0ng#Passw0rd");

        // taken username / email -> the form comes back with a message (this used to blow up the page)
        mvc.perform(post("/register").with(csrf()).param("fullName", "X").param("username", name).param("email", "other-" + email)
                .param("phone", mobile()).param("password", "Str0ng#Passw0rd").param("accountType", "CUSTOMER"))
                .andExpect(status().isOk()).andExpect(view().name("register")).andExpect(model().attributeExists("errorKey"));
        // validation failure (bad mobile, short password) -> same
        mvc.perform(post("/register").with(csrf()).param("fullName", "X").param("username", "okname1").param("email", "a@b.co")
                .param("phone", "12345").param("password", "short").param("accountType", "CUSTOMER"))
                .andExpect(status().isOk()).andExpect(view().name("register")).andExpect(model().hasErrors());
    }
}
