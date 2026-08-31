package com.tsix.immocrm.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * One-off tool, not a real test: prints a BCrypt hash for the password given via
 * {@code -Dpassword=...}, to seed the very first {@code AppUserCredential} row by hand (see
 * project auth bootstrap discussion — no account can authenticate against the new API until one
 * exists). Deliberately named without a "Test" suffix so Surefire's default naming-convention
 * inclusion (classes ending in "Test") skips it during a normal {@code mvn test} run — it only
 * runs when targeted explicitly.
 * <p>
 * Run: {@code mvnw.cmd test -Dtest=GenerateBootstrapHashTool#printHash -Dpassword=MotDePasseTemp123}
 */
class GenerateBootstrapHashTool {

    @Test
    void printHash() {
        String password = System.getProperty("password");
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Pass -Dpassword=... , ex: mvnw.cmd test -Dtest=GenerateBootstrapHashTool#printHash -Dpassword=MotDePasseTemp123");
        }
        String hash = new BCryptPasswordEncoder().encode(password);
        System.out.println("BCRYPT_HASH=" + hash);
    }
}
