package utils;

import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;


/**
 * classe RMI (Remote Method Invocation)
 */
public class RMI
{

	/**
	 * main da classe RMI
	 * @param args argumentos passados na linha de comandos
	 */
	public static void main(String[] args)
	{		
		try
		{
			LocateRegistry.createRegistry(1099);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		System.out.println("serviço RMI a correr");

	}

} 