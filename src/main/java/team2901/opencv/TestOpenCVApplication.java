package team2901.opencv;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/*
 * Run from team2901.opencv.Main to avoid "JavaFX runtime components are missing, and are required to run this application"
 * https://stackoverflow.com/questions/25873769/launch-javafx-application-from-another-class
 *
 * or create module-info
 * https://stackoverflow.com/questions/52578072/gradle-openjfx11-error-javafx-runtime-components-are-missing
 */

public class TestOpenCVApplication extends Application {

    public static final String SAMPLE_IMAGES_DIR = "src" + File.separator + "sample-images" + File.separator;
    public static final String TEMPLATES_DIR = SAMPLE_IMAGES_DIR + "templates" + File.separator;
    public static final String DEFAULT_IMAGE = SAMPLE_IMAGES_DIR + "open-cv-logo.png";

    public static final FileChooser.ExtensionFilter IMAGE_FILTER
            = new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png");

    public static final CountDownLatch latch = new CountDownLatch(1);
    public static TestOpenCVApplication testJavaFXApplication = null;

    private Stage stage;

    private ImageView transformImageView;

    private Mat originalImage;
    private Mat templateImage;
    private Mat editImage;

    private Transform selectedTransform = Transform.ORIGINAL;

    private int blurKernels = 1;
    private int threshold = 1;

    public static void main(String[] args) {
        Application.launch(args);
    }

