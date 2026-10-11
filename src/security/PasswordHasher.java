package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "pbkdf2-sha256";

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean matches(String password, String encoded) {
        if (password == null || encoded == null) return false;
        if (!encoded.startsWith(PREFIX + "$")) {
            return MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 1 || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean needsUpgrade(String encoded) {
        return encoded == null || !encoded.startsWith(PREFIX + "$");
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.spec.InvalidKeySpecException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("The configured password-hashing algorithm is unavailable.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
