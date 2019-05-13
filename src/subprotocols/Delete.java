package subprotocols;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;

import peer.Peer;

public class Delete implements Runnable {
	
	private String fileID;
	private Peer peer;
	
	public Delete(String file, Peer peer) {
		this.fileID = file;
		this.peer = peer;
	}
	
	@Override
	public void run() {
		File[] chunks = searchChunks(this.fileID);
		
		if(chunks != null) {
			deleteChunks(chunks);
		}
		
		this.peer.get_manager().save_metadata();
	}

	private void deleteChunks(File[] chunks) {
    	for(File file : chunks) {
    		try {
    			String filename = file.getName();
				Files.delete(file.toPath());
				
				//Update memory info
				this.peer.get_manager().remove_chunk_info(filename, this.peer.get_ID());
				int size = this.peer.get_manager().get_chunks_stored_size().get(filename);
				this.peer.get_manager().get_chunks_stored_size().remove(filename);
				this.peer.get_manager().set_space_used(this.peer.get_manager().get_space_used() - size);
			} catch (IOException e) {
				System.out.println("Error deleting chunk file.");
			}
    	}
	}

	private File[] searchChunks(String fileID) {
		File chunksDirectory = new File(Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer.get_ID() + "/" + Peer.CHUNKS_FOLDER);

    	File[] matches = chunksDirectory.listFiles(new FilenameFilter()
    	{
    	  public boolean accept(File chunksDirectory, String name)
    	  {
    	     return name.endsWith(fileID);
    	  }
    	});
    	
    	return matches;
	}

}
