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
  // A transformation is a string that describes the operation (or set of operations)
  // to be performed on the given input, to produce some output.
  // A transformation always includes the name of a cryptographic algorithm (e.g., AES),
  // and may be followed by a feedback mode and padding scheme

  /** @param key */ /*
  public void set_key(String key) {
    MessageDigest sha;
    try {
      this.key = key.getBytes("UTF-8");
      sha = MessageDigest.getInstance("SHA-1");

      this.key = sha.digest(this.key);
      this.key = Arrays.copyOf(this.key, 16); // era 16
      private_key = new SecretKeySpec(this.key, "AES");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }*/

  /**
   * Cifra mensagem com chave privada
   *
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
          IllegalBlockSizeException, BadPaddingException,
          InvalidAlgorithmParameterException { // , String private_key,

    set_key(key);

    // createKey();

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    SecretKeySpec keySpec = new SecretKeySpec((this.private_key).getEncoded(), "AES");

    IvParameterSpec aes_IV = new IvParameterSpec(AES_IV);//IV
    cipher.init(decryptMode, keySpec, aes_IV);
    return cipher.doFinal((message));
  }

  private void createIV() {
    // byte[] IV = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(IV);
  }

  private void createKey() throws NoSuchAlgorithmException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(256);
    key3 = keyGenerator.generateKey();
  }

  void testStrings() {
    // para testar
    String plainText =
        "This is a plain text which need to be encrypted by Java AES 256 Algorithm in CBC Mode";
    System.out.println("Original Text  : " + plainText);
    byte[] cipherText = encrypt(plainText.getBytes(), new String(this.key)); // ,key3, IV);
    System.out.println("Encrypted Text : " + Base64.getEncoder().encodeToString(cipherText));
    String decryptedText = new String(decrypt(cipherText, new String(this.key))); // IV);
    System.out.println("DeCrypted Text : " + decryptedText);
  }

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


    /*
    ECB (electronic code book) is basically raw cipher.
     For each block of input, you encrypt the block and get some output.
     The problem with this transform is that any resident properties of the plaintext might well show up in the ciphertext – possibly not as clearly – that's what blocks and key schedules are supposed to protect againt, but analyzing the patterns you may be able to deduce properties that you otherwise thought were hidden.

     The transformation AES/ECB/PKCS5Padding tells the getInstance method to instantiate the Cipher object as an AES cipher with ECB mode of operation and PKCS5 padding scheme.
ECB and PKCS5Padding are defaults

ECB isn't a good choice of modes to use because it isn't very secure. CBC is a much better option since it makes use of an initialization vector.

     */
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

  private byte[] doCipher(byte[] message, String private_key, int decryptMode)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    set_key(private_key);
    Cipher cipher = Cipher.getInstance(ECB_TRANSFORMATION);
    cipher.init(decryptMode, this.private_key);
    return cipher.doFinal((message));
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

/*
String[]  defaultSuite={"TLS_RSA_WITH_AES_128_CBC_SHA"};
//public abstract void setEnabledCipherSuites(String[] suites)
socket.setEnabledCipherSuites(defaultSuite);
//antes do handshake
//https://stackoverflow.com/questions/13943351/how-to-specify-the-ciphersuite-to-be-used-in-ssl-session
 */



//[TODO]alterar relatório ENCRYPT
//[TODO]alterar estrutura files
//[TODO]alterar portos de 3000 para outro numero qq -> ver google usual user ports

//[TODO]rever aquilo no Client e no Peer getRegistry

//[TODO]ver porque só relica 1
//[TODO]ver porque só restaura 1  - problemas no Synchronized (peer.Peer ln 436)
//[TODO]DELETE - é suposto só apagar os chunks que estão distribuidos?


//BACKUP - probs na replic (se digo 2 com 3 peers, ele replica 1...)  (se digo 3 com 3 peers, ele replica 1...)
//diz que Backup not completed => no wait_backup_result(...) -> meter um while que não sai enquanto !backup_done

//RESTORE - probls no segundo? - problemas no Synchronized (peer.Peer ln 436)
//STATE OK
//RECLAIM OK
//DELETE só apaga os chunks que estão distribuidos...