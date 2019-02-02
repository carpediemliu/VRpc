package org.vitoliu.client;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.vitoliu.registry.Registry;

/**
 *
 * @author yukun.liu
 * @since 30 一月 2019
 */
public class RpcClient {

	private String serverAddr;

	private Registry serviceDiscovery;

	private static ListeningExecutorService listeningExecutorService = MoreExecutors.newDirectExecutorService();


	static void submit(Runnable task){
		listeningExecutorService.execute(task);
	}
}
