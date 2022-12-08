package org.iushu.market.component;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.NoIvGenerator;
import org.jasypt.salt.ZeroSaltGenerator;

public class PropertyEncryptor {

    static void encrypt(String password, String input) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        encryptor.setSaltGenerator(new ZeroSaltGenerator());
        encryptor.setIvGenerator(new NoIvGenerator());
        String encrypted = encryptor.encrypt(input);
        String decrypted = encryptor.decrypt(encrypted);
        System.out.println("password: " + password);
        System.out.println("input: " + input);
        System.out.println("encrypted: " + encrypted);
        System.out.println("decrypted: " + decrypted + " " + decrypted.equals(input));
    }

    public static void main(String[] args) {

    }

}
