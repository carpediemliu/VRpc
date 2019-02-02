package org.vitoliu.client;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitoliu.protocol.Request;
import org.vitoliu.protocol.Response;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<Response> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

	private ConcurrentMap<String, RpcFuture> pendingRpc = Maps.newConcurrentMap();

	@Getter
	private volatile Channel channel;

	@Getter
	private SocketAddress remoteAddr;


	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
		String requestId = response.getRequestId();
		RpcFuture future = pendingRpc.get(requestId);
		if (future != null) {
			pendingRpc.remove(requestId);
			future.done(response);
		}
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		channel = ctx.channel();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		this.remoteAddr = channel.remoteAddress();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOGGER.error("client caught exception :{}", cause);
		ctx.close();
	}

	void close() {
		channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}

	public <T> RpcFuture<T> sendRequest(Request<T> request) {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		RpcFuture requestRpcFuture = new RpcFuture<>(request);
		pendingRpc.put(request.getRequestId(), requestRpcFuture);
		channel.writeAndFlush(request).addListener((future -> countDownLatch.countDown()));
		try {
			countDownLatch.await(10, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			LOGGER.error("close channel caught exception : {}", e);
		}
		return requestRpcFuture;
	}
}
