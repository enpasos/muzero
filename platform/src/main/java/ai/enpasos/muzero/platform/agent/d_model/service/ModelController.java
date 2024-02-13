package ai.enpasos.muzero.platform.agent.d_model.service;

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
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.Network;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.djl.BatchFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyEasyTrain;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyEpochTrainingListener;
import ai.enpasos.muzero.platform.agent.d_model.djl.TrainingConfigFactory;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.CausalityFreezing;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.a_training.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.TrainingDatasetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.d_model.djl.EpochHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;
import static ai.enpasos.muzero.platform.common.FileUtils.mkDir;
import static java.util.Map.entry;


@Component
@Slf4j
public class ModelController implements DisposableBean, Runnable {

    @Autowired
    ModelQueue modelQueue;

    @Autowired
    MuZeroConfig config;
    NDManager nDManager;
    @Autowired
    BatchFactory batchFactory;

    @Autowired
    TrainingConfigFactory trainingConfigFactory;
    @Autowired
    GameBuffer gameBuffer;
    private Network network;
    @Autowired
    private ModelState modelState;

    private final DurAndMem inferenceDuration = new DurAndMem();

    private NDScope ndScope;

    private volatile boolean running;


    @PostConstruct
    void init() {
        this.running = true;
        Thread thread = new Thread(this, "model");
        thread.start();
    }

