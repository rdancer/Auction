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
        extends ClientServer
{
    static Map<String,Item> itemsForSale
            = new HashMap<String,Item>();
    static int numberOfThreads = 0;
    String clientId; // Note: this variable is set per-thread
    
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
                        System.out.println("Connection accepted.");
                        socketOut = new PrintWriter(ioSocket.getOutputStream(), true);
                        socketIn = new BufferedReader(new InputStreamReader(
                                ioSocket.getInputStream()));
                        
                        String firstLine = socketIn.readLine();
                        if (!firstLine.trim().matches("HELLO " + Protocol.PROTOCOL_NAME_AND_VERSION))
                        {
                            respond("ERROR Protocol mismatch");
                            return;  // XXX from a thread?
                        }
                        
                        Map<String,String> labelValuePairs;
                        
                        try {
                            labelValuePairs = readFromClient();
                        } catch (Exception e) {
                            respond("ERROR Malformed input");
                            return;
                        }
                        
                        if (!labelValuePairs.containsKey("ID"))
                        {
                            respond("ERROR Client must give ID");
                            return;  // XXX from a thread?
                        }
                        else
                        {
                            clientId = labelValuePairs.get("ID");
                            // XXX authenticate
                        }
                        
                        respond("OK " + Protocol.PROTOCOL_NAME_AND_VERSION);
                        
                        commandLoop();
                        
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

    private void commandLoop()
    {
        System.out.println("Waiting for commands from client...");
        String line;
        
        Scanner in = new Scanner(System.in);
 
        for (;;)
        {
            line = in.nextLine();
            Scanner lineScanner = new Scanner(line.trim());
            
            if (lineScanner.hasNext())
            {
                String command = lineScanner.next();
                
                List<String> arguments = new ArrayList<String>();
                while (lineScanner.hasNext())
                        arguments.add(lineScanner.next());
                
                Map<String,String> labelValuePairs;
                if (command.equals("THANKS"))
                    continue;
                else
                {
                    try {
                        labelValuePairs = readFromClient();
                    } catch (Exception e) {
                        respond("ERROR Malformed input");
                        continue;
                    }
                }

                if (command.equals("BYE"))  // Not officially part of the protocol
                {
                    return;
                }
                else if (command.equals("BROWSE") && arguments.isEmpty())
                {
                    System.out.println("Sending all auctions...");
                    String response = "";
                    
                    for (Item item : itemsForSale.values())  // XXX non-thread-safe??
                    {
                        response += "ITEM\r\n";
                        response += item.toLabelValuePairsString().replaceAll("$", "\r");
                        response += "ENDITEM\r\n";
                    }
                    respond(response);
                }
                else if (command.equals("BROWSE"))
                {
                    System.out.println("Sending specific auctions...");                    
                    String response = "";
                    
                    for (String itemId : arguments)  // XXX non-thread-safe??
                    {
                        Item item = itemsForSale.get(itemId);
                        
                        response += "ITEM\r\n";                        
                        response += item.toLabelValuePairsString().replaceAll("$", "\r");
                        response += "ITEM\r\n";                        
                    }
                    respond(response);
                }
                else if (command.equals("SELL"))
                {
                    Item item;
                    try {
                        item = new Item(labelValuePairs);
                    } catch (Exception e) {
                        respond("ERROR " + e.getMessage());
                        continue;
                    }
                    String itemId = java.util.UUID.randomUUID().toString();
                    
                    item.setId(itemId);
                    itemsForSale.put(itemId, item);
                    
                    respond("ID " + itemId);
                }
                else if (command.equals("BID"))
                {
                    String itemId = null;
                    java.math.BigDecimal amount = null;
                    
                    try
                    {
                        itemId = labelValuePairs.get("ID");
                        amount = new java.math.BigDecimal(labelValuePairs.get("PRICE"));
                        bid(itemId, amount);
                    }
                    catch (Exception e)
                    {
                        respond("ERROR " + e.getMessage());
                    }
                    
                    respond("OK You're the highest bidder");
                }
                else if (command.equals("CANCEL"))
                {
                    if (!labelValuePairs.containsKey("ID"))
                    {
                        respond("ERROR Malformed request: ID missing");
                        continue;
                    }
                    String itemId = labelValuePairs.get("ID");
                    Item item;
                    try {
                        item = itemsForSale.get(itemId);
                    } catch (Exception e) {
                        respond("ERROR No such item");
                        continue;
                    }

                    String token = java.util.UUID.randomUUID().toString();
                    
                    respond(item.toLabelValuePairsString() + "\r\nTOKEN " + token);
                    
                    try {
                        labelValuePairs = readFromClient();
                    } catch (Exception e) {
                        respond("ERROR Malformed input");
                        continue;
                    }
                    
                    if (!labelValuePairs.containsKey("CONFIRM"))
                            continue;
                            
                    if (!labelValuePairs.containsKey("TOKEN"))
                            respond("ERROR Malformed request: TOKEN missing");
                    else if (!labelValuePairs.get("TOKEN").equals(token))
                            respond("ERROR Tokens don't match");
                    
                    try
                    {
                        cancel(itemId);
                    }
                    catch (Exception e)
                    {
                        respond("ERROR " + e.getMessage());
                    }
                    
                    respond("OK Item removed");
                }
                else
                {
                    respond("ERROR Unknown command: " + command);
                }
            }
        }
        

    }

    private void bid(String itemId, java.math.BigDecimal amount)
            throws Exception
    {
        synchronized(itemsForSale) {
            Item item;
            
            try {
                item = itemsForSale.get(itemId);
            } catch (Exception e) {
                throw new Exception("Item not on sale");
            }
            
            if (item.getPrice().compareTo(amount) >= 0)
                    throw new Exception("Bid lower than the current asking price");
            else
            {
                item.setPrice(amount);
                itemsForSale.put(itemId, item);
            }
        }
    }

    private void cancel(String itemId)
            throws Exception
    {
        synchronized(itemsForSale) {
            if (!itemsForSale.containsKey(itemId))
                    throw new Exception("No such item on sale");
            else
                    itemsForSale.remove(itemId);
        }
    }

  
    private Map<String,String> readFromClient()
            throws Exception
    {
        String response = readWholeMessage();
        return parseResponse(response);
    }

    private void respond(String message)
    {
        socketOut.println(message.trim() + "\r\nTHANKS\r" );
                // Can't use print(), as it doesn't flush buffer, or something
    }

    protected String readWholeMessage()
    {
        String message = readWholeMessage(socketIn);
        Scanner messageScanner = new Scanner(message);

        while (messageScanner.hasNextLine())
                System.err.println("client> " + messageScanner.nextLine());        
        
        return message;
    }
    
/*
    private void receivedMessageFrom(String clientId)
    {

        if (!countOfMessagesFromIndividualClientIDs.containsKey(clientId))
                countOfMessagesFromIndividualClientIDs.put(clientId,
                        BigInteger.ONE);
        else
                countOfMessagesFromIndividualClientIDs.put(clientId,
                        countOfMessagesFromIndividualClientIDs.get(clientId)
                        .add(BigInteger.ONE));
                        
        System.out.println("after: " + countOfMessagesFromIndividualClientIDs.get(clientId));
    }
*/    
    public static void runServerOnTCPPort(int tcpPortNumber)
            throws IOException
    {
        System.out.println(tcpPortNumber);
        new Server(tcpPortNumber);
    }
    
    public static void main(String[] args)
            throws IOException
    {
        int tcpPortNumber = Protocol.DEFAULT_PORT_NUMBER;
        try {
            int number = new Integer(args[0]);
            if (number >= 1 && number <= 65535)
                    tcpPortNumber = number;
        } catch (Exception e) {}

        runServerOnTCPPort(tcpPortNumber);
    }
}