package ai.enpasos.muzero.platform.agent.e_experience.memory;

import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.e_experience.Game;



public class ObservationStateNode extends StateNode {

    Game game;
    int t;



    protected ObservationModelInput observationModelInput;


    public ObservationStateNode(Game game, int t) {
        super();
        this.game = game;
        this.t = t;
        this.observationModelInput = game.getObservationModelInput(t);
    }



    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ObservationStateNode)) return false;
        final ObservationStateNode other = (ObservationStateNode) o;
        return observationModelInput.equals(other.observationModelInput);
    }


    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(observationModelInput.getValue());

        // TODO
       // return java.util.Arrays.hashCode(this.game.getEpisodeDO().getTimeStep(t).getLegalact().getLegalActions());
    }
}
