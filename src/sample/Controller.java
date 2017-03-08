package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class Controller {

    @FXML private TableView<TestFile> table;
    @FXML private TableColumn<TestFile, String> fileColumn;
    @FXML private TableColumn<TestFile, String> actualClassColumn;
    @FXML private TableColumn<TestFile, Double> probabilityColumn;
    @FXML private TextField accuracyID;
    @FXML private TextField precisionID;
    @FXML private TextField trainpathID;
    @FXML private TextField testpathID;

    private HashMap<String,Double> HFreq = new HashMap<String, Double>();
    private HashMap<String,Integer> HamWordCount = new HashMap<String,Integer>();

    private HashMap<String,Double> SFreq = new HashMap<String, Double>();
    private HashMap<String,Integer> SpamWordCount = new HashMap<String,Integer>();

    private HashMap<String,Double> SpamGivenWord = new HashMap<String,Double>();

    double numTruePostives = 0;
    double numFalsePositives = 0;
    double numTrueNegatives = 0;
    double accuracy;
    double precision;
    double numTestFiles;

    private boolean isWord(String str){
        String pattern = "^[a-zA-Z]*$";
        if (str.matches(pattern)){
            return true;
        }
        return false;
    }


/*--------------------------------------------------------------------------------------------------------------------*/
//TRAINING PHASE

    // Train button event handlers
    public void TrainButtonAction(ActionEvent event){

        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File("."));
        File md = dc.showDialog(null);

        if (md != null){
            String path = md.getAbsolutePath();
            trainpathID.setText(path);
            processTrain(md);

            // P(S|W) = P(W|S) / ( P(W|S) + P(W|H) ) and store in map
            trainSpamGivenWord();
        }else{
            System.out.println("Directory not valid");
        }
    }

    // process spam and ham folders for training directory
    public void processTrain(File file){

        if (file.isDirectory()){
            if (file.getName().equals("ham")){
                try {
                    trainHamFrequency(file);
                }catch (IOException e){
                    e.printStackTrace();
                }
                System.out.println("DONE Folder: ../ham");
            }else if(file.getName().equals("spam")) {
                try {
                    trainSpamFrequency(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("DONE Folder: ../spam");
            }else if(file.getName().equals("ham2")){
                try {
                    trainHamFrequency(file);
                }catch (IOException e){
                    e.printStackTrace();
                }
                System.out.println("DONE Folder: ../ham2");
            }else {
                File[] filesInDir = file.listFiles();
                for (int i = 0; i < filesInDir.length; i++){
                    processTrain(filesInDir[i]);
                }
            }
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/
//TESTING PHASE


    // test button event handler
    public void TestButtonAction(ActionEvent event){

        // launch dialog for directory chooser
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File("."));
        File md = dc.showDialog(null);  // main directory

        if (md != null){
            String path = md.getAbsolutePath();
            testpathID.setText(path);
            processTest(md);
            System.out.println(numTestFiles);
            System.out.println(numTruePostives + " " + numFalsePositives + " " + numTrueNegatives);

            // calculate and format accuracy and precision
            DecimalFormat df = new DecimalFormat("0.00000");
            accuracy = (numTruePostives + numTrueNegatives)/numTestFiles;
            accuracyID.setText(df.format(accuracy));

            precision = numTruePostives/ (numFalsePositives + numTrueNegatives);
            precisionID.setText(df.format(precision));

            // add values to table columns
            fileColumn.setCellValueFactory(new PropertyValueFactory<TestFile, String>("filename"));
            actualClassColumn.setCellValueFactory(new PropertyValueFactory<TestFile, String>("actualClass"));
            probabilityColumn.setCellValueFactory(new PropertyValueFactory<TestFile, Double>("spamProbability"));
        }else{
            System.out.println("Directory not valid");
        }
    }

    public void processTest(File file){

        if (file.isDirectory()){

            //process all files recursively
            File[] filesInDir = file.listFiles();
            for (int i = 0; i < filesInDir.length; i++){
                processTest(filesInDir[i]);
            }
        }else if (file.exists()){
            double spamProbability = 0.0;
            // calculate spam probability of test files
            try {
                spamProbability = testProbability(file);

            } catch (IOException e) {
                e.printStackTrace();
            }

            // format data and add to table data
            DecimalFormat df = new DecimalFormat("0.00000");

            if (file.getParent().contains("ham")){
                table.getItems().add(new TestFile(file.getName(), df.format(spamProbability), "ham"));
            }else{
                table.getItems().add(new TestFile(file.getName(),df.format(spamProbability), "spam"));
            }
        }
    }

/*--------------------------------------------------------------------------------------------------------------------*/
//PROBABILITY METHODS

    // probability Pr(S|F)
    public double testProbability(File file) throws FileNotFoundException {
        double pSF;
        double n = 0.0;
        double threshold = 0.5;
        /* Explanation:
           For each word
                ->  n += Math.log(1-P(S|W)) - Math.log(Pr(S|W)
           P(S|F) = 1/(1+Math.pow(Math.E,n)                     */

        Scanner scanner = new Scanner(file);
        while(scanner.hasNext()){
            String word = scanner.next();
            if (isWord(word)) {
                if (SpamGivenWord.containsKey(word)){
                    n += Math.log( (1 - SpamGivenWord.get(word) - Math.log(SpamGivenWord.get(word))) );
                }
            }
        }
        pSF = 1/(1 + Math.pow(Math.E,n));

        // accumulate accuracy/precision statistics
        if (file.getParent().contains("spam") && pSF > threshold){
            numTruePostives += 1;
        }
        if (file.getParent().contains("ham") && pSF > threshold){
            numFalsePositives += 1;
        }
        if (file.getParent().contains("ham") && pSF < threshold){
            numTrueNegatives += 1;
        }
        numTestFiles += 1;
       return pSF;
    }

    // probability Pr(W|H)
    public void trainHamFrequency(File file) throws IOException{

            File[] filesInDir = file.listFiles();
            System.out.println("Training...");
            System.out.println("# of Files: " + filesInDir.length);
            for (int i = 0; i < filesInDir.length; i++){
                HashMap<String, Integer> temp = new HashMap<String, Integer>();

                // Gather list of words in specific file and put in temporary Map
                Scanner scanner = new Scanner(filesInDir[i]);
                while(scanner.hasNext()){
                    String word = scanner.next();
                    if (isWord(word)) {
                        if (!temp.containsKey(word)) {
                            temp.put(word, 1);
                        }
                    }
                }

                // iterate through temp and insert word list into WordCount Map
                for (Map.Entry<String,Integer> entry: temp.entrySet()){
                    if (HamWordCount.containsKey(entry.getKey())){
                        int oldCount = HamWordCount.get(entry.getKey());
                        HamWordCount.put(entry.getKey(), oldCount + 1);
                    }else{
                        HamWordCount.put(entry.getKey(), 1);
                    }
                }

                // Clear word list so temporary Map can be reused for later files
                temp.clear();

                // Calculate W|Ham Frequency and put in Map
                // # of ham files containing word/# of ham files
                for (Map.Entry<String,Integer> entry: HamWordCount.entrySet()){
                    double pWH = (double)entry.getValue()/(double)filesInDir.length;
                    HFreq.put(entry.getKey(),pWH);
                }
            }
    }

    // probability (W}S)
    public void trainSpamFrequency(File file) throws IOException{

        File[] filesInDir = file.listFiles();
        System.out.println("Training...");
        System.out.println("# of Files: " + filesInDir.length);
        for (int i = 0; i < filesInDir.length; i++){
            HashMap<String, Integer> temp = new HashMap<String, Integer>();

            // Gather list of words in specific file and put in temporary Map
            Scanner scanner = new Scanner(filesInDir[i]);
            while(scanner.hasNext()){
                String word = scanner.next();
                if (isWord(word)) {
                    if (!temp.containsKey(word)) {
                        temp.put(word, 1);
                    }
                }
            }

            // iterate through temp and insert word list into WordCount Map
            for (Map.Entry<String,Integer> entry: temp.entrySet()){
                if (SpamWordCount.containsKey(entry.getKey())){
                    int oldCount = SpamWordCount.get(entry.getKey());
                    SpamWordCount.put(entry.getKey(), oldCount + 1);
                }else{
                    //System.out.println(entry.getKey());
                    SpamWordCount.put(entry.getKey(), 1);
                }
            }

            // Clear word list so temporary Map can be reused for later files
            temp.clear();

            // Calculate W|Spam Frequency and put in Map
            // # of spam files containing Word / # of spam files
            for (Map.Entry<String,Integer> entry: SpamWordCount.entrySet()){
                double pWS = (double)entry.getValue()/(double)filesInDir.length;
                SFreq.put(entry.getKey(),pWS);
            }
        }
    }

    // probability Pr(S|W)
    public void trainSpamGivenWord(){
        for (Map.Entry<String,Double> entry: SFreq.entrySet()){
            if (HFreq.containsKey(entry.getKey())) {
                double pSW = entry.getValue() / (entry.getValue() + HFreq.get(entry.getKey()));
                SpamGivenWord.put(entry.getKey(),pSW);
            }
        }

    }


}
