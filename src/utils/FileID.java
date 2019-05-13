package utils;

import java.security.MessageDigest;
import java.io.FileInputStream;

/**
 * Classe de FileID
 */
public class FileID {
	private String identifier;

	/**
	 * Construtor de FileID
	 * @param path caminho do ficheiro
	 */
	public FileID(String path) {
		try {
			MessageDigest md_instance = MessageDigest.getInstance("SHA-256");
			FileInputStream stream = new FileInputStream(path);

			byte[] bytesize = new byte[8192];

			int bytes_read = 0;
			while ((bytes_read = stream.read(bytesize)) != -1) {
				md_instance.update(bytesize, 0, bytes_read);
			}

			byte[] byte_array_hash_value = md_instance.digest();
			stream.close();

			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < byte_array_hash_value.length; i++) {
				buffer.append(Integer.toString((byte_array_hash_value[i] & 0xff) + 0x100, 16).substring(1));
			}

			this.identifier = buffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Devolve o identificador do ficheiro
	 * @return identificador do ficheiro
	 */
	@Override
	public String toString() {
		return this.identifier;
	}
}
