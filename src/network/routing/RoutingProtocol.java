package network.routing;

import java.io.IOException;

import network.NetworkPacket;

public interface RoutingProtocol {
	public void rout(NetworkPacket networkPacket) throws IOException;
}