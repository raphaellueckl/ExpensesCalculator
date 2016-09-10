package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Expense;
import model.MessageToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileService {

    private final MessageToast errorMessage;

    public FileService(MessageToast errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean writeListToJson(File path, ObservableList<Expense> expenseList) {
        try (OutputStreamWriter outputFile = new OutputStreamWriter(new FileOutputStream(path), Charset.forName("UTF8"))){
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonToSave = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(expenseList);
            outputFile.write(jsonToSave);

            errorMessage.showSuccessMessage("Saved! :)");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (path != null) {
                errorMessage.showErrorMessage("Random\nError!");
            }
            return false;
        }
    }

//    public boolean readListFromJson(File path) {
//        try {
//            FileChooser fileChooser = new FileChooser();
//            fileChooser.setTitle("Load JSON-File");
//            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON-Document", "*.json"));
//            if (path != null && path.isDirectory()) fileChooser.setInitialDirectory(path);
//            path = fileChooser.showOpenDialog(new Stage());
//            if (path == null) return false;
//
//            loadFile(path.toString());
//
//        } catch (Exception e) {
//            if (path != null)
//                errorMessage.showErrorMessage("Error!");
//        }
//    }

    /**
     * Loads a file by from the path parameter. If the file isn't conform, an errormessage will be displayed.
     */
    public List<Expense> loadFile(String path) {
        try {
            if (path.endsWith(".json")) {
                List<String> loadedJsonAsList = Files.readAllLines(Paths.get(path));

                final String loadedJsonFile = loadedJsonAsList.stream().collect(Collectors.joining());

                ObjectMapper mapper = new ObjectMapper();

                List<Expense> loadedExpenses = Arrays.asList(mapper.readValue(loadedJsonFile, Expense[].class));
                errorMessage.clear();
                return loadedExpenses;
            } else {
                errorMessage.showErrorMessage("Invalid\nFile!");
            }
        } catch (Exception e) {
            if (path.endsWith(".json"))
                errorMessage.showErrorMessage("JSON file\ncorrupted!");
            else if (path != null)
                errorMessage.showErrorMessage("Invalid\nFile!");
        }
        return null;
    }

}
