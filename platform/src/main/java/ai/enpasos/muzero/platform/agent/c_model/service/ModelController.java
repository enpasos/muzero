package ai.enpasos.muzero.platform.agent.c_model.service;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.NDScope;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.c_model.ModelState;
import ai.enpasos.muzero.platform.agent.c_model.Network;
import ai.enpasos.muzero.platform.agent.c_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.c_model.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.c_model.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.c_model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.c_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.d_experience.Game;
import ai.enpasos.muzero.platform.agent.d_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.b_planning.Node;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.c_model.Network.getEpoch;
import static ai.enpasos.muzero.platform.agent.c_model.djl.NetworkHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.agent.c_model.service.ModelService.INTERRUPTED;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;
import static ai.enpasos.muzero.platform.common.FileUtils.mkDir;
import static java.util.Map.entry;


@Component
@Slf4j

public class ModelController {

    @Autowired
    ModelQueue modelQueue;

    @Autowired
    MuZeroConfig config;

    private Network network;


    NDManager nDManager;


    @Autowired
    NetworkHelper networkHelper;

    @Autowired
    GameBuffer gameBuffer;

   // int epoch;

    @Autowired
    private ModelState modelState;



    private DurAndMem inferenceDuration = new DurAndMem();


    private NDScope ndScope;


    public void run()    {
        log.info("ModelController started.");
        try {
            while (true) {
                int numParallelInferences = config.getNumParallelInferences();
                controllerTasks();
                initialInferences(numParallelInferences);
                recurrentInferences(numParallelInferences);
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
           // e.printStackTrace();
            log.error("ModelController stopped.");
        }  catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (network != null)
                network.getModel().close();
            if (nDManager != null)
                nDManager.close();
        }

    }

