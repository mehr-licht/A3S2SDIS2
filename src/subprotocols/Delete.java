package subprotocols;

import java.nio.file.Files;
import java.io.IOException;
import java.io.File;
import peer.Peer;

/**
 * Classe do subprotocolo delete
 */
public class Delete implements Runnable {
	
	private String file_ID;
	private Peer peer;

	/**
	 * construtos do subprotocolo delete
	 * @param file ficheiro
	 * @param peer peer
	 */
	public Delete(String file, Peer peer) {
		this.file_ID = file;
		this.peer = peer;
	}

	/**
	 * run do subprotocolo delete
	 */
	@Override
	public void run() {
		File[] chunks = search_chunks(this.file_ID);
		
		if(chunks != null) {
			delete_chunks(chunks);
		}
		
		this.peer.get_manager().save_metadata();
	}

	/**
	 * Apaga chunks
	 * @param chunks chunks
	 */
	private void delete_chunks(File[] chunks) {
    	for(File file : chunks) {
    		try {
    			String filename = file.getName();
				Files.delete(file.toPath());

					update_mem_info(filename);
				} catch (IOException e) {
				System.out.println("Erro ao apagar chunk");
			}
    	}
	}

	/**
	 * actualiza os dados da memoria usada
	 * @param filename
	 */
	private void update_mem_info(String filename) {
		this.peer.get_manager().remove_chunk_info(filename, this.peer.get_ID());
		int size = this.peer.get_manager().get_chunks_stored_size().get(filename);
		this.peer.get_manager().get_chunks_stored_size().remove(filename);
		this.peer.get_manager().set_space_used(this.peer.get_manager().get_space_used() - size);
	}

	/**
	 * Procura chunks
	 * @param file_ID identificador do ficheiro
	 * @return lista de encontrados
	 */
	private File[] search_chunks(String file_ID) {
		File chunks_dir = new File(Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer.get_ID() + "/" + Peer.CHUNKS_FOLDER);
    	File[] matches = chunks_dir.listFiles((File chunksDirectory, String name)->
    	  name.endsWith(file_ID));
    	return matches;
	}

}
