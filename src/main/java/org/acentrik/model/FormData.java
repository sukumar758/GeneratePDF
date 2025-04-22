package org.acentrik.model;


import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;

@Setter
@Getter
public class FormData {
    // Getters & setters
    private String firstName;
    private String lastName;
    private String email;
    private String domain;
    private String manager;
    private LocalDate joiningDate;
    private String role;



    // Add more fields if needed

}
