package ai.enpasos.muzero.platform.config;

import ai.djl.Device;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.play.Action;
import ai.enpasos.muzero.platform.agent.c_planning.KnownBounds;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.config.KnownBoundsType.FROM_VALUES;
import static ai.enpasos.muzero.platform.config.KnownBoundsType.MINUSONE_ONE;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;

@Component
@ConfigurationProperties("muzero")
@Data
@Slf4j
@SuppressWarnings({"squid:S1104", "unchecked"})
public class MuZeroConfig {
    public static final boolean HIDDEN_STATE_REMAIN_ON_GPU = false;
    public Map<GameType, Conf> games;
    Action[] actions;
    private RunType run = RunType.NONE;
    private GameType activeGame;

    public Class<? extends Game> getGameClass() {
        try {
            return (Class<? extends Game>) Class.forName(getGameClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public Class<? extends Action> getActionClass() {
        try {
            return (Class<? extends Action>) Class.forName(getActionClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public KnownBounds getKnownBounds() {
        if (this.getKnownBoundsType() == MINUSONE_ONE) {
            return new KnownBounds(-1d, 1d);
        }
        if (this.getKnownBoundsType() == FROM_VALUES) {
            return new KnownBounds(this.getValues()[0], this.getValues()[this.getValues().length - 1]);
        }
        return new KnownBounds();
    }

    public Game newGame() {
        try {
            Constructor<?> constructor = this.getGameClass().getConstructor(MuZeroConfig.class);
            return (Game) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Action newAction(int index) {
        if (actions == null) {
            int n = this.getActionSpaceSize();
            actions = new Action[n];
            for (int i = 0; i < n; i++) {
                try {
                    Constructor<?> constructor = this.getActionClass().getConstructor(MuZeroConfig.class);
                    actions[i] = (Action) constructor.newInstance(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                actions[i].setIndex(i);
            }

        }
        return actions[index];
    }

    public @NotNull String getGamesBasedir() {
        return getOutputDir() + "games";
    }


    public Device getInferenceDevice() {
        switch (this.getInferenceDeviceType()) {
            case CPU:
                return Device.cpu();
            case GPU:
            default:
                return Device.gpu();
        }
    }

    private Conf getConf() {
        return this.games.get(this.activeGame);
    }


    public String getModelName() {
        return getConf().modelName;
    }

    public String getGameClassName() {
        return getConf().gameClassName;
    }

    public String getActionClassName() {
        return getConf().actionClassName;
    }

    public PlayerMode getPlayerMode() {
        return getConf().playerMode;
    }


    public boolean isNetworkWithRewardHead() {
        return getConf().networkWithRewardHead;
    }


    public boolean offPolicyCorrectionOn() {
        return getConf().offPolicyCorrectionOn;
    }

    public boolean allOrNothingOn() {
        return getConf().allOrNothingOn;
    }


    public SymmetryType getSymmetryType() {
        return getConf().symmetryType;
    }

    public String getNetworkBaseDir() {
        if (getConf().networkBaseDir != null) return getConf().networkBaseDir;
        return getConf().getOutputDir() + "networks";
    }

    public void setNetworkBaseDir(String networkBaseDir) {
        getConf().setNetworkBaseDir(networkBaseDir);
    }


    public int getNumObservationLayers() {
        return getConf().numObservationLayers;
    }

    public int getNumActionLayers() {
        return getConf().numActionLayers;
    }

    public int getNumChannels() {
        return getConf().numChannels;
    }

    public int getNumBottleneckChannels() {
        return getConf().numBottleneckChannels;
    }


    public int getNumResiduals() {
        return getConf().numResiduals;
    }


    public int[] getValues() {

        int[] result = new int[getValueSpan() + 1];
        int[] vs = getConf().getValueInterval();
        IntStream.range(0, result.length).forEach(i -> result[i] = vs[0] + i);
        return result;
    }


    public int getValueSpan() {
        int[] vs = getConf().getValueInterval();
        return vs[vs.length - 1] - vs[0];
    }

    public int getNumberOfTrainingSteps() {
        return getConf().numberOfTrainingSteps;
    }

    public int getNumberOfTrainingStepsPerEpoch() {
        return getConf().numberOfTrainingStepsPerEpoch;
    }


    public int getBatchSize() {
        return getConf().batchSize;
    }

    public int getNumUnrollSteps() {
        return getConf().numUnrollSteps;
    }

    public int getNumberOfEpisodesPerJVMStart() {
        return getConf().numberOfEpisodesPerJVMStart;
    }


    public int getTdSteps() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).getTdSteps();
    }

    public float getDiscount() {
        return getConf().discount;
    }




    public float getWeightDecay() {
        return getConf().weightDecay;
    }

    public float getValueLossWeight() {
        return getConf().valueLossWeight;
    }

    public float getLrInit() {
        return getConf().lrInit;
    }


    public int getSize() {
        return getConf().size;
    }

    public int getMaxMoves() {
        return getConf().maxMoves;
    }

    public int getBoardHeight() {
        return getConf().boardHeight;
    }

    public int getBoardWidth() {
        return getConf().boardWidth;
    }

    public int getActionSpaceSize() {
        return getConf().actionSpaceSize;
    }

    public int getNumberTrainingStepsOnStart() {
        return getConf().numberTrainingStepsOnStart;
    }


    public double getRootDirichletAlpha() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).rootDirichletAlpha;
    }

    public double getRootExplorationFraction() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).rootExplorationFraction;
    }


    public KnownBoundsType getKnownBoundsType() {
        return getConf().knownBoundsType;
    }

    public NetworkType getNetworkType() {
        return getConf().networkType;
    }


    public DeviceType getInferenceDeviceType() {
        return getConf().inferenceDeviceType;
    }

    // https://arxiv.org/pdf/1611.01144.pdf

    public void setInferenceDeviceType(DeviceType deviceType) {
        getConf().setInferenceDeviceType(deviceType);
    }

    public double getTemperatureRoot() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).temperatureRoot;
    }

    public void setTemperatureRoot(double temperature) {
        getConf().getPlayTypes().get(getConf().getPlayTypeKey()).temperatureRoot = temperature;
    }

    public double getFractionOfAlternativeActionGames() {
        return getConf().fractionOfAlternativeActionGames;
    }

    public String getOutputDir() {
        return getConf().outputDir;
    }

    public void setOutputDir(String outputDir) {
        getConf().setOutputDir(outputDir);
    }

    public int getNumEpisodes() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numEpisodes;
    }

    public int getInitialGumbelM() {
        return getConf().initialGumbelM;
    }

    public int getNumSimulations() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numSimulations;
    }

