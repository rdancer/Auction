/**
 * Abstract class ClientServer implementation.
 * 
 * Copyright 2011 Jan Minar <rdancer@rdancer.org>  All rights reserved.
 *
 * @author Jan Minar <rdancer@rdancer.org> 
 */

import java.net.*;
import java.util.*;
import java.io.*;
import java.math.BigDecimal;


public abstract class ClientServer
{
    protected PrintWriter socketOut;
    protected BufferedReader socketIn;



    
    protected String readWholeMessage(BufferedReader in)    
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

    protected void mustNotContainNewlines(String s)
            throws Exception
    {
        if (s.matches("[\r\n]"))
                throw new Exception("Internal error: string contains invalid characters");
    }
    
    protected Map<String,String> parseResponse(String response)
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
    
}