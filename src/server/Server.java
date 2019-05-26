package server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import peer.Peer;

/** classe Server */
public class Server {
  private SSLServerSocket socket;
  private static ArrayList<Server_peer_listener> peers;
  private static ArrayList<ServerToServerChannel> other_servers;

  public static final String SERVER_FOLDER = "Server";
  public static final String METADATA_FILE = "db";
  public static final String PEER_FOLDER = "Peer";
  private static int server_ID;
  private int server_port;

  /**
   * main da classe Server
   *
   * @param args argumentos passados na linha de comandos
   */
  public static void main(String args[]) throws UnknownHostException{
    if (!usage(args)) System.exit(-1);

    int server_ID = Integer.valueOf(args[0]);
    int server_port = Integer.valueOf(args[1]);

    if (server_port < 2000 || server_port > 2002) {
      System.out.println("porto tem de estar compreendido entre 2000 e 2002");
      System.exit(-1);
    }

    peers = new ArrayList<>();
    new Server(server_ID, server_port);
  }

  /**
   * Verifica a chamada correcta na linha de comandos
   *
   * @param args argumentos passados na linha de comandos
   * @return verdadeiro ou falso
   */
  public static boolean usage(String[] args) {
    if (args.length != 2) {
      System.out.println("Numero inválido de argumentos");
      System.out.println("usage: [TODO]");
      return false;
    }
    return true;
  }

  /**
   * construtor da classe Server
   *
   * @param id identificador do servidor
   * @param port porto do servidor
   */
  public Server(int id, int port) throws UnknownHostException{
    server_ID = id;
    this.server_port = port;
    make_directory(Peer.FILESYSTEM_FOLDER+ Server.SERVER_FOLDER + server_ID);

		set_key_truststore();

		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		create_socket(port, ssf);

		socket.setNeedClientAuth(true);

    new Thread(new ServerChannel(socket)).start();

    InetAddress IP=InetAddress.getLocalHost();
    System.out.println("Server criado com o IP:porto <"+IP.getHostAddress()+":"+this.server_port+">" );
    System.setProperty("java.rmi.server.hostname","IP.getHostAddress()");
    connect_to_other_servers();
  }

	/**
	 * Cria socket
	 * @param port porto
	 * @param ssf SSL server Factory
	 */
	private void create_socket(int port, SSLServerSocketFactory ssf) {
		try {
			socket = (SSLServerSocket) ssf.createServerSocket(port);
		} catch (IOException e) {
			System.out.println("Erro ao criar socket SSL servidor: " + e);
			System.exit(-1);
		}
	}

