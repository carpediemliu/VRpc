package org.vitoliu.registry;

/**
 *
 * @author yukun.liu
 * @since 29 一月 2019
 */
public interface ZkConstants {

	int ZK_SESSION_TIMEOUT = 5000;

	String ZK_REGISTRY_PATH = "/registry";

	String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}
