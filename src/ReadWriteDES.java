import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

public class ReadWriteDES {
    private static final String PASS = "0123456701234567";

    static String encode(byte[] bytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key k = new SecretKeySpec(PASS.getBytes(), "AES");
        c.init(Cipher.ENCRYPT_MODE, k, new IvParameterSpec(new byte[16]));

        OutputStream cos = new CipherOutputStream(out, c);
        cos.write(bytes);
        cos.close();

        String s = Base64.getMimeEncoder().encodeToString( out.toByteArray() );
        return s;
    }

    static String decode(String message) throws Exception {
        byte[] decode = Base64.getMimeDecoder().decode( message.getBytes(StandardCharsets.ISO_8859_1) );
        InputStream is = new ByteArrayInputStream( decode );
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key k = new SecretKeySpec(PASS.getBytes(), "AES");
        c.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(new byte[16]));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherInputStream cis = new CipherInputStream(is, c);

        int b = 0;
        // TODO Input length must be multiple of 8 when decrypting with padded cipher
        while ((b = cis.read()) != -1) {
            bos.write(b);
        }

        cis.close();

        byte[] decodedBytes = Base64.getMimeDecoder().decode(bos.toByteArray());
        String decodedUrl = new String(decodedBytes);

        return decodedUrl;
    }
}
