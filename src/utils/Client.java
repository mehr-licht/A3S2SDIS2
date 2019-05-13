package utils;

import java.util.ArrayList;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * classe Client
 */
public class Client {

  private static String sub_protocol;
  private static ArrayList<String> sub_protocol_args;
  private static String rmi;

  /**
   * main da classe Client
   * @param args argumentos passados na linha de comandos
   */
  public static void main(String args[]) {
    if (!usage(args)) return;

    try {
      Registry registry = LocateRegistry.getRegistry("localhost");
      My_Remote_Interface rmi = (My_Remote_Interface) registry.lookup(Client.rmi);
      switch (sub_protocol) {
        case "BACKUP":
          case_is_backup(rmi);
          break;

        case "DELETE":
          case_is_delete(rmi);
          break;

        case "RECLAIM":
          case_is_reclaim(rmi);
          break;

        case "RESTORE":
          case_is_restore(rmi);
          break;

        case "STATE":
          case_is_state(rmi);
          break;

        default:
          System.err.println("Unknown command");
          break;
      }
    } catch (NotBoundException e) {
      e.printStackTrace();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  /**
   * Lança o sub-protocolo de BACKUP
   * @param rmi interface rmi
   */
  private static void case_is_backup(My_Remote_Interface rmi) {
    try {
      rmi.backup(sub_protocol_args.get(0), Integer.parseInt(sub_protocol_args.get(1)));
    } catch (Exception e) {
      System.err.println("Excepção no sub-protocolo de BACKUP:");
      e.printStackTrace();
    }
  }

  /**
   * Lança o sub-protocolo de DELETE
   * @param rmi interface rmi
   */
  private static void case_is_delete(My_Remote_Interface rmi) {
    try {
      rmi.delete(sub_protocol_args.get(0));
    } catch (Exception e) {
      System.err.println("Excepção no sub-protocolo de DELETE:");
      e.printStackTrace();
    }
  }

  /**
   * Lança o sub-protocolo de RECLAIM
   * @param rmi interface rmi
   */
  private static void case_is_reclaim(My_Remote_Interface rmi) {
    try {
      rmi.reclaim(Integer.parseInt(sub_protocol_args.get(0)));
    } catch (Exception e) {
      System.err.println("Excepção no sub-protocolo de RECLAIM:");
      e.printStackTrace();
    }
  }

  /**
   * Lança o sub-protocolo de RESTORE
   * @param rmi interface rmi
   */
  private static void case_is_restore(My_Remote_Interface rmi) {
    try {
      rmi.restore(sub_protocol_args.get(0));
    } catch (Exception e) {
      System.err.println("Excepção no sub-protocolo de RESTORE:");
      e.printStackTrace();
    }
  }

  /**
   * Lança o sub-protocolo de STATE
   * @param rmi interface rmi
   */
  private static void case_is_state(My_Remote_Interface rmi) {
    try {
      System.out.println("State of Peer " + Client.rmi + ":");
      System.out.println(rmi.state());
      // rmi.state();
    } catch (Exception e) {
      System.err.println("Excepção no sub-protocolo de STATE:");
      e.printStackTrace();
    }
  }

  /**
   * Verifica a chamada correcta na linha de comandos
   * @param args argumentos passados na linha de comandos
   * @return verdadeiro ou falso
   */
  public static boolean usage(String[] args) {
    sub_protocol = args[1];
    rmi = args[0];

    if (!(args.length >= 2 && args.length <= 4)) {
      System.out.println("Número errado de argumentos: "+args.length);
      System.out.println("usage: [TODO]");
      return false;
    }

    return check_args(args);
  }

  /**
   * Verifica e atribui os argumentos passados na linha de comandos
   * @param args argumentos passados na linha de comandos
   * @return verdadeiro ou falso
   */
  private static boolean check_args(String[] args) {
    switch (args.length) {
      case 4:
        if (sub_protocol.equals("BACKUP")) {
          sub_protocol_args.add(args[2].trim());
          sub_protocol_args.add(args[3].trim());
          return true;
        }
        break;
      case 2:
        if (sub_protocol.equals("STATE")) return true;
        break;
      case 3:
        if (sub_protocol.equals("DELETE")
            || sub_protocol.equals("RESTORE")
            || sub_protocol.equals("RECLAIM")) {
          sub_protocol_args.add(args[2].trim());
          return true;
        }
        break;
      default:
        System.out.println("Invalid usage");
        return false;

    }
    return false;
  }
}
