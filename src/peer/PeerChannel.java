package peer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.IOException;
import utils.Protocol_handler;

/**
 * classe PeerChannel
 */
public class PeerChannel implements Runnable {
	private Peer peer;
	private DatagramSocket socket;

	/**
	 * construtor PeerChannel
	 * @param peer peer do canal
	 * @throws IOException Excepção de entrada/saída
	 */
	public PeerChannel(Peer peer)  {//throws IOException
		this.peer = peer;
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * run do PeerChannel
	 */
	@Override
	public void run() {		
		while(true) {
			byte[] requestPacket = new byte[64500];
			DatagramPacket packet = new DatagramPacket(requestPacket, requestPacket.length);
			
			//System.out.println("Peer socket " + peer.getServer_ID() + " listening to messages in 'multicast' channel...");

			receive_packet_from_socket(packet);

			new Thread(new Protocol_handler(packet, this.peer)).start();
		}
	}

	/**
	 * recebe desde o socket
	 * @param packet packet
	 */
	private void receive_packet_from_socket(DatagramPacket packet) {
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Obtem o porto do socket
	 * @return porto do socket
	 */
	public int get_socket_port()
	{
		return socket.getLocalPort();		// host is localhost
	}

}
