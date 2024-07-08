package com.cassens.autotran.data.remote;

public class ExceptionHandling
{
	public static String exception= "No address associated with hostname";
	public static String exceptionReturn = "No Internet Connection";
	public static String exceptionTimeOut = "ETIMEDOUT";
	public static String exceptionTimeoutReturn = "Connection Time out, try again";
	public static String exceptionConnectionRefused = "Connection to remote server refused";
	public static String exceptionConRefusedReturn = "Connection lost, please try again...";
	public static String exceptionEconnReset = "ECONNRESET";
	public static String exceptionEConnResetReturn = "Trying to connect ... ";
	
	
	
	public static String getFormattedMessageFromException(String exceptionMessage)
	{
		try
		{
			if(exceptionMessage.contains(exception))
			{
				return exceptionReturn;
			}
			if(exceptionMessage.contains(exceptionTimeOut))
			{
				return exceptionTimeoutReturn;
			}
			if(exceptionMessage.contains(exceptionConnectionRefused))
			{
				return exceptionConRefusedReturn;
			}
			if(exceptionMessage.contains(exceptionEconnReset))
			{
				return exceptionEConnResetReturn;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return exceptionMessage;
	}
	
}
