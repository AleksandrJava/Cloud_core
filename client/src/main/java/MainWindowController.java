import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.*;

import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;



import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class MainWindowController implements Initializable {

    @FXML
    ListView<FileInStorage> leftListView;
    @FXML
    ListView<FileInStorage> rightListView;
    @FXML
    HBox warning;


    private String userDirectory = "client" + File.separator + "LocalStorage";
    private int localStorageLevel = 0;
    private int cloudStorageLevel = 0;

    private ArrayList<File> returnCloudStorage;
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
                 String name = listLocalFiles[i].getName();
                 if (listLocalFiles[i].isDirectory()){
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
            List<File> localFiles = new LinkedList<File>();
            localFiles.addAll(getSelectedFilesInLocal());
            content.putFiles(localFiles);
            leftListView.setStyle("-fx-opacity: 1;");
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


    public void openMenuChangeOrExit(){
        System.out.println("1");
    }





}

