package subprotocols;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import peer.Peer;
import peer.Peer.channel_type;
import utils.Protocol_handler;

/**
 * classe Reclaim
 */
public class Reclaim implements Runnable {

  private long space_reclaim;
  private Peer peer;
  private ArrayList<String> chunks_deleted;
  private long disk_used;
  private String chunks_path;

  /**
	 * construtor reclaim
   * @param kbytes espaço que ser quer
   * @param peer peer
   */
  public Reclaim(long kbytes, Peer peer) {
    this.space_reclaim = kbytes * 1000;
    this.peer = peer;
    this.disk_used = peer.get_manager().get_space_used();
    this.chunks_path =
        Peer.FILESYSTEM_FOLDER  + "Peer"+this.peer.get_ID() + "/" + Peer.CHUNKS_FOLDER;
    this.chunks_deleted = new ArrayList<>();
  }

  /** run do Reclaim */
  @Override
  public void run() {
    get_chunks_to_remove();
    send_remove_messages();

    System.out.println(
        "Reclaim terminou. Info de armazenamento actualizado para: "
            + this.peer.get_manager().get_space_used());
  }

  /** Obtem chunks para apagar */
  private void get_chunks_to_remove() {
    while (this.disk_used > space_reclaim) {
      File chunks_folder = new File(this.chunks_path);
      File[] chunks_list = chunks_folder.listFiles();

      if (chunks_list == null) {
        System.out.println("Não há mais nenhum chunk para apagar.");
        return;
      }

      File chunkToDelete = chunks_list[0];

      String chunkName = chunkToDelete.getName();
      chunks_deleted.add(chunkName);
      this.peer.get_manager().remove_chunk_info(chunkName, this.peer.get_ID());
      this.peer.get_manager().get_chunks_stored_size().remove(chunkName);
      this.disk_used = this.disk_used - chunkToDelete.length();

      chunkToDelete.delete();
    }

    this.peer.get_manager().set_space_used(this.disk_used);
    this.peer.get_manager().set_max_space(this.space_reclaim);
    this.peer.get_manager().save_metadata();
  }

  /** Envia mensagem remove*/
  private void send_remove_messages() {
    for (String key : chunks_deleted) {
      byte[] packet = make_remove_message(key);
      try {
        this.peer.send_reply_to_peers(channel_type.MC, packet);
      } catch (IOException e) {
        System.out.println("Erro ao enviar messgem.");
      }
    }
  }

  /**
	 * Cria mensagem remove
   * @param key chave
   * @return mensagem pronta
   */
  private byte[] make_remove_message(String key) {
    String[] fileInfo = key.split("_");

    String message =
        "REMOVED"
            + " "
            + this.peer.get_protocol_version()
            + " "
            + this.peer.get_ID()
            + " "
            + fileInfo[1]
            + " "
            + fileInfo[0]
            + " ";
    message = message + Protocol_handler.bi_CRLF;

    return message.getBytes();
  }
}
