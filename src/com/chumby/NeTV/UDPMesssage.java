package com.chumby.NeTV;

public class UDPMesssage
{
	//Header
	public static final int MESSAGE_HEADER = 0x55;
	public static final int MESSAGE_HEADER_SIZE = 2;
	
	//Types
	public static final int MESSAGE_CURSOR = 100;
	
	private byte[] 	_buffer;
	private int 	_length;
	
	public UDPMesssage(byte[] buffer, int length)
	{
		this._buffer = buffer;
		this._length = length;
	}
	
	public boolean isValidMessage()
	{
		if (this._buffer[0] != MESSAGE_HEADER)
			return false;
		
		switch (_buffer[1])
		{
			case MESSAGE_CURSOR:
				if (_length != MESSAGE_HEADER_SIZE + 8)
					return false;
				break;
				
			default:
				if (_length != MESSAGE_HEADER_SIZE)
					return false;
				break;
		}
		
		return true;
	}
	
	public int getSize()
	{
		return _length;
	}
	
	public int getDataSize()
	{
		return _length - MESSAGE_HEADER_SIZE;
	}
	
	/*
	public HashMap<String, int> getParameter()
	{
		switch (_buffer[1])
		{
			case MESSAGE_CURSOR:
				
				break;
				
			default:
				break;
		}
	}
	*/
}
