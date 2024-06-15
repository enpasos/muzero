package ai.enpasos.muzero.platform.agent.d_model;

public class Boxing {



    public static int intervall(int box) {
        return (int)Math.pow(2, box);
    }

    public static boolean isUsed(int box, int epoch) {
        return epoch % intervall(box) == 0;
    }
}
