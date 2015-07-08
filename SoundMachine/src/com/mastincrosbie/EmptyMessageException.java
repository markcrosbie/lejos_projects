package com.mastincrosbie;

public class EmptyMessageException extends Exception {

	public EmptyMessageException()
	{
		super();
	}

	public EmptyMessageException(String message)
	{
		super(message);
	}	
}
