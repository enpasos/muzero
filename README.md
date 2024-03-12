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
    * **TicTacToe** is used for integration testing. Starting from scratch it learns playing on a single GPU (NVIDIA GeForce RTX 4090). We are testing for perfect play: Each relevant decision on the decision tree. This includes exploitability.
    * **Go**. We have started training the game of go, board sizes 5x5 and 9x9.
* One player games with a final reward only:
    * **PegSolitair**: On the classic english board it learns perfect play: starting with one hole and end up with one
      peg in the middle.

We implemented [onnx export for the muzero network](https://enpasos.ai/muzero/here/How#onnx).

You can find out the inference time while running MuZero on your edge device:

* [TicTacToe](https://enpasos.ai/muzero/here/TicTacToe)
* [Go](https://enpasos.ai/muzero/here/Go)


## Build



### Prerequisites

Install [Java JDK 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html).

We are using PyTorch 2.1.1 which is automatically installed during the build.
However, it needs to have [CUDA 12.1](https://developer.nvidia.com/cuda-12-1-1-download-archive) and [cuDNN 8.2.1](https://developer.nvidia.com/rdp/cudnn-download) installed.

### Build everything

The gradle wrapper automatically downloads the correct gradle version. It is not necessary to install gradle.
Just run the following command in the root directory of the project:

```
gradlew build
```

## Run integration test on tictactoc

``` 
java -jar games/tictactoe/build/libs/tictactoe-0.8.0-SNAPSHOT-exec.jar  
```

## Further info

... [more details on enpasos.ai](https://enpasos.ai/)

## License

This project is licensed under the [Apache-2.0 License](platform/LICENSE).


## Database

postgres 16.2
https://www.enterprisedb.com/downloads/postgres-postgresql-downloads

database superuser
postgres
363-cbe-724-kew-4g3            (or something safe ;-)

port 5432

#### Local Database Setup

```
Run as database postgres user;
```
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM muzero;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM muzero;
REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM muzero;
REVOKE ALL ON DATABASE muzero FROM muzero;  
REVOKE ALL ON SCHEMA public FROM muzero;  
DROP USER IF EXISTS muzero;
DROP DATABASE IF EXISTS muzero;
 
CREATE DATABASE muzero  WITH ENCODING 'UTF8';
CREATE USER muzero WITH ENCRYPTED PASSWORD 'hd7sge';
\c muzero;
grant usage,create on schema public to muzero;
```
