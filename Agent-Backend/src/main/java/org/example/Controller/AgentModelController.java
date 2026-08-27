package org.example.Controller;

import org.example.common.Result;
import org.example.config.model.ModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/model")
public class AgentModelController {

    @Autowired
    private ModelProperties modelProperties;

    @GetMapping("/list")
    public Result<List<String>> list(){
        List<ModelProperties.ChatModelConfig> chatList = modelProperties.getChat();
        List<String> modelTypeList = new ArrayList<>();
        for (ModelProperties.ChatModelConfig chat : chatList){
            // 返回路由 name（deepseek/kimi），与 agent_def.model_type、/agent/{code}/chat 的 modelType 参数口径一致
            modelTypeList.add(chat.getName());
        }
        return Result.success(modelTypeList);
    }


}
