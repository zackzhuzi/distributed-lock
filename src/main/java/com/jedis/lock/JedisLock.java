package com.jedis.lock;

import redis.clients.jedis.JedisPool;

/**
 * Jedis实现分布式锁
 * 
 * @author yuzhu.peng
 *
 */
public class JedisLock {
	private static final String PREFIX = "redis.lock.";

	// 超时时间:默认30s
	private int waitTimeOut = 30;
	// 锁的过期时间：默认1分钟
	private int lockExpireTime = 60;
	// 锁的名字
	private String lockName;

	private JedisPool jedisPool;

	public JedisLock(String lockName) {
		this.lockName = PREFIX + lockName;
	}

	public JedisLock(String lockName, int waitTimeOut, int lockExpireTime) {
		this.lockName = lockName;
		this.waitTimeOut = waitTimeOut;
		this.lockExpireTime = lockExpireTime;
	}

	public JedisLock(JedisPool jedisPool, String lockName, int waitTimeOut,
			int lockExpireTime) {
		this.jedisPool = jedisPool;
		this.lockName = PREFIX + lockName;
		this.waitTimeOut = waitTimeOut;
		this.lockExpireTime = lockExpireTime;
	}

	public static void main(String[] args) {
		try {
			JedisPool jedisPool2 = JedisPoolFactory.getJedisPool();
			JedisPoolFactory.poolTest(jedisPool2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
