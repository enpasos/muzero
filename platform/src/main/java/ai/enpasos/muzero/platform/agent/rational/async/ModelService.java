package ai.enpasos.muzero.platform.agent.rational.async;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.Node;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Component
@Slf4j
public class ModelService {

    public static final String INTERRUPTED = "Interrupted!";
    @Autowired
    ModelQueue modelQueue;

    @Autowired
    EnableSchedulingConfig enableSchedulingConfig;

//    @Async()
//    public CompletableFuture<List<NetworkIO>> initialInference(List<Game> games) {
//
//        List<InitialInferenceTask> tasks = new ArrayList<>();
//        games.forEach(game -> tasks.add(new InitialInferenceTask(game)));
//        tasks.forEach(task -> modelQueue.addInitialInferenceTask(task));
//
//
//        while (tasks.stream().anyMatch(task -> !task.isDone())) {
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                log.warn(INTERRUPTED, e);
//                Thread.currentThread().interrupt();
//            }
//        }
//        tasks.forEach(task -> modelQueue.removeInitialInferenceTask(task));
//        List<NetworkIO> results = tasks.stream().map(InitialInferenceTask::getNetworkOutput).collect(Collectors.toList());
//        return CompletableFuture.completedFuture(results);
//
//    }


    @Async()
    public CompletableFuture<Void> loadLatestModel() {
        ControllerTask task = new ControllerTask(ControllerTaskType.loadLatestModel);
        return handleControllerTask(task);
    }

    @Async()
    public CompletableFuture<Void> loadLatestModelOrCreateIfNotExisting() {
        ControllerTask task = new ControllerTask(ControllerTaskType.loadLatestModelOrCreateIfNotExisting);
        return handleControllerTask(task);
    }

    @NotNull
    private CompletableFuture<Void> handleControllerTask(ControllerTask task) {
        modelQueue.addControllerTask(task);
        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                log.warn(INTERRUPTED, e);
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeControllerTask(task);
        return CompletableFuture.completedFuture(null);
    }


    @Async()
    public CompletableFuture<NetworkIO> initialInference(Game game) {
        InitialInferenceTask task = new InitialInferenceTask(game);
        modelQueue.addInitialInferenceTask(task);


        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                log.warn(INTERRUPTED, e);
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeInitialInferenceTask(task);


        return CompletableFuture.completedFuture(task.getNetworkOutput());

    }


    @Async()
    public CompletableFuture<NetworkIO> recurrentInference(List<Node> searchPath) {
        RecurrentInferenceTask task = new RecurrentInferenceTask(searchPath);
        modelQueue.addRecurrentInferenceTask(task);

        while (!task.isDone()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                log.warn(INTERRUPTED, e);
                Thread.currentThread().interrupt();
            }
        }
        modelQueue.removeRecurrentInferenceTask(task);

    //    throw new MuZeroException("hallo");

        return CompletableFuture.completedFuture(task.getNetworkOutput());
    }

    @Async()
    public CompletableFuture<Void> trainModel() {
        ControllerTask task = new ControllerTask(ControllerTaskType.trainModel);
        return handleControllerTask(task);
    }
}
