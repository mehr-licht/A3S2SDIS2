package server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;

/**
 * classe de ServerChannel
 */
public class ServerChannel implements Runnable {
	private SSLServerSocket socket;

	/**
	 * construtor de ServerChannel
	 * @param socket socket
	 */
	public ServerChannel(SSLServerSocket socket) {
		this.socket = socket;
		System.out.println("socket do servidor a correr");
	}

	/**
	 * run de de ServerChannel
	 */
	@Override
	public void run() {
		while(true) {
			try {
				SSLSocket s = (SSLSocket) socket.accept();
		        System.out.println("Peer authenticado com sucesso.\n");
		        Server.add_peer_listener(s);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
