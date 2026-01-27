package com.example.messenger.service;

import com.example.messenger.domain.AppUser;
import com.example.messenger.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public UserDetailsServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> appUserRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail)));
        return user;
    }
}
