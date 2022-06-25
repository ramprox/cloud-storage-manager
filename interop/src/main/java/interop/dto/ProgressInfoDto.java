package interop.dto;

import java.io.Serializable;

public class ProgressInfoDto implements Serializable {

    private String currentFilePath;

    private double totalPercentage;

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public void setCurrentFilePath(String currentFilePath) {
        this.currentFilePath = currentFilePath;
    }

    public double getTotalPercentage() {
        return totalPercentage;
    }

    public void setTotalPercentage(double totalPercentage) {
        this.totalPercentage = totalPercentage;
    }
}
