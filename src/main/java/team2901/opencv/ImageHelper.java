package team2901.opencv;

import javafx.stage.FileChooser;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageHelper {

    public static Mat getChannelMat(Mat originalImage, int channel) {
        List<Mat> list = new ArrayList<Mat>();
        Core.split(originalImage, list);
        return list.get(channel);
    }

    public static Mat getGrayscaleMat(Mat originalImage) {
        Mat mat = new Mat();
        Imgproc.cvtColor(originalImage, mat, Imgproc.COLOR_BGR2GRAY);
        return mat;
    }
}
