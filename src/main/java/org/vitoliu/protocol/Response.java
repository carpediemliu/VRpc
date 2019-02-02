package org.vitoliu.protocol;

import lombok.Data;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
@Data
public class Response<T> {

	private String requestId;

	private String error;

	private T result;

	public boolean isError() {
		return error != null;
	}
}
