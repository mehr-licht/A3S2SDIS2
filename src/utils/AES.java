package utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * classe AES (Advanced Encryption Standard)
 */
public class AES {
 
    private SecretKeySpec private_key;
    private byte[] key;

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
            set_key(private_key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, this.private_key);
            return cipher.doFinal(message);
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
            set_key(private_key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, this.private_key);
            return cipher.doFinal((message));
        }
        catch (Exception e)
        {
            System.out.println("Erro ao decifrar: " + e.toString());
        }
        return null;
    }
}