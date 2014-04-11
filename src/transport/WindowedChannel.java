package transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import sun.rmi.transport.Transport;

import network.NetworkInterface;
import network.NetworkListener;
import network.NetworkPacket;

public class WindowedChannel implements NetworkListener {
	public static final int WNDSZ = 5;
	private static final int MSS = 20;
	private InetAddress localAddress;
	private InetAddress address;
	private NetworkInterface networkInterface;

	private OutputStream out;
	private InputStream in;

	private PipedOutputStream pipedOut;
	private PipedInputStream pipedIn;
	private InputHandler inputHandler;
	private QueueSender queueSender;
	private Timer timer;
	// QUEUE for important packets
	private ArrayList<TransportPacket> packetList = new ArrayList<TransportPacket>();
	// private ArrayList<TranportPacket>

	private byte streamNumber = 0;

	public WindowedChannel(InetAddress localAddress, InetAddress address,
			NetworkInterface networkInterface) throws IOException {
		this.localAddress = localAddress;
		this.address = address;
		this.networkInterface = networkInterface;

		pipedOut = new PipedOutputStream();
		in = new PipedInputStream(pipedOut);

		pipedIn = new PipedInputStream();
		out = new PipedOutputStream(pipedIn);

		inputHandler = new InputHandler(pipedIn);
		Thread t = (Thread) inputHandler;
		t.start();
		queueSender = new QueueSender(packetList);
		this.timer = new Timer();
		long DELAY = 500;
		this.timer.scheduleAtFixedRate(queueSender, DELAY, DELAY);
		// this.timer.scheduleAtFixedRate(new ackSimulator(), DELAY*2, DELAY*2);

	}

	// Reads the queue of the channel and sends data in a windows. continues
	// after every send packet is ack'ed
	private class QueueSender extends TimerTask {
		private int currentStream;

		private int sendIndex;
		private ArrayList<Integer> expectedACK;
		private ArrayList<TransportPacket> currentWindow;
		private ArrayList<TransportPacket> newPackets = new ArrayList<TransportPacket>();

		public void priorityPacket(TransportPacket packet) {
			if (currentWindow.size() > 0) {
				int i = 0;
				while (i<currentWindow.size()) {
					if (i < currentWindow.size()
							&& !currentWindow.get(i).isFlagSet(
									TransportPacket.ACK_FLAG)) {
						currentWindow.set(i, packet);
						break;
					}
					i++;
				}
			} else {
				currentWindow.add(packet);
			}
			// System.out.println("NEW PRIORITY " + currentWindow.size());
		}

		public QueueSender(ArrayList<TransportPacket> queue) {
			expectedACK = new ArrayList<Integer>();
			currentWindow = new ArrayList<TransportPacket>();
		}

		/**
		 * Fill the sendWindow with n Packets to be send
		 */
		private void fillWindow() {
			int index = 0;
			if (currentWindow.size() == 0) {
				System.out.println("window empty");
				while (currentWindow.size() < WNDSZ && packetList.size() > 0
						&& index < packetList.size()) {

					System.out.print("filling window--");
					TransportPacket t = null;
					// fill expectedACK with next seqs
					t = packetList.get(index);
					// Check whether packet has same stream number
					if (t.getStreamNumber() != this.currentStream) {
						System.out.println("wrong stream");
						break;
					} else {
						// If not continue polling and adding expected ACK's
						// packetList.poll();
						System.out.println("sup");
						if (!t.isFlagSet(TransportPacket.ACK_FLAG)) {
							System.out.println(t.getSequenceNumber());
							expectedACK.add(t.getSequenceNumber());
						} else {
							System.out.println("ack");
							System.out.println("ACK_FLAG SET "
									+ t.getAcknowledgeNumber());
						}
						currentWindow.add(t);
						// Reset sendIndex to 0 to start at the beginning of
						// each window
						sendIndex = 0;

					}
					index++;
				}
			}

		}

