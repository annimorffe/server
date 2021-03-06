package com.exscudo.eon.IT;

import java.io.IOException;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.eon.TimeProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@SuppressWarnings("WeakerAccess")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AddPeerTestIT {

	protected static String GENERATOR = "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
	protected static String GENERATOR2 = "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";
	TimeProvider mockTimeProvider;

	protected PeerContext ctx1;
	protected PeerContext ctx2;

	@Before
	public void setUp() throws Exception {
		mockTimeProvider = Mockito.mock(TimeProvider.class);

		ctx1 = new PeerContext(GENERATOR, mockTimeProvider);
		ctx2 = new PeerContext(GENERATOR2, mockTimeProvider);

		ctx1.setPeerToConnect(ctx2);
		ctx2.setPeerToConnect(ctx1);

		Block lastBlock = ctx1.context.getInstance().getBlockchainService().getLastBlock();
		Mockito.when(mockTimeProvider.get()).thenReturn(lastBlock.getTimestamp() + 180 * 2 + 1);

		ctx1.generateBlockForNow();
		ctx2.fullBlockSync();
	}

	@Test
	public void step_1_OK() throws IOException, RemotePeerException {

		// Add incorrect peerID
		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(0L, "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Different PeerID in attributes", ex.getMessage());
		}
		// PeerID from target per
		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx1.context.getHost().getPeerID(), "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Adding by myself", ex.getMessage());
		}

		// All OK
		Assert.assertTrue(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));

		// Add twice
		Assert.assertTrue(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));

		// Exist host, different port
		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123:8888"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Peer IP already registered", ex.getMessage());
		}
	}

	@Test
	public void step_2_IncorrectID() throws IOException, RemotePeerException {
		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(0L, "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Different PeerID in attributes", ex.getMessage());
		}
	}

	@Test
	public void step_4_IncorrectNetwork() throws IOException, RemotePeerException {

		IFork fork = Mockito.spy(ctx2.context.getCurrentFork());
		Mockito.when(fork.getGenesisBlockID()).thenReturn(0L);
		Mockito.when(ctx2.context.getCurrentFork()).thenReturn(fork);

		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Different NetworkID", ex.getMessage());
		}
	}

	@Test
	public void step_5_IncorrectFork() throws IOException, RemotePeerException {

		IFork fork = Mockito.spy(ctx2.context.getCurrentFork());
		Mockito.when(fork.getNumber(Mockito.anyInt())).thenReturn(5);
		Mockito.when(ctx2.context.getCurrentFork()).thenReturn(fork);

		try {
			Assert.assertFalse(ctx1.syncMetadataPeerService.addPeer(ctx2.context.getHost().getPeerID(), "123"));
			Assert.assertTrue(false);
		} catch (Exception ex) {
			Assert.assertEquals("Different Fork", ex.getMessage());
		}
	}

}
