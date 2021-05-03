package team2901.opencv;


import javafx.application.Application;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.core.Mat;

public class OpenCVMain {
    public static void main(String args[]) {
        loadOpenCV();
    }

    public static void loadOpenCV() {
            new Thread(() -> Application.launch(TestOpenCVApplication.class)).start();
        TestOpenCVApplication testJavaFXApplication = TestOpenCVApplication.waitForStartUpTest();
    }
}
