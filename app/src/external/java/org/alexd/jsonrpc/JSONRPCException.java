package org.alexd.jsonrpc;

/**
 * Represents an error during JSON-RPC method call.
 * Various reasons can make a JSON-RPC call fail 
 * (network not available, non existing method, error during the remote execution ...)
 * You can use the inherited method getCause() to see which Exception has caused a JSONRPCException to be thrown
 * @author Alexandre
 *
 */
public class JSONRPCException extends Exception {

	private static final long serialVersionUID = 4657697652848090922L;

	public JSONRPCException(Object error)
	{
		super(error.toString());
	}
	
	public JSONRPCException(String message, Throwable innerException)
	{
		super(message, innerException);
	}
}
