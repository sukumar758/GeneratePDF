package org.acentrik.service;

import org.acentrik.model.OfferLetter;
import org.acentrik.model.User;
import org.acentrik.repository.OfferLetterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OfferLetterService {

    @Autowired
    private OfferLetterRepository offerLetterRepository;

    /**
     * Save an offer letter for a user
     * 
     * @param user The user to save the offer letter for
     * @param fileName The name of the file
     * @param content The content of the offer letter as a byte array
     * @return The saved OfferLetter entity
     */
    public OfferLetter saveOfferLetter(User user, String fileName, byte[] content) {
        OfferLetter offerLetter = new OfferLetter(user, fileName, content, LocalDateTime.now());
        return offerLetterRepository.save(offerLetter);
    }

    /**
     * Get all offer letters for a user
     * 
     * @param user The user to get offer letters for
     * @return List of offer letters for the user
     */
    public List<OfferLetter> getOfferLettersForUser(User user) {
        return offerLetterRepository.findByUser(user);
    }

    /**
     * Get the latest offer letter for a user
     * 
     * @param user The user to get the latest offer letter for
     * @return Optional containing the latest offer letter if found
     */
    public Optional<OfferLetter> getLatestOfferLetterForUser(User user) {
        return offerLetterRepository.findFirstByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Remove an offer letter by its ID
     * 
     * @param id The ID of the offer letter to remove
     * @return true if the offer letter was found and removed, false otherwise
     */
    public boolean removeOfferLetterById(Long id) {
        if (offerLetterRepository.existsById(id)) {
            offerLetterRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get an offer letter by its ID
     * 
     * @param id The ID of the offer letter to retrieve
     * @return Optional containing the offer letter if found
     */
    public Optional<OfferLetter> getOfferLetterById(Long id) {
        return offerLetterRepository.findById(id);
    }
}
