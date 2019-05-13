package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import peer.Peer.channel_type;
import peer.Peer;

/** classe Chunk */
public class Chunk implements Callable<Boolean> {

  private String file_ID;
  private int chunk_no;
  private byte[] data;
  private int replication;
  private Peer peer;

  /**
   * Construtor da class Chunk
   *
   * @param file_ID
   * @param chunk_No
   * @param data
   * @param replic
   * @param peer
   */
  public Chunk(String file_ID, int chunk_No, byte[] data, int replic, Peer peer) {
    this.file_ID = file_ID;
    this.chunk_no = chunk_No;
    this.data = data;
    this.replication = replic;
    this.peer = peer;
  }

  /** @return */
  @Override
  public Boolean call() {
    byte[] packet = generate_putchunk();
    try {
      this.peer.send_reply_to_peers(channel_type.MDB, packet);
    } catch (IOException e1) {
      System.out.println("Erro ao enviar PUTCHUNK");
    }

    boolean result = false;

    try {
      result = is_stored();
    } catch (InterruptedException e) {
    } catch (ExecutionException e) {
    }

    return result;
  }

  /**
   * Vai buscar o chunk
   *
   * @param peer_ID identificação do peer
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @return tamanho do ficheiro se bem sucedido
   */
  public static byte[] get_chunk(int peer_ID, String file_ID, String chunk_No) {
    File file = get_file_path(peer_ID, file_ID, chunk_No);

    byte[] bytesize = new byte[(int) file.length()];

    read_input_stream(file, bytesize);

    return bytesize;
  }

  /**
   * Concatena o caminho do ficheiro
   *
   * @param peer_ID identificação do peer
   * @param file_ID identificação do ficheiro
   * @param chunk_No numero do chunk
   * @return string concatenada com o caminho do ficheiro
   */
  private static File get_file_path(int peer_ID, String file_ID, String chunk_No) {
    return new File(
        Peer.PEERS_FOLDER
            + Peer.DISK_FOLDER
            + peer_ID
            + "/"
            + Peer.CHUNKS_FOLDER
            + chunk_No
            + "_"
            + file_ID);
  }

  /**
   * Lê do stream de entrada
   *
   * @param file path do ficheiro
   * @param bytesize numero de bytes a ler
   */
  private static void read_input_stream(File file, byte[] bytesize) {
    try {
      FileInputStream stream = new FileInputStream(file);
      stream.read(bytesize);
      stream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Verifica se já foi guardado e continua a tentar caso ainda não
   *
   * @return verdadeiro ou falso
   * @throws InterruptedException Excepção de interrupção
   * @throws ExecutionException Excepção de Execução
   */
  private boolean is_stored() throws InterruptedException, ExecutionException {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    boolean veredict = false;
    int attempts = 0;
    int interval = 1;

    veredict = retry_loop(executor, veredict, attempts, interval);

    executor.shutdownNow();
    return veredict;
  }

  /**
   * Loop para continuar a tentar fazer o store
   *
   * @param executor
   * @param veredict
   * @param attempts
   * @param interval
   * @return Verdadeiro de conseguiu fazer o store ou falso caso contrário
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private boolean retry_loop(
      ScheduledExecutorService executor, boolean veredict, int attempts, int interval)
      throws InterruptedException, ExecutionException {
    while (veredict == false && attempts < 5) {
      Future<Boolean> future = executor.schedule(is_degree, interval, TimeUnit.SECONDS);

      veredict = future.get();

      // If the desired replication degree is not fulfilled, the time interval doubles
      if (!veredict) {
        interval = interval * 2;
        attempts++;
      }
    }
    return veredict;
  }

  /**
   * Verifica se o chunk já atingiu o nivel de replicação e envia novamente o PUTCHUNK caso ainda
   * não
   */
  Callable<Boolean> is_degree =
      () -> {
        String hashmap_key = this.chunk_no + "_" + this.file_ID;

        Boolean backed_up = is_backed_up(hashmap_key);

        if (!backed_up) resend_PUTCHUNK();

        return backed_up;
      };

  /**
   * reenvia o PUTCHUNK
   *
   * @throws IOException Excepção de I/O
   */
  private void resend_PUTCHUNK() throws IOException {
    byte[] packet = generate_putchunk();
    this.peer.send_reply_to_peers(channel_type.MDB, packet);
  }

  /**
   * Verifica se já foi guardado
   *
   * @param hashmap_key chave da hashmap
   * @return verdadeiro ou falso
   */
  private boolean is_backed_up(String hashmap_key) {
    if (this.peer.get_manager().get_chunks_hosts().get(hashmap_key) != null) {
      int current_degree = this.peer.get_manager().get_current_degrees().get(hashmap_key);
      if (!(current_degree < this.replication)) return true;
    }
    return false;
  }

  /**
   * Cria o datagrama de putchunk
   *
   * @return o datagrama de putchunk a enviar
   */
  private byte[] generate_putchunk() {
    byte[] header = create_header();
    byte[] datagram = new byte[header.length + this.data.length];
    System.arraycopy(header, 0, datagram, 0, header.length);
    System.arraycopy(this.data, 0, datagram, header.length, this.data.length);
    return datagram;
  }

  /**
   * Cria o header do datagrama de putchunk
   *
   * @return header do datagrama de putchunk
   */
  private byte[] create_header() {
    String msg = "PUTCHUNK ";
    msg += this.peer.get_protocol_version() + " ";
    msg += this.peer.get_ID() + " ";
    msg += this.file_ID + " ";
    msg += this.chunk_no + " ";
    msg += this.replication + " ";
    msg += Protocol_handler.bi_CRLF;
    return msg.getBytes();
  }
}
