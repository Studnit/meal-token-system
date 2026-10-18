package com.mealtoken.web;

import com.mealtoken.dto.RegistrationForm;
import com.mealtoken.exception.BusinessException;
import com.mealtoken.repository.UserRepository;
import com.mealtoken.service.CustomerService;
import com.mealtoken.service.RegistrationService;
import com.mealtoken.support.AbstractIntegrationTest;
import com.mealtoken.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UsernameLoginIT extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired RegistrationService registration;
    @Autowired UserRepository users;
    @Autowired CustomerService customers;

    private static String mobile() { return "9" + String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L)); }

    private RegistrationForm form(String username, String email, String phone) {
        RegistrationForm f = new RegistrationForm();
        f.setAccountType("CUSTOMER");
        f.setFullName("Asha");
        f.setUsername(username);
        f.setEmail(email);
        f.setPhone(phone);
        f.setPassword(Fixtures.PASSWORD);
        return f;
    }

    private boolean loginWorks(String identifier) throws Exception {
        MvcResult r = mvc.perform(post("/login").with(csrf()).param("username", identifier).param("password", Fixtures.PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn();
        return !r.getResponse().getRedirectedUrl().contains("error");
    }

    @Test
    void signsInWithUsernameEmailOrMobile_butNotWithAWrongPassword() throws Exception {
        String name = Fixtures.unique("asha").replace("-", "");
        String email = name + "@test.local";
        String phone = mobile();
        registration.register(form(name, email, phone), "en");

        assertThat(loginWorks(name)).isTrue();
        assertThat(loginWorks(name.toUpperCase())).isTrue();
        assertThat(loginWorks(email)).isTrue();
        assertThat(loginWorks(phone)).isTrue();
        assertThat(loginWorks("+91" + phone)).isTrue();

        MvcResult bad = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(bad.getResponse().getRedirectedUrl()).contains("error");
    }

    @Test
    void failuresAreToldApart_wrongPasswordVersusSuspendedAccount() throws Exception {
        String name = Fixtures.unique("meera").replace("-", "");
        Long id = registration.register(form(name, name + "@test.local", mobile()), "en").getId();

        MvcResult wrong = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", "not-the-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(wrong.getResponse().getRedirectedUrl()).endsWith("/login?error");

        var u = users.findById(id).orElseThrow();
        u.setStatus("SUSPENDED");
        users.save(u);
        MvcResult locked = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", Fixtures.PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(locked.getResponse().getRedirectedUrl()).endsWith("/login?locked");
    }

    @Test
    void usernameEmailAndMobileMustEachBeUnique() {
        String name = Fixtures.unique("ravi").replace("-", "");
        String phone = mobile();
        registration.register(form(name, name + "@test.local", phone), "en");

        assertThatThrownBy(() -> registration.register(form(name.toUpperCase(), "other-" + name + "@test.local", mobile()), "en"))
                .hasMessage("error.username.exists");
        assertThatThrownBy(() -> registration.register(form("x" + name, name + "@test.local", mobile()), "en"))
                .hasMessage("error.email.exists");
        assertThatThrownBy(() -> registration.register(form("y" + name, "y" + name + "@test.local", phone), "en"))
                .hasMessage("error.phone.exists");
    }

    @Test
    void invalidUsernamesAreRejected() {
        for (String bad : new String[]{"ab", "1abcdef", "has space", "a@b.com", "9876543210"})
            assertThatThrownBy(() -> registration.register(form(bad, Fixtures.unique("e") + "@test.local", mobile()), "en"))
                    .as(bad).isInstanceOf(BusinessException.class).hasMessage("error.username.invalid");
    }

    @Test
    void registrationPageNoLongerNeedsACode_andOldVerifyPageIsGone() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/verify-otp")).andExpect(status().is3xxRedirection());
    }

    @Test
    void hotelFinderFiltersByCityAndArea() {
        var all = customers.activeHotels("", "", "");
        var none = customers.activeHotels("", "NoSuchCity-" + System.nanoTime(), "");
        assertThat(none).isEmpty();
        assertThat(all.size()).isGreaterThanOrEqualTo(none.size());
        assertThat(customers.locations()).isNotNull();
    }
}
