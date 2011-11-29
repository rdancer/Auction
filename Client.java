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
import java.math.BigDecimal;


public class Client
{
    static String clientId = "CLIENT_ID_007";
    //String clientSecret;
    PrintWriter socketToServer;
    BufferedReader socketFromServer;
    
    public Client(String host, int tcpPortNumber)
            throws IOException
    {
        Socket ioSocket;
        PrintWriter out;
        BufferedReader in;

        ioSocket = new Socket(host, tcpPortNumber);
        System.out.println("Connection established.");

        socketToServer = out = new PrintWriter(ioSocket.getOutputStream(), true);
        socketFromServer = in = new BufferedReader(new InputStreamReader(
                ioSocket.getInputStream()));
    
    
        out.println("HELLO " + Auction.PROTOCOL_NAME_AND_VERSION + "\r");
        out.println("ID " + clientId + "\r");
        // XXX out.println("PASS " + clientPassPhrase + "\r\n");
        out.println("THANKS\r");
        
        String message = readWholeMessage();
    
        if (message.replaceAll("\n", "").matches(".*ERROR.*"))
        {
            System.err.println("Error connecting to server: \"" + message + "\"");
        }
        else
        {
            System.out.println("Logged in!");
        }
    }
    
    private void commandLoop()
    {
        System.out.println("Enter commands");
        String line;
        
        System.out.print("> ");  // prompt

        Scanner in = new Scanner(System.in);
 
        while (in.hasNextLine())
        {
           
            line = in.nextLine();
            Scanner lineScanner = new Scanner(line.trim());
            
            if (lineScanner.hasNext())
            {
                String command = lineScanner.next();
                
                List<String> arguments = new ArrayList<String>();
                while (lineScanner.hasNext())
                        arguments.add(lineScanner.next());


                if (command.equals("quit") || command.equals("exit"))
                {
                    return;
                }
                else if (command.equals("list") && arguments.isEmpty())
                {
                    System.out.println("Getting list of auctions...");
                    for (Item item : itemsForSale())
                            System.out.println(item);
                }
                else if (command.equals("list"))
                {
                    System.out.println("Printing out specific auctions...");                    
                    for (Item item : itemsForSale())
                            if (arguments.contains(item.getId()))
                                    System.out.println(item);
                }
                else if (command.equals("sell"))
                {
                    try
                    {
                        sellItem(Item.itemFromConsole());
                    }
                    catch (Exception e)
                    {
                        System.err.println("Error: " + e.getMessage());
                    }
                }
                else if (command.equals("bid"))
                {
                    String itemId = null; 
                    BigDecimal amount = null;
                    
                    try
                    {
                        itemId = arguments.get(0);
                        amount = new BigDecimal(arguments.get(1));
                        if (arguments.size() > 2) throw new Exception("Too many arguments");
                    }
                    catch (Exception e)
                    {
                        System.err.println("Usage: bid <ITEM_ID> <AMOUNT>");
                    }
                    
                    try
                    {
                        bid(new Item(itemId), amount);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Error: " + e.getMessage());
                    }
                }
                else if (command.equals("cancel"))
                {
                    if (arguments.isEmpty())
                    {
                        System.err.println("Usage: cancel <ITEM_ID>[, ...]");
                        continue;
                    }
                    
                    for (String itemId : arguments)
                    {
                        Item item;
                        String token;
                        
                        try
                        {
                            Map<String,Item> tokenAndItemTuple = requestCancelItem(new Item(itemId));
                            token = (String)(tokenAndItemTuple.keySet().toArray())[0];  // this is horrible
                            item = tokenAndItemTuple.get(token);
                            
                            System.out.println("token: " + token);
                            System.out.println("item: " + item);
                        }
                        catch (Exception e)
                        {
                            throw new Error(e);
                            //System.err.println("Error: " + e.getMessage());
                            //continue;
                        }
                        
                        System.out.print(item);
                        String questionPrompt = "Remove item? [yes/NO] ";
                        System.out.println(questionPrompt);
                        while (in.hasNextLine())
                        {
                            String yesOrNo = in.nextLine().trim().toLowerCase();
                            if (yesOrNo.matches("yes|y|yup|remove"))
                            {
                                try
                                {
                                    confirmAction(token);
                                }
                                catch (Exception e)
                                {
                                    System.err.println("Error: " + e.getMessage());
                                }
                                break;
                            }
                            else if (yesOrNo.matches("no|n|nope|do not remove|don't|don't remove"))
                            {
                                break;
                            }
                            else
                            {
                                System.out.print(questionPrompt);
                            }
                        }
                    }
                }
                else if (command.toLowerCase().matches("help|h|\\?|hilfe"))
                {
                    System.out.println("quit                     Terminate program");
                    System.out.println("list [<ITEM_ID>[, ...]]  Show specified items (or all items if none apecified)");
                    System.out.println("sell                     Place an item up for sale");
                    System.out.println("bid <ITEM_ID> <AMOUNT>   Bid GBP AMOUNT on item ITEM_ID");
                    System.out.println("cancel <ITEM_ID>[, ...]  Remove one or more items from sale");
                    System.out.println("bid <ITEM_ID> <AMOUNT>   Bid GBP AMOUNT for item ITEM_ID");
                }
                else
                {
                    System.out.println("Unknown command: " + line);
                    System.out.println("Type \"help\" to get help");                    
                }
            }
            System.out.print("> ");  // prompt
        }
        

    }
    
