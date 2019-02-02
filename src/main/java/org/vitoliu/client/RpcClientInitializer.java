package org.vitoliu.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.vitoliu.protocol.Request;
import org.vitoliu.protocol.Response;
import org.vitoliu.protocol.RpcDecoder;
import org.vitoliu.protocol.RpcEncoder;

/**
 *
 * @author yukun.liu
 * @since 30 一月 2019
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(new RpcEncoder(Request.class));
		pipeline.addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0));
		pipeline.addLast(new RpcDecoder(Response.class));
		pipeline.addLast(new RpcClientHandler());
	}
}
