package com.app.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void fullNameShouldCombineFirstAndLastName() {
        User user = new User("1", "Ada", "Lovelace", "ada@test.com");

        assertEquals("Ada Lovelace", user.fullName());
    }

    @Test
    void fullNameShouldReturnLastNameWhenFirstNameIsMissing() {
        User user = new User("1", null, "Lovelace", "ada@test.com");

        assertEquals("Lovelace", user.fullName());
    }

    @Test
    void fullNameShouldReturnFirstNameWhenLastNameIsMissing() {
        User user = new User("1", "Ada", null, "ada@test.com");

        assertEquals("Ada", user.fullName());
    }

    @Test
    void fullNameShouldReturnUnknownWhenNamesAreMissing() {
        User user = new User("1", null, null, "ada@test.com");

        assertEquals("Inconnu", user.fullName());
    }
}