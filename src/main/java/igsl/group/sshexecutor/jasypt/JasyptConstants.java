package igsl.group.sshexecutor.jasypt;

public class JasyptConstants {
    private JasyptConstants() {}

    public static final byte[] K = new byte[] {0x41, 0x70, 0x31, 0x73, 0x40, 0x32, 0x6F, 0x32, 0x33, 0x21, 0x32, 0x6F, 0x32, 0x34, 0x21, 0x21};
    public static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
    public static final String KEY_OBTENTION_ITERATIONS = "1000";
    public static final String POOL_SIZE = "1";
    public static final String PROVIDER_NAME = "SunJCE";
    public static final String SALT_GENERATOR_CLASS_NAME = "org.jasypt.salt.RandomSaltGenerator";
    public static final String IV_GENERATOR_CLASS_NAME = "org.jasypt.iv.RandomIvGenerator";
    public static final String STRING_OUTPUT_TYPE = "base64";
    public static final int IV_LENGTH = 16;
}
