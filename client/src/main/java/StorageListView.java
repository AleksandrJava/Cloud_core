import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class StorageListView extends ListCell<FileInStorage> {

    FXMLLoader loader;

    public Label name;
    public Label size;
    public CheckBox isChoose;

    public VBox cellView;

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(selected);
        if(selected){
            isChoose.setSelected(true);
        } else isChoose.setSelected(false);
    }

    @Override
    protected void updateItem(FileInStorage item, boolean empty) {
        super.updateItem(item, empty);

        if(item == null){
            setText(null);
            setGraphic(null);
        } else {
            if(loader == null){
                loader = new FXMLLoader(getClass().getResource("/CellView.fxml"));
                loader.setController(this);
                try {
                    loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            name.setText(item.getName());

            long currentSize = item.getSize();

            if(currentSize / (1024*1024*1024) > 0){
                size.setText(currentSize / (1024*1024*1024) + " GB");
            } else if(currentSize / (1024*1024) > 0){
                size.setText(currentSize / (1024*1024) + " MB");
            } else if(currentSize / 1024 > 0){
                size.setText(currentSize / 1024 + " KB");
            } else size.setText(currentSize + " bytes");

            item.setChoose(isChoose.isSelected());
            isChoose.setSelected(item.isChoose());

        }
        setText(null);
        setGraphic(cellView);
    }
}
