package ai.enpasos.muzero.platform.agent.rational.async;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class JustInference {

    @Autowired
    ModelService inferenceDispatcher;


    public @Nullable List<NetworkIO> initialInferenceListDirect(List<Game> games) {

        List<NetworkIO> result = new ArrayList<>();
        CompletableFuture<NetworkIO>[]  futures = games.stream().map(g ->
            inferenceDispatcher.initialInference(g)
        ).toArray(CompletableFuture[]::new);

        // wait for all futures to complete
        CompletableFuture.allOf(futures).join();


        // collect games from futures
        for (CompletableFuture<NetworkIO> future : futures) {
            try {
                result.add(future.get());
            } catch (InterruptedException e) {


                log.warn("Interrupted!", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new MuZeroException(e);
            }
        }

        return result;

    }

}
