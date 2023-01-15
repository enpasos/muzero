# MuZero on DJL

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
    * **TicTacToe** is used for integration testing. Starting from scratch it learns perfect play in games from start on a single GPU (NVIDIA GeForce RTX 3090) in less than an hour.
    * **Go**. We have started training the game of go, board sizes 5x5 and 9x9.
* One player games with a final reward only:
    * **PegSolitair**: On the classic english board it learns perfect play: starting with one hole and end up with one
      peg in the middle.

We implemented [onnx export for the muzero network](https://enpasos.ai/muzero/How#onnx).

You can find out the inference time while running MuZero on your edge device:

* [TicTacToe](https://enpasos.ai/muzero/TicTacToe)
* [Go](https://enpasos.ai/muzero/Go)



## Patch DJL
A solution to the performance issue https://github.com/deepjavalibrary/djl/issues/2210
is in the status of a pull request https://github.com/deepjavalibrary/djl/pull/2321
```
git clone https://github.com/enpasos/djl.git
cd djl
git checkout reference-counting
gradlew build -x test
gradlew publishToMavenLocal
```


## Build

```
gradlew build
```

## Run integration test on tictactoc

``` 
java -jar games/tictactoe/build/libs/tictactoe-0.5.0-SNAPSHOT-exec.jar  
```

## Further info

... [more details on enpasos.ai](https://enpasos.ai/)

## License

This project is licensed under the [Apache-2.0 License](platform/LICENSE).