    @Override
    public void run() {
        log.info("ModelController started.");
        try {
            while (running) {
                int numParallelInferences = config.getNumParallelInferences();
                controllerTasks();
                initialInferences(numParallelInferences);
                recurrentInferences(numParallelInferences);
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("ModelController error", e);
        } finally {
            close();
        }
        log.info("ModelController stopped.");
    }

    private void close() {
        if (nDManager != null)
            nDManager.close();
        if (network != null && network.getModel() != null)
            network.getModel().close();

    }


    private void controllerTasks() {

        List<ControllerTask> localControllerTaskList = modelQueue.getControllerTasksNotStarted();

        if (localControllerTaskList.isEmpty()) return;

        for (ControllerTask task : localControllerTaskList) {
            switch (task.getTaskType()) {
                case LOAD_LATEST_MODEL:
                    close();
                    Model model = Model.newInstance(config.getModelName(), Device.gpu());
                    log.info("loadLatestModel with model name {}", config.getModelName());
                    if (task.epoch == -1) {
                        network = new Network(config, model);
                    } else {
                        network = new Network(config, model, Paths.get(config.getNetworkBaseDir()),
                            Map.ofEntries(entry("epoch", task.epoch + "")));
                    }
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case LOAD_LATEST_MODEL_OR_CREATE_IF_NOT_EXISTING:
                    close();
                    model = Model.newInstance(config.getModelName(), Device.gpu());
                    if (model.getBlock() == null) {
                        MuZeroBlock block = new MuZeroBlock(config);
                        model.setBlock(block);
                        if (task.epoch == -1) {
                            loadModelParametersOrCreateIfNotExisting(model);
                        } else {
                            loadModelParametersOrCreateIfNotExisting(model, Paths.get(config.getNetworkBaseDir()),
                                    Map.ofEntries(entry("epoch", task.epoch + "")));
                        }
                    }
                    network = new Network(config, model);
                    nDManager = network.getNDManager().newSubManager();
                    network.initActionSpaceOnDevice(nDManager);
                    network.setHiddenStateNDManager(nDManager);
                    this.modelState.setEpoch(getEpochFromModel(model));
                    break;
                case TRAIN_MODEL:
                    model = network.getModel();
                    trainNetwork(model, task.freeze, task.getTrainingDatasetType());
                    break;
                case START_SCOPE:
                    if (ndScope != null) {
                        ndScope.close();
                    }
                    ndScope = new NDScope();
                    break;
                case END_SCOPE:
                    if (ndScope != null) {
                        ndScope.close();
                    }
                    break;

            }
            task.setDone(true);
        }

    }


    private void trainNetwork(Model model, boolean[] freeze, TrainingDatasetType trainingDatasetType) {
        switch (trainingDatasetType) {
            case RANDOM_FROM_BUFFER:
                try (NDScope nDScope = new NDScope()) {
//                    if (config.offPolicyCorrectionOn()) {
//                        determinePRatioMaxForCurrentEpoch();
//                    }

                    int epochLocal;
                    int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
                    boolean withSymmetryEnrichment = true;
                    epochLocal = getEpochFromModel(model);
                    DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epochLocal);
                    int finalEpoch = epochLocal;
                    djlConfig.getTrainingListeners().stream()
                            .filter(MyEpochTrainingListener.class::isInstance)
                            .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
                    try (Trainer trainer = model.newTrainer(djlConfig)) {
                        Shape[] inputShapes = batchFactory.getInputShapes();
                        trainer.initialize(inputShapes);
                        trainer.setMetrics(new Metrics());


                        ((CausalityFreezing) model.getBlock()).freeze(freeze);


                        //                switch(trainingDatasetType) {
                        //                    case RANDOM_FROM_BUFFER:
                        for (int m = 0; m < numberOfTrainingStepsPerEpoch; m++) {
                            try (Batch batch = batchFactory.getRamdomBatchFromBuffer(trainer.getManager(), withSymmetryEnrichment, config.getNumUnrollSteps(), config.getBatchSize())) {
                                log.debug("trainBatch " + m);
                                MyEasyTrain.trainBatch(trainer, batch);
                                trainer.step();
                            }
                        }


                        // number of action paths

                        handleMetrics(trainer, model, epochLocal);

                        trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                    }
                }

                modelState.setEpoch(getEpochFromModel(model));
                break;
//            case SEQUENTIAL_FROM_ALL_EXPERIENCE:
//              //  SequentialCursor cursor = SequentialCursor.builder()
//                    //    .batchSize(config.getBatchSize())
//                  //      .build();
//             //   while (cursor.hasNext()) {
//                    try (NDScope nDScope = new NDScope()) {
////                        if (config.offPolicyCorrectionOn()) {
////                            determinePRatioMaxForCurrentEpoch();
////                        }
//
//                        int epochLocal;
//                        int numberOfTrainingStepsPerEpoch = config.getNumberOfTrainingStepsPerEpoch();
//                        boolean withSymmetryEnrichment = true;
//                        epochLocal = getEpochFromModel(model);
//                        DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epochLocal);
//                        int finalEpoch = epochLocal;
//                        djlConfig.getTrainingListeners().stream()
//                                .filter(MyEpochTrainingListener.class::isInstance)
//                                .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(finalEpoch));
//                        try (Trainer trainer = model.newTrainer(djlConfig)) {
//                            Shape[] inputShapes = batchFactory.getInputShapes();
//                            trainer.initialize(inputShapes);
//                            trainer.setMetrics(new Metrics());
//
//
//                            ((CausalityFreezing) model.getBlock()).freeze(freeze);
//                            try (Batch batch = batchFactory.getSequentialBatchFromAllExperience(trainer.getManager(), withSymmetryEnrichment, config.getNumUnrollSteps(), null)) {
//                                //  log.debug("trainBatch " + m);
//
//
//                                MyEasyTrain.trainBatch(trainer, batch);
//                                trainer.step();
//                            }
//                        }
//                    }
//              //  }
//                modelState.setEpoch(getEpochFromModel(model));
//               break;

            default:
                throw new MuZeroException("not implemented");
        }

    }

//    private void determinePRatioMaxForCurrentEpoch() {
//        int epoch = this.modelState.getEpoch();
//        List<Game> games = this.gameBuffer.getGamesFromBuffer().stream()
//            .filter(game -> game.getEpisodeDO().getTrainingEpoch() == epoch && game.isReanalyse() )
//            .collect(Collectors.toList());
//        double pRatioMax = determinePRatioMax(games);
//        log.info("pRatioMaxREANALYSE({}): {}", epoch, pRatioMax);
//
//        List<Game> games2 = this.gameBuffer.getGamesFromBuffer().stream()
//            .filter(game -> game.getEpisodeDO().getTrainingEpoch() == epoch && !game.isReanalyse() )
//            .collect(Collectors.toList());
//        double pRatioMax2 = determinePRatioMax(games2);
//        log.info("pRatioMax({}): {}", epoch, pRatioMax2);
//    }

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
            .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("value_0") && !name.contains("entropy_loss_value"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        gameBuffer.putMeanValueLoss(epoch, meanValueLoss);
        meanValueLoss += metrics.getMetricNames().stream()
            .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("value_0") && name.contains("value") && !name.contains("entropy_loss_value"))
            .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
            .sum();
        model.setProperty("MeanValueLoss", Double.toString(meanValueLoss));
        log.info("MeanValueLoss: " + meanValueLoss);

