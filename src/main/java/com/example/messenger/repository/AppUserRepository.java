package com.example.messenger.repository;

import com.example.messenger.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//extending JPA gives us save(), findByID(), findAll(), deleteById()
//SPring Data generates the implementations at runtime
public interface AppUserRepository extends JpaRepository<AppUser,Long> {
    Optional<AppUser> findByUsername(String username);//OPtional = might not exist
    Optional<AppUser> findByEmail(String email);
}
