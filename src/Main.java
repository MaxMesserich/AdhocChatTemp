
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.util.HashMap;

import network.NetworkInterface;
import network.discovery.NetworkDiscovery;
import network.discovery.NetworkDiscoveryListener;
import transport.WindowedChannel;


public class Main implements NetworkDiscoveryListener {
	private HashMap<InetAddress, String> devices;
	
	@Override
	public void onDeviceDiscovery(InetAddress device, String identifier) {
		devices.put(device, identifier);
		
		System.out.println(devices);
	}

	@Override
	public void onDeviceTimeout(InetAddress device) {
		devices.remove(device);
		
		System.out.println(devices);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		new Main();
	}
	
	public Main() throws IOException {
		devices = new HashMap<>();
		System.out.println("START");
		// 130.89.130.41
		// 130.89.130.15
		// 55555
		devices = new HashMap();
		NetworkInterface networkInterface = new NetworkInterface(InetAddress.getByName("130.89.140.43"), 55555);
		networkInterface.start();
		
		NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkInterface, "yolo");
		networkDiscovery.setNetworkDiscoveryListener(this);
		
		networkInterface.addNetworkListener(networkDiscovery);
		
		
		WindowedChannel channel = new WindowedChannel(InetAddress.getByName("130.89.169.104"), InetAddress.getByName("130.89.140.43"), networkInterface);

		networkInterface.addNetworkListener(channel);
		
//		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(channel.getOutputStream()));
	}
	
}