import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/firstWindow.fxml"));
        primaryStage.setTitle("Oblachko");
        Image icon = new Image(getClass().getResourceAsStream("/iconfinder_cloud_1287533.png"));
        primaryStage.getIcons().add(icon);
        Scene scene = new Scene(root, 700, 450, Color.BEIGE);
        primaryStage.setResizable(false); //неизменяемость размера
        primaryStage.setScene(scene);
        primaryStage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }
}
