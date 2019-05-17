import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.input.*;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class MainWindowController implements Initializable {

    @FXML
    ListView<FileInStorage> leftListView;
    @FXML
    ListView<FileInStorage> rightListView;
    @FXML
    HBox warning;
    @FXML
    Menu menu;
    @FXML
    MenuBar menuBar;
    @FXML
    Button backBut1;
    @FXML
    Button backBut2;

    private String userDirectory = "client" + File.separator + "LocalStorage";
    private int localStorageLevel = 0;
    private int cloudStorageLevel = 0;

    private HashMap<Integer, ArrayList<File>> hashMapCloudStorage;
    private ArrayList<File> returnCloudStorage;
    private ArrayList<File> pathsCloudFiles;
    FileChooser fileChooser = new FileChooser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ConnectWithServer.connect();
        initializeLocalList();
        dragInLocalStorage();
        dragFromLocalToCloudStorage();
        deleteFilesByDelete();
        ServerListener.setDaemon(true);
        ServerListener.start();
        upgradeCloud();
    }

    Thread ServerListener = new Thread(() -> {
        try {
            while(true) {
                    Object messageFromServer;
                    messageFromServer = ConnectWithServer.readInObject();
                    if (messageFromServer instanceof SynchroMessage) {
                        SynchroMessage message = (SynchroMessage) messageFromServer;
                        returnCloudStorage = new ArrayList<>();
                        returnCloudStorage.addAll(message.getCloudStorageContents());
                        hashMapCloudStorage = new HashMap<>();
                        hashMapCloudStorage.put(cloudStorageLevel, message.getCloudStorageContents());
                        Platform.runLater(() -> initializeCloudList(returnCloudStorage));
                    } else if (messageFromServer instanceof FileMessage){
                        FileMessage fileMessage = (FileMessage) messageFromServer;
                        Path pathToNewFile = Paths.get("client/LocalStorage/" + fileMessage.getFileName());
                        if (fileMessage.isDirectory() && fileMessage.isEmpty()) {
                            if (Files.exists(pathToNewFile)) {
                                System.out.println("Файл с таким именем уже существует");
                            } else {
                                Files.createDirectory(pathToNewFile);
                            }
                        } else {
                            if (Files.exists(pathToNewFile)) {
                                System.out.println("Файл с таким именем уже существует");
                            } else {
                                Files.write(Paths.get("client/LocalStorage/" + fileMessage.getFileName()), fileMessage.getData(), StandardOpenOption.CREATE);
                            }
                        }
                        Platform.runLater(() -> initializeLocalList());

                    } else if (messageFromServer instanceof MessageGetListLocalOrCloud){
                        MessageGetListLocalOrCloud message = (MessageGetListLocalOrCloud) messageFromServer;
                        synchroLocalStorageFromCloud(message.getCloudStorageContents());
                        Platform.runLater(() -> initializeLocalList());
                    } else if (messageFromServer instanceof UpdateMessageCloud){
                        UpdateMessageCloud message = (UpdateMessageCloud) messageFromServer;
                        synchroCloudStorageFromCloud(message.getCloudStorageContents());
                        warning.setVisible(false);
                        Platform.runLater(() -> upgradeCloud());
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
    });


    public void initializeLocalList(){
         ObservableList<FileInStorage> localItemsList = FXCollections.observableArrayList();
         File pathTo = new File(userDirectory);
         File[] listLocalFiles = pathTo.listFiles();
         if(listLocalFiles.length == 0 && localStorageLevel == 0){
             leftListView.setItems(localItemsList);
             leftListView.setCellFactory(param -> new StorageListView());
         } else if(listLocalFiles.length > 0){
             for (int i = 0; i < listLocalFiles.length; i++) {
                 long size;
                 boolean choose = false;
                 String name = listLocalFiles[i].getName();
                 if (listLocalFiles[i].isDirectory()){
                     choose = true;
                     size = getSizeOfDirectory(listLocalFiles[i]);
                 } else {
                     size = listLocalFiles[i].length();
                 }
                 File path = new File(listLocalFiles[i].getAbsolutePath());

                 localItemsList.addAll(new FileInStorage(name, path, size, false));
             }
             leftListView.setItems(localItemsList);
             leftListView.setCellFactory(param -> new StorageListView());
         }else{
             leftListView.setItems(localItemsList);
             leftListView.setCellFactory(param -> new StorageListView());
         }

    }

    public void initializeCloudList(ArrayList<File> array){
        ObservableList<FileInStorage> cloudItemsList = FXCollections.observableArrayList();

        if(array.size() == 0 && cloudStorageLevel == 0) {
            rightListView.setItems(cloudItemsList);
            rightListView.setCellFactory(param -> new StorageListView());
        } else if(array.size() > 0){
            for (File file: array) {
                long size;
                String name = file.getName();
                if (file.isDirectory()){
                    size = getSizeOfDirectory(file);
                } else {
                    size = file.length();
                }
                File path = new File(file.getAbsolutePath());

                cloudItemsList.addAll(new FileInStorage(name, path, size, false));
            }
            rightListView.setItems(cloudItemsList);
            rightListView.setCellFactory(param -> new StorageListView());
        }else{
            rightListView.setItems(cloudItemsList);
            rightListView.setCellFactory(param -> new StorageListView());
        }
    }

    public void upgradeCloud(){
        ConnectWithServer.upgradeCloudStorage(UserLogin.getLogin());
    }


    public void dragInLocalStorage(){
        leftListView.setOnDragOver(event -> {
            if (event.getGestureSource() != leftListView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        leftListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success;

            ObservableList<FileInStorage> listOfFiles = FXCollections.observableArrayList();
            String newPath = userDirectory + File.separator;

            if (db.hasFiles()) {
                for (int i = 0; i < db.getFiles().size(); i++) {

                    long size = 0;

                    String name = db.getFiles().get(i).getName();
                    if(db.getFiles().get(i).isDirectory()){
                        try {
                            size = getSizeOfDirectory(db.getFiles().get(i));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else size = db.getFiles().get(i).length();

                    File filePath = db.getFiles().get(i).getAbsoluteFile();
                    Path srcPath = Paths.get(db.getFiles().get(i).getAbsolutePath());
                    File destPath = new File(newPath + db.getFiles().get(i).getName());

                    try {
                        Files.copy(srcPath, Paths.get(destPath.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    listOfFiles.add(new FileInStorage(name, filePath, size, false));
                    leftListView.setItems(listOfFiles);
                    leftListView.setCellFactory(param -> new StorageListView() );
                }
                warning.setVisible(true);
                initializeLocalList();
            }
            success = true;
            event.setDropCompleted(success);
            event.consume();
        });

    }

    public void addFilesInLocal(){
        Stage stage = new Stage(StageStyle.DECORATED);

        File file = fileChooser.showOpenDialog(stage);
        String newPath = userDirectory + File.separator;

        ObservableList<FileInStorage> listOfFiles = FXCollections.observableArrayList();

        if (file != null) {

                long size = 0;

                String name = file.getName();

                if(file.isDirectory()){
                    try {
                        size = getSizeOfDirectory(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else size = file.length();

                File filePath = file.getAbsoluteFile();
                Path srcPath = Paths.get(file.getAbsolutePath());
                File destPath = new File(newPath + file.getName());

                try {
                    Files.copy(srcPath, Paths.get(destPath.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                listOfFiles.add(new FileInStorage(name, filePath, size, false));
                leftListView.setItems(listOfFiles);
                leftListView.setCellFactory(param -> new StorageListView() );

            }
        warning.setVisible(true);
        initializeLocalList();

    }


    public static long getSizeOfDirectory(File file){
        long size = 0;
        if (file.isDirectory()){
            for (File a: file.listFiles()){
                if (a.isFile()){
                    size += a.length();
                }else if (a.isDirectory()){
                    size += getSizeOfDirectory(a);
                }
            }
        }
        return size;
    }


    public void deleteLocalStorageFile(){
        //leftListView.getSelectionModel().clearSelection();
        for (int i = 0; i < getSelectedFilesInLocal().size() ; i++) {
            String filePath = getSelectedFilesInLocal().get(i).toString();
            Path path = Paths.get(filePath);
            File file = new File(filePath);
            try {
                if(file.isDirectory()){
                    deleteIfFileIsDirectory(file);
                }else {
                    Files.delete(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        initializeLocalList();
    }

    public ArrayList getSelectedFilesInLocal(){
        try {
            leftListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ArrayList<File> selectedFilesList = new ArrayList<>();
            if(leftListView.getSelectionModel().getSelectedItems().size() != 0){
                for (int i = 0; i < leftListView.getSelectionModel().getSelectedItems().size(); i++) {
                    selectedFilesList.add(leftListView.getSelectionModel().getSelectedItems().get(i).getPath());
                }
                return selectedFilesList;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteIfFileIsDirectory(File file){
        try {
            if (file.isDirectory()) {
                for (File a : file.listFiles()) {
                    deleteIfFileIsDirectory(a);
                }
            } else {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void deleteFilesByDelete(){
        leftListView.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.DELETE){
                deleteLocalStorageFile();
            }
        });
        initializeLocalList();
    }


    public void dragFromLocalToCloudStorage(){
        leftListView.setOnDragDetected(event -> {
            Dragboard db = leftListView.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            List<File> localFiles = new LinkedList<>();
            localFiles.addAll(getSelectedFilesInLocal());
            content.putFiles(localFiles);
            db.setContent(content);
        });

        leftListView.setOnDragExited(event -> {
            event.acceptTransferModes(TransferMode.NONE);

        });

        rightListView.setOnDragOver(event -> {
            if (event.getGestureSource() != leftListView){
                event.acceptTransferModes(TransferMode.NONE);
            }else {
                event.acceptTransferModes(TransferMode.COPY);
            }

        });

        rightListView.setOnDragExited(event -> {
            event.acceptTransferModes(TransferMode.NONE);
        });
        rightListView.setOnDragDropped(event -> {
            event.acceptTransferModes(TransferMode.COPY);
            transportToCloud();
        });
    }

    public void transportToCloud(){
        ConnectWithServer.transportFileToCloud(UserLogin.getLogin(), getSelectedFilesInLocal());
    }

    public void downloadFileFromCloud(){
        ConnectWithServer.sendFileRequest(getSelectedFilesInCloud());
    }

    public ArrayList getSelectedFilesInCloud(){
        try {
            rightListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ArrayList<File> selectFilesList = new ArrayList<>();
            if(rightListView.getSelectionModel().getSelectedItems().size() != 0){
                for (int i = 0; i < rightListView.getSelectionModel().getSelectedItems().size(); i++) {
                    selectFilesList.add(rightListView.getSelectionModel().getSelectedItems().get(i).getPath());
                }
                return selectFilesList;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void synchLocal(){
        ConnectWithServer.sendSynchroFromCloud(UserLogin.getLogin());
    }

    public void synchroLocalStorageFromCloud(ArrayList<File> files){
        File pathTo = new File(userDirectory);
        File[] listLocalFiles = pathTo.listFiles();
        ArrayList<File> needFiles = new ArrayList<>();
        if(files.size() > 0) {

            boolean flag = false;
            for (File a: files) {
                if(listLocalFiles.length > 0) {
                    for (int i = 0; i < listLocalFiles.length; i++) {

                        String name = listLocalFiles[i].getName();

                        if (a.getName().equals(name)) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag == false) {
                        needFiles.add(a);
                    } else flag = false;
                } else {
                    needFiles.add(a);
                }

            }
            if(needFiles.size() > 0){
                System.out.println("SendFileRequest");
                ConnectWithServer.sendFileRequest(needFiles);
            }
        }
    }

    public void downloadFileFromLocal(){
        ConnectWithServer.sendSynchroFromLocal(UserLogin.getLogin());
    }

    public void synchroCloudStorageFromCloud(ArrayList<File> files){
        File pathTo = new File(userDirectory);
        File[] listLocalFiles = pathTo.listFiles();
        ArrayList<File> needFiles = new ArrayList<>();
        if(listLocalFiles.length > 0) {

            boolean flag = false;
            for (int i = 0; i < listLocalFiles.length; i++) {
                if(files.size() > 0) {
                    for (File a: files) {

                        String name = listLocalFiles[i].getName();

                        if (a.getName().equals(name)) {
                            flag = true;
                            break;
                        }
                    }
                    if (flag == false) {
                        needFiles.add(listLocalFiles[i]);
                    } else flag = false;
                } else {
                    needFiles.add(listLocalFiles[i]);
                }

            }
            if(needFiles.size() > 0){
                System.out.println("SendFileMessage");
                ConnectWithServer.transportFileToCloud(UserLogin.getLogin(), needFiles);
            }
        }
    }

    public void deleteCloudStorageFile(){
        ConnectWithServer.sendDeleteMessage(UserLogin.getLogin(), getSelectedFilesInCloud());
        warning.setVisible(true);
    }

    public void goNextDirectoryOrOpenFileInLocal(MouseEvent event){
        if(event.getClickCount() == 1){
            leftListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } else if(event.getClickCount() == 2){
            leftListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            if(leftListView.getSelectionModel().getSelectedItems().size() == 1){
                File pathToFile = leftListView.getSelectionModel().getSelectedItem().getPath();
                if(pathToFile.isDirectory()){
                    File[] clickedDirectory = pathToFile.listFiles();
                    if(clickedDirectory.length != 0){
                        localStorageLevel++;
                        if(localStorageLevel > 0){
                            backBut1.setVisible(true);
                            userDirectory += File.separator + pathToFile.getName();
                        }
                        ObservableList<FileInStorage> list = FXCollections.observableArrayList();
                        for (int i = 0; i < clickedDirectory.length; i++) {
                            String name = clickedDirectory[i].getName();
                            long size = 0;
                            if(clickedDirectory[i].isDirectory()){
                                size = getSizeOfDirectory(clickedDirectory[i]);
                            } else size = clickedDirectory[i].length();
                            File pathToThisFile = new File(clickedDirectory[i].getAbsolutePath());
                            list.addAll(new FileInStorage(name, pathToThisFile, size, false));
                            leftListView.setItems(list);
                            leftListView.setCellFactory(param -> new StorageListView());
                        }
                    }
                } else {
                    Desktop desktop = null;
                    if(desktop.isDesktopSupported()){
                        desktop = desktop.getDesktop();
                        try {
                            desktop.open(pathToFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    public void goPreviousDirectoryInLocal(){
        ObservableList<FileInStorage> list = FXCollections.observableArrayList();
        ArrayList<File> files = new ArrayList<>();
        File file = new File(userDirectory);
        File prevDerect = new File(file.getParent());
        File[] content = prevDerect.listFiles();
        for (int i = 0; i < content.length; i++) {
            files.add(content[i]);
        }
        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i).getName();
            long size;
            if (files.get(i).isDirectory()) {
                size = getSizeOfDirectory(files.get(i));
            } else size = files.get(i).length();

            File path = files.get(i).getAbsoluteFile();
            list.addAll(new FileInStorage(name, path, size, false));
        }
        leftListView.setItems(list);
        leftListView.setCellFactory(param -> new StorageListView());
        localStorageLevel--;
        if(localStorageLevel <= 0){
            backBut1.setVisible(false);
            userDirectory = "client" + File.separator + "LocalStorage";
        } else userDirectory = prevDerect.toString();

    }



    public void goNextDirectoryOrOpenFileInCloud(MouseEvent event){
        pathsCloudFiles = new ArrayList<>();
        if(event.getClickCount() == 1){
            rightListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } else if(event.getClickCount() == 2){
            rightListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            if(rightListView.getSelectionModel().getSelectedItems().size() != 1){
                File pathToFile = new File("");
                for (int i = 0; i < hashMapCloudStorage.get(cloudStorageLevel).size() ; i++) {
                    File file = hashMapCloudStorage.get(cloudStorageLevel).get(i);
                    if(rightListView.getSelectionModel().getSelectedItem().getName().equals(file.getName())){
                       pathToFile = hashMapCloudStorage.get(cloudStorageLevel).get(i);
                    }
                }
                if(pathToFile.isDirectory()){
                    if(cloudStorageLevel > 0){
                        backBut2.setVisible(true);
                    }
                    File[] directory = pathToFile.listFiles();
                    if(!(directory.length == 0)){
                        for (int i = 0; i < directory.length; i++) {
                            try {
                                pathsCloudFiles.add(directory[i]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        cloudStorageLevel++;

                        hashMapCloudStorage.put(cloudStorageLevel, pathsCloudFiles);
                        ObservableList<FileInStorage> listOfCloudItems = FXCollections.observableArrayList();
                        for (int i = 0; i < directory.length; i++) {
                            String name = directory[i].getName();
                            long size = directory[i].length();
                            File path = new File(directory[i].getAbsolutePath());
                            listOfCloudItems.addAll(new FileInStorage(name, path, size, false));
                            rightListView.setItems(listOfCloudItems);
                            rightListView.setCellFactory(param -> new StorageListView());
                        }
                    }
                }

            }
        }
    }

    public void goPreviousDirectoryInCloud(){
        ObservableList<FileInStorage> list = FXCollections.observableArrayList();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < hashMapCloudStorage.get(cloudStorageLevel - 1).size(); i++) {
            files.add(hashMapCloudStorage.get(cloudStorageLevel - 1).get(i));
        }
        for (int i = 0; i < files.size(); i++) {
            String name = files.get(i).getName();
            long size;
            if(files.get(i).isDirectory()){
                size = getSizeOfDirectory(files.get(i));
            } else size = files.get(i).length();
            File path = new File(files.get(i).getAbsolutePath());
            list.addAll(new FileInStorage(name, path, size, false));
        }
        rightListView.setItems(list);
        rightListView.setCellFactory(param -> new StorageListView());
        hashMapCloudStorage.remove(cloudStorageLevel);
        cloudStorageLevel--;
        if(cloudStorageLevel <= 0) backBut2.setVisible(false);
    }


    public void changeProfile(){
        try {
            Stage stage;
            Parent root;
            stage = (Stage)menuBar.getScene().getWindow();
            root = FXMLLoader.load(getClass().getResource("/firstWindow.fxml"));
            stage.setTitle("Oblachko");
            Image icon = new Image(getClass().getResourceAsStream("/iconfinder_cloud_1287533.png"));
            stage.getIcons().add(icon);
            Scene scene = new Scene(root, 700, 450, Color.BEIGE);
            stage.setResizable(false); //неизменяемость размера
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exitFromProg(){
        Stage stage;
        stage = (Stage) menuBar.getScene().getWindow();
        stage.close();
    }


}

