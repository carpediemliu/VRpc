package org.vitoliu.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
public interface Serde<T> {


	/**
	 * 序列化
	 * @param input
	 * @return
	 */
	default byte[] serialize(T input) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Hessian2Output hessian2Output = new Hessian2Output(byteArrayOutputStream);
		try {
			hessian2Output.writeObject(input);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			closeOutput(hessian2Output);
		}
		return byteArrayOutputStream.toByteArray();
	}


	T deserialize(byte[] input);

	default void closeOutput(Hessian2Output hessian2Output) {
		try {
			hessian2Output.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	default void closeInput(Hessian2Input hessian2Input) {
		try {
			hessian2Input.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