        // mean reward
        // loss
        double meanRewardLoss = metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_reward_0"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
       // gameBuffer.putMeanEntropyValueLoss(epoch, meanEntropyValueLoss);
        meanRewardLoss += metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_reward_0") && name.contains("loss_reward"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        model.setProperty("MeanRewardLoss", Double.toString(meanRewardLoss));
        log.info("MeanRewardLoss: " + meanRewardLoss);

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
        model.setProperty("MeanSimilarityLoss", Double.toString(meanSimLoss));
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
        model.setProperty("MeanPolicyLoss", Double.toString(meanPolicyLoss));
        log.info("MeanPolicyLoss: " + meanPolicyLoss);

        // mean legal action loss
        double meanLegalActionLoss = metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && name.contains("loss_legal_actions_0"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        meanLegalActionLoss += metrics.getMetricNames().stream()
                .filter(name -> name.startsWith(TRAIN_ALL) && !name.contains("loss_legal_actions_0") && name.contains("loss_legal_actions"))
                .mapToDouble(name -> metrics.getMetric(name).stream().mapToDouble(Metric::getValue).average().orElseThrow(MuZeroException::new))
                .sum();
        model.setProperty("MeanLegalActionLoss", Double.toString(meanLegalActionLoss));
        log.info("MeanLegalActionLoss: " + meanLegalActionLoss);
    }

    private void loadModelParametersOrCreateIfNotExisting(Model model) {
        loadModelParametersOrCreateIfNotExisting(model,  null,  null);
    }
    private void loadModelParametersOrCreateIfNotExisting(Model model, Path modelPath, Map<String, ?> options) {
        try {
            String outputDir = config.getNetworkBaseDir();
            mkDir(outputDir);
            model.load(modelPath == null ? Paths.get(outputDir) : modelPath,  null,  options);
        } catch (Exception e) {

            final int epoch = -1;
            DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(epoch);

            djlConfig.getTrainingListeners().stream()
                .filter(MyEpochTrainingListener.class::isInstance)
                .forEach(trainingListener -> ((MyEpochTrainingListener) trainingListener).setNumEpochs(epoch));
            try (Trainer trainer = model.newTrainer(djlConfig)) {
                Shape[] inputShapes = batchFactory.getInputShapes();
                trainer.initialize(inputShapes);
                trainer.notifyListeners(listener -> listener.onEpoch(trainer));
            }
        }


        long numberOfParameters = model.getBlock().getParameters().stream().mapToLong(p -> p.getValue().getArray().size()).sum();
        log.info("{} parameters in the model.", numberOfParameters);

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

        IntStream.range(0, localRecurrentInferenceTaskList.size()).forEach(g -> {
            RecurrentInferenceTask task = localRecurrentInferenceTaskList.get(g);
            task.setNetworkOutput(networkOutput.get(g));
            task.setDone(true);
        });

    }


    public DurAndMem getInferenceDuration() {
        return inferenceDuration;
    }

    @Override
    public void destroy() {
        running = false;
    }


}
