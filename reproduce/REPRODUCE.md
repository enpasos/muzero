# MuZero on DJL


## Reference Stack

- DJL: 0.21.0 (automatically installed with the app)
  - PYTORCH: 1.13.1 
- Java: Corretto-17.0.6 (needs to be installed)
- CUDA (needs to be installed)
  - cudnn: 8.8.0
  - CUDA SDK: 11.7.1
  - GPU Driver: 528.02
- OS: Microsoft Windows 11
- Hardware
  - GPU: NVIDIA GeForce RTX 4090
  - CPU: Intel Core i9-13900K
  - RAM: 128 GB


## App Install, Build and Unit Test

```
gradlew build
```


## Run 1

Hybrid Policy
Epochs: 50000/40 = 1250

```
java -jar games/tictactoe/build/libs/tictactoe-0.5.0-SNAPSHOT-exec.jar --spring.config.location=file:./reproduce/run1/
```