    private String readWholeMessage()
    {
        String message = readWholeMessage(socketFromServer);
        Scanner messageScanner = new Scanner(message);

        while (messageScanner.hasNextLine())
                System.err.println("server> " + messageScanner.nextLine());        
        
        return message;
    }

    
    private String readWholeMessage(BufferedReader in)    
    {
        String message = "";
        String line;
        
        try {
            while ((line = in.readLine()) != null)
            {
                message += line.replaceAll("[\r\n]*$", "") + "\n";
                if (line.matches("^THANKS\\b"))
                        break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
                
        return message;
    }
    
   
    public static void main(String[] args)
            throws IOException
    {
        String hostNameOrIPAddress;
        int tcpPortNumber;
        
        try {
            hostNameOrIPAddress = args[0];
            tcpPortNumber = new Integer(args[1]);
            clientId = args[2];
        }
        catch (Exception e)
        {
            System.err.println("Usage: Client <HOST> <PORT> <CLIENT_ID>");
            return;
        }
        
        Client client;
        
        System.out.println("Connecting to " + hostNameOrIPAddress + ":" + tcpPortNumber + "...");
        try
        {
            client = new Client(hostNameOrIPAddress, tcpPortNumber);
        }
        catch (IOException e)
        {
            System.err.println("Connection failed: " + e.getMessage());
            return;
        }
        
        client.commandLoop();
    }

    private void mustNotContainNewlines(String s)
            throws Exception
    {
        if (s.matches("[\r\n]"))
                throw new Exception("Internal error: string contains invalid characters");
    }
    
    private void sellItem(Item item)
            throws Exception
    {
        String request = "";
        String name = item.getName().replaceAll("[\r\n]", ""),
                price = item.getPrice().toString(),
                end = item.getAuctionEndTime().toString();
        
        // Sanity checks
        mustNotContainNewlines(name);
        mustNotContainNewlines(price);
        mustNotContainNewlines(end);
    
        request += "SELL\r\n";
        request += "NAME " + name + "\r\n";
        request += "PRICE " + price + "\r\n";
        request += "END " + end + "\r\n";
        request += "THANKS\r\n";
        
        Map<String,String> response = sendToServer(request);
        
        if (response.containsKey("ID"))
                System.out.println("Auction created, with ID: " + response.get("ID"));
        else if (response.containsKey("ERROR"))
                throw new Exception("Failed to create auction"
                        + " -- server said: " + response.get("ERROR"));
        else
                throw new Exception("Failed to create auction");
    }

    public Map<String,String> sendToServer(String request)
            throws Exception
    {
        
        socketToServer.println(request.trim() + "\r");
        String response = readWholeMessage();
        return parseResponse(response);
    }
    
    private Map<String,String> parseResponse(String response)
    {
        Scanner sc = new Scanner(response);
        Map<String,String> map = new HashMap<String,String>();
        
        while (sc.hasNextLine())
        {
            String line = sc.nextLine();
            Scanner lineScanner = new Scanner(line);
            String label = "", value = "";
            
         
            if (lineScanner.hasNext())
            {
                label = lineScanner.next().trim();
                if (lineScanner.hasNext())
                {
                    value = lineScanner.nextLine().trim();
                }
                
                map.put(label, value);
            }
        }
        
        return map;
    }
    
    private Map<String,Item> requestCancelItem(Item item)
            throws Exception
    {
        String request = "";
        String id = item.getId();
        //String secret = item.getSecret();
        
        mustNotContainNewlines(id);
        //mustNotContainNewlines(secret);
        
        request += "CANCEL\r\n";
        request += "ID " + id + "\r\n";
        //request += "SECRET " + secret + "\r\n";
        request += "THANKS\r\n";
        
        Map<String,String> response = sendToServer(request);
        
        if (response.containsKey("ID") && response.containsKey("TOKEN"))
        {
            System.out.println("Confirmation requested");
            Map<String,Item> map = new HashMap<String,Item>();

            map.put(response.get("TOKEN"), new Item(response));
            
            return map;
        }
        else if (response.containsKey("ERROR"))
                throw new Exception("Failed to request canceling auction " + id
                        + " -- server said: " + response.get("ERROR"));
        else
                throw new Exception("Failed to request canceling auction " + id);
    }

    private void confirmAction(String token)
            throws Exception
    {
        String request = "";
        
        mustNotContainNewlines(token);
        
        request += "CONFIRM " + token + "\r\n";
        request += "THANKS\r\n";
        
        Map<String,String> response = sendToServer(request);
        
        if (response.containsKey("OK"))
                System.out.println("Action confirmed");
        else if (response.containsKey("ERROR"))
                throw new Exception("Failed to confirm action"
                        + " -- server said: " + response.get("ERROR"));
        else
                throw new Exception("Failed to confirm action");
    }

    
    /**
     * Note: negative bid amount is perfectly valid -- it is up to the server to enforce
     * bid range restrictions.
     */
    private void bid(Item item, BigDecimal amount)
            throws Exception
    {
        String request = "";
        
        request += "BID\r\n";
        request += "ID " + item.getId() + "\r\n";
        request += "PRICE " + amount + "\r\n";
        request += "THANKS\r\n";
        
        
        Map<String,String> response = sendToServer(request);
        
        if (response.containsKey("ID"))
                System.out.println("Bid accepted");
        else if (response.containsKey("ERROR"))
                throw new Exception("Bid not accepted -- server said: " + response.get("ERROR"));
        else
                throw new Exception("Bid not accepted");
    }    

    public static void test()
            throws Exception
    {
        main(new String[]{ "localhost", "31337", "" + Math.random() });
    }
    
    private List<Item> itemsForSale()
    {
        List<Item> itemList = new ArrayList<Item>();
        
        String request = "BROWSE\r";
        
        socketToServer.println(request);
        String response = readWholeMessage();
        
        Scanner scanner = new Scanner(response);
        
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            if (line.matches("^ITEM\\b"))
            {
                String itemDescription = "";
                while(scanner.hasNextLine() && (line = scanner.nextLine()).matches("^ENDITEM\\>"))
                        itemDescription += line;
                                    
                Map<String,String> labelValuePairs = parseResponse(itemDescription);
                
                if (!labelValuePairs.isEmpty())
                {
                    try
                    {
                        itemList.add(new Item(labelValuePairs));
                    }
                    catch (Exception e)
                    {
                        System.err.println("Bad item description: \"" + itemDescription + "\"");
                    }
                }
            }
        }
        
        return itemList;
    }
    
}