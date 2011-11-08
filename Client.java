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
    public Client(String host, int tcpPortNumber)
    {
        Socket ioSocket;
        PrintWriter out;
        BufferedReader in;
        String clientId = "CLIENT_ID_007";
        
        try {
            ioSocket = new Socket(host, tcpPortNumber);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("Connection established.");
        try {
            out = new PrintWriter(ioSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    ioSocket.getInputStream()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        
        out.println(clientId + " " + "Hello, world!");
        System.out.println("Received message: " + readWholeMessage(in));
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
    {
        System.out.println("Connecting to " + hostNameOrIPAddress + ":" + tcpPortNumber);
        new Client(hostNameOrIPAddress, tcpPortNumber);
    }
    
    public static void main(String[] args)
    {
        String hostNameOrIPAddress = args[0];
        int tcpPortNumber = new Integer(args[1]);
        
        connectToTCPPort(hostNameOrIPAddress, tcpPortNumber);
    }}
