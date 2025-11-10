package org.example.user.pojo.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Result {

    private Integer code; // 0成功，1失败
    private String msg;
    private Map<String, Object> data = new HashMap<>();

    // 成功
    public static Result ok(String msg) {
        Result result = new Result();
        result.setCode(0);
        result.setMsg(msg);
        return result;
    }

    // 失败
    public static Result fail(String msg) {
        Result result = new Result();
        result.setCode(1);
        result.setMsg(msg);
        return result;
    }

    // 添加数据
    public Result put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public Integer getCode() {
        return code;
    }
    public void setCode(Integer code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public Map<String, Object> getData() {
        return data;
    }
}