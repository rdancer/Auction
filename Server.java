/**
 * Class Server implementation.
 * 
 * Copyright 2011 Jan Minar <rdancer@rdancer.org>  All rights reserved.
 *
 * @author Jan Minar <rdancer@rdancer.org> 
 */

import java.net.*;
import java.io.*;

public class Server
{
    /**
     * Constructor for objects of class Server
     */
    public Server(int tcpPortNumber)
    {
        ServerSocket serverSocket;
        
        try {
            serverSocket = new ServerSocket(tcpPortNumber);
            serverSocket.accept();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("Connection accepted: exiting");
    }
    
    public static void runServerOnTCPPort(int tcpPortNumber)
    {
        System.out.println(tcpPortNumber);
        new Server(tcpPortNumber);
    }
    
    public static void main(String[] args)
    {
        int tcpPortNumber = new Integer(args[0]);
        runServerOnTCPPort(tcpPortNumber);
    }
}
