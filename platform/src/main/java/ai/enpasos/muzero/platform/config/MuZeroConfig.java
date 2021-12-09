package ai.enpasos.muzero.platform.config;

import ai.djl.Device;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.KnownBounds;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.function.BiFunction;

import static ai.enpasos.muzero.platform.config.KnownBoundsType.BOARD_GAME;

@Component
@ConfigurationProperties("muzero")
@Data
@Slf4j
public class MuZeroConfig {
    public static final boolean HIDDEN_STATE_REMAIN_ON_GPU = false;
 //   protected boolean hiddenStateRemainOnGPU = HIDDEN_STATE_REMAIN_ON_GPU;

    protected double komi;


    protected String modelName;
    protected String gameClassName;
    protected String actionClassName;
    protected PlayerMode playerMode;
    protected boolean networkWithRewardHead;

    protected SymmetryType symmetryType;

    protected String networkBaseDir;
    protected boolean withRewardHead;
    protected int numObservationLayers;


//    protected Network network;
//    @Data
//    public static class Network {
    protected int numActionLayers;
    protected int numChannels;
    protected int numHiddenStateChannels;
    protected int numResiduals;
    // training
    protected int numberOfTrainingSteps;
    protected int numberOfTrainingStepsPerEpoch;
    protected int windowSize;
    protected int batchSize;
    protected int numUnrollSteps;
    protected int tdSteps;
    protected float discount;
    protected float weightDecay;
    protected float valueLossWeight;
    protected float lrInit;
    protected boolean absorbingStateDropToZero;
    //    protected Environment environment;
//    @Data
//    public static class Environment {
    protected int size;
    protected int maxMoves;

//        protected Training training;
//        @Data
//        public static class Training {
//
//        }


    //   }
    protected int boardHeight;
    protected int boardWidth;
    protected int actionSpaceSize;
    //    protected Play play;
//    @Data
//    public static class Play {
    protected int numberTrainingStepsOnRandomPlay;
    protected double rootDirichletAlpha;

    //}
    protected double rootExplorationFraction;
    protected KnownBoundsType knownBoundsType;
    protected double pbCInit;
    protected double pbCBase;
    protected DeviceType inferenceDeviceType;
    protected String outputDir;
    protected int numEpisodes;
    protected int numSimulations;
    protected int numParallelGamesPlayed;
    int visitSoftmaxTemperatureThreshold;

    public Class getGameClass() {
        try {
            return Class.forName(gameClassName);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public Class getActionClass() {
        try {
            return Class.forName(actionClassName);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public BiFunction<Integer, Integer, Double> visitSoftmaxTemperatureFunction() {
        return (numMoves, trainingSteps) -> (numMoves < visitSoftmaxTemperatureThreshold) ? 1.0 : 0.0;
    }


    //  }

    public KnownBounds getKnownBounds() {
        if (this.getKnownBoundsType() == BOARD_GAME) {
            return new KnownBounds(-1d, 1d);
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
        Action action = this.newAction();
        action.setIndex(index);
        return action;
    }

    public Action newAction() {
        try {
            Constructor<?> constructor = this.getActionClass().getConstructor(MuZeroConfig.class);
            return (Action) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public @NotNull String getGamesBasedir() {
        return  getOutputDir() + "games";
    }


    public String getNetworkBaseDir() {
        if ( networkBaseDir != null) return  networkBaseDir;
        return  getOutputDir() + "networks";
    }

    public Device getInferenceDevice() {
        switch(this.getInferenceDeviceType()) {
            case CPU: return Device.cpu();
            case GPU:
            default: return Device.gpu();
        }
    }
}
