package org.acentrik.repository;

import org.acentrik.model.OfferLetter;
import org.acentrik.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferLetterRepository extends JpaRepository<OfferLetter, Long> {
    
    /**
     * Find all offer letters for a specific user
     * @param user The user to find offer letters for
     * @return List of offer letters for the user
     */
    List<OfferLetter> findByUser(User user);
    
    /**
     * Find the latest offer letter for a specific user
     * @param user The user to find the latest offer letter for
     * @return Optional containing the latest offer letter if found
     */
    Optional<OfferLetter> findFirstByUserOrderByCreatedAtDesc(User user);
}