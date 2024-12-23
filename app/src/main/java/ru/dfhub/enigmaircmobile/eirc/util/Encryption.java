package ru.dfhub.enigmaircmobile.eirc.util;


import ru.dfhub.enigmaircmobile.eirc.Config;
import ru.dfhub.enigmaircmobile.eirc.Gui;
import ru.dfhub.enigmaircmobile.eirc.Main;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Base64;
/**
 * Class for initializing and using encryption
 */
public class Encryption {

    /**
     * Various encryption related errors.
     * The essence of the errors is conveyed in the message
     */
    public static class EncryptionException extends Exception {
        public EncryptionException(String message) {
            super(message);
        }
    }

    private static SecretKey key;
    private static IvParameterSpec iv;
    private static Cipher encryptCipher;
    private static Cipher decryptCipher;

    /**
     * Getting the encryption key from the config
     * @throws EncryptionException Encryption key is empty
     * @throws IllegalArgumentException Key is invalid
     */
    public static void initKey() throws EncryptionException, IllegalArgumentException {
        String[] keyString = Config.getConfig().optString("security-key", "").split("<->");

        if (keyString.length == 1) throw new EncryptionException("Encryption key not specified");
        if (keyString.length != 2) throw new IllegalArgumentException("Encryption key is incorrect");

        key = new SecretKeySpec(
                Base64.getDecoder().decode(keyString[0]),
                "AES"
        );
        iv = new IvParameterSpec(
                Base64.getDecoder().decode(keyString[1])
        );
    }

    /**
     * Initializing encryption and decryption methods
     * @throws InvalidKeyException Invalid key
     */
    public static void initEncryption() throws Exception {
        try {
            encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (Exception e) {
            Gui.breakInput();
            Gui.showNewMessage(String.format("An unexpected error occurred while initializing encryption (%s)", e.getMessage()), Gui.MessageType.SYSTEM_ERROR);
        }

        encryptCipher.init(Cipher.ENCRYPT_MODE, key, iv);
        decryptCipher.init(Cipher.DECRYPT_MODE, key, iv);
    }

    /**
     * Generate new valid key and encode id to String(base64)
     * @return Encoded key
     */
    public static String generateNewKey() {
        try {
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            keygen.init(256);

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            String encKey = Base64.getEncoder().encodeToString(keygen.generateKey().getEncoded());
            String ivString = Base64.getEncoder().encodeToString(iv);

            return String.format("%s<->%s", encKey, ivString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate new key and write it to file
     * @throws IOException Errors when writing key to file
     */
    public static void generateNewKeyFile() throws IOException {
        Files.write(Paths.get("newkey.txt"), String.format("New encryption key: %s", generateNewKey()).getBytes());
    }

    /**
     * Encrypt text
     * @param text Text
     * @return Encrypted text (base64)
     * @throws Exception Wrong encryption key
     */
    public static String encrypt(String text) throws Exception {
        byte[] encryptedBytes = encryptCipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypt text
     * @param text Encrypted text (base64)
     * @return Decrypted text
     * @throws Exception Wrong encryption key
     */
    public static String decrypt(String text) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(text);
        byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }

    /**
     * Tells you that the encryption key is incorrect and closes the program
     */
    public static void showIncorrectKeyError() {
        Gui.showNewMessage("Your encryption key is damaged or incorrect!", Gui.MessageType.SYSTEM_ERROR);
        Gui.showNewMessage("Generate new one by clicking \"refresh\" button on the start screen", Gui.MessageType.SYSTEM_INFO);
        Gui.breakInput();
    }

    public static void showNullKeyErrorAndGenerateNewOne() {
        Gui.showNewMessage("You haven't set the encryption key!", Gui.MessageType.SYSTEM_ERROR);
        Gui.breakInput();
    }

}
