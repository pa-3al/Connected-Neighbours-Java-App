package com.app.domain.model;

public record User(
        String id,
        String firstName,
        String lastName,
        String email
) {
    public String fullName() {
        if (firstName == null && lastName == null) {
            return "Inconnu";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}