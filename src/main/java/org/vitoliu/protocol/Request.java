package org.vitoliu.protocol;

import lombok.Data;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
@Data
public class Request<T> {

	private String requestId;

	private String className;

	private String methodName;

	private Class<?>[] parameterTypes;

	private T[] parameters;
}
