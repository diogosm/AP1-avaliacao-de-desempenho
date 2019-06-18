/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.DTNHost;
import core.Message;
import core.World;
import core.Settings;
import java.util.Random;
import core.Debug;

import java.util.ArrayList;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	private Boolean isMultiCast = true;

	/**
	 * Creates a message creation event with a optional response request
	 * 
	 * @param from         The creator of the message
	 * @param to           Where the message is destined to
	 * @param id           ID of the message
	 * @param size         Size of the message
	 * @param responseSize Size of the requested response message or 0 if no
	 *                     response is requested
	 * @param time         Time, when the message is created
	 */
	public MessageCreateEvent(int from, int to, String id, int size, int responseSize, double time) {
		super(from, to, id, time);
		this.size = size;
		this.responseSize = responseSize;
	}

	/**
	 * Creates the message this event represents.
	 */
	@Override
	public void processEvent(World world) {
		if (isMultiCast) {
			DTNHost to = world.getNodeByAddress(this.toAddr);
			DTNHost from = world.getNodeByAddress(this.fromAddr);

			Settings settings = new Settings("Events1");

			Random random = new Random();
			int numAleatorio = random.nextInt(100);
			int numdestinos = 0;
			ArrayList<DTNHost> destinations = new ArrayList<>();
			int[] hosts = { 0, 25 };

			// acesso a variavel de numdestinos no arquivo
			if (settings.contains("numdestinos")) {
				numdestinos = settings.getInt("numdestinos", numAleatorio);
			}

			if (settings.contains("hosts")) {
				hosts = settings.getCsvInts("hosts");
			}

			// geração dos destinos aleatórios
			for (int i = 0; i < numdestinos; i++) {
				int destination = random.nextInt(hosts[1]);
				DTNHost destinationHost = world.getNodeByAddress(destination);
				destinations.add(destinationHost);
			}
		
			Message m = new Message(from, to, this.id, this.size, destinations);
			m.setReceivArrayList(destinations);
			from.createNewMessage(m);

		} else {
			DTNHost to = world.getNodeByAddress(this.toAddr);
			DTNHost from = world.getNodeByAddress(this.fromAddr);

			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
			from.createNewMessage(m);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " + "size:" + size + " CREATE";
	}
}
