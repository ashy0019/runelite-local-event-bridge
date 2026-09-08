package com.ashy0019.localeventbridge.protocol;

import com.ashy0019.localeventbridge.event.ActorDeathObservation;
import com.ashy0019.localeventbridge.event.InventoryOccupancyEvent;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalEventPublisherTest
{
	@Test
	public void startsWithoutReceiverAndReplaysOnlyLatestStateWhenReceiverAppears()
		throws Exception
	{
		TransportWireCodec codec = new TransportWireCodec(new Gson());
		int port = unusedLoopbackPort();

		try (LocalEventPublisher publisher = new LocalEventPublisher(
			"runelite",
			codec,
			EnumSet.allOf(SourceCapability.class),
			port
		))
		{
			publisher.seedInventory(new InventoryOccupancyEvent("runelite", 5, 28));
			publisher.seedInventory(new InventoryOccupancyEvent("runelite", 17, 28));
			publisher.onActorDeath(new ActorDeathObservation("runelite"));
			assertFalse(publisher.isConnected());

			try (FakeReceiver receiver = new FakeReceiver(codec, port, 2))
			{
				List<TransportMessage> messages = receiver.awaitMessages();

				assertEquals(2, messages.size());
				assertTrue(messages.get(0) instanceof TransportMessage.Reset);
				assertTrue(messages.get(1) instanceof TransportMessage.State);

				TransportMessage.State state = (TransportMessage.State) messages.get(1);
				InventoryOccupancyEvent inventory =
					(InventoryOccupancyEvent) state.getEvent();
				assertEquals(17, inventory.getFilledSlots());

				for (TransportMessage message : messages)
				{
					assertFalse(message instanceof TransportMessage.Event);
				}
			}
		}
	}

	private static int unusedLoopbackPort() throws IOException
	{
		try (ServerSocket socket = new ServerSocket(0, 1, LoopbackEndpoint.address()))
		{
			return socket.getLocalPort();
		}
	}

	private static final class FakeReceiver implements AutoCloseable
	{
		private final TransportWireCodec codec;
		private final ServerSocket server;
		private final ExecutorService executor;
		private final CompletableFuture<List<TransportMessage>> messages;

		private FakeReceiver(TransportWireCodec codec, int port, int expectedMessages)
			throws IOException
		{
			this.codec = codec;
			this.server = new ServerSocket(port, 1, LoopbackEndpoint.address());
			this.executor = Executors.newSingleThreadExecutor();
			this.messages = CompletableFuture.supplyAsync(
				() -> receive(expectedMessages),
				executor
			);
		}

		private List<TransportMessage> receive(int expectedMessages)
		{
			try (Socket socket = server.accept())
			{
				socket.setSoTimeout(5_000);
				String helloFrame = FrameIo.read(socket.getInputStream());
				TransportMessage helloMessage = codec.decode(helloFrame);
				if (!(helloMessage instanceof TransportMessage.Hello))
				{
					throw new AssertionError("Expected hello frame");
				}
				TransportMessage.Hello hello = (TransportMessage.Hello) helloMessage;
				FrameIo.write(
					socket.getOutputStream(),
					codec.encode(new TransportMessage.HelloAck(
						hello.getEventProtocol(),
						hello.getEventVersion(),
						hello.getCapabilities()
					))
				);

				List<TransportMessage> received = new ArrayList<>();
				while (received.size() < expectedMessages)
				{
					String frame = FrameIo.read(socket.getInputStream());
					if (frame == null)
					{
						throw new AssertionError("Publisher closed before expected frames arrived");
					}
					received.add(codec.decode(frame));
				}
				return received;
			}
			catch (IOException failure)
			{
				throw new RuntimeException(failure);
			}
		}

		private List<TransportMessage> awaitMessages()
			throws InterruptedException, ExecutionException, TimeoutException
		{
			return messages.get(5, TimeUnit.SECONDS);
		}

		@Override
		public void close() throws IOException
		{
			server.close();
			executor.shutdownNow();
		}
	}
}
