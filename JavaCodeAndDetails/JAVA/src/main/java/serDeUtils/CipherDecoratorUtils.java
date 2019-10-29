package serDeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.crypto.stream.CryptoInputStream;
import org.apache.commons.crypto.stream.CryptoOutputStream;

/**
 * This class decorates stream to have encryption/decryption based on whether it is selected
 * 
 * @author Kunj
 *
 */
public class CipherDecoratorUtils {

    // parameters used to create cipher stream
    private static final String KEY_STR = "1234567890123456";
    private static final SecretKeySpec KEY = new SecretKeySpec(KEY_STR.getBytes(StandardCharsets.UTF_8), "AES");
    private static final IvParameterSpec IV = new IvParameterSpec(KEY_STR.getBytes(StandardCharsets.UTF_8));
    private static final Properties DUMMY_PROPS = new Properties();
    private static final String TRANSFORM_STR = "AES/CBC/PKCS5Padding";

    /**
     * This method decorates inputStream making it decrypt an already encrypted data
     * 
     * @param addDecoration
     * @param is
     * @return
     * @throws IOException
     */
    public static InputStream decorateInput(boolean addDecoration, InputStream is) throws IOException {
        if (!addDecoration) {
            return is;
        } else {
            return new CryptoInputStream(TRANSFORM_STR, DUMMY_PROPS, is, KEY, IV);
        }
    }

    /**
     * This method decorates outputStream making it encrypt the data being written
     * 
     * @param addDecoration
     * @param is
     * @return
     * @throws IOException
     */
    public static OutputStream decorateOutput(boolean addDecoration, OutputStream os) throws IOException {
        if (!addDecoration) {
            return os;
        } else {
            return new CryptoOutputStream(TRANSFORM_STR, DUMMY_PROPS, os, KEY, IV);
        }
    }
}
