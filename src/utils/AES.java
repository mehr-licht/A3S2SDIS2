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

  private static SecretKey key3;
  private static byte[] IV = new byte[16];
  private static final byte[] AES_IV = "1234567890123456".getBytes();

  private static final String ECB_TRANSFORMATION ="AES/ECB/PKCS5PADDING";
  private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";

  /**
   * Cifra mensagem com chave privada
   * @param key chave privada
   * @param message mensagem para cifrar
   * @return
   */
  public byte[] encrypt2(byte[] message, String key) {
    createIV();
    System.out.println("Elength:"+IV.length);
    try {
      return doCipher(addIVtoMsg(message), ENCRYPT_MODE, key);
    } catch (Exception e) {
      System.out.println("Erro ao cifrar: " + e.toString());
    }
    return null;
  }

  private byte[] addIVtoMsg(byte[] msg) {
    System.out.println("get(ira ser Estring)."+new String(IV));
    return ((new String(IV)) + (new String(msg))).getBytes();
  }

  private byte[] extractIVfromMsg(byte[] msg) {
    return ((new String(msg)).substring(16)).getBytes();
  }

  private byte[] getIVfromMsg(byte[] msg) {
    System.out.println("get(ira ser Dstring)."+(new String(msg)).substring(0, 16));
    return ((new String(msg)).substring(0, 16)).getBytes();
  }

  /**
   * Decifra mensagem com a chave privada
   *
   * @param message mensagem a decifrar
   * @return
   */
  public byte[] decrypt2(byte[] message, String key) {
    System.out.println("antes Dlength(supostamente ainda Elength:"+IV.length);
    IV = getIVfromMsg(message);
    System.out.println("Dlength:"+IV.length);
    try {
      return new String(doCipher(extractIVfromMsg(message), Cipher.DECRYPT_MODE, key), "UTF-8")
          .getBytes();
    } catch (Exception e) {
      System.out.println("Erro ao decifrar: " + e.toString());
    }
    return null;
  }


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
          IllegalBlockSizeException, BadPaddingException,
          InvalidAlgorithmParameterException { // , String private_key,

    set_key(key);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    SecretKeySpec keySpec = new SecretKeySpec((this.private_key).getEncoded(), "AES");

    IvParameterSpec aes_IV = new IvParameterSpec(AES_IV);//IV
    cipher.init(decryptMode, keySpec, aes_IV);
    return cipher.doFinal((message));
  }


  //criação do vetor de initialização
  private void createIV() {
    // byte[] IV = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(IV);
  }


  /**
   * initialização da chave de encriptação
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
   * Cifra ou decifra a mensagem
   *
   * @param message mensagem a cifrar ou decifrar
   * @param private_key chave privada
   * @param decryptMode encriptar ou decifrar
   * @return texto cifrado ou decifrado consoante o decryptMode passado
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
