package ai.enpasos.muzero.platform.agent.d_model;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Data
public class ModelState {

    @Autowired
    MuZeroConfig config;

  //  int hyperepoch;

    @Getter(onMethod_={@Synchronized}) @Setter(onMethod_={@Synchronized})
    int epoch;

    public String getCurrentNetworkNameWithEpoch() {
        return String.format(Locale.ROOT, "%s-%04d", config.getModelName(), epoch);
    }
}
