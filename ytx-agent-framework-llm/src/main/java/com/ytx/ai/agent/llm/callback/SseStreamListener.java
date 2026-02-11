package com.ytx.ai.agent.llm.callback;

import com.ytx.ai.agent.llm.vo.ChatCompletionResponse;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Function;

@Setter
@Slf4j
@Builder
public class SseStreamListener implements StreamCallback{

    SseEmitter sseEmitter;
    Function<ChatCompletionResponse,Object> replyConsumer;

    @Override
    public void onDelta(String deltaText) {
        System.out.println("deltaText:"+deltaText);
    }

    @Override
    public void onCompleted(String fullText) {
        System.out.println("fullText:"+fullText);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("error:"+throwable);
    }
}
