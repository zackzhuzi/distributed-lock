package com.jedis.lock;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis实现分布式锁
 * 
 * @author yuzhu.peng
 *
 */
public class JedisPoolFactory {
    private static final String REDISCONFIGFILE = "redis.properties";
    private static JedisPool jedisPool;

    static {
        try {
            Properties pps = new Properties();
            InputStream inputStream = JedisPoolFactory.class.getClassLoader()
                    .getResourceAsStream(REDISCONFIGFILE);
            pps.load(inputStream);
            String host = pps.getProperty("redis.host");
            int port = Integer.parseInt(pps.getProperty("redis.port"));
            int maxTotal = Integer.parseInt(pps
                    .getProperty("redis.pool.maxActive"));
            int maxIdle = Integer.parseInt(pps
                    .getProperty("redis.pool.maxIdle"));
            long maxWaitMillis = Long.parseLong(pps
                    .getProperty("redis.pool.maxWaitMillis"));
            long minEvictableIdleTimeMillis = Long.parseLong(pps
                    .getProperty("redis.pool.minEvictableIdleTimeMillis"));
            long timeBetweenEvictionRunsMillis = Long.parseLong(pps
                    .getProperty("redis.pool.timeBetweenEvictionRunsMillis"));

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(maxTotal); // 最大分配的对象数
            config.setMaxIdle(maxIdle); // 最大能够保持idel状态的对象数
            config.setMaxWaitMillis(maxWaitMillis); // 当池内没有返回对象时，最大等待时间
            // 表示一个对象至少停留在idle状态的最短时间，
            // 然后才能被idle object
            // evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
            config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
            config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis); // 表示idle
                                                                                    // object
                                                                                    // evitor两次扫描之间要sleep的毫秒数
            config.setTestOnBorrow(true);
            jedisPool = new JedisPool(config, host, port, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JedisPool getJedisPool() {
        return jedisPool;
    }

    /**
     * test JedisPool
     * 
     * @param jedisPool
     */
    public static void poolTest(final JedisPool jedisPool) {
        int count = 1000;
        ExecutorService executorService = new ThreadPoolExecutor(20, 50, 100,
                TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(count));
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            final int inx = i;
            executorService.execute(new Runnable() {

                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    Jedis jedis = null;
                    boolean needReturn = true;
                    try {
                        jedis = jedisPool.getResource();
                        jedis.set("test" + inx, inx + "");
                        System.out.println(Thread.currentThread().getName()
                                + " test" + inx + "=" + jedis.get("test" + inx));
                    } catch (Exception e) {
                        if (jedis != null) {
                            needReturn = false;
                            jedisPool.returnBrokenResource(jedis);
                        }
                        e.printStackTrace();
                    } finally {
                        if (needReturn && jedis != null) {
                            jedisPool.returnResource(jedis);
                        }
                    }
                    latch.countDown();
                }
            });
        }

        try {
            System.out
                    .println(Thread.currentThread().getName() + " is waiting");
            latch.await();
            System.out.println(Thread.currentThread().getName()
                    + " done waiting");
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            JedisPool jedisPool = getJedisPool();
            poolTest(jedisPool);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
