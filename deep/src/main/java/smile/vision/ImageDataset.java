/*
 * Copyright (c) 2010-2024 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */
package smile.vision;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.ToIntFunction;
import javax.imageio.ImageIO;
import smile.deep.Dataset;
import smile.deep.SampleBatch;
import smile.deep.tensor.Tensor;
import smile.math.MathEx;
import smile.vision.transform.Transform;

/**
 * Each of these directories should contain one subdirectory for each class
 * in the dataset. The subdirectories are named after the corresponding
 * class and contain all the images for that class. Ensure that each image
 * file is named uniquely and stored in a common format such as JPEG or PNG.
 *
 * @author Haifeng Li
 */
public class ImageDataset implements Dataset {
    record ImageFile(File file, String label) { }
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImageDataset.class);
    private final ArrayList<ImageFile> samples = new ArrayList<>();
    private final int batch;
    private final Transform transform;
    private final ToIntFunction<String> targetTransform;

    /**
     * Constructor.
     * @param batch the mini-batch size.
     * @param root the root directory of image dataset.
     * @param transform the transformation from image to tensor.
     * @param targetTransform the transform from image label to class index.
     */
    public ImageDataset(int batch, String root, Transform transform, ToIntFunction<String> targetTransform) {
        this.batch = batch;
        this.transform = transform;
        this.targetTransform = targetTransform;

        File dir = new File(root);
        for (var child : dir.listFiles()) {
            if (child.isDirectory()) {
                String label = child.getName();
                File[] images = child.listFiles();
                for (var image : images) {
                    if (image.isFile()) {
                        String name = image.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                            samples.add(new ImageFile(image, label));
                        }
                    }
                }
            }
        }
    }

    @Override
    public long size() {
        return samples.size();
    }

    @Override
    public Iterator<SampleBatch> iterator() {
        final int size = samples.size();
        final int[] permutation = MathEx.permutate(size);
        final BlockingQueue<SampleBatch> queue = new LinkedBlockingQueue<>(100);

        final int start = Math.min(batch, size);
        final int[] index = new int[start];
        for (int i = 0; i < start; i++) {
            index[i] = permutation[i];
        }

        try {
            // prefetch the first batch
            queue.put(readImages(index));
        } catch (Exception ex) {
            logger.error("Failed to load the first batch", ex);
        }

        final Runnable worker = () -> {
            for (int i = start; i < size; ) {
                int n = Math.min(batch, size - i);
                for (int j = 0; j < n; j++, i++) {
                    index[j] = permutation[i];
                }
                try {
                    queue.put(readImages(n == index.length ? index : Arrays.copyOf(index, n)));
                } catch (Exception ex) {
                    logger.error("Failed to load images", ex);
                }
            }
        };

        Thread thread = new Thread(worker, "ImageDatasetLoader");
        thread.start();

        return new Iterator<>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public SampleBatch next() {
                try {
                    var sample = queue.take();
                    i += sample.data().size(0);
                    return sample;
                } catch (InterruptedException ex) {
                    logger.error("Failed to take next sample batch", ex);
                    return null;
                }
            }
        };
    }

    /**
     * Reads a mini-batch of image samples.
     * @param index the sample index.
     * @return the sample batch.
     * @throws IOException
     */
    private SampleBatch readImages(int[] index) throws IOException {
        int n = index.length;
        int[] target = new int[n];
        BufferedImage[] images = new BufferedImage[n];
        for (int i = 0; i < n; i++) {
            var sample = samples.get(index[i]);
            images[i] = ImageIO.read(sample.file);
            target[i] = targetTransform.applyAsInt(sample.label);
        }
        return new SampleBatch(transform.forward(images), Tensor.of(target, images.length));
    }
}
