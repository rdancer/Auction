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
    private static final boolean DEFAULT_DEBUG_VALUE = true;
    
    private static Map<String,Item> itemsForSale
            = new HashMap<String,Item>();
    private static int numberOfThreads = 0;
    private static Map<String,Runnable> actionsAwaitingConfirmation = new HashMap<String,Runnable>();
    private static HashMap<String,InetSocketAddress> clientsRegisteredForBroadcastAlerts
            = new HashMap<String,InetSocketAddress>();
    
    
    private static ThreadLocal clientId = new ThreadLocal();
    private static String getClientId() { return (String) clientId.get(); }
    private static void setClientId(String clientId) { Server.clientId.set(clientId); }
     

    private static Thread expiredItemsChecker = new Thread() {
        public void run() {
            
            for (;;)
            {
                List<Item> expiredItems = new ArrayList<Item>();;
                Date now = new Date();
                
                synchronized(itemsForSale)
                {
                    if (itemsForSale != null)
                    {
                        for (Item item : itemsForSale.values())
                        {
                            if (!item.finished() && item.getAuctionEndTime().compareTo(now) < 0)
                            {
                                item.setFinished(true);
                                itemsForSale.put(item.getId(), item);
                                expiredItems.add(item);  // XXX Not thread-safe?  Should do a deep copy?
                                                         // Work around: once an item is finished, do not alter it!
                            }
                        }
                    }
                }

                for (Item item : expiredItems)
                {
                    String seller = item.getSellerId();
                    String winningBidder = item.getWinningBidderId();
                    String itemId = item.getId();
                    String itemName = item.getName();
                    
                    if (winningBidder != null)
                    {
                        sendToSingleClient(winningBidder, "You have won " + itemName);
                        sendToSingleClient(seller, "You have sold " + itemName + " to bidder "
                                + winningBidder);
                    }
                    else
                    {
                        sendToSingleClient(seller, "Item not sold: " + itemName);
                    }
                }
                
                expiredItems.clear();
                long millisecondsPerSecond = 1000;
                try {
                    sleep(1 * millisecondsPerSecond); // Somewhat arbitrariy duration
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    };
    
    static {
        expiredItemsChecker.start();
    }
    
    private static void sendToSingleClient(String clientId, String s)
    {
        String message = "";
        
        message += Protocol.PROTOCOL_NAME_AND_VERSION + "\r\n";
        message += "ALERT " + s + "\r\n";
        message += "THANKS\r\n";
                        
        byte[] buffer = new byte[512],
                messageBytes = message.getBytes();

        for (int i = 0; i < buffer.length; i++)
                buffer[i] = '\0';

        int length = Math.min(messageBytes.length, buffer.length
                - /* leave one byte for the terminating NUL */ 1);
        for (int i = 0; i < length; i++)
                buffer[i] = messageBytes[i];

        log("Sending message to: " + clientId);        
        DatagramPacket packet;
        
        InetSocketAddress portAndAddress = clientsRegisteredForBroadcastAlerts.get(clientId);
        if (portAndAddress == null)
        {
            log("Cannot send message: no socket address stored for client " + clientId);
            return;
        }
    
        InetAddress address = portAndAddress.getAddress();
        int port = portAndAddress.getPort();
        packet = new DatagramPacket(buffer, buffer.length, address, port);
        
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.send(packet);
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        } finally {
            if (socket != null)
                    socket.close();
        }
    }
    
    private static void broadcastToAllRegisteredClients(String s)
    {
        String message = "";
        
        message += Protocol.PROTOCOL_NAME_AND_VERSION + "\r\n";
        message += "ALERT " + s + "\r\n";
        message += "THANKS\r\n";
 
        byte[] buffer = new byte[512],
                messageBytes = message.getBytes();

        for (int i = 0; i < buffer.length; i++)
                buffer[i] = '\0';

        int length = Math.min(messageBytes.length, buffer.length
                - /* leave one byte for the terminating NUL */ 1);
        for (int i = 0; i < length; i++)
                buffer[i] = messageBytes[i];

        log("Sending message to: " + clientsRegisteredForBroadcastAlerts);        
        List<DatagramPacket> packetQueue = new ArrayList<DatagramPacket>();
        synchronized(clientsRegisteredForBroadcastAlerts)
        {
            for (InetSocketAddress portAndAddress : clientsRegisteredForBroadcastAlerts.values())
            {
                InetAddress address = portAndAddress.getAddress();
                int port = portAndAddress.getPort();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                packetQueue.add(packet);
            }
        }
        
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            for (DatagramPacket packet : packetQueue)
                    socket.send(packet);
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        } finally {
            if (socket != null)
                    socket.close();
        }
    }
         
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
                            ioSocket.close();
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
                            setClientId(labelValuePairs.get("ID"));
                            // XXX authenticate
                        }
                        
                        InetAddress address = ioSocket.getInetAddress();
                        int port = Protocol.DEFAULT_PORT_NUMBER;
                        if (labelValuePairs.containsKey("UDP"))
                                port = Integer.parseInt(labelValuePairs.get("UDP"));
                        
                        if (port /* is valid */ >= 1 && port <= 65535)
                        {
                            log("Registering client for broadcasts...");
                            synchronized(clientsRegisteredForBroadcastAlerts)
                            {   
                                clientsRegisteredForBroadcastAlerts
                                        .put(getClientId(), new InetSocketAddress(address, port));
                            }
                            log("... done.");
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
        
        Scanner in = new Scanner(socketIn);
 
        while (in.hasNextLine())
        {
            line = in.nextLine();
            log(/* XXX sanitize */ getClientId() + " <<< " + line);
            Scanner lineScanner = new Scanner(line.trim());
            
            if (lineScanner.hasNext())
            {
                String command = lineScanner.next();
                
                List<String> arguments = new ArrayList<String>();
                while (lineScanner.hasNext())
                        arguments.add(lineScanner.next());
                
                Map<String,String> labelValuePairs;
                if (command.equals("THANKS"))
                {
                    continue;
                }
                else
                {
                    try {
                        labelValuePairs = readFromClient(in); // readFromClient() DNW, try to pass the Scanner
                    } catch (Exception e) {
                        log("Malformed input: " + e.getMessage());
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
                    
                    synchronized(itemsForSale)
                    {
                        for (Item item : itemsForSale.values())
                        {
                            response += "ITEM\r\n";
                            response += item.toLabelValuePairsString().replaceAll("$", "\r").trim() + "\r\n";
                            response += "ENDITEM\r\n";
                        }
                    }
                    respond(response.isEmpty() ? "OK No items" : response);
                }
                else if (command.equals("BROWSE"))
                {
                    System.out.println("Sending specific auctions...");                    
                    String response = "";
                    
                    synchronized(itemsForSale)
                    {
                        for (String itemId : arguments)
                        {
                            // Note: We quietly ignore items that are not present.  There is a good reason:
                            // An item may have been removed after the client have made sure it ID was valid.
                            // The client has no means of locking our database while it is waiting for the
                            // network round-trip to complete, and so it would not be right to punish them:
                            // we cannot discern whether it is their fault, and even a well-behaved client
                            // will request a stale itemId ever so often.
                            if (itemsForSale.containsKey(itemId))
                            {
                                Item item = itemsForSale.get(itemId);
                                
                                response += "ITEM\r\n";                        
                                response += item.toLabelValuePairsString().replaceAll("$", "\r");
                                response += "ITEM\r\n";                        
                            }
                        }
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
                    item.setSellerId(getClientId());
                    synchronized(itemsForSale)
                    {
                        itemsForSale.put(itemId, item);
                    }
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
                        bid(itemId, amount, getClientId());
                    }
                    catch (Exception e)
                    {
                        respond("ERROR " + e.getMessage());
                        continue;
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
                    final String itemId = labelValuePairs.get("ID");
                    Item item;
                    synchronized(itemsForSale)
                    {
                        if (!itemsForSale.containsKey(itemId))
                        {
                            respond("ERROR No such item");
                            continue;
                        }
                        else
                        {
                            item = itemsForSale.get(itemId);
                            if (!item.getSellerId().equals(getClientId()))
                            {
                                respond("ERROR You are not the seller");
                                continue;
                            }
                            else if (item.finished())
                            {
                                respond("ERROR Not removing expired item");
                            }
                        }
                    }
                        
                    String token = java.util.UUID.randomUUID().toString();
                    
                    actionsAwaitingConfirmation.put(token, new Thread()
                            {
                                public void run()
                                        /* throws Exception */ // no really, it does: via Thread.stop(Throwable)
                                {
                                    try {
                                        cancel(itemId);
                                    } catch (Exception e) {
                                        this.stop(e);
                                    }
                                }
                            });
                    
                    assert(item != null);
                    respond(item.toLabelValuePairsString() + "\r\nTOKEN " + token);
                }
                else if (command.equals("CONFIRM"))
                {
                    if (arguments.size() == 0)
                            respond("ERROR Malformed request: token missing");
                    else if (arguments.size() > 1)
                            respond("ERROR Malformed request: contains more than one token");

                    Runnable action;
                    String token = arguments.get(0);
                    synchronized(actionsAwaitingConfirmation) {
                        if (!actionsAwaitingConfirmation.containsKey(token))
                                respond("ERROR No such confirmation token");
                        action = actionsAwaitingConfirmation.remove(token);
                    }
                    try
                    {
                        action.run();
                    }
                    catch (Exception e)
                    {
                        respond("ERROR " + e.getMessage());
                        continue;
                    }
                    respond("OK Performed successfully");
                }
                else if (command.equals("PING"))
                {
                    String response = "PONG";
                    
                    for (String argument : arguments)
                            response += " " + argument;
                            
                    respond(response);
                }
                else
                {
                    respond("ERROR Unknown command: " + command);
                }
            }
        }
        

    }

    private void bid(String itemId, java.math.BigDecimal amount, String clientId)
            throws Exception
    {
        synchronized(itemsForSale) {
            if (!itemsForSale.containsKey(itemId))
                    throw new Exception("Item not on sale: " + itemId);
            
            Item item = itemsForSale.get(itemId);
            
            if (item.getPrice().compareTo(amount) >= 0)
                    throw new Exception("Bid " + amount 
                            + " lower than or equal to the current price " + item.getPrice());
            else if(item.finished())
                    throw new Exception("Expired item");
            else
            {
                item.setPrice(amount);
                item.setWinningBidderId(clientId);
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
            {
                Item item = itemsForSale.get(itemId);
                
                if (item.finished())
                        throw new Exception("Will not remove expired item");
                else
                        itemsForSale.remove(itemId);
            }
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
        message = message.trim() + "\r\nTHANKS\r";
        
        log(message.replaceAll("^|(\n)(.)", "$1" + /* XXX sanitize */ getClientId() + " >>> $2"));                
        
        socketOut.println(message);  // Can't use print(): it doesn't flush buffer, or something
    }

    protected String readWholeMessage()
    {
        String message = readWholeMessage(socketIn);
        
        log(message.replaceAll("^|(\n)(.)", "$1" + /* XXX sanitize */ getClientId() + " <<< $2"));

        return message;
    }

    /*
     * XXX For some reason, opening a new scanner on socketIn is causing problems.
     * The following versions of the methods from above solve this issue by reusing
     * the scanner opened in the caller's context.  This is kludgy.
     */
    
    /* ******************* Kludge begin ***************** */    
    
    private Map<String,String> readFromClient(Scanner reuseScanner)
            throws Exception
    {
        String request = readWholeMessage(reuseScanner);
        log(request.replaceAll("^|(\n)(.)", "$1" + /* XXX sanitize */ getClientId() + " <<< $2"));
        return parseResponse(request);
    }
    


    private String readWholeMessage(Scanner reuseScanner)    
    {
        String message = "";
        String line;
        
        try {
// 
//             while ((line = in.readLine()) != null)
//             {
//                 message += line.replaceAll("[\r\n]*$", "") + "\n";
//                 if (line.matches("^THANKS\\b"))
//                         break;
//             }
            
            Scanner sc = reuseScanner;
            while(sc.hasNextLine())
            {
                line = sc.nextLine().replaceAll("[\r\n]*$", "") + "\n";
                message += line;
                if (line./* XXX DNW matches("^THANKS\\b") */trim().equals("THANKS"))
                {
                    //System.out.println("Found end of message");
                    break;
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
                
        return message;
    }
    
    /* ******************* Kludge end ***************** */
   
    public static void runServerOnTCPPort(int tcpPortNumber)
            throws IOException
    {
        log("Listening on port " + tcpPortNumber + "...");
        new Server(tcpPortNumber);
    }
    
    public static void main(String[] args)
            throws IOException
    {
        debug = DEFAULT_DEBUG_VALUE; // bit of a kludge
                
        int tcpPortNumber = Protocol.DEFAULT_PORT_NUMBER;
        try {
            int number = new Integer(args[0]);
            if (number >= 1 && number <= 65535)
                    tcpPortNumber = number;
        } catch (Exception e) {}

        runServerOnTCPPort(tcpPortNumber);
    }

}