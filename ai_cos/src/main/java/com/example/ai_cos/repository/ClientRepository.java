package com.example.ai_cos.repository;

import com.example.ai_cos.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
    boolean existsByEmail(String email);
    
    List<Client> findByFirstNameContainingIgnoreCase(String firstName);
    
    List<Client> findByLastNameContainingIgnoreCase(String lastName);
    
    @Query("SELECT c FROM Client c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Client> searchByName(String name);
    
    List<Client> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
}
