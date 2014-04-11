package network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkPacket {
	public static final byte ARP_FLAG 		= 0b00000001;
	public static final byte TRANSPORT_FLAG = 0b00000010;
	
	byte flags = 0;
	byte hopcount = 0;
	byte reserved = 0;
		
	private InetAddress sourceAddress;
	private List<InetAddress> destinationAddresses;
	
	private byte[] data;
	
	public NetworkPacket(InetAddress sourceAddress, InetAddress destinationAddress, byte hopcount, byte[] data) {
		this.sourceAddress = sourceAddress;
		this.destinationAddresses = new ArrayList<>();
		this.destinationAddresses.add(destinationAddress);
		this.hopcount = hopcount;
		this.data = data;
	}
	
	public NetworkPacket(InetAddress sourceAddress, List<InetAddress> destinationAddresses, byte hopcount, byte[] data) {
		this.sourceAddress = sourceAddress;
		this.destinationAddresses = destinationAddresses;
		this.hopcount = hopcount;
		this.data = data;
	}
	
	public boolean isMulticast() {
		return destinationAddresses.size() > 1;
	}
	
	public byte getFlags() {
		return flags;
	}
	
	public boolean isFlagSet(byte flag) {
		return (flags & flag) == flag;
	}
	
	public byte getReserved() {
		return reserved;
	}
	
	public byte getHopcount() {
		return hopcount;
	}
	
	public byte getHeaderSize() {
		return (byte) (2 + destinationAddresses.size());
	}
	
	public int getHeaderLength() {
		return (byte) (8 + 4 * destinationAddresses.size());
	}
	
	public InetAddress getSourceAddress() {
		return sourceAddress;
	}
	
	public List<InetAddress> getDestinationAddresses() {
		return destinationAddresses;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getLength() {
		return data.length;
	}
	
	public void setFlags(byte flags) {
		this.flags = flags;
	}
	
	public void setHopcount(byte hopcount) {
		this.hopcount = hopcount;
	}
	
	public void decrementHopcount() {
		this.hopcount--;
	}
	
	public void setReserved(byte reserved) {
		this.reserved = reserved;
	}
	
	public void setSourceAddress(InetAddress sourceAddress) {
		this.sourceAddress = sourceAddress;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[getHeaderLength() + data.length];
		
		bytes[0] = flags;
		bytes[1] = hopcount;
		bytes[2] = getHeaderSize();
		bytes[3] = reserved;
		
		System.arraycopy(sourceAddress.getAddress(), 0, bytes, 4, 4);
		
		for (int i = 0; i < destinationAddresses.size(); i++) {
			byte[] destinationAddress = destinationAddresses.get(i).getAddress();
			
			System.arraycopy(destinationAddress, 0, bytes, (i + 2) * 4, 4);
		}
		
		System.arraycopy(data, 0, bytes, getHeaderLength(), data.length);
		
		return bytes;
	}
	
	public static NetworkPacket parseBytes(byte[] bytes) {
		NetworkPacket networkPacket = null;
		
		if (bytes.length < 8) {
			return null;
		}
		
		byte flags = bytes[0];
		byte hopcount = bytes[1];
		byte headerSize = bytes[2];
		byte reserved = bytes[3];
		
		InetAddress sourceAddress = null;
		
		try {
			sourceAddress = InetAddress.getByAddress(Arrays.copyOfRange(bytes, 4, 8));
		} catch (UnknownHostException e) {
			return null;
		}
		
		List<InetAddress> destinationAddresses = new ArrayList<>();
		
		for (int i = 2; i < headerSize; i++) {
			if ((i * 4) + 4 >= bytes.length) {
				return null;
			}
			
			InetAddress destinationAddress = null;
			
			try {
				destinationAddress = InetAddress.getByAddress(Arrays.copyOfRange(bytes, i * 4, (i * 4) + 4));	
			} catch (UnknownHostException e) {
				return null;
			}
			
			destinationAddresses.add(destinationAddress);
		}
		
		byte[] data = Arrays.copyOfRange(bytes, headerSize * 4, bytes.length);

		networkPacket = new NetworkPacket(sourceAddress, destinationAddresses, hopcount, data);
		networkPacket.setFlags(flags);
		networkPacket.setReserved(reserved);
		
		return networkPacket;
	}
	
}