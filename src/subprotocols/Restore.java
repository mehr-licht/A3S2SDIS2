package subprotocols;

import utils.AES;
import utils.FileID;
import peer.Peer.channel_type;
import utils.Protocol_handler;
import peer.Peer;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * classe Restore
 */
public class Restore implements Runnable {

	private String file_ID;
	private String filepath;
	private String new_filepath;
	private Peer peer;
	private final int CHUNK_MAX_SIZE = 64000;
	private int actual_chunk;
	private HashMap<Integer, byte[]> file_chunks;

	/**
	 * construtor do restore
	 * @param filename nome do ficheiro
	 * @param peer peer
	 */
	public Restore(String filename, Peer peer) {
		this.file_chunks = new HashMap<>();
		this.filepath = Peer.FILES_FOLDER + filename;
		this.peer = peer;
		this.new_filepath = Peer.FILESYSTEM_FOLDER  + "Peer"+this.peer.get_ID() + "/" + Peer.RESTORED_FOLDER + filename;
	}

	/**
	 * run do Restore
	 */
	@Override
	public void run() {

		if (!check_if_peer_can_restore()) {
			return;
		}

		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

		boolean restored = false;
		int time_task = 0;
		this.actual_chunk = 0;
		int attempts = 0;

		while (restored == false) {

			if (attempts == 3) {
				System.out.println("Restauro de ficheiro terminou sem sucesso");
				return;
			}
			send_message_try(attempts);

			Future<Boolean> future = scheduledPool.schedule(is_restored, time_task, TimeUnit.SECONDS);

			boolean result = get_result(future);

			if (!result) {
				time_task = time_task + 1;
				attempts++;
			} else {

				byte[] chunk = this.peer.get_restored_chunks().get(this.actual_chunk + "_" + this.file_ID);
				System.out.println("byte[] chunk = "+new String(this.peer.get_restored_chunks().get(this.actual_chunk + "_" + this.file_ID)));
				String secret_key = "peer" + this.peer.get_ID();
				AES AES = new AES();
				byte[] chunk_decrypted = AES.decrypt(chunk, secret_key);
				
				this.file_chunks.put(this.actual_chunk, chunk_decrypted);

				if (chunk.length < CHUNK_MAX_SIZE) {
					restored = true;
				} else {
					this.actual_chunk++;
					time_task = 0;
					attempts = 0;
				}
			}
		}

		if (restored) {
			restore_file();
		}
	}

	/**
	 * verifica se já recebeu
	 * @param future booleano de promessa de receber
	 * @return se já recebeu
	 */
	private boolean get_result(Future<Boolean> future) {
		boolean result = false;
		try {
			result = future.get();
		} catch (InterruptedException e) {
			System.out.println("Erro ao enviar mensagem de getchunk");
		} catch (ExecutionException e) {
			System.out.println("Erro ao enviar mensagem de getchunk");
		}
		return result;
	}

	/**
	 * Verifica se o peer pode restaurar o ficheiro
	 * @return verdadeiro ou falso
	 */
	private boolean check_if_peer_can_restore() {
		File file = new File(this.filepath);
		if(file.exists()) {
			this.file_ID = new FileID(this.filepath).toString();
		} else {
			System.out.println("You can't restore a file that you didn't backup.");
			return false;
		}
		return true;
	}

	/**
	 * Envia uma vez a mensagem getchunk
	 * @param attempts tentativas
	 */
	private void send_message_try(int attempts) {
		if (attempts == 0) {
			byte[] packet = make_get_chunk_message(this.file_ID, this.actual_chunk);

			try {
				this.peer.send_reply_to_peers(channel_type.MC, packet);
				this.peer.get_wait_restored_chunks().add(this.actual_chunk + "_" + this.file_ID);
			} catch (IOException e) {
				System.out.println("Erro ao enviar mensagem getchunk");
			}
		}
	}

	/**
	 * Restaurar ficheiro
	 */
	private void restore_file() {
		System.out.println("Restauro em: "+this.new_filepath);

		try {
			FileOutputStream fos = new FileOutputStream(this.new_filepath);
			
			this.file_chunks.forEach( (key, value) -> {
				try {
					fos.write(value);
				} catch (IOException e) {
					System.out.println("Erro na espera pelo chunk");
				}
			});

			fos.close();
		} catch (IOException e) {
			System.out.println("Erro ao gravar os chunks restaurados");
		}
	}

	/**
	 * Verifica se restauro terminou
	 */
	Callable<Boolean> is_restored = () -> {
		String hashmapKey = this.actual_chunk + "_" + this.file_ID;
		boolean restored_done = false;

		if (this.peer.get_restored_chunks().get(hashmapKey) != null) {
			restored_done = true;
		}

		return restored_done;
	};

	/**
	 * Cria a mensagem do chunk
	 * @param file_ID identificador do ficheiro
	 * @param chunk_no numero do chunk
	 * @return mensagem a enviar
	 */
	private byte[] make_get_chunk_message(String file_ID, int chunk_no) {
		String message = "GETCHUNK " + this.peer.get_protocol_version() + " " + this.peer.get_ID() + " " + file_ID
				+ " "
				+ chunk_no + " ";
		message = message + Protocol_handler.bi_CRLF;

		return message.getBytes();
	}

}
