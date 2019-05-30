package subprotocols;

import static peer.Peer.FILES_FOLDER;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import peer.Peer;
import utils.AES;
import utils.Chunk;
import utils.FileID;

/** classe do Backup */
public class Backup implements Runnable {
  private String file_ID;
  private String filename;
  private String filepath;
  private int replication_degree;
  private Peer peer;

  private final int CHUNK_MAX_SIZE = 64000;
  private final long FILE_MAX_SIZE = 64000000000L;

  /**
   * construtor do Backup
   *
   * @param file ficheiro
   * @param replication replicação
   * @param peer peer
   * @throws FileNotFoundException Excepção de ficheiro não encontrado
   * @throws IOException Excepção de entrada/saida
   */
  public Backup(
      String file, int replication, Peer peer) { // throws FileNotFoundException, IOException
    this.filepath =FILES_FOLDER + file;
    this.replication_degree = replication;
    this.peer = peer;
    this.filename = file;
    this.file_ID = new FileID(filepath).toString();
  }

  /** run do Backup */
  @Override
  public void run() {
    if (this.peer.get_manager().get_files_ids().containsValue(this.file_ID)
        && this.peer.get_manager().get_backup_state().get(this.file_ID) == true) {
      System.out.println("Já tinha sido feito backup deste ficheiro");
      return;
    }

    if (new File(this.filepath).length() >= FILE_MAX_SIZE) {
      System.out.println("Tamanho do ficheiro excede o limite definido.");
    }

    String old_file_ID = this.peer.get_manager().get_files_ids().get(this.filename);

    if (old_file_ID != null && !old_file_ID.equals(this.file_ID)) {
      this.peer.send_delete_request(this.filename);
    }

    try {
      split_file();
    } catch (IOException e) {
      System.out.println("Ficheiro não encontrado.");
    }
  }


	/**
   * Espera pelas threads do chunk
   *
   * @param scheduled_pool SchedulePool
   * @param thread_results lista dos resultados
   * @return verdadeiro caso tenham terminado ou falso
   */
  private boolean wait_backup_result(
      ScheduledExecutorService scheduled_pool, List<Future<Boolean>> thread_results) {

    boolean backup_done = false;
    for (Future<Boolean> result : thread_results) {
      try {
        if (result.get()) {
					backup_done = true;
        } else {
					backup_done = false;
					scheduled_pool.shutdownNow();
					break;
        }
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      }
    }

    return backup_done;
  }


	/**
	 * extrai cabeçalho
	 *
	 * @throws FileNotFoundException Excepção de ficheiro não encontrado
	 * @throws IOException Excepção de entrada/saída
	 */
	private void split_file() throws  IOException {
		byte[] buffer = new byte[CHUNK_MAX_SIZE];
		int chunk_no = 0;
		List<Future<Boolean>> thread_results = new ArrayList<>();
		ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(100);

		DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Vou comecar o backup: " + format.format(date));

		this.peer.get_manager().get_files_ids().put(this.filename, this.file_ID);
		this.peer.get_manager().get_backup_state().put(this.file_ID, false);

		File file = new File(this.filepath);

		boolean need_empty_chunk = false;

		if((file.length() % CHUNK_MAX_SIZE) == 0) {
			need_empty_chunk = true;
		}

		try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis)) {
			chunk_no = read_loop(buffer, chunk_no, thread_results, scheduled_pool, bis);
			add_empty_chunk(chunk_no, thread_results, scheduled_pool, need_empty_chunk);
		}

		boolean backup_done = wait_backup_result(scheduled_pool, thread_results);

		handle_backup_done(format, backup_done);

		this.peer.get_manager().save_metadata();
	}


	/**
	 * Cria o último chunk de 0bytes
	 * @param chunk_no numero do chunk
	 * @param thread_results lista de resultados
	 * @param scheduled_pool SchedulePool
	 * @param need_chunk_zero se é necessário ser criado
	 */
	private void add_empty_chunk(int chunk_no, List<Future<Boolean>> thread_results,
			ScheduledExecutorService scheduled_pool, boolean need_chunk_zero) {

		if(need_chunk_zero) {
			byte[] empty = new byte[0];

			Future<Boolean> result = scheduled_pool
					.submit(new Chunk(this.file_ID, chunk_no, empty, this.replication_degree, this.peer));
			thread_results.add(result);
			this.peer.get_manager().get_degrees().put(chunk_no + "_" + this.file_ID, this.replication_degree);
			chunk_no++;
		}
	}


	/**
	 * Notifica se o backup foi feito
	 * @param format formato da data
	 * @param backup_done se backup foi feito
	 */
	private void handle_backup_done(DateFormat format, boolean backup_done) {
		if(backup_done) {
			Date date2 = new Date();
			System.out.println("Backup completed. " + format.format(date2));
			this.peer.get_manager().get_backup_state().replace(file_ID, true);
		} else {
			Date date2 = new Date();
			System.out.println("Backup was not completed. " + format.format(date2));
		}
	}

	/**
	 * loop de leitura
	 * @param buffer buffer de entrada
	 * @param chunk_no numero do chunk
	 * @param thread_results lista dos resultados
	 * @param scheduled_pool SchedulePool
	 * @param bis buffer input stream
	 * @return numero do chunk
	 * @throws IOException excepção de entrada/saída
	 */
	private int read_loop(byte[] buffer, int chunk_no, List<Future<Boolean>> thread_results,
			ScheduledExecutorService scheduled_pool, BufferedInputStream bis) throws IOException {
		int size;
		while ((size = bis.read(buffer)) > 0) {
			byte[] content = new byte[size];
			System.arraycopy(buffer, 0, content, 0, size);
			byte[] content_encrypted = encrypt_chunk(content);

			Future<Boolean> result = scheduled_pool
					.submit(new Chunk(this.file_ID, chunk_no, content_encrypted, this.replication_degree, this.peer));
			thread_results.add(result);

			this.peer.get_manager().get_degrees().put(chunk_no + "_" + this.file_ID, this.replication_degree);
			chunk_no++;
		}
		return chunk_no;
	}


	/**
	 * Encripta o chunk
	 * @param msg texto a ser encriptado
	 * @return
	 */
	private byte[] encrypt_chunk(byte[] msg) {
		String secret_key = "peer" + this.peer.get_ID();
		AES AES = new AES();
		return AES.encrypt(msg, secret_key);
	}
}
