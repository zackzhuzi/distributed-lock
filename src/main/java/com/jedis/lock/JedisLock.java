package com.jedis.lock;

import java.io.InputStream;
import java.util.Properties;

import redis.clients.jedis.Jedis;

/**
 * Jedis实现分布式锁
 * 
 * @author yuzhu.peng
 *
 */
public class JedisLock {
    private static final String REDISCONFIGFILE = "redis.properties";

    // 超时时间:默认30s
    private int waitTimeOut = 30;
    // 锁的过期时间：默认1分钟
    private int lockExpireTime = 60;

    private static Jedis jedis;

    static {
        try {
            Properties pps = new Properties();
            InputStream inputStream = JedisLock.class.getClassLoader()
                    .getResourceAsStream(REDISCONFIGFILE);
            pps.load(inputStream);
            String host = pps.getProperty("redis.host");
            int port = Integer.parseInt(pps.getProperty("redis.port"));
            jedis = new Jedis(host, port);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

        }

        // TODO
        // connect
    }

    // 等待间隔：默认

    public JedisLock() {
        // TODO Auto-generated constructor stub
    }

    public JedisLock(String lockName, int waitTimeOut, int lockExpireTime) {
        // TODO Auto-generated constructor stub
    }

    public JedisLock(String host, String password, int port, String lockName,
            int waitTimeOut, int lockExpireTime) {
        // TODO Auto-generated constructor stub
    }

}
