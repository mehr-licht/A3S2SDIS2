package peer;

import javax.net.ssl.SSLSocket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * classe PeerServerListener
 */
public class PeerServerListener implements Runnable {
	private Peer peer;
	private SSLSocket socket;
	PrintWriter out;
	BufferedReader in;

	/**
	 * construtor PeerServerListener
	 * @param peer peer
	 * @param socket socket
	 */
	public PeerServerListener(Peer peer, SSLSocket socket) {
		this.peer = peer;
		this.socket = socket;
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * run do peerServerListener
	 */
	@Override
	public void run() {				
		boolean alive = true;

		while(alive) {						
			String msg = null;
			
			try {
				msg = in.readLine();
			} catch (IOException e) {
				System.out.println("Ligação ao servidor perdida");
				alive = false;
			}
			
			if(msg != null) {
				handle_message(msg.split(" "));
			} else alive = false;
		}  
		
		// tenta reconnectar depois da ligação ao servidor se perder
		peer.server_connection();
	}

	/**
	 * Trata da mensagem
	 * @param msg mensagem
	 */
	private void handle_message(String[] msg) {
		switch(msg[0]) {
			case "PEER":
				int id = Integer.parseInt(msg[2]);
				
				if(id == this.peer.get_ID())
					break;
					
				String host = msg[1];
				int MC_port = Integer.parseInt(msg[3]);
				int MDB_port = Integer.parseInt(msg[4]);
				int MDR_port = Integer.parseInt(msg[5]);
				
				this.peer.add_endpoint(host, id, MC_port, MDB_port, MDR_port);
				
				break;
				
			case "DONE":
				this.peer.set_collected_peers(true);
				break;
				
			case "METADATA":
				this.peer.set_metadata_response(1);
				receive_metadata_from_server();
				break;

			case "METADATA_EMPTY":
				this.peer.set_metadata_response(0);
				break;
				
			default:
				System.out.println("Peer:: Erro ao processar a mensagem do servidor.");
				System.out.println(msg);
				break;
		}
	}

	/**
	 * Recebe metadados desde o servidor
	 */
	private void receive_metadata_from_server() {
		try
		{
			byte [] array  = new byte [256000];
				InputStream input = socket.getInputStream();
				int bytes2read = input.read(array);

				File file = new File(Peer.FILESYSTEM_FOLDER  + "Peer"+peer.get_ID() + "/" + Peer.METADATA_FILE);

				if(file.exists())
					file.delete();

				FileOutputStream fos = new FileOutputStream(file);
				fos.write(array, 0, bytes2read);
				fos.close();

				System.out.println("Recebida a metadata do servidor com " + bytes2read + " bytes");
		}
		catch(Exception e)
		{
			System.out.println("Erro ao guardar metadata no servidor");
			return;
		}
	}

	/**
	 * Envia header da mensagem
	 * @param message_type tipo da mensagem a ser enviada
	 */
	public void send_message(String message_type)
	{		
		out.println(message_type);
	}

	/**
	 * Envia o corpo da mensagem
	 * @param message mensagem
	 */
	public void send_bytes(byte[] message)
	{
		try
		{
			socket.getOutputStream().write(message, 0, message.length);
		}
		catch (IOException e)
		{
			System.out.println("Erro ao enviar mensagem para o servidor");
		}
	}
}
