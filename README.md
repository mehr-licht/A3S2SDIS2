# SDIS 2018/2019
## Sistema distribuído de Backup pela internet com SSL
### turma 3 grupo 1

#### Luis Oliveira 201607946
#### Ricardo Silva 201607780
#### Ricardo Lopes 201306009
#### Miguel Andrade 201709051


### como correr (linux)


* compilar com ```sh compile.sh​``` na raiz do projecto.

* iniciar o serviço RMI com ```sh rmi.sh``` e este ficará a correr numa nova tab.

* criar _'servidores'_ com ```sh create_server <server_ID> <port>```. Serão criados numa nova tab cada um.\
(num máximo de 3: tanto os IDs como os portos devem ser únicos e os portos devem situar-se entre 2000 e 2002.)\
Os Master Peers devem estar na mesma máquina.

* criar peers com ```sh create_peer <port_ID> <Server_IP>```. Abrirá uma nova _tab_ com o peer criado.
  
* pedir serviços de cliente com um dos seguintes comandos, no formato ```sh run.sh SERVICE <peer_ID> <operand1> <operand2>```
    * BACKUP example: ```sh run.sh BACKUP 2 "files/cenas.txt" 1```
    * DELETE example: ```sh run.sh DELETE 2 "files/cenas.txt"```
    * RESTORE example: ```sh run.sh RESTORE 2 "files/cenas.txt"```
    * RECLAIM example: ```sh run.sh RECLAIM 2 9```
    * STATE example: ```sh run.sh STATE 2```


* O sistema continua em bom funcionamento mesmo após a perda de um servidor, enviando um <SIGINT>
com Ctrl + C. Do mesmo modo, é possível desconectar um peer, enviando um <SIGINT> com Ctrl + C. O 'servidor' irá removê-lo da sua lista
de peers ativos.


* exemplo:
    * ```sh compile.sh​```
    * ```sh rmi.sh```
    * ```sh create_server.sh 1 2000```
    * ```sh create_server.sh 2 2001```
    * ```sh create_peer.sh 1 <Server_IP>```
    * ```sh create_peer.sh 2 <Server_IP>```
    * ```sh create_peer.sh 3 <Server_IP>```
    * ```sh run.sh BACKUP 1 <fileName> <replication>```
    * ```sh run.sh RESTORE 1 <fileName>```
    * ```sh run.sh  DELETE 1 <fileName>```
    * ```sh run.sh RECLAIM 3 64```
    * ```sh run.sh STATE 3```
    * ```sh create_peer.sh 4 <Server_IP>```
    * ```sh run.sh BACKUP 1 <fileName> <replication>```
    * ( Em qualquer momento introduzir Ctrl + C na tab de um dos servidores ou peers que o sistema continua )

### Descrição

Neste sistema de backup estão asseguradas as seguintes operações:
 * _Backup_ de ficheiros em chunks encriptados divididos até um máximo de 64KB por chunk 
 * _Restore_ juntando os chunks separados num novo ficheiro igual ao original decifrado 
 * _Reclaim_ de espaço fornecendo o espaço final dedicado a este sistema 
 * _State_ que devolve a informação sobre a ocupação do sistema de ficheiros do peer 
 * _Delete_ que elimina os chunks distribuidos de um dado ficheiro que tinha sido copiado a partir de um certo peer.

A arquitectura escolhida é um modelo semi-centralizado.

Para tratar da concorrência usamos threads, thread pools e synchronized methods conforme descrito na secção respectiva.

A comunicação segura está assegurada através de JSSE.

Garantimos ainda Escalabilidade e Tolerência a falhas.


### Arquitectura

A arquitectura escolhida para a implementação deste trabalho segue um modelo (semi-)centralizado com um master peer, podendo ser-lhe adicionado até mais dois peers para melhorar a fault tolerance (redundância por replicação).

Neste modelo só os master peers sabem da onde existem todos os ficheiros, guardando pra esse efeito um registo dos mesmos.

Os peers precisam de pedir aos master peers informações sobre onde se encontram os ficheiros para se poderem depois pedir os mesmos aos outros peers.

Podemos criar no máximo 3 master peers(servers) que ficam à escuta de autenticações dos peers nos portos 2000 a 2002. A comunicação inter servers é feita nas portas 3000 a 3002 do mesmo IP.   

Ao criar um peer este regista-se junto de um server comunicando o seu ID e os portos dos canais multicast, de backup e de restore. O master peer (server) fica assim a saber tudo o que se passa com esse peer e poderá comunicar a outros servers se estes solicitarem ou a outros peers a si registados se também pedirem.

Sendo assim, as ligações possíveis de se estabelecer são:
* Entre Peers.
* Entre Master Peers.
* Entre um Peer e um Master Peer e vice-versa.

