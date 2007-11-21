/* Copyright 2007 Freenet Project Inc.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package freenet.clients.http;

import java.io.IOException;
import java.net.URI;

import freenet.client.HighLevelSimpleClient;
import freenet.io.AddressTracker;
import freenet.io.InetAddressAddressTrackerItem;
import freenet.io.PeerAddressTrackerItem;
import freenet.io.comm.UdpSocketHandler;
import freenet.l10n.L10n;
import freenet.node.Node;
import freenet.node.NodeClientCore;
import freenet.support.HTMLNode;
import freenet.support.TimeUtil;
import freenet.support.api.HTTPRequest;

/**
 * Toadlet displaying information on the node's connectivity status.
 * Eventually this will include all information gathered by the node on its
 * connectivity from plugins, local IP detection, packet monitoring etc.
 * For the moment it's just a dump of the AddressTracker.
 * @author toad
 */
public class ConnectivityToadlet extends Toadlet {
	
	private final Node node;
	private final NodeClientCore core;

	protected ConnectivityToadlet(HighLevelSimpleClient client, Node node, NodeClientCore core) {
		super(client);
		this.node = node;
		this.core = core;
	}

	public String supportedMethods() {
		return "GET";
	}

	public void handleGet(URI uri, final HTTPRequest request, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		PageMaker pageMaker = ctx.getPageMaker();
		
		HTMLNode pageNode = pageMaker.getPageNode(L10n.getString("ConnectivityToadlet.title", new String[]{ "nodeName" }, new String[]{ core.getMyName() }), ctx);
		HTMLNode contentNode = pageMaker.getContentNode(pageNode);

		/* add alert summary box */
		if(ctx.isAllowedFullAccess())
			contentNode.addChild(core.alerts.createSummary());

		// One box per port
		
		UdpSocketHandler[] handlers = node.getPacketSocketHandlers();
		
		String noreply = l10n("noreply");
		String local = l10n("local");
		String remote = l10n("remote");
		
		for(int i=0;i<handlers.length;i++) {
			// Peers
			AddressTracker tracker = handlers[i].getAddressTracker();
			HTMLNode portsBox = pageMaker.getInfobox(L10n.getString("ConnectivityToadlet.byPortTitle", new String[] { "port", "status" }, new String[] { handlers[i].getName(), AddressTracker.statusString(tracker.getPortForwardStatus()) }));
			contentNode.addChild(portsBox);
			HTMLNode portsContent = pageMaker.getContentNode(portsBox);
			PeerAddressTrackerItem[] items = tracker.getPeerAddressTrackerItems();
			HTMLNode table = portsContent.addChild("table");
			HTMLNode row = table.addChild("tr");
			row.addChild("th", l10n("addressTitle"));
			row.addChild("th", l10n("sentReceivedTitle"));
			row.addChild("th", l10n("localRemoteTitle"));
			row.addChild("th", l10n("firstSendLeadTime"));
			row.addChild("th", l10n("firstReceiveLeadTime"));
			for(int j=0;j<items.length;j++) {
				row = table.addChild("tr");
				PeerAddressTrackerItem item = items[j];
				// Address
				row.addChild("td", item.peer.toString());
				// Sent/received packets
				row.addChild("td", item.packetsSent() + "/ " + item.packetsReceived());
				// Initiator: local/remote FIXME something more graphical e.g. colored cells
				row.addChild("td", item.packetsReceived() == 0 ? noreply :
						(item.weSentFirst() ? local : remote));
				// Lead in time to first packet sent
				row.addChild("td", TimeUtil.formatTime(item.timeFromStartupToFirstSentPacket()));
				// Lead in time to first packet received
				row.addChild("td", TimeUtil.formatTime(item.timeFromStartupToFirstReceivedPacket()));
			}

			// IPs
			portsBox = pageMaker.getInfobox(L10n.getString("ConnectivityToadlet.byIPTitle", new String[] { "ip", "status" }, new String[] { handlers[i].getName(), AddressTracker.statusString(tracker.getPortForwardStatus()) }));
			contentNode.addChild(portsBox);
			portsContent = pageMaker.getContentNode(portsBox);
			InetAddressAddressTrackerItem[] ipItems = tracker.getInetAddressTrackerItems();
			table = portsContent.addChild("table");
			row = table.addChild("tr");
			row.addChild("th", l10n("addressTitle"));
			row.addChild("th", l10n("sentReceivedTitle"));
			row.addChild("th", l10n("localRemoteTitle"));
			row.addChild("th", l10n("firstSendLeadTime"));
			row.addChild("th", l10n("firstReceiveLeadTime"));
			for(int j=0;j<ipItems.length;j++) {
				row = table.addChild("tr");
				InetAddressAddressTrackerItem item = ipItems[j];
				// Address
				row.addChild("td", item.addr.toString());
				// Sent/received packets
				row.addChild("td", item.packetsSent() + "/ " + item.packetsReceived());
				// Initiator: local/remote FIXME something more graphical e.g. colored cells
				row.addChild("td", item.packetsReceived() == 0 ? noreply :
						(item.weSentFirst() ? local : remote));
				// Lead in time to first packet sent
				row.addChild("td", TimeUtil.formatTime(item.timeFromStartupToFirstSentPacket()));
				// Lead in time to first packet received
				row.addChild("td", TimeUtil.formatTime(item.timeFromStartupToFirstReceivedPacket()));
			}

		}
		
		writeHTMLReply(ctx, 200, "OK", pageNode.generate());
	}
	
	private String l10n(String key) {
		return L10n.getString("ConnectivityToadlet."+key);
	}
}
