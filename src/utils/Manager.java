package utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import peer.Peer;

public class Manager implements Serializable {

  private static final long serialVersionUID = 1L;

  private int peer_ID;

  // Mapeia todos os file_IDs com o respectivo nome de arquivo para cada um cujo backup iniciou
  private ConcurrentHashMap<String, String> files_ids;

  // Armazena o estado de backup para cada arquivo cujo backup foi iniciado
  private ConcurrentHashMap<String, Boolean> backup_state;

  // Por cada chunk, armazena o tamanho do ficheiro  -
  private ConcurrentHashMap<String, Integer> chunks_stored_size;

  // Armazena o grau de replicação percepcionado de cada chunk
  private ConcurrentHashMap<String, Integer> current_degrees;

  // Armazena o grau de replicação de cada chunk
  private ConcurrentHashMap<String, Integer> degrees;

  // Hosts que armazenam o chunk
  private ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> chunks_hosts;

  // Espaço disponivel para armazenar chunks
  private long max_space;

  // Espaço usado por chunks armazenados
  private long space_used;

  /**
   * construtor de Manager
   *
   * @param peer_ID
   */
  public Manager(int peer_ID) {
    this.peer_ID = peer_ID;
    this.files_ids = new ConcurrentHashMap<String, String>();
    this.backup_state = new ConcurrentHashMap<String, Boolean>();
    this.max_space = 8000000; // [TODO]macro
    this.space_used = 0;
    this.current_degrees = new ConcurrentHashMap<String, Integer>();
    this.degrees = new ConcurrentHashMap<String, Integer>();
    this.chunks_hosts = new ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>();
    this.chunks_stored_size = new ConcurrentHashMap<String, Integer>();
  }

  /** Guarda dados do peer */
  public synchronized void save_metadata() {
    try {
      ObjectOutputStream oos =
          new ObjectOutputStream(
              new FileOutputStream(
                  Peer.FILESYSTEM_FOLDER  +"Peer"+ this.peer_ID + "/" + Peer.METADATA_FILE));

      oos.writeObject(this);

      oos.close();
    } catch (IOException e) {
      System.err.println("Error writing the server info file.");
    }
  }

  /**
	 * Elimina a informação relativa aos hosts do chunk e ao seu grau de replicação
	 * @param file_ID identificação do chunk
	 * */
  public void remove_info_of_file(String file_ID) {
		remove_info(file_ID, this.chunks_hosts.keySet().iterator());

		remove_info(file_ID, this.current_degrees.keySet().iterator());
	}

	/**
	 * Elimina a informação relativa aos hosts do chunk ou ao seu grau de replicação
	 * @param file_ID identificação do chunk
	 * @param iterator iterador da hashmap (current_degrees ou chunks_hosts)
	 */
	private void remove_info(String file_ID, Iterator<String> iterator) {
		Iterator<String> it = iterator;

		while (it.hasNext()) {
			String key = it.next();

			if (key.endsWith(file_ID)) {
				it.remove();
			}
		}
	}

  /**
	 * Guarda a informação do chunk
	 * @param sender_ID id de quem enviou
	 * @param file_ID id do ficheiro
	 * @param chunk_no numero do chunk
	 */
  public void store_chunk_info(int sender_ID, String file_ID, int chunk_no) {
    String hashmap_key = chunk_no + "_" + file_ID;

    CopyOnWriteArrayList<Integer> chunk_hosts = this.chunks_hosts.get(hashmap_key);
		is_first(sender_ID, hashmap_key, chunk_hosts);
	}

	/**
	 * Verifica se é a primeira mensagem de armazenamento do chunk
	 * @param sender_ID id de quem enviou
	 * @param hashmap_key chave da hashmap
	 * @param chunk_hosts hosts com o chunk
	 */
	private void is_first(int sender_ID, String hashmap_key,
			CopyOnWriteArrayList<Integer> chunk_hosts) {
		if (chunk_hosts == null) {
			chunk_hosts = new CopyOnWriteArrayList<Integer>();
			chunk_hosts.add(sender_ID);

			this.chunks_hosts.put(hashmap_key, chunk_hosts);
			this.current_degrees.put(hashmap_key, chunk_hosts.size());
		} else {
			is_sender_listed(sender_ID, hashmap_key, chunk_hosts);
		}
	}

	/**
	 * Verifica de quem enviou já está na lista
	 * @param sender_ID quem enviou
	 * @param hashmap_key chave da hashmap
	 * @param chunk_hosts hosts com o chunk
	 */
	private void is_sender_listed(int sender_ID, String hashmap_key,
			CopyOnWriteArrayList<Integer> chunk_hosts) {
		if (!chunk_hosts.contains(sender_ID)) {
			chunk_hosts.add(sender_ID);
			this.chunks_hosts.replace(hashmap_key, chunk_hosts);
			this.current_degrees.replace(hashmap_key, chunk_hosts.size());
		}
	}

