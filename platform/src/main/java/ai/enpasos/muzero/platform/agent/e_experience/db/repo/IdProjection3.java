package ai.enpasos.muzero.platform.agent.e_experience.db.repo;

public interface IdProjection3 extends IdProjection {
    Long getEpisodeId();
    Long getId();
    Integer getUOk();
    Integer getBox();
    Integer getNextUOk();
    Integer getNextUOkTarget();
    Integer getT();

}
