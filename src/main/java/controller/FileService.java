package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ObservableList;
import model.Transaction;

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

    public boolean writeListToJson(File path, ObservableList<Transaction> transactionList) {
        try (OutputStreamWriter outputFile = new OutputStreamWriter(new FileOutputStream(path), Charset.forName("UTF8"))){
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonToSave = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transactionList);
            outputFile.write(jsonToSave);

            errorMessage.showSuccessMessage("Saved! :)");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (path != null) {
                errorMessage.showErrorMessage("Random Error!");
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
    public List<Transaction> loadFile(String path) {
        try {
            if (path.endsWith(".json")) {
                List<String> loadedJsonAsList = Files.readAllLines(Paths.get(path));

                final String loadedJsonFile = loadedJsonAsList.stream().collect(Collectors.joining());

                ObjectMapper mapper = new ObjectMapper();

                List<Transaction> loadedEntities = Arrays.asList(mapper.readValue(loadedJsonFile, Transaction[].class));
                errorMessage.clear();
                return loadedEntities;
            } else {
                errorMessage.showErrorMessage("Invalid File!");
            }
        } catch (Exception e) {
            if (path.endsWith(".json"))
                errorMessage.showErrorMessage("JSON file corrupted!");
            else if (path != null)
                errorMessage.showErrorMessage("Invalid File!");
        }
        return null;
    }

}
