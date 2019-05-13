package utils;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import peer.Peer;
import peer.Peer.channel_type;
import subprotocols.Delete;

/** classe do Protocol_handler */
public class Protocol_handler implements Runnable {
  private Peer peer;
  private String[] header;
  private byte[] body;

  public static final byte CR = 0xD;
  public static final byte LF = 0xA;
  public static final String bi_CRLF = "" + (char) 0x0D + (char) 0x0A + (char) 0x0D + (char) 0x0A;

  /**
   * construtor do Protocol_handler
   *
   * @param packet packet
   * @param peer peer
   */
  public Protocol_handler(DatagramPacket packet, Peer peer) {
    this.peer = peer;
    split_message(packet);
  }

  /**
   * Extrai cabeçalho
   *
   * @param packet packet
   */
  public void split_message(DatagramPacket packet) {

    byte[] message = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), packet.getOffset(), message, 0, packet.getLength());

    ByteArrayInputStream input = new ByteArrayInputStream(message);

    byte character = 0;
    String header = read_each_char(input, character);

    character = (byte) input.read();
    if (character != LF) {
      System.out.println("ERRO: cabeçalho incorrecto");
      return;
    } else {
      header += (char) character;
    }

    this.body = Arrays.copyOfRange(message, header.length() + 2, message.length);
    this.header = header.trim().split(" ");
  }

  /**
   * Lê os caracteres
   *
   * @param input input
   * @param character caracter
   * @return string lida
   */
  private String read_each_char(ByteArrayInputStream input, byte character) {
    String result = "";
    while (character != CR && character != -1) {
      character = (byte) input.read();
      result += (char) character;
    }
    return result;
  }

  /** run do Protocol_handler */
  @Override
  public void run() {

    if (Integer.parseInt(header[2]) == this.peer.get_ID()) {
      return;
    }

    switch (header[0]) {
      case "PUTCHUNK":
        case_is_putchunk();
        break;

      case "STORED":
        case_is_stored();
        break;

      case "GETCHUNK":
        case_is_getchunk();
        break;

      case "CHUNK":
        case_is_chunk();
        break;

      case "DELETE":
        case_is_delete();
        break;

      case "REMOVED":
        case_is_removed();
        break;
    }
  }

  /**
   * trata de mensagem "REMOVED"
   */
  private void case_is_removed() {
    String hashmap_key;
    if (header.length != 5) {
      System.out.println("[" + header[0] + "]" + "cabeçalho inválido.");
      return;
    }

    hashmap_key = header[4] + "_" + header[3];

    if (this.peer.get_received_put_chunk_messages().contains(hashmap_key)) {
      this.peer.get_received_put_chunk_messages().remove(hashmap_key);
    }

    this.peer.get_manager().remove_chunk_info(hashmap_key, Integer.parseInt(header[2]));
    this.peer.get_manager().save_metadata();
    try_backup_until_replic(hashmap_key);
  }

  /**
   * Verifica se já guardei o chunk para ver o grau de replicação e tenta novamente se não atingido
   * @param hashmap_key chave da hashmap
   */
  private void try_backup_until_replic(String hashmap_key) {
    if (this.peer.get_manager().get_chunks_stored_size().get(hashmap_key) != null) {
      int actual_replication_degree =
          this.peer.get_manager().get_current_degrees().get(hashmap_key);
      int desired_replication_degree = this.peer.get_manager().get_degrees().get(hashmap_key);

      if (actual_replication_degree < desired_replication_degree) {
        backup_again(desired_replication_degree);
      }
    }
  }

  /**
   * trata de mensagem "DELETE"
   */
  private void case_is_delete() {
    if (header.length != 4) {
      System.out.println("[" + header[0] + "]" + "cabeçalho inválido.");
      return;
    }

    new Thread(new Delete(header[3], this.peer)).start();
  }

  /**
   * trata de mensagem "CHUNK"
   */
  private void case_is_chunk() {
    String hashmap_key;
    if (header.length != 5) {
      System.out.println("[" + header[0] + "]" + "cabeçalho inválido.");
      return;
    }

    hashmap_key = header[4] + "_" + header[3];

    if (!this.peer.get_received_chunk_messages().contains(hashmap_key)) {
      this.peer.get_received_chunk_messages().add(hashmap_key);
    }

    if (this.peer.get_wait_restored_chunks().contains(hashmap_key)) {
      this.peer.get_wait_restored_chunks().remove(hashmap_key);
      this.peer.get_restored_chunks().put(hashmap_key, this.body);
    }
  }

  /**
   * trata de mensagem "PUTCHUNK"
   */
  private void case_is_putchunk() {
    String hashmap_key;
    Random random;
    int wait_time;
    if (header.length != 6) {
      System.out.println("[" + header[0] + "]" + "cabeçalho inválido.");
      return;
    }

    hashmap_key = header[4] + "_" + header[3];

    if (!this.peer.get_received_put_chunk_messages().contains(hashmap_key)) {
      this.peer.get_received_put_chunk_messages().add(hashmap_key);
    }

    if (this.peer.get_manager().get_backup_state().get(header[3]) != null) {
      return;
    }

    this.peer.get_manager().get_degrees().put(hashmap_key, Integer.parseInt(header[5]));

    CopyOnWriteArrayList<Integer> chunk_hosts =
        peer.get_manager().get_chunks_hosts().get(hashmap_key);

    if (chunk_hosts != null && chunk_hosts.contains(this.peer.get_ID())) {
      return;
    }

    if (this.body.length + this.peer.get_manager().get_space_used()
        > this.peer.get_manager().get_max_space()) {
      return;
    }

    random = new Random();
    wait_time = random.nextInt(400);

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    executor.schedule(store_chunk, wait_time, TimeUnit.MILLISECONDS);
  }

  /**
   * trata de mensagem "GETCHUNK"
   */
  private void case_is_getchunk() {
    String hashmap_key;
    Random random;
    int wait_time;
    if (header.length != 5) {
      System.out.println("[" + header[0] + "]" + "cabeçalho inválido.");
      return;
    }

    hashmap_key = header[4] + "_" + header[3];

    if (this.peer.get_manager().get_chunks_hosts().get(hashmap_key) != null
        && !this.peer
            .get_manager()
            .get_chunks_hosts()
            .get(hashmap_key)
            .contains(this.peer.get_ID())) {
      return;
    }

    check_sent();
  }

  /**
   * verifica se chunk já foi enviado e notifica peers
   */
  private void check_sent() {
    Random random;
    int wait_time;
    random = new Random();
    wait_time = random.nextInt(400);

    ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(1);

    Future<Boolean> future =
        scheduled_pool.schedule(received_chunk_msg, wait_time, TimeUnit.MILLISECONDS);
    boolean chunk_already_sent = false;
    try {
      chunk_already_sent = future.get();
    } catch (InterruptedException e) {
    } catch (ExecutionException e) {
    }

    if (!chunk_already_sent) {
      byte[] packet = make_chunk_message(header[3], header[4]);
      try {
        this.peer.send_reply_to_peers(channel_type.MDR, packet);
      } catch (IOException e) {
        System.out.println("Erro ao enviar chunk");
      }
    }
  }

  /**
   * trata de mensagem "STORED"
   */
  private void case_is_stored() {
    if (header.length != 5) {
      System.out.println("[" + header[0] + "]" + "cabeçalho inválido.");
      return;
    }

    this.peer
        .get_manager()
        .store_chunk_info(
            Integer.parseInt(this.header[2]), this.header[3], Integer.parseInt(this.header[4]));
    this.peer.get_manager().save_metadata();
  }

  /**
   * Faz backup
   * @param replication grau de replicação
   * */
  private void backup_again(int replication) {
    Random random = new Random();
    int wait_time = random.nextInt(400);

    ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(1);

    Future<Boolean> future =
        scheduled_pool.schedule(received_putchunk_msg, wait_time, TimeUnit.MILLISECONDS);
    boolean chunk_backed_up = false;
    try {
      chunk_backed_up = future.get();
    } catch (InterruptedException e) {
    } catch (ExecutionException e) {
    }

    if (!chunk_backed_up) {
      prepare_reply_to_peers(replication);
    }
  }

  /**
   * tenta que mais peers guardem para atingir o grau de replicação
   * @param replication grau de replicação
   */
  private void prepare_reply_to_peers(int replication) {
    byte[] packet = make_putchunk_request(header[3], header[4], replication);
    try {
      this.peer.send_reply_to_peers(channel_type.MDR, packet);
    } catch (IOException e) {
      System.out.println("Error sending chunk message");
    }
  }

  /**
   * prepara datagrama de pedido de putchunk
   * @param file_ID identificação do ficheiro
   * @param chunk_no numero do chunk
   * @param replication_degree grau de replicação desejado
   * @return datagrama
   */
  private byte[] make_putchunk_request(String file_ID, String chunk_no, int replication_degree) {
    String message =
        "PUTCHUNK"
            + " "
            + this.peer.get_protocol_version()
            + " "
            + this.peer.get_ID()
            + " "
            + file_ID
            + " "
            + chunk_no
            + " "
            + replication_degree
            + " ";
    message = message + Protocol_handler.bi_CRLF;

    byte[] chunk = Chunk.get_chunk(peer.get_ID(), file_ID, chunk_no);

    byte[] header = message.getBytes();
    byte[] packet = new byte[header.length + chunk.length];
    System.arraycopy(header, 0, packet, 0, header.length);
    System.arraycopy(chunk, 0, packet, header.length, chunk.length);

    return packet;
  }

  /**
   * Prepara o datagram de resposta
   * @param file_ID identifdicação do ficheiro
   * @param chunk_no numero do chunk
   * @return bytes a enviar
   */
  private byte[] make_store_chunk_reply(String file_ID, String chunk_no) {
    String message =
        "STORED "
            + this.peer.get_protocol_version()
            + " "
            + this.peer.get_ID()
            + " "
            + file_ID
            + " "
            + chunk_no
            + " ";
    message = message + Protocol_handler.bi_CRLF;

    return message.getBytes();
  }

  /**
   * Prepara o datagrama do chunk
   * @param file_ID identificaçao do ficheiro
   * @param chunk_no numero do chunk
   * @return pacote a enviar
   */
  private byte[] make_chunk_message(String file_ID, String chunk_no) {
    byte[] chunk = Chunk.get_chunk(peer.get_ID(), file_ID, chunk_no);

    String message =
        "CHUNK "
            + this.peer.get_protocol_version()
            + " "
            + this.peer.get_ID()
            + " "
            + file_ID
            + " "
            + chunk_no
            + " ";
    message = message + Protocol_handler.bi_CRLF;

    byte[] header = message.getBytes();
    byte[] packet = new byte[header.length + chunk.length];
    System.arraycopy(header, 0, packet, 0, header.length);
    System.arraycopy(chunk, 0, packet, header.length, chunk.length);

    return packet;
  }

  /**
   * Guarda o chunk
   * */
  Runnable store_chunk =
      () -> {
        String filePath =
            Peer.PEERS_FOLDER
                + Peer.DISK_FOLDER
                + peer.get_ID()
                + "/"
                + Peer.CHUNKS_FOLDER
                + this.header[4]
                + "_"
                + this.header[3];

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
          fos.write(this.body);
        } catch (IOException e) {
          System.out.println("Erro ao guardar chunk");
        }

        save_chunks_info();

        this.peer
            .get_manager()
            .set_space_used(this.peer.get_manager().get_space_used() + this.body.length);

        // Save non volatile memory
        this.peer.get_manager().save_metadata();
        send_stored();
      };

  /**
   * envia mensagem STORED
   */
  private void send_stored() {
    byte[] packet = make_store_chunk_reply(this.header[3], this.header[4]);
    try {
      this.peer.send_reply_to_peers(channel_type.MC, packet);
    } catch (IOException e) {
      System.out.println("Erro ao enviar mensagem para o canal multicast");
    }
  }

  /**
   * Guarda a informação dos chunks
   */
  private void save_chunks_info() {
    this.peer
        .get_manager()
        .get_chunks_stored_size()
        .put(this.header[4] + "_" + this.header[3], this.body.length);
    this.peer
        .get_manager()
        .store_chunk_info(this.peer.get_ID(), this.header[3], Integer.parseInt(this.header[4]));
  }

  /**
   * Verifica se já recebeu a mensagem do chunk
   * @return verdadeiro ou falso
   * */
  Callable<Boolean> received_chunk_msg =
      () -> {
        boolean result = false;
        String key = this.header[4] + "_" + this.header[3];

        if (this.peer.get_received_chunk_messages().contains(key)) {
          result = true;
        }

        return result;
      };

  /**
   * Verifica se recebeu uma mensagem de putchunk
   * @ return verdadeiro ou falso
   * */
  Callable<Boolean> received_putchunk_msg =
      () -> {
        boolean result = false;
        String key = this.header[4] + "_" + this.header[3];

        if (this.peer.get_received_put_chunk_messages().contains(key)) {
          result = true;
        }

        return result;
      };
}
