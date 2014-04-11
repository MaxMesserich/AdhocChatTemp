package network.discovery;

import java.net.InetAddress;

public interface NetworkDiscoveryListener {
	public void onDeviceDiscovery(InetAddress device, String identifier);
	public void onDeviceTimeout(InetAddress device);
}