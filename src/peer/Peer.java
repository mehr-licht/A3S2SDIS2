package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import utils.BackupUtil;
import utils.Manager;
import subprotocols.Reclaim;
import subprotocols.Restore;
import subprotocols.State;
import utils.My_Remote_Interface;
import utils.Protocol_handler;

/** classe Peer */
public class Peer implements My_Remote_Interface {

  /** portos de entrada no canais */
  public class PeerEndpoint {
    public String host;
    public int id;
    public int MC_port;
    public int MDB_port;
    public int MDR_port;
  }

  public enum channel_type {
    MC,
    MDB,
    MDR
  }

  /**
   * Guarda os chunks restaurados recebidos - <ChunkNo_FileID><File Bytes>
   */
  private ConcurrentHashMap<String, byte[]> restored_chunks;

  /**
   * Guarda os chunks que está à espera - <ChunkNo_FileID>
   */
  private CopyOnWriteArrayList<String> wait_restored_chunks;

  /**
   *  Guarda as mensagens de chunks recebidos - <ChunkNo_FileID>
   */
  private CopyOnWriteArrayList<String> received_chunk_messages;

  /**
   * Guarda as mensagens PUTCHUNK recebidas - <ChunkNo_FileID>
   */
  private CopyOnWriteArrayList<String> received_put_chunk_messages;

  private Manager data_manager;

  private volatile ArrayList<PeerEndpoint> endpoints;
  private volatile boolean collected_all_peers;

  /**
   * 0 não existe, 1 existe, -1 à espera de resposta
   */
  private volatile int metadata_server;

  public static final String PEERS_FOLDER = "Peers/";
  public static final String DISK_FOLDER = "DiskPeer";

  public static final String FILES_FOLDER = "MyFiles/";
  public static final String RESTORED_FOLDER = "RestoredFiles/";
  public static final String CHUNKS_FOLDER = "Chunks/";
  public static final String METADATA_FILE = "metadata.ser";

  private String host_IP;
  private SSLSocket socket;
  private DatagramSocket sender_socket;
  private PeerServerListener server_channel;

  private int peer_ID;
  private String protocol_version;

  private PeerChannel multicast_channel;
  private PeerChannel backup_channel;
  private PeerChannel restore_channel;

  /**
   * contrutor de peer
   *
   * @param protocol protocolo pedido
   * @param id id
   * @param host_IP ip do host
   * @throws IOException Excepção de entrada/saída
   * @throws InterruptedException Excepção de interrupção
   * @throws ExecutionException Excepção de execução
   */
  public Peer(String protocol, int id, String host_IP)
      throws IOException, InterruptedException, ExecutionException {
    this.protocol_version = protocol;
    this.peer_ID = id;
    this.host_IP = host_IP;

    String peer_disk = PEERS_FOLDER + DISK_FOLDER + id;
    String backup_files = peer_disk + "/" + FILES_FOLDER;
    String chunks_files = peer_disk + "/" + CHUNKS_FOLDER;
    String restored_file = peer_disk + "/" + RESTORED_FOLDER;

    make_directories(peer_disk, backup_files, chunks_files, restored_file);

    this.received_chunk_messages = new CopyOnWriteArrayList<>();
    this.received_put_chunk_messages = new CopyOnWriteArrayList<>();
    this.restored_chunks = new ConcurrentHashMap<>();
    this.wait_restored_chunks = new CopyOnWriteArrayList<>();

    multicast_channel = new PeerChannel(this);
    backup_channel = new PeerChannel(this);
    restore_channel = new PeerChannel(this);

    server_connection();

    //// criar threads para cada canal
    new Thread(multicast_channel).start();
    new Thread(backup_channel).start();
    new Thread(restore_channel).start();

    // para enviar mensagens para outros peers
    this.sender_socket = new DatagramSocket();

    get_metadata();
    new Thread(new BackupUtil(this)).start();
  }

