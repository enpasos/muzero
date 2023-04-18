# MuZero on DJL


## Reference Stack

- DJL: 0.22.0-SNAPSHOT (automatically installed with the app)
  - PYTORCH: 2.0.0 
- Java: Corretto-17.0.6 (needs to be installed)
- CUDA (needs to be installed)
  - cudnn: 8.9
  - CUDA SDK: 11.8
  - GPU Driver: 517.89
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

Hybrid Policy, T=5
```
java -jar games/tictactoe/build/libs/tictactoe-0.6.0-SNAPSHOT-exec.jar --spring.config.location=file:./reproduce/run1/
```

## Run 2

Best Effort, T=5
```
java -jar games/tictactoe/build/libs/tictactoe-0.6.0-SNAPSHOT-exec.jar --spring.config.location=file:./reproduce/run2/
```