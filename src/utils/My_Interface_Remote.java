package utils;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface My_Interface_Remote extends Remote
{

    /**
     * Faz BackupUtil do ficheiro
     * @param filename ficheiro para ser feito o backup
     * @param replication_degree
     * @throws RemoteException Excepção de ligação remota
     */
    void backup(String filename, int replication_degree) throws RemoteException;

    /**
     * Elimina o ficheiro
     * @param filename ficheiro a eliminar
     * @throws RemoteException Excepção de ligação remota
     */
    void delete(String filename) throws RemoteException;

    /**
     * Restaura o ficheiro
     * @param filename ficheiro a restaurar
     * @throws RemoteException Excepção de ligação remota
     */
    void restore(String filename) throws RemoteException;

    /**
     * Liberta espaço
     * @param space Espaço total que se quer (kbytes)
     * @throws RemoteException Excepção de ligação remota
     */
    void reclaim(int space) throws RemoteException;

    /**
     * Devolve o estado
     * @return Estado
     * @throws RemoteException Excepção de ligação remota
     */
    String state() throws RemoteException;
}