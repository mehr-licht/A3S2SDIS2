package subprotocols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import utils.AES;
import utils.FileID;
import peer.Peer.channel_type;
import utils.Protocol_handler;
import peer.Peer;

public class Restore implements Runnable {

	private String fileID;
	private String filePath;
	private String newFilePath;
	private Peer peer;
	private final int CHUNK_MAX_SIZE = 64000;
	private int actualChunk;
	private HashMap<Integer, byte[]> fileChunks;

	public Restore(String filename, Peer peer) {
		this.fileChunks = new HashMap<Integer, byte[]>();
		this.filePath = Peer.PEERS_FOLDER + Peer.DISK_FOLDER + peer.get_ID() + "/" + Peer.FILES_FOLDER + filename;
		this.peer = peer;
		this.newFilePath = Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer.get_ID() + "/" + Peer.RESTORED_FOLDER + filename;
	}

	@Override
	public void run() {
		// Check if Peer can restore the file
		File file = new File(this.filePath);
		if(file.exists()) {
			this.fileID = new FileID(this.filePath).toString();
		} else {
			System.out.println("You can't restore a file that you didn't backup.");
			return;
		}		
		
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

		boolean restored = false;
		int timeTask = 0;
		this.actualChunk = 0;
		int attempts = 0;

		while (restored == false) {

			// After 3 attempts the restore protocol stops
			if (attempts == 3) {
				System.out.println("Restauro de ficheiro terminou sem sucesso");
				return;
			}

			// It only sends the getchunk message one time
			if (attempts == 0) {
				byte[] packet = makeGetChunkMessage(this.fileID, this.actualChunk);

				try {
					this.peer.send_reply_to_peers(channel_type.MC, packet);
					this.peer.get_wait_restored_chunks().add(this.actualChunk + "_" + this.fileID);
				} catch (IOException e) {
					System.out.println("Error sending getchunk message");
				}
			}

			Future<Boolean> future = scheduledPool.schedule(isRestored, timeTask, TimeUnit.SECONDS);

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
				timeTask = timeTask + 1;
				attempts++;
			} else {
				// Check if it was the last chunk
				byte[] chunk = this.peer.get_restored_chunks().get(this.actualChunk + "_" + this.fileID);
				
				String secretKey = "peer" + this.peer.get_ID();
				AES AES = new AES();
				byte[] chunkDecrypted = AES.decrypt(chunk, secretKey);
				
				this.fileChunks.put(this.actualChunk, chunkDecrypted);

				if (chunk.length < CHUNK_MAX_SIZE) {
					restored = true;
				} else {
					this.actualChunk++;
					timeTask = 0;
					attempts = 0;
				}
			}
		}

		if (restored) {
			restoreFile();
		}
	}
	
	private void restoreFile() {		
		System.out.println("Restore em: "+this.newFilePath);

		try {
			FileOutputStream outputStream = new FileOutputStream(this.newFilePath);
			
			this.fileChunks.forEach( (key, value) -> {
				try {
					outputStream.write(value);
				} catch (IOException e) {
					System.out.println("Erro na espera pelo chunk");
				}
			});

			outputStream.close();
		} catch (IOException e) {
			System.out.println("Erro ao gravar os chunks restaurados");
		}
	}

	Callable<Boolean> isRestored = () -> {
		String hashmapKey = this.actualChunk + "_" + this.fileID;
		boolean restoredDone = false;

		if (this.peer.get_restored_chunks().get(hashmapKey) != null) {
			restoredDone = true;
		}

		return restoredDone;
	};

	private byte[] makeGetChunkMessage(String fileID, int chunkNo) {
		String message = "GETCHUNK " + this.peer.get_protocol_version() + " " + this.peer.get_ID() + " " + fileID + " "
				+ chunkNo + " ";
		message = message + Protocol_handler.bi_CRLF;

		return message.getBytes();
	}

}
