# MuZero on DJL

## About

We have implemented [MuZero](https://deepmind.com/blog/article/muzero-mastering-go-chess-shogi-and-atari-without-rules)
with DJL following closely the [deepmind paper](https://www.nature.com/articles/s41586-020-03051-4).
We have tested it on the trivial game "TicTacToe" on a single GPU (NVIDIA GeForce RTX 3090).
Starting from scratch it learns perfect play within 100.000 training steps and 4.000.000 game plays in a little less than a day.

## Build

```
    mvn clean install
```

## Run GamePlay and Training

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