	/**
	 * Remove a informação do chunk
	 * @param hashmap_key chave da hashmap
   * @param sender_ID quem enviou
   */
  public void remove_chunk_info(String hashmap_key, int sender_ID) {
    CopyOnWriteArrayList<Integer> chunk_hosts = this.chunks_hosts.get(hashmap_key);
		if_first(hashmap_key, sender_ID, chunk_hosts);
	}

	/**
	 * Verifica se é a primeira mensagem de armazenamento do chunk
	 * @param sender_ID id de quem enviou
	 * @param hashmap_key chave da hashmap
	 * @param chunk_hosts hosts com o chunk
	 */
	private void if_first(String hashmap_key, int sender_ID,
			CopyOnWriteArrayList<Integer> chunk_hosts) {
		if (chunk_hosts != null && chunk_hosts.contains(sender_ID)) {
			int index = chunk_hosts.indexOf(sender_ID);
			chunk_hosts.remove(index);
			this.chunks_hosts.replace(hashmap_key, chunk_hosts);
			this.current_degrees.replace(hashmap_key, chunk_hosts.size());
		}
	}

	/**
	 * Obtem os identificadores dos ficheiros
	 * @return identificadores dos ficheiros
	 * */
  public ConcurrentHashMap<String, String> get_files_ids() {
    return files_ids;
  }

  /**
   * Estipula os identificadores dos ficheiros
	 * @param files_ids identificadores dos ficheiros
   */
  public void set_files_ids(ConcurrentHashMap<String, String> files_ids) {
    this.files_ids = files_ids;
  }

  /**
	 * Obtem o estado do backup
	 * @return estado do backup
	 * */
  public ConcurrentHashMap<String, Boolean> get_backup_state() {
    return backup_state;
  }

  /**
	 *  Estipula o estado do backup
	 *  @param backup_state estado do backup
	 *  */
  public void set_backup_state(ConcurrentHashMap<String, Boolean> backup_state) {
    this.backup_state = backup_state;
  }

  /**
	 *
	 * @return
	 * */
  public ConcurrentHashMap<String, Integer> get_chunks_stored_size() {
    return chunks_stored_size;
  }

  /**
	 *
	 * @param chunks_stored_size
	 * */
  public void set_chunks_stored_size(ConcurrentHashMap<String, Integer> chunks_stored_size) {
    this.chunks_stored_size = chunks_stored_size;
  }

  /**
	 * Obtem os graus de replicação percepcionados
	 * @return graus de replicação percepcionados
	 * */
  public ConcurrentHashMap<String, Integer> get_current_degrees() {
    return current_degrees;
  }

  /**
	 * Estipula os graus de replicação percepcionados
	 * @param current_degrees graus de replicação percepcionados
	 * */
  public void set_current_degrees(ConcurrentHashMap<String, Integer> current_degrees) {
    this.current_degrees = current_degrees;
  }

  /**
	 * Obtem os graus de replicação pedidos
	 * @return graus de replicação pedidos
	 * */
  public ConcurrentHashMap<String, Integer> get_degrees() {
    return degrees;
  }

  /**
	 * Estipula os graus de replicação pedidos
	 * @param degrees  graus de replicação pedidos
	 * */
  public void set_degrees(ConcurrentHashMap<String, Integer> degrees) {
    this.degrees = degrees;
  }

  /**
	 *
	 * @return
	 * */
  public ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> get_chunks_hosts() {
    return chunks_hosts;
  }

  /**
	 *
	 * @param chunks_hosts
	 * */
  public void setChunks_hosts(ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> chunks_hosts) {
    this.chunks_hosts = chunks_hosts;
  }

  /**
	 * Obtem o espaço maximo disponivel
	 * @return espaço maximo disponivel
	 * */
  public long get_max_space() {
    return max_space;
  }

  /**
	 * Estipula o espaço maximo disponivel
	 * @param max_space espaço maximo disponivel
	 * */
  public void set_max_space(long max_space) {
    this.max_space = max_space;
  }

  /**
	 * Obtem o espaço utilizado
	 * @return espaço utilizado
	 * */
  public long get_space_used() {
    return space_used;
  }

  /**
	 * Estipula o espaço utilizado
	 * @param space_used espaço utilizado
	 * */
  public void set_space_used(long space_used) {
    this.space_used = space_used;
  }
}
