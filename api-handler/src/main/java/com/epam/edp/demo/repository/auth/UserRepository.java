package com.epam.edp.demo.repository.auth;

import com.epam.edp.demo.entity.auth.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    List<User> getAll();
    String findEmailById(String userId);
    boolean existsByEmail(String email);

    void updateNames(String userId, String firstName, String lastName);
    void updatePassword(String userId, String newPassword);
    void updateEmail(String userId, String newEmail);
}
