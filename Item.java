/**
 * Class Item implementation.
 * 
 * Copyright 2011 Jan Minar <rdancer@rdancer.org>  All rights reserved.
 *
 * @author Jan Minar <rdancer@rdancer.org> 
 */

import java.util.concurrent.atomic.AtomicLong;
import java.math.BigDecimal;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class Item
{
    private String id;
    public String getId() { return id; }
    private String secret;
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    private BigDecimal price;
    public BigDecimal getPrice() { return price; }
    private RemoteClient seller;
    private RemoteClient winningBidder;
    private Date auctionEndTime;
    public Date getAuctionEndTime() { return auctionEndTime; }
    private boolean finished = false;
    public boolean finished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; };
    

    private static AtomicLong uniqueIdCounter = new AtomicLong();

    /**
     * Generate new Item, getting all the properties from reading the console
     * 
     * @return new Item
     */
    public static Item itemFromConsole()
    {
        Item item = new Item();
        String s = "";
        Scanner consoleScanner = new Scanner(System.in);

        try
        {
            
            System.out.print("Item name: ");
            //s = "   Some silly item name \n";
            s = consoleScanner.nextLine();
            item.name = s.replaceAll("^[[:space:]]*", "").replaceAll("[[:space:]]*$", "");
            
            for (;;)
            {
                System.out.print("Starting price: ");

                s = consoleScanner.nextLine();
                
                try
                {
                    item.price = new BigDecimal(s);
                    if (item.price.signum() < 0)
                    {
                        System.err.println("Non-negative price, please");
                        continue;
                    }
                    else if (item.price.scale() > 2)
                    {
                         System.err.println("No fractional pennies, please");
                         continue;
                    }
                    item.price = item.price.setScale(2);
                    break;
                }
                catch (NumberFormatException e)
                {
                    continue;
                }
            }
            
            
            for(;;)
            {
                System.out.print("Auction end time (10 minutes if blank): ");
                s = consoleScanner.nextLine();
                s = s.replaceAll("^[[:space:]]*", "").replaceAll("[[:space:]]*$", "");
                if (s.equals(""))
                {
                    long millisecondsPerMinute = 60 * 1000;
                    Date tenMinutesFromNow = new Date(System.currentTimeMillis()
                            + 10 * millisecondsPerMinute);
                    item.auctionEndTime = tenMinutesFromNow;

                    break;
                }
                else
                {
                    try
                    {
                        // XXX DNW
                        item.auctionEndTime = (new SimpleDateFormat()).parse(s);
                    }
                    catch (ParseException e)
                    {
                        System.err.println("I have trouble parsing this date");
                        continue;
                    }
                }
            }
        }
        catch (NoSuchElementException e)
        {
            throw new RuntimeException("End of file reading item description from console");
        }
        catch (IllegalStateException e)
        {
            throw new RuntimeException("End of file reading item description from console");
        }
        
        return item;
    }
    
    
    public Item()
    {
        id = uniqueItemId();
    }
    
    public Item(String id)
    {
        this.id = id;
    }

    public Item(Map<String,String> itemDescription)
            throws Exception
    {
        if (itemDescription.isEmpty())
                throw new Exception("Empty item description");
        if (itemDescription.containsKey("ID"))
                id = itemDescription.get("ID");
        if (itemDescription.containsKey("NAME"))
                name = itemDescription.get("NAME");
        if (itemDescription.containsKey("PRICE"))
                price = new BigDecimal(itemDescription.get("PRICE"));
        if (itemDescription.containsKey("END"))
                auctionEndTime = new Date(itemDescription.get("END"));
        if (itemDescription.containsKey("SELLER"))
                seller = new RemoteClient(itemDescription.get("SELLER"));
        if (itemDescription.containsKey("BIDDER"))
                winningBidder = new RemoteClient(itemDescription.get("BIDDER"));
        if (itemDescription.containsKey("FINISHED"))
                finished = true;
                
        // Tacitly ignore unknown label-value pairs
    }
    
    private static String uniqueItemId()
    {
        return "" + uniqueIdCounter.getAndIncrement();
    }

    public boolean placeBid(BigDecimal amount)
    {
        throw new RuntimeException("Unimplemented");
    }
    
    public String toString()
    {
        String s = "";
        
        s += "ID: " + getId() + "\n";
        s += "name: " + getName() + "\n";
        s += "price: " + (getPrice() == null ? "" : "GBP ") + getPrice() + "\n";
        s += "auction ends: " + getAuctionEndTime() + "\n";
        
        // XXX
        //s += "seller:    " + seller + "\n";
        //s += "winning bidder: " + winningBidder + "\n";
        
        return s;
    }
    
    
    public static void main(String[] args)
    {        
        while (true)
        {
            Item item = itemFromConsole();
            System.out.println(item);
        }
    }
}