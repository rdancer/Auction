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
import java.math.BigInteger;

public class Server
{
    static Map<String,BigInteger> countOfMessagesFromIndividualClientIDs
            = new HashMap<String,BigInteger>();
    
    /**
     * Constructor for objects of class Server
     */
    public Server(int tcpPortNumber)
            throws IOException
    {
        ServerSocket serverSocket;
        Socket ioSocket;
        PrintWriter out;
        BufferedReader in;
        String message;        
        
        serverSocket = new ServerSocket(tcpPortNumber);
        
        for (;;)
        {
            ioSocket = serverSocket.accept();
                    
            System.out.println("Connection accepted.");
            out = new PrintWriter(ioSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    ioSocket.getInputStream()));
            message = in.readLine();
            
            System.out.println("Received message: " + message);
            String clientId = (new Scanner(message)).next();
            receivedMessageFrom(clientId);
            out.println("Nice to hear from you, " + clientId + "!");
            out.println("I have heard from you this many times: "
                    + numberOfMessagesFromClient(clientId));
            ioSocket.close();
        }
        
        //maybeTellClientNotToSendAnyMoreMessages(out);
    }    
    
    public BigInteger numberOfMessagesFromClient(String clientId)
    {
        BigInteger returnValue = countOfMessagesFromIndividualClientIDs.get(clientId);
        
        return returnValue == null ? BigInteger.ZERO : returnValue;
    }
    
    private void receivedMessageFrom(String clientId)
    {
        System.out.println("before: " + countOfMessagesFromIndividualClientIDs.get(clientId));

        if (!countOfMessagesFromIndividualClientIDs.containsKey(clientId))
                countOfMessagesFromIndividualClientIDs.put(clientId,
                        BigInteger.ONE);
        else
                countOfMessagesFromIndividualClientIDs.put(clientId,
                        countOfMessagesFromIndividualClientIDs.get(clientId)
                        .add(BigInteger.ONE));
                        
        System.out.println("after: " + countOfMessagesFromIndividualClientIDs.get(clientId));
    }
    
    public static void runServerOnTCPPort(int tcpPortNumber)
            throws IOException
    {
        System.out.println(tcpPortNumber);
        new Server(tcpPortNumber);
    }
    
    public static void main(String[] args)
            throws IOException
    {
        int tcpPortNumber = new Integer(args[0]);
        runServerOnTCPPort(tcpPortNumber);
    }
}