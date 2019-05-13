package utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import peer.Peer;


/** classe do sub-protocolo de BACKUP */
public class BackupUtil implements Runnable {

  private static final int BACKUP_INTERVAL = 30; // seconds
  private Peer peer;

  /**
   * construtor da classe do sub-protocolo de BACKUP
   *
   * @param peer
   */
  public BackupUtil(Peer peer) {
    this.peer = peer;
  }

  /** Run da classe do sub-protocolo de BACKUP */
  @Override
  public void run() {
    while (true) {
      // Schedule task to send metadata to server
      ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
      Future<Boolean> future =
          scheduledPool.schedule(send_Metadata, BACKUP_INTERVAL, TimeUnit.SECONDS);
      try {
        future.get();
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      }
    }
  }

  /**
   * Envia metadata para o Server
   * */
  Callable<Boolean> send_Metadata =
      () -> {
        File file = get_file_path();

        if (file.exists()) {
          try {

            byte[] bytes = new byte[(int) file.length()];
            FileInputStream stream = new FileInputStream(file);
            BufferedInputStream buffer = new BufferedInputStream(stream);
            buffer.read(bytes, 0, bytes.length);

            buffer.close();
            stream.close();

            DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            System.out.println(
                "A enviar metadata para o servidor ("
                    + bytes.length
                    + " bytes): "
                    + format.format(date));

            peer.get_server_channel().send_message("SAVE_METADATA");
            peer.get_server_channel().send_bytes(bytes);

            return true;
          } catch (IOException e) {
            System.out.println("Erro ao enviar metadata para o servidor");
            return false;
          }
        }
        return false;
      };

  /**
   * Concatena o caminho do ficheiro
   * @return string concatenada com o caminho do ficheiro
   */
	private File get_file_path() {
    return new File(Peer.PEERS_FOLDER + Peer.DISK_FOLDER + peer.get_ID() + "/" + Peer.METADATA_FILE);
  }
}
