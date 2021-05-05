package team2901.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;

/*
 * resources
 *
 * https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#background-removal-result
 *
 * https://www.analyticsvidhya.com/blog/2019/03/opencv-functions-computer-vision-python/
 * - this is in python but may be useful
 */
public class ImageHelper {

    public static Mat getChannelMatGray(Mat originalImage, int channel, int kernels) {
        Mat blurImage = getBlurMat(originalImage, kernels);
        List<Mat> blurPlanes = new ArrayList<>();
        Core.split(blurImage, blurPlanes);
        return blurPlanes.get(channel);
    }

    public static Mat getChannelMat(Mat originalImage, int channel, int kernels) {

        // Blur the image
        Mat blurImage = getBlurMat(originalImage, kernels);

        // Split the image into BGR planes
        List<Mat> blurPlanes = new ArrayList<>();
        Core.split(blurImage, blurPlanes);

        // Create BGR planes of all zeros
        // See https://stackoverflow.com/questions/6699374/access-to-each-separate-channel-in-opencv
        List<Mat> outputPlanes = new ArrayList<>();
        outputPlanes.add(Mat.zeros(blurImage.size(), CV_8UC1));
        outputPlanes.add(Mat.zeros(blurImage.size(), CV_8UC1));
        outputPlanes.add(Mat.zeros(blurImage.size(), CV_8UC1));

        // Set the planes for the desired channel
        outputPlanes.set(channel, blurPlanes.get(channel));

        // Merge the planes together
        Mat dest = new Mat();
        Core.merge(outputPlanes, dest);

        return dest;
    }

    public static Mat getGrayscaleMat(Mat originalImage, int kernels) {
        Mat mat = getBlurMat(originalImage, kernels);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        return mat;
    }

    public static Mat getBlurMat(Mat originalImage, int kernels) {
        Mat blueMat = new Mat();
        Imgproc.blur(originalImage, blueMat, new Size(kernels, kernels));
        return blueMat;
    }

    public static Mat getCannyMat(Mat originalImage, int kernels, int threshold) {
        // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#background-removal-result

        // init
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();

        // convert to grayscale
        Imgproc.cvtColor(originalImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // reduce noise with a 3x3 kernel
        Imgproc.blur(grayImage, detectedEdges, new Size(kernels, kernels));

        // canny detector, with ratio of lower:upper threshold of 3:1
        Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold * 3);

        // using Canny's output as a mask, display the result
        Mat dest = new Mat();
        Core.add(dest, Scalar.all(0), dest);
        originalImage.copyTo(dest, detectedEdges);

        return dest;
    }

    public static Mat doBackgroundRemoval(Mat originalImage, int kernels) {
        // init
        Mat hsvImg = new Mat();
        List<Mat> hsvPlanes = new ArrayList<>();
        Mat thresholdImg = new Mat();

        int thresh_type = Imgproc.THRESH_BINARY_INV;

        // threshold the image with the average hue value
        hsvImg.create(originalImage.size(), CvType.CV_8U);
        Imgproc.cvtColor(originalImage, hsvImg, Imgproc.COLOR_BGR2HSV);
        Core.split(hsvImg, hsvPlanes);

        // get the average hue value of the image
        double threshValue = getHistAverage(hsvImg, hsvPlanes.get(0));

        Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, thresh_type);

        Imgproc.blur(thresholdImg, thresholdImg, new Size(kernels, kernels));

        // dilate to fill gaps, erode to smooth edges
        Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 1);
        Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 3);

        Imgproc.threshold(thresholdImg, thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY);

        // create the new image
        Mat foreground = new Mat(originalImage.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        originalImage.copyTo(foreground, thresholdImg);

        return foreground;
    }

    /**
     * Get the average hue value of the image starting from its Hue channel
     * histogram
     *
     * @param hsvImg
     *            the current frame in HSV
     * @param hueValues
     *            the Hue component of the current frame
     * @return the average Hue value
     */
    private static double getHistAverage(Mat hsvImg, Mat hueValues) {
        // init
        double average = 0.0;
        Mat hist_hue = new Mat();
        // 0-180: range of Hue values
        MatOfInt histSize = new MatOfInt(180);
        List<Mat> hue = new ArrayList<>();
        hue.add(hueValues);

        // compute the histogram
        Imgproc.calcHist(hue, new MatOfInt(0), new Mat(), hist_hue, histSize, new MatOfFloat(0, 179));

        // get the average Hue value of the image
        // (sum(bin(h)*h))/(image-height*image-width)
        // -----------------
        // equivalent to get the hue of each pixel in the image, add them, and
        // divide for the image size (height and width)
        for (int h = 0; h < 180; h++)
        {
            // for each bin, get its value and multiply it for the corresponding
            // hue
            average += (hist_hue.get(h, 0)[0] * h);
        }

        // return the average hue of the image
        return average / hsvImg.size().height / hsvImg.size().width;
    }

    public static Mat getISpyMat(Mat originalImage, Mat template) {

        // https://riptutorial.com/opencv/example/22915/template-matching-with-java

        // TODO how to improve threshold to handle low confidence matches?
        // TODO how to detect when the template is rotated?
        // TODO how to detect when the template is scaled?

        Mat source = originalImage.clone();

        Mat outputImage = new Mat();
        int machMethod = Imgproc.TM_CCOEFF;

        //Template matching method
        Imgproc.matchTemplate(source, template, outputImage, machMethod);

        Core.MinMaxLocResult mmr = Core.minMaxLoc(outputImage);
        Point matchLoc = mmr.maxLoc;

        //Draw rectangle on result image
        Imgproc.rectangle(source, matchLoc, new Point(matchLoc.x + template.cols(),
                matchLoc.y + template.rows()), new Scalar(0, 255, 0));

        return source;
    }

}
