package org.vitoliu.client;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
public interface Callback<T> {

	void onSuccess(T result);

	void onFailed(Throwable t);

}
