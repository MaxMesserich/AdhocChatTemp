package network.routing;

import java.io.IOException;

import network.NetworkInterface;
import network.NetworkPacket;

public class SimpleRoutingProtocol implements RoutingProtocol {
	private NetworkInterface networkInterface;
	
	public SimpleRoutingProtocol(NetworkInterface networkInterface) {
		this.networkInterface = networkInterface;
	}
	
	@Override
	public void rout(NetworkPacket networkPacket) throws IOException {
		if (networkPacket.isFlagSet(NetworkPacket.ARP_FLAG)) {
			if (!networkPacket.getSourceAddress().equals(networkInterface.getLocalHost())) {
				networkInterface.process(networkPacket);
				
				if (networkPacket.getHopcount() > 0) {
					networkPacket.decrementHopcount();
					networkInterface.send(networkPacket);
				}
			}
			
			return;
		} 
		
		if (networkPacket.isFlagSet(NetworkPacket.TRANSPORT_FLAG)) {
			if (!networkPacket.getDestinationAddresses().contains(networkInterface.getLocalHost())) {
				if (networkPacket.getSourceAddress().equals(networkInterface.getLocalHost())) {
					return;
				}
				
				if (networkPacket.getHopcount() > 0) {
					networkPacket.decrementHopcount();
					networkInterface.send(networkPacket);
				} else {
					return;
				}
			} else {
				if (networkPacket.getHopcount() >= 0) {					
					networkInterface.process(networkPacket);
				} else {
					networkInterface.send(networkPacket);
				}
			}
			
			return;
		}
	}
	
}