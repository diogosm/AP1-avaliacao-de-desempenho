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
import routing.community.KCliqueCommunityDetection;
import routing.community.CWindowCentrality;

/**
 * Algoritmo SEBAR: Secure Energy based Ant Routing
 */
public class SEBARRouter extends ActiveRouter {

    private static final int ENERGIA_CONSUMIDA = 1;
    private static final double PORC_ENERGIA = 0.9;
    private KCliqueCommunityDetection comunidade;
    private CWindowCentrality centralidade;

	public SEBARRouter(Settings s) {
		super(s);
        this.comunidade = new KCliqueCommunityDetection(s);
        this.centralidade = new CWindowCentrality(s);
	}

	protected SEBARRouter(SEBARRouter r) {
		super(r);
        this.comunidade = r.comunidade;
        this.centralidade = r.centralidade;
	}

    private double calculoEnk(DTNHost peer) {
        return (1 - PORC_ENERGIA) * (ENERGIA_CONSUMIDA / 2);
    }

    private double calculoEk(DTNHost peer) {
        return calculoEnk(peer) + calculoEck(peer);
    }

    private double calculoEck(DTNHost peer) {
        return 0.7;
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);
        comunidade.newConnection(getHost(), m.getTo(), this.comunidade);
        return m;
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

        // SEBAR Algorithm
        for (Connection c : getConnections()) {
            for (Message m : getMessageCollection()) {
                if (m.getTo() == c.getOtherNode(getHost())) {
                    startTransfer(m, c);
                    this.deleteMessage(c.getMessage().getId(), false);
                } else if (m.getHops().contains(m.getTo())) {
                    continue;
                } else if (comunidade.isHostInCommunity(m.getTo()) &&
                           calculoEk(getHost()) < calculoEk(m.getTo())) {
                    startTransfer(m, c);
                } else if (comunidade.isHostInCommunity(getHost()) &&
                           comunidade.isHostInCommunity(m.getTo()) &&
                           calculoEck(getHost()) < calculoEck(m.getTo())) {
                    startTransfer(m, c);
                }
            }
        }
	}

	@Override
	public SEBARRouter replicate() {
		return new SEBARRouter(this);
	}
}
