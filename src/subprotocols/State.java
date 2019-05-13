package subprotocols;

import peer.Peer;

public class State {
	
	private Peer peer;
	private String state;
	
	public State(Peer peer) {
		this.peer = peer;
		this.state = "";
		
		getInfoStateBackup();
		getInfoStateChunks();
		getInfoStateDisk();
	}
	
	private void getInfoStateBackup() {
		this.state += "\n--- Files whose backup has initiated ---";
		
		if(this.peer.get_manager().get_files_ids().size() == 0) {
			this.state += "\nNo backup has been started.\n";
		} else {
			this.peer.get_manager().get_files_ids().forEach( (filename, fileid) -> {
				this.state += "\n- File Path: " + Peer.PEERS_FOLDER + Peer.DISK_FOLDER + peer.get_ID() + "/" + Peer.FILES_FOLDER + filename;
				this.state += "\n- File ID: "+ fileid + " - Desired Replication Degree: " + this.peer.get_manager().get_degrees().get("0_"+fileid);
				
				if(this.peer.get_manager().get_backup_state().get(fileid) == false) {
					this.state += "\nThis file started a backup, but this backup was later deleted.";
				} else {
					this.state += "\nChunks of the file:";
					
					this.peer.get_manager().get_current_degrees().forEach( (key, perseived) -> {
						if(key.endsWith(fileid)) {
							String [] info = key.split("_");
							this.state += "\nChunkNo: "+info[0] + " - Perceived Replication Degree: "+perseived;
						}
					});
				}
				
				this.state += "\n";
			});
		}
	}
	
	private void getInfoStateChunks() {
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
	
	private void getInfoStateDisk() {
		this.state += "\n--- Disk Info ---";
		this.state += "\n- Disk Space: "+this.peer.get_manager().get_max_space() / 1000 + " KBytes";
		this.state += "\n- Disk Used: "+this.peer.get_manager().get_space_used() / 1000 + " KBytes";
		
		this.state += "\n";
	}
	
	public String getState() {
		return this.state;
	}
}
