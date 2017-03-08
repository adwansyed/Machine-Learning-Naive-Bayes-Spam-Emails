package sample;

import java.text.DecimalFormat;

/**
 * Created by asyed on 06/03/17.
 */
public class TestFile {
    private String filename;
    private String spamProbability;
    private String actualClass;

    public TestFile(String filename,
                    String spamProbability,
                    String actualClass) {
        this.filename = filename;
        this.spamProbability = spamProbability;
        this.actualClass = actualClass;
    }

    public String getFilename() { return this.filename; }
    public String getSpamProbability() { return this.spamProbability; }
   /* public String getSpamProbRounded() {
        DecimalFormat df = new DecimalFormat("0.00000");
        return df.format(this.spamProbability);
    }*/
    public String getActualClass() { return this.actualClass; }

    public void setFilename(String value) { this.filename = value; }
    public void setSpamProbability(String val) { this.spamProbability = val; }
    public void setActualClass(String value) { this.actualClass = value; }
}
