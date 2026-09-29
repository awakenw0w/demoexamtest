package ru.demoexam.algorithms.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** Создаёт и проверяет хеши паролей пользователей. */
public class PasswordHasher {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    private static final int ITERATIONS = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Превращает пароль в строку с солью и хешем для сохранения в базе. */
    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        // Для каждого пароля создаётся своя случайная соль.
        RANDOM.nextBytes(salt);

        byte[] hash = makeHash(password.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Сравнивает введённый пароль с ранее сохранённым хешем. */
    public static boolean passwordMatches(String password, String savedHash) {
        String[] parts = savedHash.split(":");
        if (parts.length != 2) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = makeHash(password.toCharArray(), salt);
            return java.security.MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Получает хеш пароля с помощью встроенного алгоритма Java. */
    private static byte[] makeHash(char[] password, byte[] salt) {
        // Повторные вычисления делают подбор пароля медленнее.
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, HASH_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Не удалось обработать пароль.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}