  public static void main(String[] args)
      throws IOException, InterruptedException, ExecutionException {
    if (!valid_input(args)) usage();

    int server_ID = Integer.valueOf(args[0]);
    String host_IP = args[1];
    String rmi_address = "peer" + server_ID;
    Peer peer = new Peer("1.0", server_ID, host_IP);

    start_RMI(rmi_address, peer);
  }

  /**
   * Inicia a ligação rmi
   *
   * @param rmi_address morada rmi
   * @param peer peer
   */
  private static void start_RMI(String rmi_address, Remote peer) {
    try {
      My_Remote_Interface rmi = (My_Remote_Interface) UnicastRemoteObject.exportObject(peer, 0);
      Registry registry = LocateRegistry.getRegistry();
      registry.rebind(rmi_address, rmi);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  /** Imprime o usage correcto */
  public static void usage() {
    System.out.println("Numero de argumentos errado.");
    System.exit(1);
  }

  /**
   * Verifica se o numero de argumentos estão correctos
   *
   * @param args argumentos passados na linha de comandos
   * @return verdadeiro ou falso
   */
  public static boolean valid_input(String[] args) {
    if (args.length != 2) return false;
    return true;
  }

  /**
   * prepara os directorios
   *  @param peer_disk disco do peer
   * @param backup_files ficheiros de backup
   * @param chunks_files ficheiros dos chunks
   * @param restored_file ficheiro restaurado
   */
  private void make_directories(
      String peer_disk, String backup_files, String chunks_files, String restored_file) {
    make_directory(peer_disk);
    make_directory(backup_files);
    make_directory(chunks_files);
    make_directory(restored_file);
  }

  /**
   * Obtem a metadata
   *
   * @throws InterruptedException Excepção de Interrupção
   * @throws ExecutionException Excepção de Execução
   */
  public void get_metadata() { // throws InterruptedException, ExecutionException {
    File file =
        new File(Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer_ID + "/" + Peer.METADATA_FILE);

    if (file.exists()) {
      try {
        read_file();
      } catch (IOException | ClassNotFoundException e) {
        System.err.println("Erro ao carregar o ficheiro de metadata no peer.");
      }
    } else {
      ask_and_load();
    }
  }

  /**
   * lê do ficheiro de metadata
   *
   * @throws IOException Excepção de entrada/saída
   * @throws ClassNotFoundException Excepção de classe não encontrada
   */
  private void read_file() throws IOException, ClassNotFoundException {
    ObjectInputStream serverStream =
        new ObjectInputStream(
            new FileInputStream(
                Peer.PEERS_FOLDER + Peer.DISK_FOLDER + this.peer_ID + "/" + Peer.METADATA_FILE));
    data_manager = (Manager) serverStream.readObject();

    serverStream.close();
  }

  /** Pede metadata ao servidor, trata da resposta e saca a metadata */
  private void ask_and_load() {
    metadata_server = -1;
    server_channel.send_message("GET_METADATA");

    ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
    scheduledPool.schedule(load_metadata, 1000, TimeUnit.MILLISECONDS);
  }

  /** carrega os metadados */
  Runnable load_metadata =
      () -> {
        if (this.metadata_server == 1) {
          try {
            read_file();
          } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar o ficheiro de metadata no peer.");
          }
        } else {
          data_manager = new Manager(this.peer_ID); // Cria um Manager vazio
        }
      };

  /** Conexão a um servidor com ligação segura */
  public void server_connection() {

    set_key_truststore();

    SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
    int server_port = connect_to_port(sf);

    System.out.println("Conectado ao servidor com a porta: " + server_port);
    message_ready_from_master();
  }

  /**
   * Tenta conectar a um servidor
   *
   * @param sf SSL socket factory
   * @return porto ao qual se ligou
   */
  private int connect_to_port(SSLSocketFactory sf) {
    Random rand = new Random();
    int n = rand.nextInt(3);
    int server_port = 3000 + n;

    return connect_to_server(sf, n, server_port);
    // return server_port;
  }

  /**
   * Conecata-se a um servidor
   *
   * @param sf SSL socket factory
   * @param n numero random entre 0 e 2
   * @param server_port porto do servidor ao qual se tenta ligar
   * @return porto do servidor ao qual se conseguiu ligar
   */
  private int connect_to_server(SSLSocketFactory sf, int n, int server_port) {
    boolean connected = false;
    while (!connected) {
      try {
        socket = (SSLSocket) sf.createSocket(this.host_IP, server_port);
        connected = true;
      } catch (IOException e) {
        connected = false;
        server_port++;

        if (server_port == 3003) {
          server_port = 3000;
        }

        if (server_port == 3000 + n) {
          System.out.println("Não foi possível conectar a nenhum servidor");
          System.exit(-1);
        }
      }
    }
    return server_port;
  }

  /** Permite receber mensagens do peer master */
  private void message_ready_from_master() {
    this.server_channel = new PeerServerListener(this, socket);
    new Thread(server_channel).start();
    server_connection_notification();
  }

  /** Estabelece armazem de chaves e de certificados do cliente */
  private void set_key_truststore() {
    System.setProperty("javax.net.ssl.trustStore", "../SSL/truststore");
    System.setProperty("javax.net.ssl.trustStorePassword", "123456");
    System.setProperty("javax.net.ssl.keyStore", "../SSL/client.keys");
    System.setProperty("javax.net.ssl.keyStorePassword", "123456");
  }

  private void server_connection_notification() {
    String msg = "AUTHENTICATE ";

    msg += peer_ID + " ";
    msg += multicast_channel.get_socket_port() + " ";
    msg += backup_channel.get_socket_port() + " ";
    msg += restore_channel.get_socket_port();

    server_channel.send_message(msg);
  }


  /**
   * Envia a mensagem de DELETE para o canal multicast
   * @param filename nome do ficheiro
   */
  public void send_delete_request(String filename) {
    String file_ID = this.get_manager().get_files_ids().get(filename);

    if (file_ID != null) {
      String message = "DELETE " + this.protocol_version + " " + this.peer_ID + " " + file_ID + " ";
      message = message + Protocol_handler.bi_CRLF;

      send_to_multicast(message);

      update_metadata(file_ID);
      System.out.println("DELETE terminado.");
    } else {
      System.out.println("Erro ao apagar ficheiro: BackupUtil alheio.");
    }
  }

  /**
   * Actualiza os metadados
   *
   * @param file_ID identificador do ficheiro
   */
  private void update_metadata(String file_ID) {
    this.get_manager().get_backup_state().replace(file_ID, false);
    this.get_manager().remove_info_of_file(file_ID);
    this.get_manager().save_metadata();
  }

  /**
   * envia para o canal multicast
   *
   * @param message mensagem a enviar
   */
  private void send_to_multicast(String message) {
    try {
      send_reply_to_peers(channel_type.MC, message.getBytes());
    } catch (IOException e) {
      System.out.println("Erro ao enviar a mensagem de DELETE para o canal multicast.");
    }
  }

  /**
   * Prepara o directorio
   *
   * @param path caminho do ficheiro
   */
  private void make_directory(String path) {
    File file = new File(path);

    if (file.mkdirs()) {
      System.out.println("Directorio " + path + " criado.");
    }
  }

  /**
   * Envia resposta para os peers
   * @param type tipo de canal
   * @param packet pacote a enviar
   * @throws IOException Excepção de entrada/saída
   */
  public synchronized void send_reply_to_peers(channel_type type, byte[] packet) throws IOException {
    this.collected_all_peers = false;
    this.endpoints = new ArrayList<>();

    this.server_channel.send_message("GETPEERS");

    while (!this.collected_all_peers) {}

    loop_endpoints(type, packet);
  }

  /**
   * Percorre os endpoints para enviar
   * @param type tipo de canal
   * @param packet pacote a enviar
   * @throws IOException Excepção de entrada/saída
   */
  private void loop_endpoints(channel_type type, byte[] packet) throws IOException {
    for (PeerEndpoint peer : endpoints) {
      if (peer.id == peer_ID) { // Não enviar para si proprio
        continue;
      }

      InetAddress address = InetAddress.getByName(peer.host);

      int port = -1;
      switch (type) {
        case MC:
          port = peer.MC_port;
          break;
        case MDB:
          port = peer.MDB_port;
          break;
        case MDR:
          port = peer.MDR_port;
          break;
      }

      DatagramPacket send_packet = new DatagramPacket(packet, packet.length, address, port);
      sender_socket.send(send_packet);
    }
  }

  /**
   *  @param host
   * @param id
   * @param MC_port
   * @param MDB_port
   * @param MDR_port
   */
  public void add_endpoint(String host, int id, int MC_port, int MDB_port, int MDR_port) {
    PeerEndpoint peer = new PeerEndpoint();

    peer.host = host;
    peer.id = id;
    peer.MC_port = MC_port;
    peer.MDB_port = MDB_port;
    peer.MDR_port = MDR_port;

    endpoints.add(peer);
  }

  /**
   *
   * @return
   */
  public String get_peer_state() {
    return new State(this).get_state();
  }

   /**
   *
    * @param collected_all_peers
    */
  public void set_collected_peers(boolean collected_all_peers) {
    this.collected_all_peers = collected_all_peers;
  }

  /**
   *
   * @param response
   */
  public void set_metadata_response(int response) {
    this.metadata_server = response;
  }

  /**
   *
   * @return
   */
  public PeerServerListener get_server_channel() {
    return server_channel;
  }

  /**
   *
   * @return
   */
  public String get_protocol_version() {
    return protocol_version;
  }

  /**
   *
   * @return
   */
  public int get_ID() {
    return peer_ID;
  }

  /**
   *
   * @return
   */
  public Manager get_manager() {
    return data_manager;
  }

  /**
   *
   * @return
   */
  public ConcurrentHashMap<String, byte[]> get_restored_chunks() {
    return restored_chunks;
  }

  /**
   *
   * @return
   */
  public CopyOnWriteArrayList<String> get_wait_restored_chunks() {
    return wait_restored_chunks;
  }

  /**
   *
   * @return
   */
  public CopyOnWriteArrayList<String> get_received_chunk_messages() {
    return received_chunk_messages;
  }

  /**
   *
   * @return
   */
  public CopyOnWriteArrayList<String> get_received_put_chunk_messages() {
    return received_put_chunk_messages;
  }

  /**
   * @param filename ficheiro para ser feito o backup
   * @param replication_degree
   * @throws RemoteException
   */
  @Override
  public void backup(String filename, int replication_degree) {
    System.out.println("[SERVER " + this.peer_ID + "] Starting backup protocol...");
    try {
      new Thread(new subprotocols.Backup(filename, replication_degree, this)).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param filename ficheiro a eliminar
   * @throws RemoteException
   */
  @Override
  public void delete(String filename) {//throws RemoteException
    System.out.println("[SERVER " + this.peer_ID + "] Starting delete protocol...");
    send_delete_request(filename);
  }

  /**
   * @param filename ficheiro a restaurar
   * @throws RemoteException
   */
  @Override
  public void restore(String filename) {//throws RemoteException
    System.out.println("[SERVER " + this.peer_ID + "] Starting restore protocol...");
    new Thread(new Restore(filename, this)).start();
  }

  /**
   * @return
   * @throws RemoteException
   */
  @Override
  public String state() {//throws RemoteException
    System.out.println("[SERVER " + this.peer_ID + "] Starting state feature...");
    System.out.println("State returned.");
    return this.get_peer_state();
  }

  /**
   * @param space Espaço total que se quer (kbytes)
   * @throws RemoteException
   */
  @Override
  public void reclaim(int space)  {//throws RemoteException
    System.out.println("[SERVER " + this.peer_ID + "] Starting reclaim protocol...");
    System.out.println("Disk used: " + this.get_manager().get_space_used());
    new Thread(new Reclaim(space, this)).start();
  }
}
