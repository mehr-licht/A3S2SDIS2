# SDIS 2018/2019
## Sistema distribuído de Backup pela internet com SSL
### turma 3 grupo 1

#### Luis Oliveira 201607946
#### Ricardo Silva 201607780
#### Ricardo Lopes 201306009
#### Miguel Andrade 201709051


### como correr (linux)

Ao efetuar um pedido de backup não esquecer que a localização do ficheiro tem
que ser a pasta “​ bin/Peers/PeerDisk1/MyFiles​ ” neste caso o IniciatorPeer é o 1, este
será sempre apagado ao executar o comando ```sh compile.sh```.

* compilar com ```sh compile.sh​``` na raiz do projecto.

* iniciar o serviço RMI com ```sh rmi.sh``` e este ficará a correr numa nova tab.

* criar _'servidores'_ com ```sh create_server <server_ID> <port>```. Serão criados numa nova tab cada um.\
(num máximo de 3: tanto os IDs como os portos devem ser únicos e os portos devem situar-se entre 3000 e 3002.)\
Os Master Peers devem estar na mesma máquina.

* criar peers com ```sh create_peer <port_ID> <Server_IP>```. Abrirá uma nova _tab_ com o peer criado.
  
* pedir serviços de cliente com um dos seguintes comandos, no formato ```sh run.sh SERVICE <peer_ID> <operand1> <operand2>```
    * BACKUP example: ```sh run.sh BACKUP 2 "files/cenas.txt" 1```
    * DELETE example: ```sh run.sh DELETE 2 "files/cenas.txt"```
    * RESTORE example: ```sh run.sh RESTORE 2 "files/cenas.txt"```
    * RECLAIM example: ```sh run.sh RECLAIM 2 9```
    * STATE example: ```sh run.sh STATE 2```

* IMPORTANTE, ​ é fundamental que a localização do ficheiro que irá utilizar para
fazer backup seja “bin/Peers/PeerDisk<peerId>/MyFiles” ;

* O sistema continua em bom funcionamento mesmo após a perda de um servidor, enviando um <SIGINT>
com Ctrl + C. Do mesmo modo, é possível desconectar um peer, enviando um <SIGINT> com Ctrl + C. O 'servidor' irá removê-lo da sua lista
de peers ativos.


* exemplo:
    * ```sh compile.sh​```
    * ```sh rmi.sh```
    * ```sh create_server.sh 1 3000```
    * ```sh create_server.sh 2 3001```
    * ```sh create_peer.sh 1 <Server_IP>```
    * ```sh create_peer.sh 2 <Server_IP>```
    * ```sh create_peer.sh 3 <Server_IP>```
    * ```sh run.sh BACKUP 1 <fileName> <replication>```
    * Introduzir Ctrl + C no terminal do 'servidor' 1
    * ```sh run.sh RESTORE 1 <fileName>```
    * ```sh run.sh  DELETE 1 <fileName>```
    * ```sh run.sh RECLAIM 3 64```
    * ```sh run.sh STATE 3```
    * ```sh create_peer.sh 4 <Server_IP>```
    * ```sh run.sh BACKUP 1 <fileName> <replication>```




### Arquitectura

A arquitectura escolhida para a implementação deste trabalho segue um modelo (semi-)centralizado com um master peer, podendo ser-lhe adicionado até mais dois peers para melhorar a fault tolerance (redundância por replicação).

Neste modelo só os master peers sabem da onde existem todos os ficheiros, guardando pra esse efeito um registo dos mesmos.

Os peers precisam de pedir aos master peers informações sobre onde se encontram os ficheiros para se poderem depois pedir os mesmos aos outros peers.

Podemos criar no máximo 3 master peers(servers) que ficam à escuta de autenticações dos peers nos portos 3000 a 3002. A comunicação inter servers é feita nas portas 3100 a 3102 do memso IP.   

Ao criar um peer este regista-se junto de um server comunicando o seu ID e os portos dos canais multicast, de backup e de restore. O master peer (server) fica assim a saber tudo o que se passa com esse peer e poderá comunicar a outros servers se estes solicitarem ou a outros peers a si registados se também pedirem.

Sendo assim, as ligações possíveis de se esytabelecer são:
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