Para esse efeito foram criados:
* um canal próprio de comunicação entre servidores por onde estes enviam e recebem as comunicações entre si.
* observadores de 2 dos 3 tipos de comunicação:
    * ServerToServerListener 
    * PeerServerListener
    * ServerPeerListener
    
* a comunicação entre peers é efectuada através de UDP depois do master Peer informar a qual se deve ligar, pois pela internet já não é viável usarmos multicast.


### Protocolos
>the format of the messages, as well as the rules used for exchanging these messages. 


Estão assegurados os seguintes protocolos:
* _Backup_ [subprotocols.Backup.java] : 
* _Restore_ [subprotocols.Restore.java] : 
* _Delete_ [subprotocols.Delete.java] : 
* _State_ [subprotocols.State.java] : 
* _Reclaim_ [subprotocols.Reclaim.java] : 

A comunicação entre peers é efectuada através de UDP depois do master Peer informar a qual se deve ligar, pois pela internet já não é viável usarmos multicast.

Os peers estão à escuta de mensagens de outros peers em três diferentes canais (multicast, de dados e de restore).

O serviço RMI está implementado no interface My_Interface_Remote do package utils que é implementado pela classe Peer. Este interface extende a classe Remote da package package java.rmi e será usado por todos os protocolos do classe Peer:

```java
  Registry registry = LocateRegistry.getRegistry();
  My_Interface_Remote rmi = (My_Interface_Remote) registry.lookup(Client.rmi);
```

### concorrência
>organizar[TODO]

Para resolver os problemas da concorrência tivemos de abordar cinco aspectos:

* Como os peers estão à escuta de mensagens de outros peers em três diferentes canais (multicast, de dados e de restore), criamos threads de _listeners_ em cada canal para que uma tarefa num desses canais não impeça a recepção noutro. Classe Peer, [linhas 121 a 123 da classe Peer]
  ```java
    multicast_channel = new PeerChannel(this);
    backup_channel = new PeerChannel(this);
    restore_channel = new PeerChannel(this);  
   ```  
   e são inicializadas nas linhas 128 a 130 da classe Peer.
  
  Concumitantemente, quando um canal recebe um pacote, é lançada uma thread para tratar desse pacote especifico. [linha 136 da classe Peer]
    ```java  
     new Thread(new BackupUtil(this)).start();
    ```
    
    Nos métodos de Backup, Restore e Reclaim da class Peer [linhas 545, 572 e 598 respectivamente] é usada uma thread para lançar cada um dos subprotocolos. 
    ```java
    public void backup(String filename, int replication_degree) {
    ...
        new Thread(new subprotocols.Backup(filename, replication_degree, this)).start();
    ...
    }
    ```
     ```java
     public void restore(String filename) {
    ...
     new Thread(new Restore(filename, this)).start();
     }
     ```
    
     ```java
    public void reclaim(int space) { 
    ...
    new Thread(new Reclaim(space, this)).start();
    }
     ```  

* Quando o cliente trata de uma mensagem DELETE recebida inicia uma thread para efectuar o serviço [linha 59 da classe Protocol_handler da package utils]:
```java
private void case_is_delete() {
   ...
    new Thread(new Delete(header[3], this.peer)).start();
  } 
```

linha 229: ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
linha 269: ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(1);
linha 314: ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(1);

* Um servidor tem de estar à escuta de novos servidores que se queiram ligar a si.
Para isso acontecer é criada uma thread [linha 173 da classe Server].
     ```java
      private void make_thread_listener() {
	   ...
			ServerSocket server_socket = new ServerSocket(server_port + 1000);
			ServerToServerListener other_server_listener = new ServerToServerListener(server_socket);
			new Thread(other_server_listener).start();
		...
	  }
	 ```

e quando finalmente o adiciona também usa uma thread [linha 345 da classe Server]
 ```java
 public static void add_other_server(Socket socket) {
    ServerToServerChannel otherServerChannel = new ServerToServerChannel(socket);
    new Thread(otherServerChannel).start();

    other_servers.add(otherServerChannel);
  }
 ```

* Ao mesmo tempo é necessário garantir que simultaneamente consiga responder a pedidos do cliente.
 
    Mais uma vez, é criada uma thread nova para tratar do pedido do cliente [linha 87 da classe Server]:
    ```java
    new Thread(new ServerChannel(socket)).start();
    ```
    
    e é criado uma thread para o canal  [linha 240 da classe Peer]
     ```java
    public static void add_peer_listener(SSLSocket socket) {
    Server_peer_listener peer_channel = new Server_peer_listener(socket);
    new Thread(peer_channel).start();
    peers.add(peer_channel);
    }
    ```
    

