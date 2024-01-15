package com.example.springdemo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getUrlEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AESEncryptDecryptTest {
    public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    public static final String AES_CBC_PADDED = "AES/CBC/PKCS5Padding";
    public static int IV_BYTE_LEN = 16;
    public static int AUTH_TAG_BYTE_LEN = 12;
    public static int AUTH_TAG_BIT_LEN = AUTH_TAG_BYTE_LEN * 8;

    private String input;

    @BeforeEach
    public void init() throws JsonProcessingException {
        Map<String, String> data = Map.of(
                "p1", UUID.randomUUID().toString(),
                "p2", "MyApp",
                "usr", "me",
                "time", String.valueOf(Instant.now().getEpochSecond())
        );
        input = new ObjectMapper().writeValueAsString(data);
    }

    @Test
    void givenString_whenEncryptUsingCBC_thenSuccess()
            throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {

        SecretKey secret = genAESKey(256);
        IvParameterSpec spec = new IvParameterSpec(generateIv(IV_BYTE_LEN));
        String algorithm = AES_CBC_PADDED;
        byte[] cipherBytes = encrypt(algorithm, input.getBytes(UTF_8), secret, spec);
        byte[] plainBytes = decrypt(algorithm, cipherBytes, secret, spec);
        String plainText = new String(plainBytes, UTF_8);
        assertEquals(input, plainText);

        System.out.format("secret = %s\n", getUrlEncoder().encodeToString(secret.getEncoded()));
        System.out.format("iv = %s\n", getUrlEncoder().encodeToString(spec.getIV()));
        System.out.format("input = %s\n", input);
        System.out.format("token = %s\n", getUrlEncoder().encodeToString(cipherBytes));
        System.out.format("plainText = %s\n", plainText);
    }

    @Test
    void givenString_whenEncryptWithGCM_thenSuccess() throws Exception {
        SecretKey secret = genAESKey(256);
        String algorithm = AES_GCM_NO_PADDING;
        GCMParameterSpec spec = new GCMParameterSpec(AUTH_TAG_BIT_LEN, generateIv(IV_BYTE_LEN));
        byte[] cipherBytes = encrypt(algorithm, input.getBytes(UTF_8), secret, spec);
        byte[] plainBytes = decrypt(algorithm, cipherBytes, secret, spec);
        String plainText = new String(plainBytes, UTF_8);
        assertEquals(input, plainText);

        System.out.format("secret = %s\n", getUrlEncoder().encodeToString(secret.getEncoded()));
        System.out.format("iv = %s\n", getUrlEncoder().encodeToString(spec.getIV()));
        System.out.format("input = %s\n", input);
        System.out.format("token = %s\n", getUrlEncoder().encodeToString(cipherBytes));
        System.out.format("plainText = %s\n", plainText);
    }

    @Test
    void givenString_whenEncryptWithGCMandPrefix_thenSuccess() throws Exception {
        SecretKey secret = genAESKey(256);
        String algorithm = AES_GCM_NO_PADDING;
        byte[] cipherBytes = encryptWithPrefix(algorithm, input.getBytes(UTF_8), secret);
        byte[] plainBytes = decryptWithPrefix(algorithm, cipherBytes, secret);
        String plainText = new String(plainBytes, UTF_8);
        assertEquals(input, plainText);

        System.out.format("secret = %s\n", getUrlEncoder().encodeToString(secret.getEncoded()));
        System.out.format("input = %s\n", input);
        System.out.format("token = %s\n", getUrlEncoder().encodeToString(cipherBytes));
        System.out.format("plainText = %s\n", plainText);
    }

    public static SecretKey genAESKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator secretGenerator = KeyGenerator.getInstance("AES");
        secretGenerator.init(n, SecureRandom.getInstanceStrong());
        SecretKey secret = secretGenerator.generateKey();
        return secret;
    }

    public static byte[] generateIv(int n) {
        byte[] iv = new byte[n];
        new SecureRandom().nextBytes(iv);
        return iv;
    }


    public byte[] encryptWithPrefix(String algorithm, byte[] plainBytes, SecretKey secret) throws Exception {
        byte[] iv = generateIv(IV_BYTE_LEN);
        byte[] cipherBytes = encrypt(algorithm, plainBytes, secret, getParameterSpec(algorithm, iv));

        return ByteBuffer.allocate(iv.length + cipherBytes.length)
                .put(iv)
                .put(cipherBytes)
                .array();
    }

    public byte[] decryptWithPrefix(String algorithm, byte[] prefixText, SecretKey secret) throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(prefixText);

        byte[] iv = new byte[IV_BYTE_LEN];
        bb.get(iv);

        byte[] authText = new byte[bb.remaining()];
        bb.get(authText);

        return decrypt(algorithm, authText, secret, getParameterSpec(algorithm, iv));
    }

    public AlgorithmParameterSpec getParameterSpec(String algorithm, byte[] iv) {
        if (AES_GCM_NO_PADDING.equals(algorithm)) {
            return new GCMParameterSpec(AUTH_TAG_BIT_LEN, iv);
        } else if (AES_CBC_PADDED.equals(algorithm)) {
            return new IvParameterSpec(iv);
        } else {
            throw new IllegalArgumentException("Algorithm " + algorithm + " not supported");
        }
    }

    public static byte[] encrypt(String algorithm, byte[] plainBytes, SecretKey secret,
                                 AlgorithmParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, secret, iv);
        return cipher.doFinal(plainBytes);
    }

    public static byte[] decrypt(String algorithm, byte[] cipherBytes, SecretKey secret,
                                 AlgorithmParameterSpec spec) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, secret, spec);
        return cipher.doFinal(cipherBytes);
    }

}
