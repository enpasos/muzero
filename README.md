# MuZero implemented in Java based on DJL and PyTorch

## About

We have implemented
Gumbel [MuZero](https://deepmind.com/blog/article/muzero-mastering-go-chess-shogi-and-atari-without-rules)
with [DJL](https://djl.ai/) (pure Java code running on top of PyTorch native) following
DeepMind's [MuZero paper](https://www.nature.com/articles/s41586-020-03051-4) with network improvements as suggested in
DeepMind's [MuZero Unplugged paper](https://arxiv.org/abs/2104.06294) and the replacement of the maximizing over an
upper confidence bound by
DeepMind's [Policy improvement by planning with Gumbel](https://openreview.net/forum?id=bERaNdoegnO).

All the common logic is encapsulated in a platform module, while each game with its specific environment is implemented
in a separate module:

* Two player zero-sum games with a final reward only:
    * **TicTacToe** is used for integration testing. Starting from scratch it learns playing on a single GPU (NVIDIA GeForce RTX 4090).
    * **Go**. We have started training the game of go, board sizes 5x5 and 9x9.
* One player games with a final reward only:
    * **PegSolitair**: On the classic english board it learns perfect play: starting with one hole and end up with one
      peg in the middle.

We implemented [onnx export for the muzero network](https://enpasos.ai/muzero/How#onnx).

You can find out the inference time while running MuZero on your edge device:

* [TicTacToe](https://enpasos.ai/muzero/TicTacToe)
* [Go](https://enpasos.ai/muzero/Go)


## Build



### Prerequisites
When running on Java 21, you need to add a file `gradle.properties` to the root directory of the project with a pointer to your Java 17 installation:
```
org.gradle.java.home=C:/Program Files/Amazon Corretto/jdk17.0.9_8
```

We are using PyTorch 2.1.1.
You may need to install CUDA 12.1 and cuDNN 8.2.1.

### Build everything

```
gradlew build
```

## Run integration test on tictactoc

``` 
java -jar games/tictactoe/build/libs/tictactoe-0.7.0-SNAPSHOT-exec.jar  
```

## Further info

... [more details on enpasos.ai](https://enpasos.ai/)

## License

This project is licensed under the [Apache-2.0 License](platform/LICENSE).
