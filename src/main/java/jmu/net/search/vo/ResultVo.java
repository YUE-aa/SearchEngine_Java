package jmu.net.search.vo;

/**
 * 接口统一返回结果封装
 */
public class ResultVo {
    private Integer code;
    private String msg;
    private Object data;

    // 成功响应
    public static ResultVo success(String msg, Object data) {
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(200);
        resultVo.setMsg(msg);
        resultVo.setData(data);
        return resultVo;
    }

    // 失败响应
    public static ResultVo error(String msg) {
        ResultVo resultVo = new ResultVo();
        resultVo.setCode(500);
        resultVo.setMsg(msg);
        resultVo.setData(null);
        return resultVo;
    }

    // Getter & Setter
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}