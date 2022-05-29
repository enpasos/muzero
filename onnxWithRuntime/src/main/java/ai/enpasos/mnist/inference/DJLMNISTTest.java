package ai.enpasos.mnist.inference;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class DJLMNISTTest {
    public static void main(String[] args) throws Exception {

        testClassifications(
            "./models/mnist.onnx",
            "./onnxWithRuntime/data/mnist_png/testing/"
        );

    }

    @SuppressWarnings({"java:S2095", "java:S112"})
    private static void testClassifications(String modelPath, String dataPath) throws IOException, MalformedModelException {
        Map<String, List<Image>> data = getData(dataPath);

        try (Model model = Model.newInstance("model", "OnnxRuntime")) {
            try (InputStream is = Files.newInputStream(Paths.get(modelPath))) {
                model.load(is);
                var predictor = model.newPredictor(getImageClassificationsTranslator());

                int[] errorsTotal = {0, 0};
                data.forEach((label, images) -> images.forEach(image -> {
                    try {
                        var classifications = predictor.predict(image);
                        if (!classifications.best().getClassName().equals(label)) {
                            errorsTotal[0]++;
                        }
                        errorsTotal[1]++;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }));

                log.info("{} wrong classified images in {} non trained testimages", errorsTotal[0], errorsTotal[1]);
            }
        }
    }

    @SuppressWarnings("java:S112")
    private static Map<String, List<Image>> getData(String dataPath) {
        Map<String, List<Image>> data = new TreeMap<>();
        try (Stream<Path> stream = Files.list(Paths.get(dataPath))) {
            stream.filter(Files::isDirectory)
                .map(Path::getFileName)
                .forEach(dirname -> {
                    List<Image> images = new ArrayList<>();
                    data.put(dirname.toString(), images);
                    try (Stream<Path> stream2 = Files.list(Paths.get(dataPath + dirname + "/"))) {
                        stream2
                            .filter(file -> !Files.isDirectory(file))
                            .forEach(path -> {
                                try {
                                    images.add(ImageFactory.getInstance().fromFile(path));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                }
                            });
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return data;
    }

    private static Translator<Image, Classifications> getImageClassificationsTranslator() {
        Translator<Image, Classifications> translator = new Translator<>() {

            @Override
            public NDList processInput(TranslatorContext ctx, Image input) {
                // Convert Image to NDArray
                NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.GRAYSCALE);
                return new NDList(NDImageUtils.toTensor(array));
            }

            @Override
            public Classifications processOutput(TranslatorContext ctx, NDList list) {
                // Create a Classifications with the output probabilities
                NDArray probabilities = list.singletonOrThrow().softmax(0);
                List<String> classNames = IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
                return new Classifications(classNames, probabilities);
            }

            @Override
            public Batchifier getBatchifier() {
                // The Batchifier describes how to combine a batch together
                // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
                return Batchifier.STACK;
            }
        };
        return translator;
    }
}
