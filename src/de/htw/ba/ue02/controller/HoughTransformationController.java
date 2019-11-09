/**
 * @author Nico Hezel
 * modified by K. Jung, 28.10.2016
 */
package de.htw.ba.ue02.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;


public class HoughTransformationController extends HoughTransformationBase {

    protected enum Methods {
        Leer, Accumulator, Maximum, Linien
    }

    @Override
    public void runMethod(Methods currentMethod, int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) throws Exception {
        switch (currentMethod) {
            case Accumulator:
                calculateAcc(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, true);
                break;
            case Maximum:
                showMax(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, 21);
                break;
            case Linien:
                showLines(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
                break;
            case Leer:
            default:
                empty(dstPixels);
                break;
        }
    }

    private void empty(int[] dstPixels) {
        // all pixels black
        Arrays.fill(dstPixels, 0xff000000);
    }

    private int[] calculateAcc(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight, boolean draw) {
        // Init accumulator Array
        int[] accArray = new int[dstWidth * dstHeight];

        // Iterate over all phiIndex values
        for (int phiIndex = 0; phiIndex < dstWidth; phiIndex++) {
            double phi = Math.PI / dstWidth * phiIndex;
            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);

            // Iterate over all srcPixels positions
            for (int y = 0; y < srcHeight; y++) {
                for (int x = 0; x < srcWidth; x++) {
                    int pos = (y * srcWidth + x);
                    // Check if value is present
                    if ((srcPixels[pos] >> 16 & 0xFF) > 0) {
                        // Calculate r. Increase count in accArray by 1 at pos of r & phi
                        double r = (x - srcWidth / 2) * cosPhi + (y - srcHeight / 2) * sinPhi;
                        int accPos = (((int) (r + dstHeight/2)) * dstWidth + phiIndex);
                        accArray[accPos]++;
                    }
                }
            }
        }
        // Find max value in acc array
        int max = IntStream.of(accArray).max().getAsInt();

        // Normalize values to range of 0 - 255
        int[] normalized = IntStream.of(accArray)
                .map(val -> (int) ((double) val / max * 255))
                .map(value -> 0xFF000000 | (value << 16) | (value << 8) | value)
                .toArray();

        if(draw) {
            System.arraycopy(normalized, 0, dstPixels, 0, dstPixels.length);
        }
        return normalized;
    }

    private int[] showMax(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight, int kernelSize) {
        // Get accumulator array
        int[] accArray = calculateAcc(srcPixels, srcWidth, srcHeight,dstPixels, dstWidth, dstHeight, false);

        // Only keep values above 60% of max value
        int[] reduced = IntStream.of(accArray)
                .map(value -> value >> 16 & 0xFF)
                .map(val -> {
                    if (val > 255 * 0.6) return val;
                    else return 0;
                }).toArray();

        // Calculate delta for kernel loop
        int delta = -kernelSize / 2;

        // Iterate over all positions in reduced acc array
        for (int y = 0; y < dstHeight; y++) {
            for (int x = 0; x < dstWidth; x++) {
                int pos = (y * dstWidth + x);

                int currentValue = reduced[pos];

                if (currentValue > 0) {
                    // Iterate over kernel around current value. If a bigger value is found, set currentValue to 0
                    // and jump to next pos
                    outerKernel:
                    for (int kernelY = y + delta; kernelY <= y - delta; kernelY++) {
                        for (int kernelX = x + delta; kernelX <= x - delta; kernelX++) {
                            int kernelPos = kernelY * dstWidth + kernelX;
                            if (kernelY < 0 || kernelX < 0 || kernelY >= dstHeight || kernelX >= dstWidth) {
                                continue;
                            }
                            int kernelValue = reduced[kernelPos];
                            if (kernelValue > currentValue) {
                                currentValue = 0;
                                break outerKernel;
                            }
                        }
                    }
                }
                dstPixels[pos] = 0xFF000000 | (currentValue << 16) | (currentValue << 8) | currentValue;
            }
        }
        return dstPixels;
    }

    private void showLines(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) {
        // Get max array
        int[] maxArray = showMax(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, 21);

        // Init Buffered Image
        BufferedImage bufferedImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
        Graphics2D g2d = bufferedImage.createGraphics();

        // Loop over all positions in maxArray
        for (int rIndex = 0; rIndex < dstHeight; rIndex++) {
            for (int phiIndex = 0; phiIndex < dstWidth; phiIndex++) {
                int pos = (rIndex * dstWidth + phiIndex);

                // If max value is found, calculate r & phi
                if ((maxArray[pos] >> 16 & 0xFF) > 0) {
                    int r = rIndex - 250;
                    double phi = Math.PI / 360 * phiIndex;
                    double sinPhi = Math.sin(phi);
                    double cosPhi = Math.cos(phi);

                    int x1;
                    int x2;
                    int y1;
                    int y2;

                    if (sinPhi > cosPhi) {
                        // Set x1&x2 to the left and right borders of image (coordinate origin in center)
                        // Then calculate y with r=x*cos(phi)+y*sin(phi) solved for y
                        x1 = -srcWidth / 2;
                        y1 = (int) ((r - x1 * cosPhi) / sinPhi);

                        x2 = srcWidth / 2;
                        y2 = (int) ((r - x2 * cosPhi) / sinPhi);
                    } else {
                        // Set y1&y2 to the top and bottom borders of image (coordinate origin in center)
                        // then calculate x with r=x*cos(phi)+y*sin(phi) solved for x
                        y1 = -srcHeight / 2;
                        x1 = (int) ((r - y1 * sinPhi) / cosPhi);

                        y2 = srcHeight / 2;
                        x2 = (int) ((r - y2 * sinPhi) / cosPhi);
                    }
                    // Adjust x & y for coordinate origin of image being in the top left
                    x1 += srcWidth / 2;
                    x2 += srcWidth / 2;
                    y1 += srcHeight / 2;
                    y2 += srcHeight / 2;

                    // Draw found line
                    g2d.setColor(Color.RED);
                    g2d.drawLine(x1, y1, x2, y2);

                    // Calculate x&y values for normal vector to center. phi: angle from line, r: length
                    int normX = (int) (r * cosPhi) + srcWidth / 2;
                    int normY = (int) (r * sinPhi) + srcHeight / 2;

                    // Draw normal vector of found line
                    g2d.setColor(Color.GREEN);
                    g2d.drawLine(srcWidth / 2, srcHeight / 2, normX, normY);
                }
            }
        }
        g2d.dispose();
        bufferedImage.getRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
    }
}
