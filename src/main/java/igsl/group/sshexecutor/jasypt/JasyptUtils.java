package igsl.group.sshexecutor.jasypt;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import java.util.Base64;

import static igsl.group.sshexecutor.jasypt.JasyptConstants.*;

public class JasyptUtils {

    private static volatile StringEncryptor stringEncryptor;

    private JasyptUtils() {}

    /**
     * Checks if a string is encrypted (Base64 and proper length)
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            // Check if length is at least IV_LENGTH + 1 block (16 bytes)
            return decoded.length > IV_LENGTH + 16;
        } catch (IllegalArgumentException e) {
            // Not valid Base64
            return false;
        }
    }

    public static String decryptEnc(String value) {
        if (value.startsWith("ENC(") && value.endsWith(")")) {
            String encryptedValue = value.substring(4, value.length() - 1);
            return getStringEncryptor().decrypt(encryptedValue);
        } else {
            return value;
        }
    }
    public static String decrypt(String value) {
        if (value == null) return null;
        return getStringEncryptor().decrypt(value);
    }

    public static String encrypt(String value) {
        if (value == null) return null;
        return getStringEncryptor().encrypt(value);
    }

    public static StringEncryptor getStringEncryptor() {
        if (stringEncryptor == null) {
            synchronized (JasyptUtils.class) {
                if (stringEncryptor == null) {
                    stringEncryptor = getPooledPBEStringEncryptor();
                }
            }
        }
        return stringEncryptor;
    }

    public static PooledPBEStringEncryptor getPooledPBEStringEncryptor() {
        PooledPBEStringEncryptor newStringEncryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(new String(K));
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations(KEY_OBTENTION_ITERATIONS);
        config.setPoolSize(POOL_SIZE);
        config.setProviderName(PROVIDER_NAME);
        config.setSaltGeneratorClassName(SALT_GENERATOR_CLASS_NAME);
        config.setIvGeneratorClassName(IV_GENERATOR_CLASS_NAME);
        config.setStringOutputType(STRING_OUTPUT_TYPE);
        newStringEncryptor.setConfig(config);
        return newStringEncryptor;
    }
}
