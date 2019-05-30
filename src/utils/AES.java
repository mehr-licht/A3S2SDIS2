package utils;

import static javax.crypto.Cipher.ENCRYPT_MODE;

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
  private static final String ECB_TRANSFORMATION ="AES/ECB/PKCS5PADDING";

  /**
   *
   * @param key
   */
  public void set_key(String key)
  {
    MessageDigest sha = null;
    try {
      this.key = key.getBytes("UTF-8");
      sha = MessageDigest.getInstance("SHA-1");
      this.key = sha.digest(this.key);
      this.key = Arrays.copyOf(this.key, 16);
      private_key = new SecretKeySpec(this.key, "AES");
    }
    catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }


  /**
   * Cifra mensagem com chave privada
   * @param message mensagem para cifrar
   * @param private_key chave privada da cifra
   * @return
   */
  public byte[] encrypt(byte[] message, String private_key)
  {
    try
    {
      return doCipher(message, private_key,  Cipher.ENCRYPT_MODE);
    }
    catch (Exception e)
    {
      System.out.println("Erro ao cifrar: " + e.toString());
    }
    return null;
  }

  /**
   * Decifra mensagem com a chave privada
   * @param message mensagem a decifrar
   * @param private_key chave privada da cifra
   * @return
   */
  public byte[] decrypt(byte[] message, String private_key)
  {
    try
    {
      return doCipher(message, private_key,  Cipher.DECRYPT_MODE);
    }
    catch (Exception e)
    {
      System.out.println("Erro ao decifrar: " + e.toString());
    }
    return null;
  }

  /**
   * Encripta ou decifra
   * @param message mensagem a ser tratada
   * @param private_key chave privada
   * @param decryptMode encriptar ou decifrar
   * @return mensagem já encriptada ou decifrada
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws InvalidKeyException
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   */
  private byte[] doCipher(byte[] message, String private_key, int decryptMode)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    set_key(private_key);
    Cipher cipher = Cipher.getInstance(ECB_TRANSFORMATION);
    cipher.init(decryptMode, this.private_key);
    return cipher.doFinal((message));
  }
}
