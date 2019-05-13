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

	public Restore(String filename, Peer peer) {
		this.file_chunks = new HashMap<>();
		this.filepath = Peer.PEERS_FOLDER + Peer.DISK_FOLDER + peer.get_ID() + "/" + Peer.FILES_FOLDER + filename;
		this.peer = peer;
		this.new_filepath = Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer.get_ID() + "/" + Peer.RESTORED_FOLDER + filename;
	}

	@Override
	public void run() {
		// Check if Peer can restore the file
		File file = new File(this.filepath);
		if(file.exists()) {
			this.file_ID = new FileID(this.filepath).toString();
		} else {
			System.out.println("You can't restore a file that you didn't backup.");
			return;
		}		
		
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

		boolean restored = false;
		int time_task = 0;
		this.actual_chunk = 0;
		int attempts = 0;

		while (restored == false) {

			// After 3 attempts the restore protocol stops
			if (attempts == 3) {
				System.out.println("Restauro de ficheiro terminou sem sucesso");
				return;
			}
			send_message_try(attempts);

			Future<Boolean> future = scheduledPool.schedule(is_restored, time_task, TimeUnit.SECONDS);

			boolean result = false;

			try {
				result = future.get();
			} catch (InterruptedException e) {
				System.out.println("Erro ao enviar mensagem de getchunk");
			} catch (ExecutionException e) {
				System.out.println("Erro ao enviar mensagem de getchunk");
			}



			// If the chunk restored has not yet arrived, the time interval increases 1 second
			if (!result) {
				time_task = time_task + 1;
				attempts++;
			} else {
				// Check if it was the last chunk
				byte[] chunk = this.peer.get_restored_chunks().get(this.actual_chunk + "_" + this.file_ID);
				
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

	private void send_message_try(int attempts) {
		// It only sends the getchunk message one time
		if (attempts == 0) {
			byte[] packet = make_get_chunk_message(this.file_ID, this.actual_chunk);

			try {
				this.peer.send_reply_to_peers(channel_type.MC, packet);
				this.peer.get_wait_restored_chunks().add(this.actual_chunk + "_" + this.file_ID);
			} catch (IOException e) {
				System.out.println("Error sending getchunk message");
			}
		}
	}

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
	 *
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
	 *
	 * @param file_ID
	 * @param chunk_no
	 * @return
	 */
	private byte[] make_get_chunk_message(String file_ID, int chunk_no) {
		String message = "GETCHUNK " + this.peer.get_protocol_version() + " " + this.peer.get_ID() + " " + file_ID
				+ " "
				+ chunk_no + " ";
		message = message + Protocol_handler.bi_CRLF;

		return message.getBytes();
	}

}
