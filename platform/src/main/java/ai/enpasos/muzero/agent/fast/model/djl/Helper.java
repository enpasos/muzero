package ai.enpasos.muzero.agent.fast.model.djl;

import ai.djl.ndarray.BaseNDManager;
import ai.djl.ndarray.NDManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class Helper {


    public static void logNDManagers(@Nullable NDManager ndManager) {
        while (ndManager != null) {
            log.debug(ndManager.toString());
            if (ndManager instanceof BaseNDManager && ndManager.getParentManager() == null) {
                BaseNDManager m = (BaseNDManager) ndManager;
                m.debugDump(0);
            }
            ndManager = ndManager.getParentManager();
        }
    }
}