    public void setNumSimulations(int numSimulations) {
        getConf().getPlayTypes().get(getConf().getPlayTypeKey()).setNumSimulations(numSimulations);
    }

    public int getNumSimulationsHybrid() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numSimulationsHybrid;
    }

    public int getNumSimulations(Game game) {
        if (this.getTrainingTypeKey() == HYBRID &&
            game.getGameDTO().getActions().size() < game.getGameDTO().getTHybrid()) {
            return getNumSimulationsHybrid();
        } else {
            return getNumSimulations();
        }
    }

    public double getNumSimThreshold() {
        return getConf().numSimThreshold;
    }


    public int getNumParallelGamesPlayed() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).numParallelGamesPlayed;
    }

    public boolean isForTdStep0ValueTraining() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).forTdStep0ValueTraining;
    }

    public boolean isForTdStep0PolicyTraining() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).forTdStep0PolicyTraining;
    }

    public boolean isGumbelActionSelection() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).gumbelActionSelection;
    }
    public boolean isGumbelActionSelectionOnExploring() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).gumbelActionSelectionOnExploring;
    }



    public boolean isWithGumbel() {
        return getConf().getPlayTypes().get(getConf().getPlayTypeKey()).withGumbel;
    }


    public int getBroadcastEveryN() {
        return getConf().broadcastEveryN;
    }


    public PlayTypeKey getPlayTypeKey() {
        return getConf().getPlayTypeKey();
    }

    public void setPlayTypeKey(PlayTypeKey trainingTypeKey) {
        getConf().setPlayTypeKey(trainingTypeKey);
    }



    public int getCVisit() {
        return getConf().cVisit;
    }

    public void setCVisit(int cVisit) {
        getConf().setCVisit(cVisit);
    }


    public double getCScale() {
        return getConf().cScale;
    }

    public double getOffPolicyRatioLimit() {
        return getConf().offPolicyRatioLimit;
    }

    public int getNumPurePolicyPlays() {
        return getConf().numPurePolicyPlays;
    }

    public int getNumParallelInferences() {
        return getConf().numParallelInferences;
    }

    public int getNumChannelsHiddenLayerSimilarityProjector() {
        return getConf().numChannelsHiddenLayerSimilarityProjector;
    }

    public int getNumChannelsOutputLayerSimilarityProjector() {
        return getConf().numChannelsOutputLayerSimilarityProjector;
    }
    public int getNumChannelsHiddenLayerSimilarityPredictor() {
        return getConf().numChannelsHiddenLayerSimilarityPredictor;
    }

    public int getNumChannelsOutputLayerSimilarityPredictor() {
        return getConf().numChannelsOutputLayerSimilarityPredictor;
    }

    public FileType getGameBufferWritingFormat() {
        return getConf().gameBufferWritingFormat;
    }

    public void setGameBufferWritingFormat(FileType fileType) {
        getConf().setGameBufferWritingFormat(fileType);
    }
    public void setWindowSize(int windowSize) {
        getConf().setWindowSize(windowSize);
    }

    public PlayTypeKey getTrainingTypeKey() {
        return getConf().playTypeKey;
    }

    public int getWindowSize() {
        return getConf().windowSize;
    }

    public Set<PlayTypeKey> getPlayTypeKeysForTraining() {
        return getConf().getPlayTypes().entrySet().stream()
            .filter(entry -> entry.getValue().isForTraining())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }


    @Data
    public static class Conf {
        public Map<PlayTypeKey, PlayType> playTypes;

        protected double numSimThreshold;

        protected NetworkType networkType = NetworkType.CON;
        protected String modelName;
        protected String gameClassName;
        protected String actionClassName;
        protected PlayerMode playerMode;
        protected boolean networkWithRewardHead;

        protected SymmetryType symmetryType;
        protected String networkBaseDir;

        protected int numObservationLayers;
        protected int numActionLayers;
        protected int numChannels;

        protected int broadcastEveryN;
        protected int numBottleneckChannels;

        protected int numResiduals;

        protected int numberOfTrainingSteps;
        protected int numberOfTrainingStepsPerEpoch;
        protected int windowSize;

        protected int numChannelsHiddenLayerSimilarityProjector;
        protected int numChannelsOutputLayerSimilarityProjector;

        protected int numChannelsHiddenLayerSimilarityPredictor;
        protected int numChannelsOutputLayerSimilarityPredictor;
        protected double fractionOfAlternativeActionGames;

        protected int batchSize;
        protected int numUnrollSteps;
        protected int numberOfEpisodesPerJVMStart;



        protected float discount;
        protected float weightDecay;
        protected float valueLossWeight;
        protected float lrInit;
        protected int size;
        protected int maxMoves;
        protected int boardHeight;
        protected int boardWidth;
        protected int actionSpaceSize;
        protected int numberTrainingStepsOnStart;

        protected KnownBoundsType knownBoundsType;

        protected DeviceType inferenceDeviceType;
        protected String outputDir;
        protected FileType gameBufferWritingFormat = FileType.ZIPPED_PROTOCOL_BUFFERS;

        protected PlayTypeKey playTypeKey;
        int initialGumbelM;

        int cVisit;
        double cScale;
        int numPurePolicyPlays;
        int[] valueInterval;
        int numParallelInferences = 1;

        boolean offPolicyCorrectionOn;

        boolean allOrNothingOn;
double offPolicyRatioLimit;

        public PlayTypeKey getPlayTypeKey() {
            if (playTypeKey == null) {
                this.playTypes.entrySet().stream().filter(entry -> entry.getValue().isForTraining()).findFirst().ifPresent(entry -> this.playTypeKey = entry.getKey());
            }
            return playTypeKey;
        }


        @Data
        public static class PlayType {
            protected int numEpisodes;
            protected int numParallelGamesPlayed;

            protected boolean forTdStep0ValueTraining = true;
            protected boolean forTdStep0PolicyTraining = true;
            protected int tdSteps;
            protected int numSimulations;
            protected int numSimulationsHybrid;
            protected double rootDirichletAlpha;
            protected double rootExplorationFraction;
            double temperatureRoot = 1.0;

            boolean gumbelActionSelection = true;
            boolean gumbelActionSelectionOnExploring = true;
            boolean withGumbel = true;

            boolean forTraining = true;
        }
    }

}
