package org.quangdung.core.utils.password_util;

public interface IPasswordUtil {
    String hash(String input);
    String generatePassword(int length);
    boolean isMatch(String plainText, String hashedText);    
}
