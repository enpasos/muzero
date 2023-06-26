package ai.enpasos.muzero.platform.agent.e_experience.db;

import ai.enpasos.muzero.platform.agent.e_experience.GameDTO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DBService {

    @Autowired
    EpisodeRepo episodeRepo;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EpisodeDO> saveEpisodesAndCommit(List<EpisodeDO> episodes) {
        return episodeRepo.saveAllAndFlush(episodes);
    }



    @Transactional
    public List<GameDTO> findTopNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<EpisodeDO>  list =  episodeRepo.findTopNByOrderByIdDesc(n);
     //  List<EpisodeDO>  list =   episodeRepo.findTop100ByOrderByIdDesc();
        List<GameDTO> gameDTOList = convertEpisodeDOListToGameList(list);
        return gameDTOList;
    }


    public  List<GameDTO> convertEpisodeDOListToGameList(List<EpisodeDO> episodes) {
        return episodes.stream().map(episode ->  GameDTO.builder()
                .hybrid(episode.isHybrid())
                .nextSurpriseCheck(episode.getNextSurpriseCheck())
                .lastValueError(episode.getLastValueError())
                .surprised(episode.isSurprised())
                .tdSteps(episode.getTdSteps())
                .tHybrid(episode.getTHybrid())
                .networkName(episode.getNetworkName())
                .trainingEpoch(episode.getTrainingEpoch())
                .count(episode.getCount())
                .tdSteps(episode.getTdSteps())
                .pRandomActionRawCount(episode.getPRandomActionRawCount())
                .pRandomActionRawSum(episode.getPRandomActionRawSum())
                .timeSteps(episode.getTimeSteps())
                .build()).collect(Collectors.toList());
    }

    public  List<EpisodeDO> convertGameListToEpisodeDOList(List<Game> games) {
        List<EpisodeDO> episodes = games.stream().map(game ->  EpisodeDO.builder()
                .hybrid(game.getGameDTO().isHybrid())
                .nextSurpriseCheck(game.getGameDTO().getNextSurpriseCheck())
                .lastValueError(game.getGameDTO().getLastValueError())
                .surprised(game.getGameDTO().isSurprised())
                .tdSteps(game.getGameDTO().getTdSteps())
                .tHybrid(game.getGameDTO().getTHybrid())
                .networkName(game.getGameDTO().getNetworkName())
                .trainingEpoch(game.getGameDTO().getTrainingEpoch())
                .count(game.getGameDTO().getCount())
                .tdSteps(game.getGameDTO().getTdSteps())
                .pRandomActionRawCount(game.getGameDTO().getPRandomActionRawCount())
                .pRandomActionRawSum(game.getGameDTO().getPRandomActionRawSum())
                .timeSteps(IntStream.range(0, game.getGameDTO().getObservations().size()).mapToObj(t ->
                        TimeStepDO.builder()
                                .t(t)
                                .observation(game.getGameDTO().getObservations().get(t))
                                .action(game.getGameDTO().getActions().size() > t ? game.getGameDTO().getActions().get(t): null)
                                .entropy(game.getGameDTO().getEntropies().size() > t ? game.getGameDTO().getEntropies().get(t) : null)
                                .rootEntropyValueFromInitialInference(game.getGameDTO().getRootEntropyValuesFromInitialInference().size() > t ? game.getGameDTO().getRootEntropyValuesFromInitialInference().get(t) : null)
                                .policyTarget(game.getGameDTO().getPolicyTargets().size() > t ? game.getGameDTO().getPolicyTargets().get(t) : null)
                                .reward(game.getGameDTO().getRewards().size() > t ? game.getGameDTO().getRewards().get(t) : null)
                                .rootValueFromInitialInference(game.getGameDTO().getRootValuesFromInitialInference().size() > t ?
                                        game.getGameDTO().getRootValuesFromInitialInference().get(t) : null)
                                .legalActionMaxEntropy(game.getGameDTO().getLegalActionMaxEntropies().size() > t ?
                                        game.getGameDTO().getLegalActionMaxEntropies().get(t) : null)
                                .legalActions(game.getGameDTO().getLegalActions().size() > t ? game.getGameDTO().getLegalActions().get(t) : null)
                                .vMix(game.getGameDTO().getVMix().size() > t ? game.getGameDTO().getVMix().get(t) : null)
                                .playoutPolicy(game.getGameDTO().getPlayoutPolicy().size() > t ? game.getGameDTO().getPlayoutPolicy().get(t) : null)
                                .rootEntropyValueTarget(game.getGameDTO().getRootEntropyValueTargets().size()>t ? game.getGameDTO().getRootEntropyValueTargets().get(t) : 0f)
                                .rootValueTarget(game.getGameDTO().getRootValueTargets().size() > t ? game.getGameDTO().getRootValueTargets().get(t): null)
                                .build()
                ).collect(Collectors.toList()))
                .build()).collect(Collectors.toList());
        episodes.forEach(episode -> {
            episode.getTimeSteps().forEach(timeStep -> {
                timeStep.setEpisode(episode);
            });
        });
        return episodes;
    }


}
