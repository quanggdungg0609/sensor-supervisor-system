package org.quangdung.core.utils.uid_util;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UidUtilTest {

    @Inject
    IUidUtil uidUtil;

    @Test
    @DisplayName("Should generate UID with correct length and valid characters")
    void testGenerateUid_FormatValidation() {
        // 1. Generate a UID
        String uid = uidUtil.generateUid();
        System.out.println("Generated UID for format check: " + uid);

        // 2. Assert it is not null
        assertNotNull(uid, "UID should not be null.");

        // 3. Assert it has the correct length (8 characters)
        assertEquals(8, uid.length(), "UID should be 8 characters long.");

        // 4. Assert it only contains uppercase letters and digits
        assertTrue(uid.matches("^[A-Z0-9]{8}$"),
            "UID should only contain uppercase letters (A-Z) and digits (0-9).");
    }

    @Test
    @DisplayName("Should generate unique UIDs on multiple calls")
    void testGenerateUid_Uniqueness() {
        int numberOfUidsToGenerate = 10_000;
        Set<String> generatedUids = new HashSet<>();

        // Generate 10,000 UIDs and add them to a Set
        for (int i = 0; i < numberOfUidsToGenerate; i++) {
            generatedUids.add(uidUtil.generateUid());
        }

        // The size of the Set should be 10,000 if all UIDs were unique
        assertEquals(numberOfUidsToGenerate, generatedUids.size(),
            "All generated UIDs should be unique.");
    }

    @RepeatedTest(10)
    @DisplayName("Repeatedly check UID format")
    void testGenerateUid_RepeatedFormatCheck() {
        String uid = uidUtil.generateUid();
        assertNotNull(uid);
        assertEquals(8, uid.length());
        assertTrue(uid.matches("^[A-Z0-9]{8}$"));
    }
}