    private static void waitToFillUp(int numParallelInferences, long c, int waitMillis) {
        if (c >0 && c < numParallelInferences) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                log.warn(INTERRUPTED, e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void controllerTasks() throws InterruptedException {

        List<ControllerTask> localControllerTaskList = modelQueue.getControllerTasksNotStarted();

        if (localControllerTaskList.isEmpty());



        for (ControllerTask task : localControllerTaskList) {
            switch (task.getTaskType()) {
                case loadLatestModel:
                    Model model = Model.newInstance(config.getModelName(), Device.gpu());
                    log.info("loadLatestModel with model name {}", config.getModelName());
                    if (task.epoch == -1) {
                        network = new Network(config, model);
                    } else {
                        network = new Network(config, model, Paths.get(config.getNetworkBaseDir()),
                            Map.ofEntries(entry("epoch", task.epoch + "")));;
                    }
                    network = new Network(config, model, Paths.get(config.getNetworkBaseDir()),
                        Map.ofEntries(entry("epoch", task.epoch + "")));
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case loadLatestModelOrCreateIfNotExisting:
                    model = Model.newInstance(config.getModelName(), Device.gpu());
                    if (model.getBlock() == null) {
                        MuZeroBlock block = new MuZeroBlock(config);
                        model.setBlock(block);
                        loadModelParametersOrCreateIfNotExisting(model);
                    }
                    if (task.epoch == -1) {
                        network = new Network(config, model);
                    } else {
                        network = new Network(config, model, Paths.get(config.getNetworkBaseDir()),
                            Map.ofEntries(entry("epoch", task.epoch + "")));;
                    }
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case trainModel:
                      trainNetwork(network.getModel());
                    break;
                case startScope:
                    if (ndScope != null) {
                        ndScope.close();
                    }
                    ndScope = new NDScope();
                    break;
                case endScope:
                    if (ndScope != null) {
                        ndScope.close();
                    }
                    break;
                case shutdown:
                    throw new InterruptedException();
//                case getEpoch:
//                    task.epoch = getEpoch(network.getModel());
//                    break;
            }
            task.setDone(true);
        }

    }


    private void trainNetwork(Model model) {
        try (NDScope nDScope = new NDScope()) {
            if (config.offPolicyCorrectionOn()) {
                determinePRatioMaxForCurrentEpoch(model);
            }

            int epochLocal;
            int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
            boolean withSymmetryEnrichment = true;
            epochLocal = getEpochFromModel(model);
            DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epochLocal);
            int finalEpoch = epochLocal;
            djlConfig.getTrainingListeners().stream()
                .filter(MyEpochTrainingListener.class::isInstance)
                .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = networkHelper.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.setMetrics(new Metrics());

                for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                    try (Batch batch = networkHelper.getBatch(trainer.getManager(), withSymmetryEnrichment)) {
                        log.debug("trainBatch " + m);
                        MyEasyTrain.trainBatch(trainer, batch);
                        trainer.step();
                    }
                }

                // number of action paths
//                int numActionPaths = this.gameBuffer.getBuffer().getNumOfDifferentGames();
//                model.setProperty("NumActionPaths", Double.toString(numActionPaths));
//                log.info("NumActionPaths: " + numActionPaths);

                handleMetrics(trainer, model, epochLocal);

                trainer.notifyListeners(listener -> listener.onEpoch(trainer));
            }
        }
        // epoch = getEpochFromModel(model);
        //System.out.println(">>>>>>> epoch = " + epoch);
       // gameBuffer.setEpoch(epoch);
         modelState.setEpoch(getEpochFromModel(model));
       // return epoch;
//        return epoch * numberOfTrainingStepsPerEpoch;
        //  }
    }

    private void determinePRatioMaxForCurrentEpoch(Model model) {
        int epoch = this.modelState.getEpoch();
        List<Game> games = this.gameBuffer.getGames().stream()
            .filter(game -> game.getGameDTO().getTrainingEpoch() == epoch && game.getPlayTypeKey() == PlayTypeKey.REANALYSE)
            .collect(Collectors.toList());
        double pRatioMax = determinePRatioMax(games);
        log.info("pRatioMaxREANALYSE({}): {}", epoch, pRatioMax);

        List<Game> games2 = this.gameBuffer.getGames().stream()
            .filter(game -> game.getGameDTO().getTrainingEpoch() == epoch && game.getPlayTypeKey() != PlayTypeKey.REANALYSE)
            .collect(Collectors.toList());
        double pRatioMax2 = determinePRatioMax(games2);
        log.info("pRatioMax({}): {}", epoch, pRatioMax2);
    }

    private double determinePRatioMax(List<Game> games) {
        double pRatioMax = games.stream().mapToDouble(Game::getPRatioMax).max().orElse(1.0);
        games.stream().forEach(game -> game.setPRatioMax(pRatioMax));
        return pRatioMax;
    }

    private void handleMetrics(Trainer trainer, Model model, int epoch) {
        Metrics metrics = trainer.getMetrics();


        // mean loss
        List<Metric> ms = metrics.getMetric("train_all_CompositeLoss");
        double meanLoss = ms.stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new);
        model.setProperty("MeanLoss", Double.toString(meanLoss));
        log.info("MeanLoss: " + meanLoss);

        // mean value
        // loss
        double meanValueLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("value_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        gameBuffer.putMeanValueLoss(epoch, meanValueLoss);
        meanValueLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("value_0") && name.contains("value"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty( "MeanValueLoss", Double.toString(meanValueLoss));
        log.info("MeanValueLoss: " + meanValueLoss);


        // mean similarity
        // loss
        double meanSimLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_similarity_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        gameBuffer.putMeanValueLoss(epoch, meanSimLoss);
        meanSimLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_similarity_0") && name.contains("loss_similarity"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty( "MeanSimilarityLoss", Double.toString(meanSimLoss));
        log.info("MeanSimilarityLoss: " + meanSimLoss);

        // mean policy loss
        double meanPolicyLoss = metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("policy_0"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        meanPolicyLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("policy_0") && name.contains("policy"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty(  "MeanPolicyLoss", Double.toString(meanPolicyLoss));
        log.info("MeanPolicyLoss: " + meanPolicyLoss);
    }



    private void loadModelParametersOrCreateIfNotExisting(Model model) {
        try {
            String outputDir = config.getNetworkBaseDir();
            mkDir(outputDir);
            model.load(Paths.get(outputDir));
          //  gameBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
        } catch (Exception e) {

                final int epoch = -1;
                int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
                boolean withSymmetryEnrichment = true;
                DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);

                djlConfig.getTrainingListeners().stream()
                    .filter(MyEpochTrainingListener.class::isInstance)
                    .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(epoch));
                try (Trainer trainer = model.newTrainer(djlConfig)) {
                    Shape[] inputShapes = networkHelper.getInputShapes();
                    trainer.initialize(inputShapes);
                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                }
              //  gameBuffer.createNetworkNameFromModel(model, model.getName(), config.getNetworkBaseDir());
            }


    }

    private void initialInferences(int numParallelInferences) {

            List<InitialInferenceTask> localInitialInferenceTaskList =
                modelQueue.getInitialInferenceTasksNotStarted(numParallelInferences);

            if (localInitialInferenceTaskList.isEmpty()) return;

            log.debug("runInitialInference() for {} games", localInitialInferenceTaskList.size());

            List<Game> games = localInitialInferenceTaskList.stream().map(InitialInferenceTask::getGame).collect(Collectors.toList());

            inferenceDuration.on();
            List<NetworkIO> networkOutput = network.initialInferenceListDirect(games);
            inferenceDuration.off();

        //    int epoch = getEpoch(network.getModel());
           // networkOutput.stream().forEach(networkIO -> networkIO.setEpoch(epoch));

            IntStream.range(0, localInitialInferenceTaskList.size()).forEach(g -> {
                InitialInferenceTask task = localInitialInferenceTaskList.get(g);
                task.setNetworkOutput(networkOutput.get(g));
                task.setDone(true);
            });


    }

    private void recurrentInferences(int numParallelInferences) {

            List<RecurrentInferenceTask> localRecurrentInferenceTaskList =
                modelQueue.getRecurrentInferenceTasksNotStarted(numParallelInferences);

            if (localRecurrentInferenceTaskList.isEmpty()) return;

            log.debug("runRecurrentInference() for {} games", localRecurrentInferenceTaskList.size());

            List<List<Node>> searchPathList = localRecurrentInferenceTaskList.stream().map(RecurrentInferenceTask::getSearchPath).collect(Collectors.toList());


            inferenceDuration.on();
            List<NetworkIO> networkOutput = network.recurrentInference(searchPathList);
            inferenceDuration.off();

        //    int epoch = getEpoch(network.getModel());
        //    networkOutput.stream().forEach(networkIO -> networkIO.setEpoch(epoch));

            IntStream.range(0, localRecurrentInferenceTaskList.size()).forEach(g -> {
                RecurrentInferenceTask task = localRecurrentInferenceTaskList.get(g);
                task.setNetworkOutput(networkOutput.get(g));
                task.setDone(true);
            });

    }


    public DurAndMem getInferenceDuration() {
        return inferenceDuration;
    }
}
