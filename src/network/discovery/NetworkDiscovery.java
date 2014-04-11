package network.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import network.NetworkInterface;
import network.NetworkListener;
import network.NetworkPacket;

public class NetworkDiscovery implements NetworkListener {
	private static final int DELAY = 5000;
	private static final int TIMEOUT = 5;
	
	private NetworkInterface networkInterface;
	private String identifier;
	
	private HashMap<InetAddress, String> devices;
	private HashMap<InetAddress, Integer> timeouts;
	
	private NetworkDiscoveryListener networkDiscoveryListener = null;
	
	private Timer timer;
	
	private class BroadcastTask extends TimerTask {

		@Override
		public synchronized void run() {
			NetworkPacket networkPacket = new NetworkPacket(networkInterface.getLocalHost(), new ArrayList<InetAddress>(), NetworkInterface.HOPCOUNT, identifier.getBytes());
			networkPacket.setFlags(NetworkPacket.ARP_FLAG);
			
			for (InetAddress device : devices.keySet()) {
				int TTL = timeouts.get(device) - 1;
				
				if (TTL > 0) {
					timeouts.put(device, TTL);
				} else {
					devices.remove(device);
					timeouts.remove(device);
					
					if (networkDiscoveryListener != null) {
						networkDiscoveryListener.onDeviceTimeout(device);
					}
				}
			}
			
			try {
				networkInterface.send(networkPacket);
			} catch (IOException e) { }
		}
		
	}
	
	public NetworkDiscovery(NetworkInterface networkInterface, String identifier) {
		this.networkInterface = networkInterface;
		this.identifier = identifier;
		this.devices = new HashMap<>();
		this.timeouts = new HashMap<>();
		
		this.timer = new Timer();
		this.timer.scheduleAtFixedRate(new BroadcastTask(), 0L, DELAY);
	}
	
	@Override
	public synchronized void onReceive(NetworkPacket packet) {
		if (!packet.isFlagSet(NetworkPacket.ARP_FLAG)) {
			return;
		}
		
		if (devices.containsKey(packet.getSourceAddress())) {
			timeouts.put(packet.getSourceAddress(), NetworkDiscovery.TIMEOUT);
		} else {
			InetAddress device = packet.getSourceAddress();
			String identifier = new String(packet.getData());
			
			devices.put(device, identifier);
			timeouts.put(device, NetworkDiscovery.TIMEOUT);
			
			if (networkDiscoveryListener != null) {
				networkDiscoveryListener.onDeviceDiscovery(device, identifier);
			}
		}
	}
	
	public void setNetworkDiscoveryListener(NetworkDiscoveryListener networkDiscoveryListener) {
		this.networkDiscoveryListener = networkDiscoveryListener;
	}
	
}
