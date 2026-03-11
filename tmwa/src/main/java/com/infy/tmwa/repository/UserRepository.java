package com.infy.tmwa.repository;

import com.infy.tmwa.entity.User;
import com.infy.tmwa.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByIsActive(boolean isActive);
}