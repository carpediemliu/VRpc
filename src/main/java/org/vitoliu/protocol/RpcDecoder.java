package org.vitoliu.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
@AllArgsConstructor
@NoArgsConstructor
public class RpcDecoder extends ByteToMessageDecoder {

	private Class<?> genericClass;

	private static final int READABLE_BYTES = 4;

	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
		int readableBytes = byteBuf.readableBytes();
		if (readableBytes < READABLE_BYTES) {
			return;
		}
		byteBuf.markReaderIndex();
		int dataLength = byteBuf.readInt();
		if (readableBytes < dataLength) {
			byteBuf.resetReaderIndex();
			return;
		}
		byte[] data = new byte[dataLength];
		byteBuf.readBytes(data);
		Object obj = SerializationUtil.deserialize(data, genericClass);
		list.add(obj);
	}
}
