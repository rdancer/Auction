/**
 * Class Client implementation.
 * 
 * Copyright 2011 Jan Minar <rdancer@rdancer.org>  All rights reserved.
 *
 * @author Jan Minar <rdancer@rdancer.org> 
 */

import java.net.*;
import java.util.*;
import java.io.*;

public class Client
{
    static String clientId = "CLIENT_ID_007";

    public Client(String host, int tcpPortNumber)
            throws IOException
    {
        Socket ioSocket;
        PrintWriter out;
        BufferedReader in;

        for (;;)
        {
            ioSocket = new Socket(host, tcpPortNumber);
            System.out.println("Connection established.");

            out = new PrintWriter(ioSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    ioSocket.getInputStream()));
        
        
            out.println(clientId + " " + "Hello, world!");
            String message = readWholeMessage(in);
            System.out.println("Received message: " + message);
        
            if (message.replaceAll("\n", "").matches(".*enough.*"))
            {
                System.out.println("Server's had enough, terminating.");
                break;
            }
            else
            {
                System.out.println("Going on!");
            }
        }
    }
    
    private String readWholeMessage(BufferedReader in)
    {
        String message = "";
        String line;
        
        try {
            while ((line = in.readLine()) != null)
                    message += line + "\n";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
                
        return message;
    }
    
    
    public static void connectToTCPPort(String hostNameOrIPAddress, int tcpPortNumber)
            throws IOException
    {
        System.out.println("Connecting to " + hostNameOrIPAddress + ":" + tcpPortNumber);
        new Client(hostNameOrIPAddress, tcpPortNumber);
    }
    
    public static void main(String[] args)
            throws IOException
    {
        String hostNameOrIPAddress = args[0];
        int tcpPortNumber = new Integer(args[1]);
        clientId = args[2];
        
        connectToTCPPort(hostNameOrIPAddress, tcpPortNumber);
    }}
