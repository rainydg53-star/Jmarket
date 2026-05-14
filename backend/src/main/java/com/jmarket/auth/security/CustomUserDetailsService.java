package com.jmarket.auth.security;

import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(user -> {
                    if (user.isBanned()) {
                        throw new JmarketException(ErrorCode.USER_BANNED);
                    }
                    return new CustomUserDetails(user);
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
