/*
 * Autores: Felipe Matheus e Victor Roque (c) 2019
 * Universidade Federal do Amazonas - UFAM
 * Instituto de Computação - ICOMP
 */
package routing;

import core.Settings;
import core.Connection;
import core.Message;
import core.DTNHost;

/**
 * Algoritmo SEBAR: Secure Energy based Ant Routing
 */
public class SEBARRouter extends ActiveRouter {

	public SEBARRouter(Settings s) {
		super(s);
	}

	protected SEBARRouter(SEBARRouter r) {
		super(r);
	}

	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // can't start a new transfer
		}

		// Try only the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer
		}
	}

	@Override
	public SEBARRouter replicate() {
		return new SEBARRouter(this);
	}
}
