package com.infy.tmwa.service;

import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security calls this during JWT filter validation to load the user
    // from DB and check isEnabled() (is_active flag) + getAuthorities() (role)
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email));
    }
}