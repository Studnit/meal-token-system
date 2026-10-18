package com.mealtoken.repository;

import com.mealtoken.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    /** Old data may hold the same mobile twice; the earliest account wins instead of failing the sign-in. */
    Optional<User> findFirstByPhoneOrderByIdAsc(String phone);
    boolean existsByPhone(String phone);

    /** A username nobody has yet, starting from a suggestion (adds a number when it is taken). */
    default String freeUsername(String base) {
        String name = base;
        int i = 2;
        while (existsByUsernameIgnoreCase(name)) name = base + i++;
        return name;
    }
}
