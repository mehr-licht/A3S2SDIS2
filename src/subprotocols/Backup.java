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
      splitFile();
    } catch (IOException e) {
      System.out.println("Ficheiro não encontrado.");
    }
  }

  /**
   * extrai cabeçalho
   *
   * @throws FileNotFoundException Excepção de ficheiro não encontrado
   * @throws IOException Excepção de entrada/saída
   */
  /*private void split_file() throws FileNotFoundException, IOException {
    byte[] buffer = new byte[CHUNK_MAX_SIZE];
    int chunk_no = 0;
    List<Future<Boolean>> thread_results = new ArrayList<>();
    ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(100);

    DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    System.out.println("Vou comecar o backup: " + format.format(date));

    this.peer.get_manager().get_files_ids().put(this.filename, this.file_ID);
    this.peer.get_manager().get_backup_state().put(this.file_ID, false);
    //this.peer.server_connection();//AQUI socket= e no outro lado devolve socket // depois socket.setEnabled e envia para metodos filhos até mensagem

    File file = new File(this.filepath);

    boolean need_chunk_zero = false;

    if ((file.length() % CHUNK_MAX_SIZE) == 0) {
      need_chunk_zero = true;
    }

		read_chunks(buffer, chunk_no, thread_results, scheduled_pool, file, need_chunk_zero);

		boolean backup_done = wait_backup_result(scheduled_pool, thread_results);

		print_backup_done(format, backup_done);

		this.peer.get_manager().save_metadata();
  }*/

	/**
	 * Notifica se o backup foi feito
	 * @param format formato da data
	 * @param backup_done se backup foi feito
	 */
	/*private void print_backup_done(DateFormat format, boolean backup_done) {
		Date date2 = new Date();
		if (backup_done) {
			System.out.println("BackupUtil completed. " + format.format(date2));
			this.peer.get_manager().get_backup_state().replace(file_ID, true);
		} else {
			System.out.println("BackupUtil was not completed. " + format.format(date2));
		}
	}*/

	/**
	 * Lê chunks
	 * @param buffer buffer de entrada
	 * @param chunk_no numero do chunk
	 * @param thread_results
	 * @param scheduled_pool SchedulePool
	 * @param file ficheiro
	 * @param need_chunk_zero
	 * @throws IOException Excepção de entrada/saida
	 */
	/*private void read_chunks(byte[] buffer, int chunk_no, List<Future<Boolean>> thread_results,
			ScheduledExecutorService scheduled_pool, File file, boolean need_chunk_zero)
			throws IOException {
		try (FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis)) {

			chunk_no = read_loop(buffer, chunk_no, thread_results, scheduled_pool, bis);

			if (need_chunk_zero) {
				make_chunk(chunk_no, thread_results, scheduled_pool);
			}
		}
	}*/

	/**
	 * Cria o chunk
	 * @param chunk_no numero do chunk
	 * @param thread_results lista de resultados
	 * @param scheduled_pool SchedulePool
	 */
	/*private void make_chunk(int chunk_no, List<Future<Boolean>> thread_results,
			ScheduledExecutorService scheduled_pool) {
		byte[] empty = new byte[0];

		Future<Boolean> result =
				scheduled_pool.submit(
						new Chunk(this.file_ID, chunk_no, empty, this.replication_degree, this.peer));
		thread_results.add(result);
		this.peer
				.get_manager()
				.get_degrees()
				.put(chunk_no + "_" + this.file_ID, this.replication_degree);
		chunk_no++;
	}*/

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
	/*private int read_loop(byte[] buffer, int chunk_no, List<Future<Boolean>> thread_results,
			ScheduledExecutorService scheduled_pool, BufferedInputStream bis) throws IOException {
		int size;
		while ((size = bis.read(buffer)) > 0) {
			byte[] content = new byte[size];
			System.arraycopy(buffer, 0, content, 0, size);

			String secret_key = "peer" + this.peer.get_ID();
			AES AES = new AES();
			byte[] content_encrypted = AES.encrypt(content, secret_key);



			Future<Boolean> result =
					scheduled_pool.submit(
							new Chunk(
									this.file_ID, chunk_no, content_encrypted, this.replication_degree, this.peer));
			thread_results.add(result);

			this.peer
					.get_manager()
					.get_degrees()
					.put(chunk_no + "_" + this.file_ID, this.replication_degree);
			chunk_no++;
		}
		return chunk_no;
	}
*/
	/**
   * Espera pelas threads do chunk
   *
   * @param scheduled_pool SchedulePool
   * @param thread_results lista dos resultados
   * @return verdadeiro caso tenham terminado ou falso
   */
  private boolean wait_backup_result(
      ScheduledExecutorService scheduled_pool, List<Future<Boolean>> thread_results) {
  	System.out.println(thread_results.size() );
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



	private void splitFile() throws FileNotFoundException, IOException {
		byte[] buffer = new byte[CHUNK_MAX_SIZE];
		int chunkNr = 0;
		List<Future<Boolean>> threadResults = new ArrayList<>();
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(100);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Vou comecar o backup: " + dateFormat.format(date));

		this.peer.get_manager().get_files_ids().put(this.filename, this.file_ID);
		this.peer.get_manager().get_backup_state().put(this.file_ID, false);

		File file = new File(this.filepath);

		boolean needChunkZero = false;

		if((file.length() % CHUNK_MAX_SIZE) == 0) {
			needChunkZero = true;
		}

		try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis)) {
			int size = 0;

			chunkNr = while1(buffer, chunkNr, threadResults, scheduledPool, bis);

			//Add last chunk with zero length
			if(needChunkZero) {
				byte[] empty = new byte[0];

				Future<Boolean> result = scheduledPool.submit(new Chunk(this.file_ID, chunkNr, empty, this.replication_degree, this.peer));
				threadResults.add(result);
				this.peer.get_manager().get_degrees().put(chunkNr + "_" + this.file_ID, this.replication_degree);
				chunkNr++;
			}
		}

		boolean backupDone = wait_backup_result(scheduledPool, threadResults);

		if(backupDone) {
			Date date2 = new Date();
			System.out.println("Backup completed. " + dateFormat.format(date2));
			this.peer.get_manager().get_backup_state().replace(file_ID, true);
		} else {
			Date date2 = new Date();
			System.out.println("Backup was not completed. " + dateFormat.format(date2));
		}

		this.peer.get_manager().save_metadata();
	}

	private int while1(byte[] buffer, int chunkNr, List<Future<Boolean>> threadResults,
			ScheduledExecutorService scheduledPool, BufferedInputStream bis) throws IOException {
		int size;
		while ((size = bis.read(buffer)) > 0) {
			byte[] content = new byte[size];
			System.arraycopy(buffer, 0, content, 0, size);


			//Chunk encryption
			String secretKey = "peer" + this.peer.get_ID();
			AES AES = new AES();
			byte[] contentEncrypted = AES.encrypt(content, secretKey) ;



			Future<Boolean> result = scheduledPool.submit(new Chunk(this.file_ID, chunkNr, contentEncrypted, this.replication_degree, this.peer));
			threadResults.add(result);

			this.peer.get_manager().get_degrees().put(chunkNr + "_" + this.file_ID, this.replication_degree);
			chunkNr++;
		}
		return chunkNr;
	}
}
