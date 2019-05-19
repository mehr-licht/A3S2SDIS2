package server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;

/** classe de Server_peer_listener */
public class Server_peer_listener implements Runnable {
  private SSLSocket socket;
  private PrintWriter out;
  private BufferedReader in;
  private int peer_ID;
  private int multicast_port;
  private int backup_port;
  private int restore_port;

  /**
   * construtor de Server_peer_listener
   *
   * @param socket socket
   */
  public Server_peer_listener(SSLSocket socket) {
    this.socket = socket;
    try {
      out = new PrintWriter(this.socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    } catch (IOException e) {
      // Peer disconnected so the socket has to be removed
      Server.remove_peer_listener(this);
    }
  }

  /** run do server_peer_listener */
  @Override
  public void run() {
    boolean alive = true;

    while (alive) {
      String msg = null;

      try {
        msg = in.readLine();
      } catch (IOException e) {
        // Quer dizer que o peer se desligou por isso tem de ser removido
        Server.remove_peer_listener(this);
        alive = false;
      }

      alive = print_removed(alive, msg);
    }
  }

  /**
   * verifica se está activo ou não e notifica caso não esteja
   *
   * @param alive activo
   * @param msg mensagem
   * @return activo ou inactivo
   */
  private boolean print_removed(boolean alive, String msg) {
    if (msg != null) {
      handle_message(msg);
    } else {
      System.out.println("Peer " + peer_ID + " removido do servidor");
      Server.remove_peer_listener(this);
      alive = false;
    }
    return alive;
  }

  /** trata da mensagem consoante o cabeçalho */
  private void handle_message(String message) {
    String[] msg = message.split(" ");

    switch (msg[0]) {
      case "AUTHENTICATE":
        case_is_authenticate(msg);
        break;

      case "GETPEERS":
        case_is_getpeers();
        break;

      case "GET_METADATA":
        case_is_getmetadata();
        break;

      case "SAVE_METADATA":
        case_is_savemetadata();
        break;

      default:
        System.out.println("Erro no servidor ao processar mensagem do peer.");
        break;
    }
  }

  /** Trata da mensagem caso se trate de SAVE_METADATA */
  private void case_is_savemetadata() {
    try {
      byte[] array = new byte[256000];
      InputStream input = socket.getInputStream();
      int bytesToRead = input.read(array);

      File new_file =
					create_file_path();

      if (new_file.exists()) new_file.delete();

      FileOutputStream fos = new FileOutputStream(new_file);
      fos.write(array, 0, bytesToRead);
      fos.close();

      ArrayList<ServerToServerChannel> other_servers = Server.get_other_servers();
      for (ServerToServerChannel serverChannel : other_servers) {
        serverChannel.send_message("SAVE_METADATA " + peer_ID);
        serverChannel.send_bytes(array, bytesToRead);
      }

      System.out.println("Metadados do Peer" + peer_ID + " guardados com sucesso.");
    } catch (Exception e) {
      System.out.println("Erro ao guardar metadados do peer" + peer_ID);
      return;
    }
  }

	/**
	 * cria o caminho do ficheiro
	 * @return caminho do ficheiro
	 */
	private File create_file_path() {
		return new File(
				Server.SERVER_FOLDER
						+ Server.get_server_ID()
						+ "/"
						+ Server.PEER_FOLDER
						+ peer_ID
						+ "/"
						+ Server.METADATA_FILE);
	}

	/** Trata da mensagem caso se trate de GET_METADATA */
  private void case_is_getmetadata() {
    File file =
				create_file_path();

    if (file.exists()) {
      out.println("METADATA");
      send_metadata_to_peer(file);
    } else {
      out.println("METADATA_EMPTY");
    }
  }

  /**
   * Envia metadados para o peer
   *
   * @param file ficheiro
   */
  private void send_metadata_to_peer(File file) {
    try {
      byte[] bytes = new byte[(int) file.length()];
      FileInputStream fis = new FileInputStream(file);
      BufferedInputStream bis = new BufferedInputStream(fis);
      bis.read(bytes, 0, bytes.length);
      socket.getOutputStream().write(bytes, 0, bytes.length);

      bis.close();
      fis.close();
    } catch (Exception e) {
      System.out.println("Erro ao enviar metadata para o peer");
      return;
    }
  }

  /** Trata da mensagem caso se trate de GETPEERS */
  private void case_is_getpeers() {
    ArrayList<String> peers_from_other_servers = Server.get_peers_from_other_servers();

    for (String peer : peers_from_other_servers) {
      out.println(peer);
    }

    out.println(Server.get_peers());
  }

  /**
   * Trata da mensagem caso se trate de AUTHENTICATE
   *
   * @param msg mensagem
   */
  private void case_is_authenticate(String[] msg) {
    this.peer_ID = Integer.parseInt(msg[1]);
    this.multicast_port = Integer.parseInt(msg[2]);
    this.backup_port = Integer.parseInt(msg[3]);
    this.restore_port = Integer.parseInt(msg[4]);

    Server.make_peer_directory(peer_ID);
    System.out.println("Peer " + peer_ID + " registado com sucesso.");
  }

  /**
   * Obtem o socket
   *
   * @return socket
   */
  public SSLSocket get_socket() {
    return socket;
  }

  /**
   * Obtem o ID do peer
   *
   * @return id do peer
   */
  public int get_peer_ID() {
    return peer_ID;
  }

  /**
   * obtem o porto do canal multicast
   *
   * @return porto do canal multicast
   */
  public int get_multicast_port() {
    return multicast_port;
  }

  /**
   * Obtem porto do canal de backup
   *
   * @return porto do canal de backup
   */
  public int get_backup_port() {
    return backup_port;
  }

  /**
   * Obtem o porto do canal de restore
   *
   * @return porto do canal de restore
   */
  int get_restore_port() {
    return restore_port;
  }
}
