package sample;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DataSource {
    public static ObservableList<TestFile> getTestFiles(){
        ObservableList<TestFile> list = FXCollections.observableArrayList();
        return list;
    }
}
