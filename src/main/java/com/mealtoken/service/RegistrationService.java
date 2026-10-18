package com.mealtoken.service;

import com.mealtoken.dto.RegistrationForm;
import com.mealtoken.entity.Hotel;
import com.mealtoken.entity.User;
import com.mealtoken.exception.BusinessException;
import com.mealtoken.repository.HotelRepository;
import com.mealtoken.repository.RoleRepository;
import com.mealtoken.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final HotelRepository hotels;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, RoleRepository roles, HotelRepository hotels, PasswordEncoder encoder) {
        this.users = users; this.roles = roles; this.hotels = hotels; this.encoder = encoder;
    }

    /** Creates a customer or hotel-owner account. The username is optional only for accounts created by an administrator. */
    @Transactional
    public User register(RegistrationForm f, String language) {
        com.mealtoken.util.Validators.requireEmail(f.getEmail());
        String phone = com.mealtoken.util.Validators.mobileOrNull(f.getPhone());
        if (users.existsByEmailIgnoreCase(f.getEmail()))
            throw new BusinessException("error.email.exists");
        if (phone != null && users.existsByPhone(phone)) throw new BusinessException("error.phone.exists");
        String username;
        if (f.getUsername() == null || f.getUsername().isBlank()) username = users.freeUsername(com.mealtoken.util.Validators.usernameBase(f.getEmail()));
        else {
            username = com.mealtoken.util.Validators.requireUsername(f.getUsername());
            if (users.existsByUsernameIgnoreCase(username)) throw new BusinessException("error.username.exists");
        }
        boolean owner = "OWNER".equals(f.getAccountType());
        if (owner && (f.getHotelName() == null || f.getHotelName().isBlank()))
            throw new BusinessException("error.hotel.name.required");

        User u = new User();
        u.setFullName(f.getFullName().trim());
        u.setUsername(username);
        u.setEmail(f.getEmail().trim().toLowerCase(Locale.ROOT));
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(f.getPassword()));
        u.setPreferredLanguage(language);
        u.getRoles().add(roles.findByName(owner ? "OWNER" : "CUSTOMER").orElseThrow());
        u = users.save(u);

        if (owner) {
            Hotel h = new Hotel();
            h.setName(f.getHotelName().trim());
            h.setSlug(uniqueSlug(f.getHotelName()));
            h.setOwnerId(u.getId());
            h.setCity(blankToNull(f.getCity()));
            h.setArea(blankToNull(f.getArea()));
            h.setPhone(phone);
            h.setStatus("PENDING"); // Super Admin must approve before it goes live
            hotels.save(h);
        }
        return u;
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{Alnum}]+", "-").replaceAll("(^-|-$)", "").toLowerCase(Locale.ROOT);
        if (base.isEmpty()) base = "hotel";
        String slug = base;
        int i = 2;
        while (hotels.existsBySlug(slug)) slug = base + "-" + i++;
        return slug;
    }
}
