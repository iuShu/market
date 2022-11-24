package org.iushu.market.component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Signature {

    public static final String ALGORITHM = "HmacSHA256";

    public static String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            mac.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return "";
    }

}