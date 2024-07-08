package com.cassens.autotran.data.remote;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.constants.URLS;

import android.util.Base64;

/**
 * Project : AUTOTRAN
 * Description : TokenGenerator class generate specific encrypted token for every web service request using a standard algorithm.  
 * @author Hemant
 * Creation Date : 12-11-2013
 */
public class TokenGenerator 
{
	private static final boolean DEBUG = false;
	String sha,buf;
	
	//Method to get a encrypted token by applying sha1 and base64
	//Tokens are the url minus the hostname with '/' converted to ':'.  Then, append the  
	public String getToken(String string, long timestamp) throws UnsupportedEncodingException 
	{
		String[] str=string.split(URLS.HOST_URL_CONSTANT);
		if (str.length < 2) {
			return "DUMMY_TOKEN";
		}
		if(str[1].contains("/"))
		{
			buf=str[1].replaceAll("/", ":");
			if(DEBUG) System.out.println(buf);
		}
		else
		{
			buf=str[1];
			if(DEBUG) System.out.println(buf);
		}
		
		//This semi colon is to support the API appending it to the end of the c_url variable...
		buf=buf+ ":" + String.valueOf(timestamp) + ":AutoTran";
		if(DEBUG) System.out.println(buf);
		
		try 
		{
             sha = sha1Hash(buf);
             if(DEBUG) System.out.println("result => "+sha);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		String s = toBase64fromString(sha);
		if(DEBUG) System.out.println("result => " + s);
		
		return s;
	}
	
	//Method for converting a string to its corresponding base64 
	public String toBase64fromString(String text)
	{
	    byte bytes[] = text.getBytes();
	    return Base64.encodeToString(bytes, Constants.BASE64_ENCODE_FLAGS);
	}

	public String sha1Hash( String toHash )
	{
	    String hash = null;
	    try
	    {
	        MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
	        byte[] bytes = toHash.getBytes("UTF-8");
	        digest.update(bytes, 0, bytes.length);
	        bytes = digest.digest();

	        // This is ~55x faster than looping and String.formating()
	        hash = bytesToHex( bytes );
	    }
	    catch( NoSuchAlgorithmException e )
	    {
	        e.printStackTrace();
	    }
	    catch( UnsupportedEncodingException e )
	    {
	        e.printStackTrace();
	    }
	    return hash;
	}

	// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();
	public static String bytesToHex( byte[] bytes )
	{
	    char[] hexChars = new char[ bytes.length * 2 ];
	    for( int j = 0; j < bytes.length; j++ )
	    {
	        int v = bytes[ j ] & 0xFF;
	        hexChars[ j * 2 ] = hexArray[ v >>> 4 ];
	        hexChars[ j * 2 + 1 ] = hexArray[ v & 0x0F ];
	    }
	    return new String( hexChars );
	}	
	
}
