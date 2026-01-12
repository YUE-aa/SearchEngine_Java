package jmu.net.search.controller;

import jmu.net.search.vo.ResultVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口 - 专门给客户端做心跳检测，无日志、无检索逻辑，不产生test日志
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping("/check")
    public ResultVo checkServerStatus() {
        // 适配你的ResultVo.success（消息+数据，数据传null即可）
        return ResultVo.success("服务器在线", null);
    }
}