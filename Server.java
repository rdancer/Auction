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
    static BigInteger TOO_MANY_MESSAGES = new BigInteger("30");
    static int numberOfThreads = 0;
    
    /**
     * Constructor for objects of class Server
     */
    public Server(int tcpPortNumber)
            throws IOException
    {
        ServerSocket serverSocket;
       
        serverSocket = new ServerSocket(tcpPortNumber);
        
        for (;;)
        {
            final Socket ioSocket = serverSocket.accept();
            
            Thread thread = new Thread(){
                int threadId = ++numberOfThreads;
                
                public void run()
                {
                    try {
                        PrintWriter out;
                        BufferedReader in;
                        String message; 
                        System.out.println("Connection accepted.");
                        out = new PrintWriter(ioSocket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(
                                ioSocket.getInputStream()));
                        message = in.readLine();
                        
                        System.out.println("Received message: " + message);
                        String clientId = (new Scanner(message)).next();
                        receivedMessageFrom(clientId);
                        out.println("Nice to hear from you, " + clientId + "!");
                        out.println("This is thread #" + threadId);
                        out.println("I have heard from you this many times: "
                                + numberOfMessagesFromClient(clientId));
                        if (numberOfMessagesFromClient(clientId).compareTo(TOO_MANY_MESSAGES) >= 0)
                                out.println("That's enough, buddy!");
                        ioSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            thread.start();
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