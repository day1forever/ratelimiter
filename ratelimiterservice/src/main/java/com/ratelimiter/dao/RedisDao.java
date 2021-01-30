package com.ratelimiter.dao;

import com.ratelimiter.mode.Limit;
import com.ratelimiter.utils.LimitInfoUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class RedisDao implements LimitDao {
    private final JedisPool jedisPool;

    public RedisDao(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Optional<Limit> getLimitInfo(String userId, String apiName) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String key = LimitInfoUtil.getUserLimitInfoKey(userId, apiName);
            long len = jedis.llen(key);
            if (len == 0) {
                return Optional.empty();
            }

            List<String> list = jedis.lrange(key, 0, len - 1);
            return Optional.of(LimitInfoUtil.getLimitBuilder(list).userId(userId).apiName(apiName).build());
        } catch (Exception e) {
            log.error("Exception in get limit info", e);
            throw e;
        }
    }

    @Override
    public void createUserLimitInfo(Limit userLimitInfo) {
        try (Jedis jedis = jedisPool.getResource()) {
            String[] limitInfoInArr = LimitInfoUtil.getLimitInfoInArr(userLimitInfo);

            final String key = LimitInfoUtil.getUserLimitInfoKey(userLimitInfo.getUserId(), userLimitInfo.getApiName());
            Transaction transaction = jedis.multi();
            transaction.del(key);
            transaction.rpush(key, limitInfoInArr[0], limitInfoInArr[1], limitInfoInArr[2]);
            transaction.exec();
        } catch (Exception e) {
            log.error("Exception in create user limit info: " + userLimitInfo, e);
            throw e;
        }
    }

    @Override
    public boolean addApiNames(String apiName) {
        try (Jedis jedis = jedisPool.getResource()) {
            long res = jedis.sadd(LimitInfoUtil.WHITE_LISTED_API_NAME_KEY, apiName);
            return res == 1;
        } catch (Exception e) {
            log.error("Exception in white listing apiName: " + apiName, e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getRequestCountInfo(String userId, String apiName) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String key = LimitInfoUtil.getRequestInfoKey(userId, apiName);
            return jedis.hgetAll(key);
        } catch (Exception e) {
            log.error("Exception in white listing apiName: " + apiName, e);
            throw e;
        }
    }

    @Override
    public void writeRequestCountInfo(String userId, String apiName, Map<String, String> requestInfo) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String key = LimitInfoUtil.getRequestInfoKey(userId, apiName);
            Transaction transaction = jedis.multi();
            transaction.del(key);
            transaction.hset(key, requestInfo);
            transaction.exec();
        } catch (Exception e) {
            log.error("Exception in white listing apiName: " + apiName, e);
            throw e;
        }
    }
}