  /** Estabelece armazem de chaves e de certificados */
	private void set_key_truststore() {
		System.setProperty("javax.net.ssl.trustStore", "../SSL/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "123456");
		System.setProperty("javax.net.ssl.keyStore", "../SSL/server.keys");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
	}

  /**
   * Liga-se a outros servidores
   */
	private void connect_to_other_servers() {
    other_servers = new ArrayList<>();

    Socket socket = null;
    boolean server_alive = true;
    int  other_server_port=0;

    add_another_server(socket, server_alive, other_server_port, "FIRST");

    add_another_server(socket, server_alive, other_server_port, "SECOND");

    print_connected_to_server(socket, server_alive);

    make_thread_listener();
	}

  /**
   * Adiciona outro servidor
   * @param socket socket
   * @param server_alive se está activo
   * @param other_server_port porto do outro servidor
   * @param next FIRST ou SECOND
   */
  private void add_another_server(Socket socket, boolean server_alive, int other_server_port, String next) {

    other_server_port = get_server_port(server_port, next);
    socket = null;
    server_alive = true;

    try {
      socket = new Socket(this.socket.getInetAddress(), other_server_port + 1000);
    } catch (Exception e) {
      server_alive = false;
    }

    print_connected_to_server(socket, server_alive);
  }

  /**
   * Se connectado notifica
   * @param socket socket
   * @param server_alive se activo
   */
  private void print_connected_to_server(Socket socket, boolean server_alive) {
    if (socket != null && server_alive) {
      add_other_server(socket);
      System.out.println("Conectado a outro servidor.");
    }
  }

  /**
   * Cria listener permitinfo que outros servidores se liguem a mim
   */
  private void make_thread_listener() {
		try {
			ServerSocket server_socket = new ServerSocket(server_port + 1000);

			ServerToServerListener other_server_listener = new ServerToServerListener(server_socket);
			new Thread(other_server_listener).start();
		} catch (IOException e) {
			System.out.println("Erro ao abrir ligação a servidores");
			System.exit(-1);
		}
	}

  /**
   * Obtem o porto do servidor
   * @param port
   * @param next
   * @return
   */
	private int get_server_port(int port, String next) {
    int other_port = -1;
    switch (port) {
      case 2000:
        if (next.equals("FIRST")) {
          other_port = 2001;
        } else {
          other_port = 2002;
        }
        break;
      case 2001:
        if (next.equals("FIRST")) {
          other_port = 2000;
        } else {
          other_port = 2002;
        }
        break;

      case 2002:
        if (next.equals("FIRST")) {
          other_port = 2000;
        } else {
          other_port = 2001;
        }
        break;

      default:
        break;
    }

    return other_port;
  }

  /**
   * Cria directorio
   * @param path caminho
   */
  private void make_directory(String path) {
    File file = new File(path);

    if (file.mkdirs()) {
      System.out.println("Directorio " + path + " criado.");
    }
  }

  /**
   * Adiciona listener do peer
   * @param socket socket
   */
  public static void add_peer_listener(SSLSocket socket) {
    Server_peer_listener peer_channel = new Server_peer_listener(socket);
    new Thread(peer_channel).start();

    peers.add(peer_channel);
  }

  /**
   * remove listener do peer
   * @param listener listener
   */
   public static void remove_peer_listener(Server_peer_listener listener) {
    peers.remove(listener);
    System.out.println("Server removed dead socket");
  }

  /**
   * obtem peers
   * @return peers
   */
  public static String get_peers() {
    String s = "";

    for (Server_peer_listener peer : peers) {
      SSLSocket socket = peer.get_socket();

      if (!socket.isConnected()
          || socket.isClosed()
          || socket.isOutputShutdown()
          || socket.isInputShutdown()) Server.remove_peer_listener(peer);
      else {
        s += "PEER ";
        s += socket.getInetAddress().getHostAddress() + " ";
        s += peer.get_peer_ID() + " ";
        s += peer.get_multicast_port() + " ";
        s += peer.get_backup_port() + " ";
        s += peer.get_restore_port() + " ";
        s += "\n";
      }
    }
    s += "DONE";
    return s;
  }

  /**
   * Obtem peers de outros servidores
   * @return peers de outros servidores
   */
  public static ArrayList<String> get_peers_from_other_servers() {

    ArrayList<String> peers_from_other_servers = new ArrayList<String>();

    for (ServerToServerChannel serverChannel : other_servers) {
      serverChannel.send_message("GETPEERS");
      schedule_task();

      ArrayList<String> otherPeers = serverChannel.get_peers_from_other_servers();
      for (String peer : otherPeers) {
        peers_from_other_servers.add(peer);
      }
      serverChannel.clean_peers_from_other_servers();
    }

    return peers_from_other_servers;
  }

  /**
   * Escala tarefa para esperar por resposta do outro servidor
   */
  private static void schedule_task() {
    ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
    Future<Boolean> future = scheduledPool.schedule(wait_for_peers, 500, TimeUnit.MILLISECONDS);
    try {
      future.get();
    } catch (InterruptedException e) {
    } catch (ExecutionException e) {
    }
  }

  /**
   * Espera pelos peers
   */
  static Callable<Boolean> wait_for_peers =
      () -> true;

  /**
   * Obtem outros servidores
   * @return outros servidores
   */
  public static ArrayList<ServerToServerChannel> get_other_servers() {
    return other_servers;
  }

  /**
   * Remove outro servidor
   * @param channel canal
   */
  public static void remove_other_server(ServerToServerChannel channel) {
    other_servers.remove(channel);
  }

  /**
   * Adiciona outro servidor
   * @param socket socket
   */
  public static void add_other_server(Socket socket) {
    ServerToServerChannel otherServerChannel = new ServerToServerChannel(socket);
    new Thread(otherServerChannel).start();

    other_servers.add(otherServerChannel);
  }

  /**
   * Compoe o directorio do peer
   * @param peer_ID identificador do peer
   */
  public static void make_peer_directory(int peer_ID) {
    File file = create_filepath(peer_ID);
    file.mkdirs();
  }

  /**
   * cria o caminho do ficheiro
   * @param peer_ID identificador do peer
   * @return caminho do ficheiro
   */
  private static File create_filepath(int peer_ID) {
    return new File(Peer.FILESYSTEM_FOLDER+Server.SERVER_FOLDER + server_ID + "/" + Server.PEER_FOLDER + peer_ID);
  }

  /**
   * Obtem o id do servidor
   * @return id do servidor
   */
  public static int get_server_ID() {
    return server_ID;
  }
}
