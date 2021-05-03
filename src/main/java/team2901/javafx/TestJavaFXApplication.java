package team2901.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.CountDownLatch;

/*
 * Run from team2901.javafx.Main to avoid "JavaFX runtime components are missing, and are required to run this application"
 * https://stackoverflow.com/questions/25873769/launch-javafx-application-from-another-class
 *
 * or create module-info
 * https://stackoverflow.com/questions/52578072/gradle-openjfx11-error-javafx-runtime-components-are-missing
 */
public class TestJavaFXApplication extends Application {
    public static final CountDownLatch latch = new CountDownLatch(1);
    public static TestJavaFXApplication testJavaFXApplication = null;

    public static TestJavaFXApplication waitForStartUpTest() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return testJavaFXApplication;
    }

    public static void setStartUpTest(TestJavaFXApplication testJavaFXApplication0) {
        testJavaFXApplication = testJavaFXApplication0;
        latch.countDown();
    }

    public TestJavaFXApplication() {
        setStartUpTest(this);
    }

    public void printSomething() {
        System.out.println("You called a method on the application");
    }

    @Override
    public void start(Stage stage) throws Exception {
        BorderPane pane = new BorderPane();
        Scene scene = new Scene(pane, 500, 500);
        stage.setScene(scene);

        Label label = new Label("Hello");
        pane.setCenter(label);

        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
