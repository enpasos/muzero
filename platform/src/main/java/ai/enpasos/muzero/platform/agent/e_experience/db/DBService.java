package ai.enpasos.muzero.platform.agent.e_experience.db;


import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.LegalActionsDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.LegalActionsRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DBService {

    @Autowired
    EpisodeRepo episodeRepo;


    @Autowired
    TimestepRepo timestepRepo;


    @Autowired
    LegalActionsRepo legalActionsRepo;

    public void clearDB() {
        episodeRepo.dropTable();
        episodeRepo.dropSequence();
        timestepRepo.dropTable();
        timestepRepo.dropSequence();
        legalActionsRepo.dropTable();
        legalActionsRepo.dropSequence();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EpisodeDO> saveEpisodesAndCommit(List<EpisodeDO> episodes) {
        List<TimeStepDO> timeStepDOS = episodes.stream().map(EpisodeDO::getTimeSteps).flatMap(Collection::stream).collect(Collectors.toList());
        Set<LegalActionsDO> las = timeStepDOS.stream().map(TimeStepDO::getLegalact).collect(Collectors.toSet());

        List<LegalActionsDO> knownLegalActions = legalActionsRepo.findAllByLegalActions(las.stream().map(LegalActionsDO::getLegalActions).collect(Collectors.toList()));

        List<LegalActionsDO> unknownLegalActions = new ArrayList<>();
        unknownLegalActions.addAll(las);
        unknownLegalActions.removeAll(knownLegalActions);

        List<LegalActionsDO> legalActions = legalActionsRepo.saveAll(unknownLegalActions);
        legalActions.addAll(knownLegalActions);


        Map<LegalActionsDO, LegalActionsDO> map = new HashMap<>();

        legalActions.forEach(la -> map.put(la, la));

        episodes.stream().filter(EpisodeDO::isHybrid).forEach(episodeDO -> {
            long t = episodeDO.getTStartNormal();
            for (TimeStepDO timeStep : episodeDO.getTimeSteps()) {
                timeStep.setExploring(timeStep.getT() < t);
            }
        });
        episodes.stream().forEach(episodeDO -> {
            for (TimeStepDO timeStep : episodeDO.getTimeSteps()) {
                LegalActionsDO legalActionsDO = map.get(timeStep.getLegalact());
                timeStep.setLegalact(legalActionsDO);
                legalActionsDO.getTimeSteps().add(timeStep);
            }
        });
        return episodeRepo.saveAllAndFlush(episodes);
    }

    @Transactional
    public List<Long> getNewEpisodeIds() {
        int limit = 50000;
        List<Long> newEpisodeIds = new ArrayList<>();
        int offset = 0;
        episodeRepo.updateMinUOK();
        List newIds;
        do {
            newIds = episodeRepo.findAllEpisodeIdsWithBoxSmallerOrEqualsMinUOk(limit, offset, -2);
            newEpisodeIds.addAll(newIds);
            offset += limit;
        } while (newIds.size() > 0);
        return newEpisodeIds;
    }


    @Transactional
    public List<EpisodeDO> findTopNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findTopNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Transactional
    public List<EpisodeDO> findRandomNByOrderByIdDescAndConvertToGameDTOList(int n) {
        List<Long> ids = episodeRepo.findRandomNEpisodeIds(n);
        List<EpisodeDO> result = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids);
        return result;
    }

    @Autowired
    private ModelState modelState;


    @Transactional
    public List<Long> updateTimesteps_SandUOkandBox(List<TimeStepDO> timesteps, List<Integer> boxesRelevant) {
        List<Long> ids = new ArrayList<>();
        int epoch = modelState.getEpoch();
        timesteps.stream().forEach(ts -> {

            boolean boxesChanged = ts.changeBoxesBasesOnUOk(boxesRelevant, epoch);

            if (ts.isSChanged() || ts.isUOkChanged() || boxesChanged || ts.isUnrollStepsChanged()) {
                ids.add(ts.getId());
                timestepRepo.updateAttributeSAndU(ts.getId(), ts.getS(), ts.isSClosed(), ts.getUOk(), ts.isUOkClosed(), ts.getBoxes(), ts.getUOkEpoch());
                if (ts.getT() > 0) {
                    long id = ts.getEpisode().getTimeStep((ts.getT() - 1)).getId();
                    ids.add(id);
                    timestepRepo.updateNextUOk(id, ts.getUOk(), ts.isUOkClosed());
                }
                if (ts.getT() == ts.getEpisode().getLastTime()) {
                    timestepRepo.updateNextUOk(ts.getId(), ts.getUOk(), true);
                }
                ts.setSChanged(false);
                ts.setUOkChanged(false);
            }
            ts.setUOkTested(false);

        });
        return ids;
    }

}
