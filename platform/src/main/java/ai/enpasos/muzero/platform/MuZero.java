/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.platform;

import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.gamebuffer.GameIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.slow.play.PlayManager;
import ai.enpasos.muzero.platform.agent.slow.play.ThinkBudget;
import ai.enpasos.muzero.platform.agent.slow.play.ThinkConf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

@Slf4j
public class MuZero {


    public static void playOnDeepThinking(Network network, ReplayBuffer replayBuffer) {
        MuZeroConfig config = network.getConfig();
        ThinkConf thinkConf = ThinkConf.instanceFromConfig(config);

        PlayManager.playParallel(network, replayBuffer, config, false, false, thinkConf, true);
    }

    public static void initialFillingBuffer(Network network, ReplayBuffer replayBuffer) {
        MuZeroConfig config = network.getConfig();
        ThinkConf thinkConf = ThinkConf.builder()
                .playerAConfig(
                        ThinkBudget.builder()
                                .numSims(config.getNumSimulations())
                                .numParallel(10000)
                                .numOfPlays(1)
                                .build())
                .playerBConfig(
                        ThinkBudget.builder()
                                .numSims(config.getNumSimulations())
                                .numParallel(10000)
                                .numOfPlays(1)
                                .build())
                .build();


        while (replayBuffer.getBuffer().getData().size() < config.getWindowSize()) {
            log.info(replayBuffer.getBuffer().getData().size() + " of " + config.getWindowSize());
            PlayManager.playParallel(network, replayBuffer, config, false, true, thinkConf, true);
            replayBuffer.saveState();
        }
    }

    public static void createNetworkModelIfNotExisting(@NotNull MuZeroConfig config) {
        NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, null, 0);
    }


    private static void makeObjectDir(@NotNull MuZeroConfig config) {
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config) + "/" + (GameIO.getLatestObjectNo(config) + 1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteNetworksAndGames(@NotNull MuZeroConfig config) {
        try {
            FileUtils.forceDelete(new File(config.getOutputDir()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.forceMkdir(new File(getNetworksBasedir(config)));
            FileUtils.forceMkdir(new File(getGamesBasedir(config)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static @NotNull String getGamesBasedir(@NotNull MuZeroConfig config) {
        return config.getOutputDir() + "games";
    }


    public static @NotNull String getNetworksBasedir(@NotNull MuZeroConfig config) {
        if (config.getNetworkBaseDir() != null) return config.getNetworkBaseDir();
        return config.getOutputDir() + "networks";
    }


    private static void restartApplication() throws URISyntaxException, IOException {
//        File currentSourceFile = new File(MuZero.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//        if(!currentSourceFile.getName().endsWith(".jar"))
//            return;
//        List<String> command = List.of("java -jar muzero.jar");


        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(MuZero.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (!currentJar.getName().endsWith(".jar"))
            return;

        final ArrayList<String> command = new ArrayList<String>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        System.exit(0);
    }

    public static void train(Network network) {
        train(network, false);
    }

    public static void train(Network network, boolean freshBuffer) {
        MuZeroConfig config = network.getConfig();
        Model model = network.getModel();
        MuZero.createNetworkModelIfNotExisting(config);


        ReplayBuffer replayBuffer = new ReplayBuffer(config);
        if (freshBuffer) {
            while (!replayBuffer.getBuffer().isBufferFilled()) {
                MuZero.playOnDeepThinking(network, replayBuffer);
                replayBuffer.saveState();
            }
        } else {
            replayBuffer.loadLatestState();
            MuZero.initialFillingBuffer(network, replayBuffer);
        }

        int trainingStep = NetworkHelper.numberOfLastTrainingStep(config);

        while (trainingStep < config.getNumberOfTrainingSteps()) {
            if (trainingStep != 0) {
                log.info("last training step = {}", trainingStep);
                log.info("numSimulations: " + config.getNumSimulations());
                MuZero.playOnDeepThinking(network, replayBuffer);
                replayBuffer.saveState();
            }
            trainingStep = NetworkHelper.trainAndReturnNumberOfLastTrainingStep(config, replayBuffer, 1);
        }

    }

}
