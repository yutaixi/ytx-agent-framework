package com.ytx.ai.web.vo;

import lombok.Data;

@Data
public class Response<T> {

    /**
     * code
     */
    private Integer code;

    /**
     * message
     */
    private String msg;

    /**
     * data
     */
    private T data;

    public Response() {
    }

    public Response(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Response(T data) {
        this.code  = Status.SUCCESS.getCode();
        this.data = data;
    }

    public Response(Status status, T data) {
        if (status != null) {
            this.code = status.getCode();
            this.data = data;
        }
    }

    public Response(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * Call this function if there is success
     * @param data data
     * @param <T> type
     * @return resule
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(data);
    }

    /**
     * 返回错误消息
     * @return
     */
    public static Response error()
    {
        return Response.error("操作失败");
    }

    /**
     * 返回错误消息
     * @param msg 返回内容
     * @return 警告消息
     */
    public static Response error(String msg)
    {
        Response response =new Response();
        response.setMsg(msg);
        response.setCode(Status.SYSTEM_EXCEPTION.getCode());
        return response;
    }

}
