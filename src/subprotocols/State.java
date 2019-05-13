package subprotocols;

import peer.Peer;

/**
 * classe State
 */
public class State {
	
	private Peer peer;
	private String state;

	/**
	 * construtor do state
	 * @param peer peer
	 */
	public State(Peer peer) {
		this.peer = peer;
		this.state = "";
		
		get_info_state_backup();
		get_info_state_chunks();
		get_info_state_disk();
	}

	/**
	 * Obtem informação so estado backup
	 */
	private void get_info_state_backup() {
		this.state += "\n--- Files whose backup has initiated ---";
		
		if(this.peer.get_manager().get_files_ids().size() == 0) {
			this.state += "\nNo backup has been started.\n";
		} else {
			this.peer.get_manager().get_files_ids().forEach( (filename, fileid) -> {
				create_oupput(filename, fileid);
			});
		}
	}

	/**
	 * cria o output
	 * @param filename nome do ficheiro
	 * @param file_id identificador do ficheiro
	 */
	private void create_oupput(String filename, String file_id) {
		this.state += "\n- File Path: " + Peer.PEERS_FOLDER + Peer.DISK_FOLDER + peer.get_ID() + "/" + Peer.FILES_FOLDER + filename;
		this.state += "\n- File ID: "+ file_id + " - Desired Replication Degree: " + this.peer.get_manager().get_degrees().get("0_"+ file_id);

		if(this.peer.get_manager().get_backup_state().get(file_id) == false) {
			this.state += "\nThis file started a backup, but this backup was later deleted.";
		} else {
			add_chunks_of_the_file(file_id);
		}

		this.state += "\n";
	}

	/**
	 * Acrescenta ao output os chuunks do ficheiro
	 * @param file_id identificador do ficheiro
	 */
	private void add_chunks_of_the_file(String file_id) {
		this.state += "\nChunks of the file:";

		this.peer.get_manager().get_current_degrees().forEach( (key, perseived) -> {
			if(key.endsWith(file_id)) {
				String [] info = key.split("_");
				this.state += "\nChunkNo: "+info[0] + " - Perceived Replication Degree: "+perseived;
			}
		});
	}

	/**
	 * obtem info do estado chunks
	 */
	private void get_info_state_chunks() {
		this.state += "\n--- Stored Chunks ---";
		
		if(this.peer.get_manager().get_chunks_stored_size().size() == 0) {
			this.state += "\nNo chunks stored.";
		} else {
			this.peer.get_manager().get_chunks_stored_size().forEach( (key, size) -> {
				this.peer.get_manager().get_current_degrees().forEach( (key2, perseived) -> {
					if(key2.equals(key)) {
						this.state += "\n- Chunk ID: "+ key + "\n- Size: "+ size/1000 + " KBytes" + " - Perceived Replication Degree: "+perseived;
					}
				});
			});
		}
		
		this.state += "\n";
	}

	/**
	 * Obtem info do estado do disco
	 */
	private void get_info_state_disk() {
		this.state += "\n--- Disk Info ---";
		this.state += "\n- Disk Space: "+this.peer.get_manager().get_max_space() / 1000 + " KBytes";
		this.state += "\n- Disk Used: "+this.peer.get_manager().get_space_used() / 1000 + " KBytes";
		
		this.state += "\n";
	}

	/**
	 * Obtem estado
	 * @return estado
	 */
	public String get_state() {
		return this.state;
	}
}