* Para executar tarefas após um certo periodo de tempo recorremos à classe java.util.concurrent.ScheduledThreadPoolExecutor
   [linha 309 da classe Server]: 
    ```java
    private static void schedule_task() {
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
        Future<Boolean> future = scheduledPool.schedule(wait_for_peers, 500, TimeUnit.MILLISECONDS);
        try {
        future.get();
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }
     }
    ```

Por outro lado no _run_ do BackupUtil também é usado um scheduledPool [linha 38 da classe BackupUtil da package utils] 
```java
   ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
```
 No run do restore [linha 54]:
 ```java
  ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
```
  
  Quando fica activo, o peer lança o método ask_and_load() que pergunta ao servidor se há metadados seus e os carrega em caso afirmativo. Este método utiliza um ScheduledThreadPool para tratar a concorrência na possibilidade de vários peers se estarem a autenticar ao mesmo tempo.[linha 237 da classe Peer]
 ```java  
   private void ask_and_load() {
      metadata_server = -1;
      server_channel.send_message("GET_METADATA");

      ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
      scheduledPool.schedule(load_metadata, 1000, TimeUnit.MILLISECONDS);
  }
   ```
   
  E quando o servidor escalona tarefas [linha 309 da classe Server]:
  ```java 
   private static void schedule_task() {
    ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
    Future<Boolean> future = scheduledPool.schedule(wait_for_peers, 500, TimeUnit.MILLISECONDS);
   ...
  }
  ```
  
  Para dividir um ficheiro em chunks [linha 115 da classe Backup da package subprotocols]:
  ```java 
  private void split_file() throws  IOException {
		..
		ScheduledExecutorService scheduled_pool = Executors.newScheduledThreadPool(100);
		...
  }
  ```
  
  Enquanto está a tentar guardar um chunk [linha 123 da classe Chunk da package utils]:
  ```java
   private boolean is_stored() throws InterruptedException, ExecutionException {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    ...
    executor.shutdownNow();
  }
  ```
  
  E quando volta a juntar os chunks no run da classe Restore da package subprotocols [linha 48]:
  ```java
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
  ```  


  
  Na classe PeerChannel da package Peer [linha 34]:
  ```java
  public void run() {		
		while(true) {
			...
			new Thread(new Protocol_handler(packet, this.peer)).start();
		}
	}			
  ```
  	
 Para gravar a metadata no disco, usamos na classe Manager da package utils [linha 60]:
  ```java
 public synchronized void save_metadata() {...}
  ```
  	
 Para enviar dados para os peers, usamos na classe Peer [linha 413]:
  ```java
   public synchronized void send_reply_to_peers(channel_type type, byte[] packet) {...}
  ```
  	e de seguida na linha 432
  	  ```java
  	private void loop_endpoints(channel_type type, byte[] packet) throws IOException {
    synchronized (endpoints) {... while{...} } }
      ```
  
* Na comunicação com o servidor usamos também uma thread [linha 325 da classe Peer]:
  ```java  
    new Thread(server_channel).start(); 
  ``` 
  
* No que toca à concorrência no acesso à memória partilhada, usamos as classes
    * java.util.concurrent.ConcurrentHashMap
    * java.util.concurrent.CopyOnWriteArrayList
    
* Nas operações com a memória não volátil utilizamos métodos synchronized.
 


### JSSE
[TODO]code e mensagens

Da framework JSSE usamos as seguintes funcionalidades:
* O algoritmo criptografico AES (Advanced Encryption Standard)
* Secure sockets and server sockets
* Factories for creating sockets, server sockets, SSL sockets, and SSL server sockets. By using socket factories, you can encapsulate socket creation and configuration behavior.
* Key and trust manager interfaces 

Nesta secção explicamos como tentamos garantir a autenticidade, confidencialidade e a integridade.

Usamos JSSE na implementação do _Backup_ e do _Restore_ para garantir que apenas os verdadeiros emissores e receptores acedem à informação. 

Quando um peer é criado autentica-se junto de um servidor enviando uma mensagem com o formato 
```AUTHENTICATE <peer_ID> <mc_port> <backup_port> <restore_port>```

A autenticidade de cada nó do sistema é assegurada através da autenticação SSL.

A confidencialidade, ou seja, impedir que informação sensível chegue seja acessivel por quem não queremos e garantir que quem queremos a recebe, é afiançada pela encriptação usada.

A integridade, isto é, o receptor ter garantia que os dados vieram do emissor e que os dados não foram alterados por terceiros durante o percurso, é também sustentada na encriptação.


#### autenticação SSL

Cada peer tem uma peerKey e cada server tem uma serverKey que são as suas senhas de autenticação.

Do mesmo modo, cada peer e cada server possuem uma trustStore onde guardam os certificados aceites.