### Implementação

#### concorrência

Para resolver os problemas da concorrência tivemos de abordar cinco aspectos:

* Como os peers estão à escuta de mensagens de outros peers em três diferentes canais (multicast, de dados e de restore), criamos threads de _listeners_ em cada canal para que uma tarefa num desses canais não impeça a recepção noutro.
  
  Concumitantemente, quando um canal recebe um pacote, é lançada uma thread para tratar desse pacote especifico.

* Ao mesmo tempo é necessário garantir que simultaneamente consiga responder a pedidos do cliente.
 
    Mais uma vez, é criada uma thread nova para tratar do pedido do cliente.

* Para executar tarefas após um certo periodo de tempo recorremos à classe java.util.concurrent.ScheduledThreadPoolExecutor
  
* No que toca à concorrência no acesso à memória partilhada, usamos as classes
    * java.util.concurrent.ConcurrentHashMap
    * java.util.concurrent.CopyOnWriteArrayList
    
* Nas operações com a memória não volátil utilizamos métodos synchronized.
 


#### segurança

Nesta secção explicamos como tentamos garantir a autenticidade, confidencialidade e a integridade.

A autenticidade de cada nó do sistema é assegurada através da autenticação SSL.

A confidencialidade, ou seja, impedir que informação sensível chegue seja acessivel por quem não queremos e garantir que quem queremos a recebe, é afiançada pela encriptação usada.

A integridade, isto é, o receptor ter garantia que os dados vieram do emissor e que os dados não foram alterados por terceiros durante o percurso, é também sustentada na encriptação.

##### autenticação SSL

Cada peer tem uma peerkey e cada server tem uma serverKey que são as suas senhas de autenticação.

Do mesmo modo, cada peer e cada server possuêm uma trustStore onde guardam os certificados aceites.

Com este sistema, oferecemos uma garantia de que o servidor só aceita informações de quem conhece.

##### encriptação

A cifra é inicializada com uma chave AES de 256bit 
com vector de inicialização especificado de modo aleatório
com o gerador criptograficamente forte de números aleatórios
 da classe SecureRandom.

 
A transformação a aplicar ao input,
além de incluir o algoritmo criptografico
(AES), também usa o modo de feedback CBC (cipher block chaining)
 e o esquema de preenchimento PKCS5PADDING que internamente é transformado em PKCS7
para permitir o AES com mais de 8bit. 


Quando se inicia um backup há encriptação no InitiatorPeer com um achave só por si conhecida.

A desencriptação acontece aquando do restore. A mensagem é decifrado novamente no InitiatorPeer.

Deste modo garante-se a confidencialidade já que mais nenhum agente externo consegue decifrar os dados.

#### tolerância a falhas

A tolerância a falhas implica que o sistema sobrevive a uma falha de qualquer um dos seus nós em qualquer altura.

Para garantir isso, os peers partilham a informação sobre os seus dados com o seu servidor (e estes entre si no caso de ser um sistema semi-centralizado):
* Cada peer, de 30 em 30 segundos, envia ao seu 'servidor' um ficheiro com os seus metadados. Além disso, se houver vários 'servidores' eles partilham os metadados dos seus peers entre si.
* Quando um peer se torna activo verifica se tem metadados. Se os tiver carrega-os. Se não os tiver pergunta ao seu 'servidor' se há metadados dele. Se houver, o 'servidor' envia-lhe o ficheiro e ele carrega-os.

Deste modo, caso um peer e/ou o seu servidor forem abaixo, quando o peer se voltar a autenticar (a qualquer servidor, ver a escalabilidade), volta a ter toda a informação relativa aos seus dados. 


#### escalabilidade e disponibilidade
 
Para garantir que o sistema e a informação estão sempre disponíveis e devido à arquitectura semi-centralizada implementada, podemos ter mais que um master peer.

Assim, se um  master peer for abaixo, os peers que estiverem registados consigo irão ligar-se a outro 'servidor' activo. Só se não houver 'servidores' onde se ligar é que um peer termina.

Na autenticação de peers, estes ligam-se de forma aleatória a um 'servidor' que esteja activo. Havendo pelo menos um 'servidor' activo, sabe-se que um novo peer tem sempre a quem se ligar.