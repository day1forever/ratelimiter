package com.ratelimiter.dao;

import com.ratelimiter.mode.DurationUnit;
import com.ratelimiter.mode.Limit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisDaoTest {
    @Mock
    private JedisPool jedisPool;
    @Mock
    private Jedis jedis;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getLimitInfo_NotExisting_Pass() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.llen(anyString())).thenReturn(0L);

        RedisDao dao = new RedisDao(jedisPool);
        Optional<Limit> limit = dao.getLimitInfo("userId", "apiName");
        verify(jedis, times(1)).llen(anyString());
        assertThat(!limit.isPresent());
    }

    @Test
    public void getLimitInfo_Existing_Pass() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.llen(anyString())).thenReturn(3L);
        List<String> list = new ArrayList<>();
        list.add("100");
        list.add("60");
        list.add("Second");
        when(jedis.lrange(anyString(), anyLong(), anyLong())).thenReturn(list);

        RedisDao dao = new RedisDao(jedisPool);
        String userId = "userId", apiName = "apiName";
        Optional<Limit> limitInfo = dao.getLimitInfo(userId, apiName);
        verify(jedis, times(1)).llen(anyString());
        verify(jedis, times(1)).lrange(anyString(), anyLong(), anyLong());
        assertThat(limitInfo.isPresent());
        Limit limit = limitInfo.get();
        assertThat(limit.getApiName().equals(apiName));
        assertThat(limit.getUserId().equals(userId));
        assertThat(limit.getLimitNum() == Integer.parseInt(list.get(0)));
        assertThat(limit.getDuration() == Integer.parseInt(list.get(1)));
        assertThat(limit.getDurationUnit() == DurationUnit.from(list.get(2)));
    }

    @Test(expected = RuntimeException.class)
    public void getLimitInfo_Exception_Pass() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.llen(anyString())).thenThrow(new RuntimeException("Test Runtime Exception"));

        RedisDao dao = new RedisDao(jedisPool);
        dao.getLimitInfo("userId", "apiName");
    }

    @Test
    public void createLimitInfo_HappyCase_Pass() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.multi()).thenReturn(Mockito.mock(Transaction.class));

        Limit limit = Limit.builder().apiName("apiName").userId("userId")
                .limitNum(100).duration(60).unit(DurationUnit.Minite).build();
        RedisDao dao = new RedisDao(jedisPool);
        dao.createUserLimitInfo(limit);
    }

    @Test(expected = RuntimeException.class)
    public void createLimitInfo_Exception_Pass() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.multi()).thenThrow(new RuntimeException("Test Runtime Exception"));

        RedisDao dao = new RedisDao(jedisPool);
        dao.createUserLimitInfo(Mockito.mock(Limit.class));
    }
}
