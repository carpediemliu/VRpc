package org.vitoliu.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
@AllArgsConstructor
@NoArgsConstructor
public class RpcEncoder extends MessageToByteEncoder {

	private Class<?> genericClass;

	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
		if (genericClass.isInstance(o)) {
			byte[] data = SerializationUtil.serialize(o);
			byteBuf.writeInt(data.length);
			byteBuf.writeBytes(data);
		}
	}
}
