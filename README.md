# MuZero on DJL

## About

We have implemented [MuZero](https://deepmind.com/blog/article/muzero-mastering-go-chess-shogi-and-atari-without-rules)
with [DJL](https://djl.ai/) (pure Java code running on top of PyTorch native) following closely
DeepMind's [MuZero paper](https://www.nature.com/articles/s41586-020-03051-4) with network improvements as suggested in
DeepMind's [MuZero Unplugged paper](https://arxiv.org/abs/2104.06294) and the replacement of the maximizing over an
upper confidence bound by the exact solution to the policy optimization problem as given by Google/Deepmind/Columbia
University's [paper](http://proceedings.mlr.press/v119/grill20a.html).

All the common logic is encapsulated in a platform module, while each game with its specific environment is
is implemented in a separate module.

We do an integration test on the trivial game TicTacToe on a single GPU (NVIDIA GeForce RTX 3090):
Starting from scratch it learns perfect play (which is the test goal) within 40.000 training steps and 100.000 game plays in less than an hour.

We have started training the game of go, board sizes 5x5 and 9x9.

## Build

```
    mvn clean install -Dmaven.test.skip=true
```

## Run integration test on tictactoc

``` 
    cd games/tictactoe
    mvn exec:java@integrationtest
```


## Further info

... [more details on enpasos.ai](https://enpasos.ai/)

## License

This project is licensed under the [Apache-2.0 License](platform/LICENSE).
