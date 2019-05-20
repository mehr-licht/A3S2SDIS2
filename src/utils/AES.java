package utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** classe AES (Advanced Encryption Standard) */
public class AES {
  private SecretKeySpec private_key;
  private byte[] key;


  private static SecretKey key3;
  private static byte[] IV = new byte[16];
  //private static final byte[] AES_IV = "1234567890123456".getBytes();

  private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";
    //A transformation is a string that describes the operation (or set of operations)
    // to be performed on the given input, to produce some output.
    // A transformation always includes the name of a cryptographic algorithm (e.g., AES),
    // and may be followed by a feedback mode and padding scheme

  /** @param key */
  public void set_key(String key) {
    MessageDigest sha = null;
    try {
      this.key = key.getBytes("UTF-8");
      sha = MessageDigest.getInstance("SHA-1");

      this.key = sha.digest(this.key);
      this.key = Arrays.copyOf(this.key, 256);//era 16
      private_key = new SecretKeySpec(this.key, "AES");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  /**
   * Cifra mensagem com chave privada
   *
   * @param message mensagem para cifrar

   * @return
   */
  public byte[] encrypt(byte[] message, String key) {
    try {
      return doCipher(message, Cipher.ENCRYPT_MODE,key);
    } catch (Exception e) {
      System.out.println("Erro ao cifrar: " + e.toString());
    }
    return null;
  }

  /**
   * Decifra mensagem com a chave privada
   *
   * @param message mensagem a decifrar

   * @return
   */
  public byte[] decrypt(byte[] message, String key) {
    try {
      return doCipher(message, Cipher.DECRYPT_MODE, key);
    } catch (Exception e) {
      System.out.println("Erro ao decifrar: " + e.toString());
    }
    return null;
  }

  /*
     ECB (electronic code book) is basically raw cipher.
      For each block of input, you encrypt the block and get some output.
      The problem with this transform is that any resident properties of the plaintext might well show up in the ciphertext – possibly not as clearly – that's what blocks and key schedules are supposed to protect againt, but analyzing the patterns you may be able to deduce properties that you otherwise thought were hidden.

      The transformation AES/ECB/PKCS5Padding tells the getInstance method to instantiate the Cipher object as an AES cipher with ECB mode of operation and PKCS5 padding scheme.
  ECB and PKCS5Padding are defaults

  ECB isn't a good choice of modes to use because it isn't very secure. CBC is a much better option since it makes use of an initialization vector.


      ECB (Electronic Codebook) is essentially the first generation of the AES.
      It is the most basic form of block cipher encryption.

  CBC (Cipher Blocker Chaining) is an advanced form of block cipher encryption.
  With CBC mode encryption, each ciphertext block is dependent on all plaintext blocks processed up to that point.
  This adds an extra level of complexity to the encrypted data.
       */

  /**
   * Cifra ou decifra a mensagem
   *
   * @param message mensagem a cifrar ou decifrar
   * @param key chave privada
   * @param decryptMode encriptar ou decifrar
   * @return texto cifrado ou decifrado consoante o decryptMode passado
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  private byte[] doCipher(byte[] message, int decryptMode, String key)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {//, String private_key,
        set_key(key);

   // createKey();
    createIV();
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKeySpec keySpec = new SecretKeySpec((this.private_key).getEncoded(), "AES");
    IvParameterSpec aes_IV = new IvParameterSpec(IV);
    cipher.init(decryptMode, keySpec, aes_IV);
    return cipher.doFinal((message));
  }

  private void createIV() {
    //byte[] IV = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(IV);
    }

    private void createKey() throws NoSuchAlgorithmException{
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        key3 = keyGenerator.generateKey();
    }

    private void testStrings(){
      // para testar
     String plainText = "This is a plain text which need to be encrypted by Java AES 256 Algorithm in CBC Mode";
        System.out.println("Original Text  : "+plainText);
     byte[] cipherText = encrypt(plainText.getBytes(),new String(this.key));//,key3, IV);
        System.out.println("Encrypted Text : "+Base64.getEncoder().encodeToString(cipherText));
      String decryptedText = new String(decrypt(cipherText,new String(this.key)));// IV);
        System.out.println("DeCrypted Text : "+ decryptedText);
    }
}
// cipher.init(int opmode, Key key, AlgorithmParameterSpec param)
/*
DEcrypt
final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encKey, "AES"), new IvParameterSpec(iv));
byte[] plainText = cipher.doFinal(cipherText);
 */

/*
Encrypt
final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //actually uses PKCS#7
cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encKey, "AES"), new IvParameterSpec(iv));
byte[] cipherText = cipher.doFinal(plainText);
 */
