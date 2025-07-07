package org.quangdung.core.utils.uid_util;

import java.security.SecureRandom;
import java.time.Instant;

import jakarta.inject.Singleton;

@Singleton
public class UidUtil implements IUidUtil {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int UID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generateUid() {
       StringBuilder uid = new StringBuilder(UID_LENGTH);
        
        // Add timestamp-based prefix for better uniqueness (2 chars)
        long timestamp = Instant.now().toEpochMilli();
        uid.append(CHARACTERS.charAt((int) (timestamp % CHARACTERS.length())));
        uid.append(CHARACTERS.charAt((int) ((timestamp / 36) % CHARACTERS.length())));
        
        // Add random characters (6 chars)
        for (int i = 2; i < UID_LENGTH; i++) {
            uid.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        
        return uid.toString();
    }
}
