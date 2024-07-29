package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;    // 使用万能的Object类型用来包含原有的数据，这样对原有的代码没有侵入性
}
