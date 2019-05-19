package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/** classe ServerToServerChannel */
public class ServerToServerChannel implements Runnable {

  private Socket socket;
  private DataOutputStream out;
  private DataInputStream in;
  private ArrayList<String> peers_from_other_servers;

  /**
   * construtor ServerToServerChannel
   *
   * @param socket socket
   */
  public ServerToServerChannel(Socket socket) {
    this.socket = socket;
    this.out = null;
    this.in = null;
    this.peers_from_other_servers = new ArrayList<>();
  }

  /** run do ServerToServerChannel */
  @Override
  public void run() {
    try {
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());
    } catch (IOException e) {
      // Quer dizer que o servidor se desligou por isso tem de se remover o socket
      Server.remove_other_server(this);
    }

    boolean alive = true;

    while (alive) {
      String msg = null;

      try {
        msg = in.readUTF();
      } catch (IOException e) {
        // Quer dizer que o servidor se desligou por isso tem de se remover o socket
        Server.remove_other_server(this);
        alive = false;
      }

      print_message_error(msg);
    }
  }

  /**
   * se houver erro com mensagem de outro servidor notifica
   *
   * @param msg mensagem
   */
  private void print_message_error(String msg) {
    if (msg != null) {
      try {
        handle_message(msg);
      } catch (IOException e) {
        System.out.println("Erro com mensagem de outro servidor.");
      }
    }
  }

  /**
   * trata da mensagem
   *
   * @param msg mensagem
   * @throws IOException Excepção de entrada/saida
   */
  private void handle_message(String msg) throws IOException {
    String[] msgSplit = msg.split(" ");

    switch (msgSplit[0]) {
      case "GETPEERS":
        out.writeUTF(Server.get_peers());
        break;

      case "PEER":
        this.peers_from_other_servers.add(msg);
        break;

      case "DONE":
        break;

      case "SAVE_METADATA":
        case_is_save_metadata(msgSplit[1]);
        break;

      default:
        System.out.println("Erro no servidor ao processar a mensagem de outro servidor.");
        break;
    }
  }

  /**
   * tratamento no caso da mensagem ser de SAVE_METADATA
   *
   * @param split datagrama já extraido
   */
  private void case_is_save_metadata(String split) {
    try {
      int peer_ID = Integer.parseInt(split);

      byte[] bytes = new byte[256000];
      int length = in.readInt();
      in.read(bytes, 0, length);

      Server.make_peer_directory(peer_ID);
      File new_file =
          create_filepath(peer_ID);

      if (new_file.exists()) new_file.delete();

      FileOutputStream fos = new FileOutputStream(new_file);
      fos.write(bytes, 0, length);
      fos.close();

      System.out.println(
          "Metadata do Peer"
              + peer_ID
              + " guardada com "
              + length
              + " bytes, recebidos de outro peer");
    } catch (Exception e) {
      System.out.println("Erro ao guardar metadados do Peer");
    }
  }

  /**
   * cria o caminho do ficheiro
   * @param peer_ID identificador do ficheiro
   * @return caminho do ficheiro
   */
  private File create_filepath(int peer_ID) {
    return new File(
        Server.SERVER_FOLDER
            + Server.get_server_ID()
            + "/"
            + Server.PEER_FOLDER
            + peer_ID
            + "/"
            + Server.METADATA_FILE);
  }

  /**
   * Obtem peers de outros servidores
   *
   * @return peers de outros servidores
   */
  public ArrayList<String> get_peers_from_other_servers() {
    return this.peers_from_other_servers;
  }

  /** Remove peers de outros servidores */
  public void clean_peers_from_other_servers() {
    this.peers_from_other_servers = new ArrayList<>();
  }

  /**
   * Envia mensagem
   *
   * @param message mensagem
   */
  public void send_message(String message) {
    try {
      out.writeUTF(message);
    } catch (IOException e) {
      System.out.println("erro ao enviar mensagem para outro servidor");
    }
  }

  /**
   * Envia mensagem
   *
   * @param message mensagem
   * @param no_bytes numero de bytes
   */
  public void send_bytes(byte[] message, int no_bytes) {
    try {
      out.writeInt(no_bytes);
      out.write(message, 0, no_bytes);
    } catch (IOException e) {
      System.out.println("Erro ao enviar mensagem para outro servidor");
    }
  }
}
