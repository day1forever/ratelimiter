package com.ratelimiter.mode;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

@JsonDeserialize(builder = Limit.Builder.class)
@AllArgsConstructor
@Value
public class Limit {
    // split limit window into 100 fixed window
    public static final int FIXED_WINDOW_NUM = 100;

    @JsonProperty("limitNum")
    private int limitNum;
    // duration for limitation window
    @JsonProperty("duration")
    private int duration;
    @JsonProperty("durationUnit")
    private DurationUnit durationUnit;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("apiName")
    private String apiName;

    @JsonPOJOBuilder(withPrefix = "")
    @Accessors(fluent = true)
    public static class Builder {
        @JsonProperty("limitNum")
        int limitNum;

        public Limit.Builder limitNum(int limitNum) {
            this.limitNum = limitNum;
            return this;
        }

        @JsonProperty("duration")
        int duration;

        public Limit.Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        @JsonProperty("durationUnit")
        DurationUnit durationUnit;

        public Limit.Builder unit(DurationUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        @JsonProperty("userId")
        String userId;

        public Limit.Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        @JsonProperty("apiName")
        String apiName;

        public Limit.Builder apiName(String apiName) {
            this.apiName = apiName;
            return this;
        }


        public Limit build() {
            return new Limit(limitNum, duration, durationUnit, userId, apiName);
        }

        @JsonIgnore
        public Limit.Builder copy(Limit limit) {
            return limitNum(limit.getLimitNum()).duration(limit.getDuration()).unit(limit.getDurationUnit());
        }
    }

    /**
     * Create a new builder.
     */
    public static Limit.Builder builder() {
        return new Limit.Builder();
    }

    @JsonIgnore
    public int getFixedWindowInMs() {
        return getWindowInMs() / FIXED_WINDOW_NUM;
    }

    @JsonIgnore
    public int getWindowInMs() {
        return duration * 1000 * (durationUnit == DurationUnit.Minite ? 60 : 1);
    }

    @JsonIgnore
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("limitNum=" + limitNum)
                .append(", duration=" + duration)
                .append(", durationUnitUnit=" + durationUnit.name());
        if (userId != null) {
            sb.append(", userId=" + userId);
        }
        if (apiName != null) {
            sb.append(", apiName=" + apiName);
        }
        return sb.toString();
    }
}

