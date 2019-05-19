package utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/** classe AES (Advanced Encryption Standard) */
public class AES {

  private SecretKeySpec private_key;
  private byte[] key;

  /** @param key */
  public void set_key(String key) {
    MessageDigest sha = null;
    try {
      this.key = key.getBytes("UTF-8");
      sha = MessageDigest.getInstance("SHA-1");
      this.key = sha.digest(this.key);
      this.key = Arrays.copyOf(this.key, 16);
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
   * @param private_key chave privada da cifra
   * @return
   */
  public byte[] encrypt(byte[] message, String private_key) {
    try {
      return doCipher(message, private_key, Cipher.ENCRYPT_MODE);
    } catch (Exception e) {
      System.out.println("Erro ao cifrar: " + e.toString());
    }
    return null;
  }

  /**
   * Decifra mensagem com a chave privada
   *
   * @param message mensagem a decifrar
   * @param private_key chave privada da cifra
   * @return
   */
  public byte[] decrypt(byte[] message, String private_key) {
    try {
      return doCipher(message, private_key, Cipher.DECRYPT_MODE);
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
  private byte[] doCipher(byte[] message, String private_key, int decryptMode)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
          IllegalBlockSizeException, BadPaddingException {
    set_key(private_key);
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
    cipher.init(decryptMode, this.private_key);
    return cipher.doFinal((message));
  }
}
