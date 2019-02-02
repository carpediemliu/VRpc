package org.vitoliu.registry;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitoliu.client.ConnectManager;

import org.springframework.util.Assert;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
public class ZkServiceDiscovery implements Registry<ZooKeeper>, ZkConstants {

	private final Logger logger = LoggerFactory.getLogger(ZkServiceDiscovery.class);

	private CountDownLatch countDownLatch = new CountDownLatch(1);

	private volatile List<String> dataList = Lists.newArrayList();


	private final String zkAddress;

	private final ZooKeeper zooKeeper;

	public ZkServiceDiscovery(String zkAddress) {
		Assert.hasText(zkAddress, "zkAddress must be text!");
		this.zkAddress = zkAddress;
		zooKeeper = register();
		Preconditions.checkNotNull(zooKeeper, "connect zkServer Failed!!");
		watchNode(zooKeeper);
	}

	private void watchNode(ZooKeeper zk) {
		try {
			List<String> nodeList = zk.getChildren(ZK_REGISTRY_PATH, event -> {
				if (event.getType() == NodeChildrenChanged) {
					watchNode(zk);
				}
			});
			List<String> dataList = Lists.newArrayList();
			for (String node : nodeList) {
				byte[] bytes = zk.getData(ZK_REGISTRY_PATH + "/" + node, false, null);
				dataList.add(new String(bytes));
			}
			this.dataList = dataList;
			updateConnectedServer();
		}
		catch (Exception e) {
			logger.error("watch zk status error.", e);
		}
	}

	private void updateConnectedServer() {
		ConnectManager.getInstance().updateConnectedServer(this.dataList);
	}

	@Override
	public ZooKeeper register() {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(zkAddress, ZK_SESSION_TIMEOUT, watchedEvent -> {
				if (watchedEvent.getState() == SyncConnected) {
					countDownLatch.countDown();
				}
			});
			countDownLatch.await();
		}
		catch (Exception e) {
			logger.error("connect zkServer failed.", e);
		}
		return zk;
	}
}
