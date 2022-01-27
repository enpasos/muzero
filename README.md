# MuZero on DJL

## About

We have implemented [MuZero](https://deepmind.com/blog/article/muzero-mastering-go-chess-shogi-and-atari-without-rules)
with [DJL](https://djl.ai/) (pure Java code running on top of PyTorch native) following closely
DeepMind's [MuZero paper](https://www.nature.com/articles/s41586-020-03051-4) with network improvements as suggested in
DeepMind's [MuZero Unplugged paper](https://arxiv.org/abs/2104.06294) and the replacement of the maximizing over an
upper confidence bound by the exact solution to the policy optimization problem as given by Google/Deepmind/Columbia
University's [paper](http://proceedings.mlr.press/v119/grill20a.html).

All the common logic is encapsulated in a platform module, while each game with its specific environment is implemented
in a separate module:

* Two player zero-sum games with a final reward only:
    * **TicTacToe** is used for integration testing. Starting from scratch it learns perfect play (which is the test
      goal) on a single GPU (NVIDIA GeForce RTX 3090) within 2.500 training steps and 70.000 game plays in 45 minutes.
    * **Go**. We have started training the game of go, board sizes 5x5 and 9x9.
* One player games with a final reward only:
    * **PegSolitair**: On the classic english board it learns perfect play: starting with one hole and end up with one
      peg in the middle.

We implemented [onnx export for the muzero network](https://enpasos.ai/muzero/How#onnx). 

You can find out the inference time while running MuZero on your edge device: 
* [TicTacToe](https://enpasos.ai/muzero/TicTacToe) 
* [Go](https://enpasos.ai/muzero/Go)

## Build

```
    mvn clean install
```

## Run integration test on tictactoc

``` 
    java -jar games/tictactoe/target/tictactoe-0.3.0-SNAPSHOT-exec.jar  
```

## Further info

... [more details on enpasos.ai](https://enpasos.ai/)

## License

This project is licensed under the [Apache-2.0 License](platform/LICENSE).
