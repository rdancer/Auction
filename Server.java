/**
 * Class Server implementation.
 * 
 * Copyright 2011 Jan Minar <rdancer@rdancer.org>  All rights reserved.
 *
 * @author Jan Minar <rdancer@rdancer.org> 
 */

import java.net.*;
import java.io.*;
import java.util.*;


public class Server
{
    /**
     * Constructor for objects of class Server
     */
    public Server(int tcpPortNumber)
    {
        ServerSocket serverSocket;
        Socket ioSocket;
        PrintWriter out;
        BufferedReader in;
        String message;        
        
        try {
            serverSocket = new ServerSocket(tcpPortNumber);
            ioSocket = serverSocket.accept();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("Connection accepted.");
        try {
            out = new PrintWriter(ioSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    ioSocket.getInputStream()));
            message = in.readLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("Received message: " + message);
        String clientId = (new Scanner(message)).next();
        out.println("Nice to hear from you, " + clientId + "!");
        
        receivedMessageFrom(clientId);
        
        //maybeTellClientNotToSendAnyMoreMessages(out);
    }    
    
    private void receivedMessageFrom(String clientId)
    {
        // XXX update the message count
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