		@Override
		// TODO: ACK, set ack/seq numbers of transportPackets, priority packets
		// (replace first packet in send queue)
		public synchronized void run() {
			// synchronized (packetList) {
			// while (true) {
			// Try sending as long as there are packets left to send
			if (packetList.size() > 0 || expectedACK.size() > 0) {
				/**
				 * Check whether the first packet in the queue has a new
				 * streamIndex -> increment streamIndex Starts transmission of a
				 * new file\message
				 **/
				if (expectedACK.size() == 0
						&& packetList.get(0).getStreamNumber() != this.currentStream) {
					currentStream++;
					// System.out.println("                   NEW STREAM: "
					// + currentStream);
				}
				this.fillWindow();
				// If all packets have been ack'ed, read load next packets
				// for in send queue
				//

				// System.out.println("");
				if (currentWindow.size() > 0) {

					if (sendIndex < currentWindow.size()) {

						NetworkPacket networkPacket = new NetworkPacket(
								localAddress, address, (byte) 2, currentWindow
										.get(sendIndex).getBytes());
						networkPacket.setFlags(NetworkPacket.TRANSPORT_FLAG);

						// Check whether ACK has been removed from the list
						// of
						// expected ACKS or packet is an ACK packet
						System.out.print("      EXP: ");
						for (int i : expectedACK) {
							System.out.print(i + " ");
						}
						System.out.println("");
						if (expectedACK.contains(currentWindow.get(sendIndex)
								.getSequenceNumber())
								|| currentWindow.get(sendIndex).isFlagSet(
										TransportPacket.ACK_FLAG)) {
							if (currentWindow.get(sendIndex).isFlagSet(
									TransportPacket.ACK_FLAG)) {
								System.out.println("SEQ: "
										+ currentWindow.get(sendIndex)
												.getSequenceNumber()
										+ " | "
										+ currentWindow.get(sendIndex)
												.getAcknowledgeNumber());
							}
							try {
								networkInterface.send(networkPacket);

							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						sendIndex++;
					} else {
						// check with packets have not been acked

						// clear for debug
						sendIndex = 0;
						// expectedACK.clear();

						if (expectedACK.size() == 0) {
							// IF list empty: all packets have been acked ->
							// new Stream
							// System.out.println("window empty. removing send packets from list");
							for (int i = 0; i < currentWindow.size(); i++) {
								if (packetList.size() > 0) {
									// System.out.println("Removing");
									packetList.remove(0);
								}
							}
							currentWindow.clear();

							// REMOVE PACKETS FROM LIST

							// currentStream++;
						} else {
							// Resent remaining packets

							// DEBUG: remove first last entry from ACK list.
							// expectedACK.remove(expectedACK.size() - 1);
							System.out.println("--------------------");
						}
					}
				}
			}
			// }

			// }

		}

		public void receivedACK(int seq, int ack) {
			System.out.println(" _ __ _ __ _ __ _  _ __ _ _ _ __ _");
			System.out.print("ACKEXP: ");
			for (int i : expectedACK) {
				System.out.print(i + " ");
			}
			System.out.println("");
			int index = expectedACK.indexOf(ack);
			if (index > -1) {
				System.out.println("ACK: " + ack);
				expectedACK.remove(index);
			}
			System.out.println(" _ __ _ __ _ __ _  _ __ _ _ _ __ _");
			System.out.print("ACKEXP: ");
			for (int i : expectedACK) {
				System.out.print(i + " ");
			}
			System.out.println("");
		}

	}

	private class InputHandler extends Thread {
		BufferedReader in;
		int seqNumber;

		public InputHandler(InputStream in) {
			this.in = new BufferedReader(new InputStreamReader(in));
		}

		public void run() {
			while (true) {

				try {
					byte[] data = in.readLine().getBytes();
					int dataPosition = 0;

					while (data.length - dataPosition > MSS) {
						// System.out.println(data.length + ", " +
						// dataPosition
						// + " -- " + MSS);

						byte[] packetData = Arrays.copyOfRange(data,
								dataPosition, dataPosition + MSS);

						TransportPacket transportPacket = new TransportPacket(
								packetData);
						// Set packet data
						transportPacket.setStreamNumber(streamNumber);
						transportPacket.setSequenceNumber(seqNumber);
						// transportPacket.setAcknowledgeNumber(seqNumber);
						packetList.add(transportPacket);

						dataPosition += MSS;
						seqNumber++;
					}
					if (dataPosition < data.length) {
						byte[] packetData = new byte[data.length - dataPosition];

						System.arraycopy(data, dataPosition, packetData, 0,
								packetData.length);

						TransportPacket transportPacket = new TransportPacket(
								packetData);
						// Set packet data
						transportPacket.setStreamNumber(streamNumber);
						transportPacket.setSequenceNumber(seqNumber);
						transportPacket.setAcknowledgeNumber(seqNumber);
						packetList.add(transportPacket);

					}
					seqNumber++;
				} catch (IOException e) {
				}
				//
				seqNumber = 0;
				streamNumber++;
			}
		}

	}

	public OutputStream getOutputStream() {
		return out;
	}

	public InputStream getInputStream() {
		return in;
	}

	@Override
	public void onReceive(NetworkPacket packet) {
		if (packet.getSourceAddress().equals(address)
				&& packet.isFlagSet(NetworkPacket.TRANSPORT_FLAG)) {
			TransportPacket received = TransportPacket.parseBytes(packet
					.getData());
			if (received != null) {
				if (received.isFlagSet(TransportPacket.ACK_FLAG)) {
					System.out.println("GOT ACK "
							+ received.getAcknowledgeNumber());
					queueSender.receivedACK(0, received.getAcknowledgeNumber());
					// React to ACK
				} else {
					// IF ACK field == -1 -> data packet
					// -> add to queue and send ack

					TransportPacket transportPacket = new TransportPacket(0,
							received.getAcknowledgeNumber(),
							TransportPacket.ACK_FLAG,
							received.getStreamNumber(), new byte[0]);
					//

					//
					transportPacket.setAcknowledgeNumber(received
							.getSequenceNumber());
					// queueSender.priorityPacket(transportPacket);
					// packetList.add(transportPacket);
					queueSender.priorityPacket(transportPacket);

					// Set packet data

				}
			}
		}
		// }

	}

	public class ackSimulator extends TimerTask {
		@Override
		public synchronized void run() {
			// while (true) {
			if (queueSender.expectedACK.size() > 0) {
				System.out.print("EXP: ");
				for (int i : queueSender.expectedACK) {
					System.out.print(i + ",");
				}
				System.out.println();
				TransportPacket pac = new TransportPacket(new byte[0]);
				pac.setAcknowledgeNumber(queueSender.expectedACK.get(0));
				onReceive(new NetworkPacket(address, localAddress, (byte) 1,
						pac.getBytes()));
				// queueSender.receivedACK(0, queueSender.expectedACK.get(0));
			}

			// }
		}
	}

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		NetworkInterface networkInterface = new NetworkInterface(
				InetAddress.getByName("130.89.130.15"), 55555);
		networkInterface.start();
		//
		// NetworkDiscovery networkDiscovery = new
		// NetworkDiscovery(networkInterface, "yolo");
		// networkDiscovery.setNetworkDiscoveryListener(this);
		//
		// networkInterface.addNetworkListener(networkDiscovery);

		WindowedChannel channel = new WindowedChannel(
				InetAddress.getByName("130.89.130.15"),
				InetAddress.getByName("130.89.130.16"), networkInterface);

		WindowedChannel channel1 = new WindowedChannel(
				InetAddress.getByName("130.89.130.16"),
				InetAddress.getByName("130.89.130.15"), networkInterface);

		networkInterface.addNetworkListener(channel);
		networkInterface.addNetworkListener(channel1);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				channel.getOutputStream()));

		out.write(new String(new byte[50]));
		out.newLine();
		out.flush();
	}

}