Com este sistema, oferecemos uma garantia de que o servidor só aceita informações de quem conhece.


#### encriptação
[TODO] code

A cifra é inicializada com uma chave AES de 128bit.

Utilizamos a API JSSE através da package javax.net.ssl e a package javax.crypto, nomeadamente a classe Cipher, na classe AES da package utils para os serviços de encriptação e desencriptação.

A transformação a aplicar ao input,
além de incluir o algoritmo criptografico
AES (Advanced Encryption Standard), também usa o modo de feedback ECB (Electronic Codebook)
 e o esquema de preenchimento PKCS5PADDING que internamente é transformado em PKCS7
para permitir o AES com mais de 8bit. 

Quando se inicia um backup há encriptação no InitiatorPeer com uma chave só por si conhecida.

A desencriptação acontece aquando do restore. A mensagem é decifrado novamente no InitiatorPeer.

Deste modo garante-se a confidencialidade já que mais nenhum agente externo consegue decifrar os dados.



### escalabilidade

Para garantir que o sistema e a informação estão sempre disponíveis e devido à arquitectura semi-centralizada implementada, podemos ter mais que um master peer.

Assim, se um  master peer for abaixo, os peers que estiverem registados consigo irão ligar-se a outro 'servidor' activo. Só se não houver 'servidores' onde se ligar é que um peer termina. No package Peer, classe PeerServerListener [linha 41]:
```java
@Override
	public void run() {				
		boolean alive = true;

		while(alive) {						
			...
			if(...) {
				...
			} else alive = false;
		}  
		
		// tenta reconnectar depois da ligação ao servidor se perder
		peer.server_connection();
	}
```


Na autenticação de peers, estes ligam-se de forma aleatória a um 'servidor' que esteja activo. Havendo pelo menos um 'servidor' activo, sabe-se que um novo peer tem sempre a quem se ligar. Na classe Peer, métodos connect_to_port(SSLSocketFactory sf) [linha 277] e connect_to_server(SSLSocketFactory sf, int n, int server_port) [linha 294]:
 ```java
 private int connect_to_port(SSLSocketFactory sf) {
    Random rand = new Random();
    int n = rand.nextInt(3);
    int server_port = 2000 + n;

    return connect_to_server(sf, n, server_port);
  }
```
Do mesmo modo é utilizado um ScheduledThreadPool, conforme descrito na secção concorrência.


Além desta implementação, através do tratamento da concorrência (ver secção própria) também garantimos a escalabilidade.

```java
private int connect_to_server(SSLSocketFactory sf, int n, int server_port) {
    boolean connected = false;
    while (!connected) {
      try {
        socket = (SSLSocket) sf.createSocket(this.host_IP, server_port);
        connected = true;
      } catch (IOException e) {
        connected = false;
        server_port++;

        if (server_port == 2003) {
          server_port = 2000;
        }

        if (server_port == 2000 + n) {
          System.out.println("Não foi possível conectar a nenhum servidor");
          System.exit(-1);
        }
      }
    }
    return server_port;
  }
```

### tolerância a falhas

A tolerância a falhas implica que o sistema sobrevive a uma falha de qualquer um dos seus nós em qualquer altura.

Para evitar estes pontos de ruptura do sistema decidimos fazer uma implementação do Paxos, ou sistema semi-centralizado em que um servidor é replicado por vários.

Para garantir isso, os peers partilham a informação sobre os seus dados com o seu servidor (e estes entre si no caso de ser um sistema semi-centralizado):
* Cada peer, de 30 em 30 segundos, envia ao seu 'servidor' um ficheiro com os seus metadados. Além disso, se houver vários 'servidores' eles partilham os metadados dos seus peers entre si. No package utils, classe BackupUtil [linha 36]:
```java
@Override
  public void run() {
    while (true) {
      ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
      Future<Boolean> future =
          scheduledPool.schedule(send_metadata, SEND_INTERVAL, TimeUnit.SECONDS);
      try {
        future.get();
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      }
    }
  }
```
 
* Quando um peer se torna activo verifica se tem metadados. Se os tiver carrega-os. Se não os tiver pergunta ao seu 'servidor' se há metadados dele. Se houver, o 'servidor' envia-lhe o ficheiro e ele carrega-os. Na classe Peer, métodos ask_and_load() [linha 237] e load_metadata() [linha 246]: 
```java
private void ask_and_load() {
    metadata_server = -1;
    server_channel.send_message("GET_METADATA");

    ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);
    scheduledPool.schedule(load_metadata, 1000, TimeUnit.MILLISECONDS);
  }

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
  ```

Deste modo, caso um peer e/ou o seu servidor forem abaixo, quando o peer se voltar a autenticar (a qualquer servidor, ver a escalabilidade), volta a ter toda a informação relativa aos seus dados. 
