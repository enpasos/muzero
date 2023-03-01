package ai.enpasos.muzero.platform.agent.rational.async;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameBuffer;
import ai.enpasos.muzero.platform.agent.rational.Node;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.agent.rational.async.ModelService.INTERRUPTED;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;
import static ai.enpasos.muzero.platform.common.FileUtils.mkDir;


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



//    @Autowired
//    GlobalState globalState;

    private DurAndMem inferenceDuration = new DurAndMem();

       // @Scheduled(fixedDelay = 10)
    public void run()  {
        log.info("ModelController started.");
        try {
            while (true) {
                int numParallelInferences = config.getNumParallelInferences();
                controllerTasks();


                //     waitToFillUp(numParallelInferences, modelQueue.countInitialInferenceTasksNotStarted(), 10);

//            do {
                initialInferences(numParallelInferences);

                int i = 42;
//                //   waitToFillUp(numParallelInferences,modelQueue.countRecurrentInferenceTasksNotStarted(), 10);
//            } while (modelQueue.countInitialInferenceTasksNotStarted() > 0);
//
//
//            //   waitToFillUp(numParallelInferences,modelQueue.countRecurrentInferenceTasksNotStarted(), 10);
//
//            do {
                recurrentInferences(numParallelInferences);
//                //    waitToFillUp(numParallelInferences,modelQueue.countRecurrentInferenceTasksNotStarted(), 10);
//            } while (modelQueue.countRecurrentInferenceTasksNotStarted() > 0);
                i = 43;
                Thread.sleep(10);
            }
        } catch (Exception e) {
            log.error("ModelController stopped.");
           // e.printStackTrace();
          //  throw new MuZeroException(e);
        } finally {
            network.getModel().close();
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

    private void controllerTasks() {

        List<ControllerTask> localControllerTaskList = modelQueue.getControllerTasksNotStarted();

        if (localControllerTaskList.isEmpty()) return;



        for (ControllerTask task : localControllerTaskList) {
            switch (task.getTaskType()) {
                case loadLatestModel:
                    Model model = Model.newInstance(config.getModelName(), Device.gpu());
                    log.info("loadLatestModel with model name {}", config.getModelName());
                    network = new Network(config, model);
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    break;
                case loadLatestModelOrCreateIfNotExisting:
                    model = Model.newInstance(config.getModelName(), Device.gpu());
                    if (model.getBlock() == null) {
                        MuZeroBlock block = new MuZeroBlock(config);
                        model.setBlock(block);
                        loadModelParametersOrCreateIfNotExisting(model);
                    }
                    network = new Network(config, model);
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    break;
                case trainModel:
                    trainNetwork(network.getModel());

            }
            task.setDone(true);
        }

    }


    private void trainNetwork(Model model) {
        if (config.offPolicyCorrectionOn()) {
            determinePRatioMaxForCurrentEpoch(model);
        }

        int epoch;
        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
        boolean withSymmetryEnrichment = true;
        epoch = getEpochFromModel(model);
        DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);
        int finalEpoch = epoch;
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
            int numActionPaths = this.gameBuffer.getBuffer().getNumOfDifferentGames();
            model.setProperty("NumActionPaths", Double.toString(numActionPaths));
            log.info("NumActionPaths: " + numActionPaths);

            handleMetrics(trainer, model, epoch);

            trainer.notifyListeners(listener -> listener.onEpoch(trainer));
        }
        // }
//        epoch = getEpochFromModel(model);
//        return epoch * numberOfTrainingStepsPerEpoch;

    }

    private void determinePRatioMaxForCurrentEpoch(Model model) {
        int epoch = getEpochFromModel(model);
        List<Game> games = this.gameBuffer.getGames().stream()
            .filter(game -> game.getGameDTO().getTrainingEpoch() == epoch && game.getPlayTypeKey() == PlayTypeKey.REANALYSE)
            .collect(Collectors.toList());
        double pRatioMax = determinePRatioMax(games);
        log.info("pRatioMaxREANALYSE({}): {}", epoch, pRatioMax);

        List<Game> games2 = this.gameBuffer.getGames().stream()
            .filter(game -> game.getGameDTO().getTrainingEpoch() == epoch && game.getPlayTypeKey() != PlayTypeKey.REANALYSE)
            .collect(Collectors.toList());
        double pRatioMax2 = determinePRatioMax(games2);
        log.info("pRatioMax({}): {}", epoch, pRatioMax);
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
            gameBuffer.createNetworkNameFromModel(model, model.getName(), outputDir);
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
            gameBuffer.createNetworkNameFromModel(model, model.getName(), config.getNetworkBaseDir());

        }
    }

    private void initialInferences(int numParallelInferences) {

        List<InitialInferenceTask> localInitialInferenceTaskList =
            modelQueue.getInitialInferenceTasksNotStarted(numParallelInferences);

        if (localInitialInferenceTaskList.isEmpty()) return ;

        log.info("runInitialInference() for {} games", localInitialInferenceTaskList.size());

        List<Game> games = localInitialInferenceTaskList.stream().map(InitialInferenceTask::getGame).collect(Collectors.toList());

        inferenceDuration.on();
        List<NetworkIO>  networkOutput =  network.initialInferenceListDirect(games);
        inferenceDuration.off();

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

        log.info("runRecurrentInference() for {} games", localRecurrentInferenceTaskList.size());

        List<List<Node>> searchPathList = localRecurrentInferenceTaskList.stream().map(RecurrentInferenceTask::getSearchPath).collect(Collectors.toList());


        inferenceDuration.on();
        List<NetworkIO>  networkOutput =  network.recurrentInference(searchPathList);
        inferenceDuration.off();

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