    public static TestOpenCVApplication waitForStartUpTest() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return testJavaFXApplication;
    }

    public static void setStartUpTest(TestOpenCVApplication testJavaFXApplication0) {
        testJavaFXApplication = testJavaFXApplication0;
        latch.countDown();
    }

    public TestOpenCVApplication() {
        setStartUpTest(this);
    }

    @Override
    public void start(Stage stage) throws IOException {

        this.stage = stage;

        nu.pattern.OpenCV.loadLocally();

        // Build Menu Bar
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(buildFileMenu());
        menuBar.getMenus().add(buildTransformMenu());
        menuBar.getMenus().add(buildOptionsMenu());

        // Build image views
        transformImageView = new ImageView();
        Group editedImageGroup = new Group(transformImageView);

        // Display
        VBox vBox = new VBox(menuBar, editedImageGroup);
        Scene scene = new Scene(vBox);

        stage.setScene(scene);
        stage.show();

        loadImage(DEFAULT_IMAGE);
    }

    public Menu buildFileMenu() {

        final Menu fileMenu = new Menu("File");

        final FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().add(IMAGE_FILTER);

        final File file = new File(SAMPLE_IMAGES_DIR);
        if (!file.exists()) {
            file.mkdir();
        }

        // Open image menu item
        final MenuItem open = new MenuItem("Open");
        fileMenu.getItems().add(open);
        open.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(stage);
            try {
                if (selectedFile != null) {
                    loadImage(selectedFile.getPath());
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        // Save image menu item
        final MenuItem save = new MenuItem("Save");
        fileMenu.getItems().add(save);
        save.setOnAction(e -> {
            File selectedFile = fileChooser.showSaveDialog(stage);
            try {
                if (selectedFile != null) {
                    writeImage(selectedFile.getPath());
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        return fileMenu;
    }

    public Menu buildTransformMenu() {

        final Menu editMenu = new Menu("Transform");

        final ToggleGroup toggleGroup = new ToggleGroup();

        editMenu.getItems().add(buildTransformMenuItem("Original",  toggleGroup, Transform.ORIGINAL));
        editMenu.getItems().add(buildTransformMenuItem("Blur",  toggleGroup, Transform.BLUR));
        editMenu.getItems().add(buildTransformMenuItem("Canny",  toggleGroup, Transform.CANNY));
        editMenu.getItems().add(buildTransformMenuItem("Remove Background",  toggleGroup, Transform.REMOVE_BACKGROUND));

        final Menu channelMenu = new Menu("RGB Channel");
        channelMenu.getItems().add(buildTransformMenuItem("Red",  toggleGroup, Transform.RED_CHANNEL));
        channelMenu.getItems().add(buildTransformMenuItem("Green",  toggleGroup, Transform.GREEN_CHANNEL));
        channelMenu.getItems().add(buildTransformMenuItem("Blue",  toggleGroup, Transform.BLUE_CHANNEL));
        channelMenu.getItems().add(buildTransformMenuItem("Gray",  toggleGroup, Transform.GRAY_CHANNEL));

        editMenu.getItems().addAll(channelMenu);

        final Menu iSpyMenu = new Menu("I Spy");
        iSpyMenu.getItems().add(buildTemplateTransformMenuItem("Box",  toggleGroup, Transform.I_SPY, "i_spy_box.png"));
        iSpyMenu.getItems().add(buildTemplateTransformMenuItem("Crayon",  toggleGroup, Transform.I_SPY, "i_spy_crayon.png"));
        iSpyMenu.getItems().add(buildTemplateTransformMenuItem("Fish",  toggleGroup, Transform.I_SPY, "i_spy_fish.png"));
        iSpyMenu.getItems().add(buildTemplateTransformMenuItem("Key",  toggleGroup, Transform.I_SPY, "i_spy_key.png"));
        iSpyMenu.getItems().add(buildTemplateTransformMenuItem("Marble",  toggleGroup, Transform.I_SPY, "i_spy_marble_1.png"));

        editMenu.getItems().add(iSpyMenu);

        return editMenu;
    }

    public Menu buildOptionsMenu() {

        final Menu optionsMenu = new Menu("Options");

        Slider kernelSlider = new Slider(1, 100, 1);
        kernelSlider.setShowTickLabels(true);
        kernelSlider.setShowTickMarks(true);
        kernelSlider.setMajorTickUnit(25);
        kernelSlider.setBlockIncrement(10);

        MenuItem kernelSliderItem = new MenuItem("kernels");
        kernelSliderItem.setGraphic(kernelSlider);
        optionsMenu.getItems().add(kernelSliderItem);

        kernelSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            blurKernels = newValue.intValue();
            System.out.println(blurKernels);
            try {
                performTransform();
            } catch (IOException ignored) {
            }

        });

        Slider thresholdSlider = new Slider(1, 100, 1);
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setMajorTickUnit(25);
        thresholdSlider.setBlockIncrement(10);

        MenuItem thresholdSliderItem = new MenuItem("threshold");
        thresholdSliderItem.setGraphic(thresholdSlider);
        optionsMenu.getItems().add(thresholdSliderItem);

        thresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            threshold = newValue.intValue();
            System.out.println(threshold);
            try {
                performTransform();
            } catch(IOException ignored){}

        });

        return optionsMenu;
    }

    private RadioMenuItem buildTransformMenuItem(final String name, final ToggleGroup toggleGroup, final Transform transform) {

        final RadioMenuItem menuItem = new RadioMenuItem(name);
        menuItem.setToggleGroup(toggleGroup);
        menuItem.setSelected(transform == selectedTransform);
        menuItem.setOnAction(e -> {
            try {
                setSelectedTransform(transform);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        return menuItem;
    }

    private RadioMenuItem buildTemplateTransformMenuItem(final String name, final ToggleGroup toggleGroup, final Transform transform, final String templateName) {

        final RadioMenuItem menuItem = new RadioMenuItem(name);
        menuItem.setToggleGroup(toggleGroup);
        menuItem.setSelected(transform == selectedTransform);
        menuItem.setOnAction(e -> {
            try {
                templateImage = loadImageFromFile(TEMPLATES_DIR + templateName);
                setSelectedTransform(transform);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        return menuItem;
    }

    public void setSelectedTransform(Transform selectedTransform) throws IOException {
        this.selectedTransform = selectedTransform;
        performTransform();
    }

    public void loadImage(String filePath) throws IOException {
        originalImage = loadImageFromFile(filePath);
        performTransform();
    }

    public void writeImage(String filePath) throws IOException {
        writeImageToFile(filePath, editImage);
    }

    public Mat loadImageFromFile(String filePath) {
        return Imgcodecs.imread(filePath);
    }

    public void writeImageToFile(String filePath, Mat image) {
        Imgcodecs.imwrite(filePath, image);
    }

    public void performTransform() throws IOException {

        if (originalImage == null) {
            return;
        }

        editImage = getTransformedImage(originalImage, selectedTransform);

        displayImage(editImage, transformImageView);

        stage.sizeToScene() ;
    }

    public void displayImage(Mat image, ImageView imageView)  {

        Rect rect = new Rect();

        //Encoding the image
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", image, matOfByte);

        //Storing the encoded Mat in a byte array
        byte[] byteArray = matOfByte.toArray();

        //Displaying the image
        InputStream in = new ByteArrayInputStream(byteArray);

        BufferedImage bufImage;
        try {
            bufImage = ImageIO.read(in);
        } catch (IOException e) {
            return;
        }

        WritableImage writableImage = SwingFXUtils.toFXImage(bufImage, null);

        imageView.setImage(writableImage);
        imageView.setX(0);
        imageView.setY(rect.height);
        imageView.setFitHeight(image.height());
        imageView.setFitWidth(image.width());
        imageView.setPreserveRatio(true);
        rect.height += image.height();
        rect.width = image.width();
    }

    public Mat getTransformedImage(Mat originalImage, Transform selectedTransform) {

        if (selectedTransform == null) {
            return originalImage;
        }

        switch (selectedTransform) {
            case BLUE_CHANNEL:
                return ImageHelper.getChannelMat(originalImage, 0, blurKernels);
            case GREEN_CHANNEL:
                return ImageHelper.getChannelMat(originalImage, 1, blurKernels);
            case RED_CHANNEL:
                return ImageHelper.getChannelMat(originalImage, 2, blurKernels);
            case GRAY_CHANNEL:
                return ImageHelper.getGrayscaleMat(originalImage, blurKernels);
            case BLUR:
                return ImageHelper.getBlurMat(originalImage, blurKernels);
            case CANNY:
                return ImageHelper.getCannyMat(originalImage, blurKernels, threshold);
            case REMOVE_BACKGROUND:
                return ImageHelper.doBackgroundRemoval(originalImage, blurKernels);
            case I_SPY:
                return ImageHelper.getISpyMat(originalImage, templateImage);
            case ORIGINAL:
            default:
                return originalImage;
        }
    }
}
