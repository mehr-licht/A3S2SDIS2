package subprotocols;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import peer.Peer.channel_type;
import utils.Protocol_handler;
import peer.Peer;

public class Reclaim implements Runnable {
	
	private long spaceReclaim;
	private Peer peer;
	private ArrayList<String> chunksDeleted;
	private long diskUsed;
	private String chunksPath;
	
	public Reclaim(long kbytes, Peer peer) {
		this.spaceReclaim = kbytes * 1000;
		this.peer = peer;
		this.diskUsed = peer.get_manager().get_space_used();
		this.chunksPath = Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer.get_ID() + "/" + Peer.CHUNKS_FOLDER;
		this.chunksDeleted = new ArrayList<String>();
	}

	@Override
	public void run() {
		getChunksToRemove();
		sendRemoveMessages();
		
		System.out.println("Reclaim finished. Disk usage updated to: " +this.peer.get_manager().get_space_used());
	}

	private void getChunksToRemove() {
		while(this.diskUsed > spaceReclaim) {			
			File chunksFolder = new File(this.chunksPath);
			File [] chunksList = chunksFolder.listFiles();
			
			//No more chunks to delete
			if(chunksList == null) {
				System.out.println("No more chunks to erase.");
				return;
			}
			
			//Remove the first chunk of the list
			File chunkToDelete = chunksList[0];
			
			//Update run-time memory
			String chunkName = chunkToDelete.getName();
			chunksDeleted.add(chunkName);
			this.peer.get_manager().remove_chunk_info(chunkName, this.peer.get_ID());
			this.peer.get_manager().get_chunks_stored_size().remove(chunkName);
			this.diskUsed = this.diskUsed - chunkToDelete.length();	
			
			//Delete file from disk
			chunkToDelete.delete();		
		}
		
		this.peer.get_manager().set_space_used(this.diskUsed);
		this.peer.get_manager().set_max_space(this.spaceReclaim);
		this.peer.get_manager().save_metadata();
	}
	
	private void sendRemoveMessages() {
		for(String key : chunksDeleted) {
			byte [] packet = makeRemoveMessage(key);
			try {
				this.peer.send_reply_to_peers(channel_type.MC, packet);
			} catch (IOException e) {
				System.out.println("Error sending removed message.");
			}
		}
	}

	private byte[] makeRemoveMessage(String key) {
		String [] fileInfo = key.split("_");
		
		String message = "REMOVED" + " " + this.peer.get_protocol_version() + " " + this.peer.get_ID() + " " + fileInfo[1]
				+ " " + fileInfo[0] + " ";
		message = message + Protocol_handler.bi_CRLF;
		
		return message.getBytes();
	}

}
