package com.mealtoken.security;

import com.mealtoken.entity.User;
import com.mealtoken.repository.UserRepository;
import com.mealtoken.util.Validators;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Sign-in accepts a username, an email address or a 10 digit mobile number. */
@Service
public class AppUserDetailsService implements UserDetailsService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppUserDetailsService.class);
    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String id = identifier == null ? "" : identifier.trim();
        Optional<User> found;
        String kind;
        if (id.contains("@")) { kind = "email"; found = users.findByEmailIgnoreCase(id); }
        else if (id.replaceAll("[\\s-]", "").matches("^(\\+?91|0)?[6-9][0-9]{9}$")) {
            String digits = id.replaceAll("[^0-9]", "");
            kind = "mobile";
            found = users.findFirstByPhoneOrderByIdAsc(digits.substring(digits.length() - 10));
        } else { kind = "username"; found = users.findByUsernameIgnoreCase(id); }
        // Server log only (never shown to the visitor): tells "no such account" apart from "wrong password".
        if (found.isEmpty()) log.info("Sign-in: no account found when looked up as {} ('{}')", kind, id.length() <= 2 ? "**" : id.substring(0, 2) + "***");
        return found.map(AppUserDetails::new).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
