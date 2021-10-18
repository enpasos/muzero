# MuZero on DJL

## About

We have implemented [MuZero](https://deepmind.com/blog/article/muzero-mastering-go-chess-shogi-and-atari-without-rules)
with [DJL](https://djl.ai/) (pure Java code running on top of PyTorch native) following closely DeepMind's [MuZero paper](https://www.nature.com/articles/s41586-020-03051-4) with 
network improvements as suggested in DeepMind's [MuZero Unplugged paper](https://arxiv.org/abs/2104.06294) and 
the replacement of the maximizing over an upper confidence bound by the exact solution to the policy optimization problem as given by
Google/Deepmind/Columbia University's [paper](http://proceedings.mlr.press/v119/grill20a.html).


Sanity check on the trivial game TicTacToe on a single GPU (NVIDIA GeForce RTX 3090):
Starting from scratch it learns perfect play within 70.000 training steps and 80.000 game plays in less than an hour.

We have just started playing the game of go ... with small board sizes.

## Build

```
    mvn clean install -Dmaven.test.skip=true
```

## Run SelfPlay and Training

```
    mvn exec:java@train
```
You can stop any time. Restart resumes at the point stopped. To start again from scratch delete dir ```tictactoe```.

## Run Test

```
    mvn exec:java@test
```


## Further info

... [more details on enpasos.ai](https://enpasos.ai/)


## License

This project is licensed under the [Apache-2.0 License](LICENSE).
