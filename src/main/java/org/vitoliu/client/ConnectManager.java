package org.vitoliu.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
public class ConnectManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectManager.class);

	private static final ConnectManager INSTANCE = new ConnectManager();

	private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

	private final ThreadPoolExecutor executor;

	private CopyOnWriteArrayList<RpcClientHandler> connectedHandler = Lists.newCopyOnWriteArrayList();

	private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = Maps.newConcurrentMap();

	private ReentrantLock lock = new ReentrantLock();

	private Condition connected = lock.newCondition();

	private long connectTimeoutMills = 6000;

	private AtomicInteger roundRobin = new AtomicInteger(0);

	private volatile boolean isRunning = true;


	private static final Splitter COLON_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults();

	private ConnectManager() {
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("manager-pool-%d").build();
		this.executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 10, TimeUnit.MINUTES.toMillis(10), TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(65536), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
	}

	public static ConnectManager getInstance() {
		return INSTANCE;
	}

	public void updateConnectedServer(List<String> allServerAddress) {
		if (CollectionUtils.isEmpty(allServerAddress)) {
			Set<InetSocketAddress> serverNodeSet = Sets.newHashSet();
			for (String address : allServerAddress) {
				List<String> hostAndPort = COLON_SPLITTER.splitToList(address);
				serverNodeSet.add(new InetSocketAddress(hostAndPort.get(0), Integer.parseInt(hostAndPort.get(1))));
			}
			// add new server nodes
			for (final InetSocketAddress nodeAddr : serverNodeSet) {
				if (!connectedServerNodes.containsKey(nodeAddr)) {
					connectServerNode(nodeAddr);
				}
			}

			//close and remove invalid server nodes
			for (RpcClientHandler handler : connectedHandler) {
				SocketAddress remoteAddr = handler.getRemoteAddr();
				if (!serverNodeSet.contains(remoteAddr)) {
					LOGGER.info("remove invalid server node :{}", remoteAddr.toString());
					RpcClientHandler rpcClientHandler = connectedServerNodes.get(remoteAddr);
					if (rpcClientHandler != null) {
						handler.close();
					}
					connectedServerNodes.remove(remoteAddr);
					connectedHandler.remove(handler);
				}
			}
		}
		else {
			//no available server node(all server nodes are down)
			LOGGER.error("no available server node, all server nodes are down!!!");
			for (final RpcClientHandler connectedServerHandler : connectedHandler) {
				SocketAddress remoteAddr = connectedServerHandler.getRemoteAddr();
				RpcClientHandler handler = connectedServerNodes.get(remoteAddr);
				handler.close();
				connectedServerNodes.remove(connectedServerHandler);
			}
			connectedHandler.clear();
		}
	}

	private void connectServerNode(InetSocketAddress nodeAddr) {
		executor.submit(() -> {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(eventLoopGroup)
					.channel(NioSocketChannel.class)
					.handler(new RpcClientInitializer());
			ChannelFuture channelFuture = bootstrap.connect(nodeAddr);
			channelFuture.addListener((future) -> {
				if (future.isSuccess()) {
					LOGGER.debug("Successfully connect to remote server . remote addr = {}", nodeAddr);
					RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
					addHandler(handler);
				}
			});
		});
	}

	private void addHandler(RpcClientHandler handler) {
		connectedHandler.add(handler);
		InetSocketAddress remoteAddr = (InetSocketAddress) handler.getChannel().remoteAddress();
		connectedServerNodes.put(remoteAddr, handler);
		signalAvailableHandler();
	}

	private void signalAvailableHandler() {
		lock.lock();
		try {
			connected.signalAll();
		}
		finally {
			lock.unlock();
		}
	}
}
