package ai.enpasos.muzero.platform.agent.gamebuffer;


import ai.enpasos.muzero.platform.agent.gamebuffer.protobuf.ReplayBufferProto;

public class ProtobufHelper {



    public static void main(String[] args) {
         ReplayBufferProto.Builder bufferBuilder =  ReplayBufferProto.newBuilder()
                .setCounter(42)
                .setGameClassName("ai.enpasos.muzero.tictactoe.config.TicTacToeGame");

//        GameBufferProto.GameProto.Builder gameBuilder = GameBufferProto.GameProto.newBuilder()
//                .getActionsList().addAll()


        System.out.println(bufferBuilder.build().toString());
        // p.getGameProtosList().add()
        //GameBufferProto.GameProto.newBuilder().getActionsList();
    }


    public  ReplayBufferProto convert(ReplayBufferDTO dto) {
         ReplayBufferProto.Builder bufferBuilder =  ReplayBufferProto.newBuilder()
                .setVersion(1)
                .setCounter((int)dto.getCounter())
                .setWindowSize(dto.getWindowSize())
                .setGameClassName(dto.getGameClassName());

        dto.getData().stream().forEach( gameDTO -> bufferBuilder.addGameProtos(gameDTO.proto()));

        return bufferBuilder.build();
    }


    public ReplayBufferDTO convert( ReplayBufferProto proto) {
        return null;
    }
}
