package org.acentrik.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "offer_letters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferLetter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String fileName;
    
    @Lob
    @Column(nullable = false)
    private byte[] content;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    // Constructor with user, fileName, content, and createdAt
    public OfferLetter(User user, String fileName, byte[] content, LocalDateTime createdAt) {
        this.user = user;
        this.fileName = fileName;
        this.content = content;
        this.createdAt = createdAt;
    }
}