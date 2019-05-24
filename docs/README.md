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
será sempre apagado ao executar o comando ```./compile.sh```.

* compilar com ```./compile.sh​``` na raiz do projecto.

* iniciar o serviço RMI com ```./rmi.sh```.

* criar _'servidores'_ com ```./create_server <server_ID> <port>```\
(num máximo de 3: tanto os IDs como os portos devem ser únicos e os portos devem situar-se entre 2000 e 2002.\
Os Master Peers devem estar na mesma máquina.

* criar peers com ```./create_peer <port_ID> <ServerIp>```. Abrirá uma nova _tab_ com o peer criado.
  
* pedir serviços de cliente com um dos seguintes comandos, no formato ```./run.sh SERVICE <peer_ID> <operand1> <operand2>```
    * BACKUP example: ```./run.sh BACKUP 2 "files/cenas.txt" 1```
    * DELETE example: ```./run.sh DELETE 2 "files/cenas.txt"```
    * RESTORE example: ```./run.sh RESTORE 2 "files/cenas.txt"```
    * RECLAIM example: ```./run.sh RECLAIM 2 64```
    * STATE example: ```./run.sh STATE 2```

* IMPORTANTE, ​ é fundamental que a localização do ficheiro que irá utilizar para
fazer backup seja “bin/Peers/PeerDisk<peerId>/MyFiles” ;

* O sistema continua em bom funcionamento mesmo após a perda de um servidor, enviando um <SIGINT>
com Ctrl + C. Do mesmo modo, é possível desconectar um peer, enviando um <SIGINT> com Ctrl + C. O 'servidor' irá removê-lo da sua lista
de peers ativos.


* exemplo:
    * ```./rmi.sh```
    * ```./compile.sh​```
    * ```./create_server 1 2000```
    * ```./create_server 2 2001```
    * ```./create_peer 1 <ServerIp>```
    * ```./create_peer 2 <ServerIp>```
    * ```./create_peer 3 <ServerIp>```
    * ```./run.sh BACKUP 1 <fileName> <replication>```
    * Introduzir Ctrl + C no terminal do 'servidor' 1
    * ```./run.sh RESTORE 1 <fileName>```
    * ```./run.sh  DELETE 1 <fileName>```
    * ```./run.sh RECLAIM 3 64```
    * ```./run.sh 3 STATE```
    * ```./create_peer 4 <ServerIp>```
    * ```./run.sh BACKUP 1 <fileName> <replication>```




### Arquitectura

A arquitectura escolhida para a implementação deste trabalho segue um modelo (semi-)centralizado com um master peer, podendo ser-lhe adicionado até mais dois peers para melhorar a fault tolerance (redundância por replicação).

Neste modelo só os master peers sabem da onde existem todos os ficheiros, guardando pra esse efeito um registo dos mesmos.

Os peers precisam de pedir aos master peers informações sobre onde se encontram os ficheiros para se poderem depois pedir os mesmos aos outros peers.

Podemos criar no máximo 3 master peers(servers) que ficam à escuta de autenticações dos peers nos portos 2000 a 2002. A comunicação inter servers é feita nas portas 3100 a 3102 do memso IP.   

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

#### encriptação

#### segurança

#### tolerância a falhas

#### escalabilidade
