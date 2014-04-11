package transport;

public class TransportPacket {
	public static final byte ACK_FLAG				= 0b00000001;
	public static final byte SYN_FLAG 				= 0b00000010;
	public static final byte FIN_FLAG 				= 0b00000100;
	public static final byte FRAGMENTED 			= 0b00001000;
	public static final byte MORE_FRAGMENTS_FLAG 	= 0b00010000;
	
	private static final int HEADER_LENGTH = 12;
	
	private int sequenceNumber;
	private int acknowledgeNumber;
	
	private byte flags;
	private byte streamNumber;
	private byte reserved;
	private byte padding;

	private byte[] data;
	
	public TransportPacket(byte[] data) {
		this (0, 0, (byte) 0, (byte) 0, data);
	}
	
	public TransportPacket(int sequenceNumber, int acknowledgeNumber, byte flags, byte streamNumber, byte[] data) {
		this.sequenceNumber = sequenceNumber;
		this.acknowledgeNumber = acknowledgeNumber;
		this.flags = flags;
		this.streamNumber = streamNumber;
		this.reserved = 0;
		this.padding = 0;
		this.data = data;
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	public int getAcknowledgeNumber() {
		return acknowledgeNumber;
	}
	
	public byte getFlags() {
		return flags;
	}
	
	public boolean isFlagSet(byte flag) {
		return (flags & flag) == flag;
	}
	
	public byte getStreamNumber() {
		return streamNumber;
	}
	
	public byte getReserved() {
		return reserved;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getLength() {
		return data.length;
	}
	
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
	public void setAcknowledgeNumber(int acknowledgeNumnber) {
		this.acknowledgeNumber = acknowledgeNumnber;
	}
	
	public void setFlags(byte flags) {
		this.flags = flags;
	}
	
	public void setStreamNumber(byte streamNumber) {
		this.streamNumber = streamNumber;
	}
	
	public void setReserved(byte reserved) {
		this.reserved = reserved;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[HEADER_LENGTH + data.length];
		
		bytes[0] = (byte) (sequenceNumber >> 24);
		bytes[1] = (byte) (sequenceNumber >> 16);
		bytes[2] = (byte) (sequenceNumber >>  8);
		bytes[3] = (byte) (sequenceNumber >>  0);
		
		bytes[4] = (byte) (acknowledgeNumber >> 24);
		bytes[5] = (byte) (acknowledgeNumber >> 16);
		bytes[6] = (byte) (acknowledgeNumber >>  8);
		bytes[7] = (byte) (acknowledgeNumber >>  0);	
		
		bytes[8] = flags;
		bytes[9] = streamNumber;
		bytes[10] = reserved;
		bytes[11] = padding;
		
		System.arraycopy(data, 0, bytes, HEADER_LENGTH, data.length);
		
		return bytes;
	}

	public static TransportPacket parseBytes(byte[] bytes) {
		if (bytes.length < TransportPacket.HEADER_LENGTH) {
			return null;
		}
		
		int sequenceNumber = (bytes[0] << 24) +
				             (bytes[1] << 16) +
				             (bytes[2] <<  8) +
				             (bytes[3] <<  0);
		
		int acknowledgeNumber = (bytes[4] << 24) +
	                            (bytes[5] << 16) +
	                            (bytes[6] <<  8) +
	                            (bytes[7] <<  0);
		
		byte flags = bytes[8];
		byte streamNumber = bytes[9];
		byte reserved = bytes[10];
		
		byte[] data = new byte[bytes.length - TransportPacket.HEADER_LENGTH];
		
		System.arraycopy(bytes, 12, data, 0, data.length);
		
		TransportPacket transportPacket = new TransportPacket(sequenceNumber, acknowledgeNumber, flags, streamNumber, data);
		transportPacket.setReserved(reserved);
		
		return transportPacket;
	}
	
}