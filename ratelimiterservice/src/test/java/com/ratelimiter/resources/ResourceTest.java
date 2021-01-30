package com.ratelimiter.resources;

import com.ratelimiter.dao.LimitDao;
import com.ratelimiter.mode.DurationUnit;
import com.ratelimiter.mode.Limit;
import com.ratelimiter.utils.RenderableException;
import com.ratelimiter.utils.ValidationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceTest {
    @Mock
    private LimitDao limitDao;

    private Resource resource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        resource = new Resource(limitDao);
    }

    @Test
    public void getLimitInfo_HappyCase_Pass() throws Exception {
        Limit limit = Limit.builder().apiName("apiName").userId("userId")
                .limitNum(100).duration(60).unit(DurationUnit.Minite).build();
        when(limitDao.getLimitInfo(anyString(), anyString())).thenReturn(Optional.of(limit));

        Limit res = resource.getLimitInfo("userId", "apiName");
        verify(limitDao, times(1)).getLimitInfo(anyString(), anyString());
        assertThat(res.getApiName().equals(limit.getApiName()));
        assertThat(res.getUserId().equals(limit.getUserId()));
        assertThat(res.getLimitNum() == limit.getLimitNum());
        assertThat(res.getDuration() == limit.getDuration());
        assertThat(res.getDurationUnit() == limit.getDurationUnit());
    }

    @Test(expected = RenderableException.class)
    public void getLimitInfo_NonExisting_Exception() throws Exception {
        when(limitDao.getLimitInfo(anyString(), anyString())).thenReturn(Optional.empty());

        resource.getLimitInfo("userId", "apiName");
    }

    @Test(expected = ValidationException.class)
    public void getLimitInfo_EmptyApiName_Exception() throws Exception {
        resource.getLimitInfo("userId", "");
    }

    @Test
    public void throttleRequest_Throttle_Pass() throws Exception {
        Limit limit = Limit.builder().apiName("apiName").userId("userId")
                .limitNum(100).duration(60).unit(DurationUnit.Minite).build();
        long now = Instant.now().toEpochMilli();
        long key = now / limit.getFixedWindowInMs();
        Map<String, String> map = new HashMap<>();
        map.put("" + key, "" + 100);

        when(limitDao.getRequestCountInfo(anyString(), anyString())).thenReturn(map);
        when(limitDao.getLimitInfo(anyString(), anyString())).thenReturn(Optional.of(limit));

        boolean res = resource.throttleRequest("userId", "apiName");
        assertThat(res);
    }

    @Test
    public void throttleRequest_NotThrottle_Pass() throws Exception {
        Limit limit = Limit.builder().apiName("apiName").userId("userId")
                .limitNum(100).duration(60).unit(DurationUnit.Minite).build();
        long now = Instant.now().toEpochMilli();
        long key = now / limit.getFixedWindowInMs() - Limit.FIXED_WINDOW_NUM - 1;
        Map<String, String> map = new HashMap<>();
        map.put("" + key, "" + 100);

        when(limitDao.getRequestCountInfo(anyString(), anyString())).thenReturn(map);
        when(limitDao.getLimitInfo(anyString(), anyString())).thenReturn(Optional.of(limit));

        boolean res = resource.throttleRequest("userId", "apiName");
        assertThat(!res);
    }

    @Test(expected = ValidationException.class)
    public void throttleRequest_EmptyApiName_Exception() throws Exception {
        resource.throttleRequest("userId", "");
    }

    @Test(expected = RenderableException.class)
    public void throttleRequest_NonExisting_Exception() throws Exception {
        when(limitDao.getLimitInfo(anyString(), anyString())).thenReturn(Optional.empty());
        resource.throttleRequest("userId", "apiName");
    }

    @Test(expected = RenderableException.class)
    public void throttleRequest_IncorrectDuration_Exception() throws Exception {
        Limit limit = Limit.builder().apiName("apiName").userId("userId")
                .limitNum(100).duration(0).unit(DurationUnit.Minite).build();
        when(limitDao.getLimitInfo(anyString(), anyString())).thenReturn(Optional.of(limit));
        resource.throttleRequest("userId", "apiName");
    }

    @Test
    public void whitelistApi_HappyCase_Pass() throws Exception {
        when(limitDao.addApiNames(anyString())).thenReturn(true);
        resource.registerApiName("apiName");
    }

    @Test(expected = ValidationException.class)
    public void whitelistApi_EmptyApiName_Pass() throws Exception {
        resource.registerApiName("");
    }

}
