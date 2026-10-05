package com.kauan.gamelog;

import com.kauan.gamelog.recommendation.Curator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;

/** No lugar do modelo: desligado, a menos que o teste diga o que responder (e desligue no fim). */
@Primary
@TestConfiguration(proxyBeanMethods = false)
public class FakeCurator implements Curator {
    private volatile Function<Input, List<Pick>> answer;
    private final AtomicInteger calls = new AtomicInteger();

    public void answerWith(Function<Input, List<Pick>> answer) {
        this.answer = answer;
        calls.set(0);
    }

    public void turnOff() {
        answer = null;
    }

    public int calls() {
        return calls.get();
    }

    @Override
    public String name() {
        return "Teste";
    }

    @Override
    public boolean enabled() {
        return answer != null;
    }

    @Override
    public List<Pick> curate(Input input) {
        calls.incrementAndGet();
        return answer.apply(input);
    }
}
