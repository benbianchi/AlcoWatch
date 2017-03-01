package edu.wpi.alcowatch.alcowatch.Classification;

import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by Jacob Watson on 2/16/2017.
 */

/**
 * Created by Christina Aiello on 3/19/2016.
 *
 * This class is used to put the generated features data into
 * a format that the Weka library can interact with.
 *
 * Reference: Weka Documentation
 * Reference URLs:
 * https://weka.wikispaces.com/Creating+an+ARFF+file
 * https://weka.wikispaces.com/Adding+attributes+to+a+dataset
 * https://weka.wikispaces.com/Save+Instances+to+an+ARFF+File
 */
public class WekaDataFormatter {
    ArrayList<Attribute> atts;
    ArrayList<String>      ratioVals;
    ArrayList<String>      thdVals;
    ArrayList<String>      drinksBinVals;
    Instances data;
    Instance myDataInstance;

    public WekaDataFormatter (Double numSteps, Double bandpower, Double totalHarmonicDistortion, Double xzSwayArea, Double xySwayArea,
                              Double yzSwayArea, Double distYForward, Double rollVelVarianceForward, Double rollVelMedianForward,  Double rollVelVarianceBackward,
                              Double pitchVelMedianForward, Double pitchVelVarianceForward, Double pitchVelMedianBackward, Double pitchVelVarianceBackward, Double yawVelMedianForward,
                              Double yawVelVarianceForward, Double yawVelMedianBackward, Double yawVelVarianceBackward, Double pitchForward, Double pitchBackward, Double yawForward,
                              Double xVelMedianBackward, Double yVelMedianForward, Double yVelVarianceBackward, Double yVelMedianBackward, Double height, Double weight, Double gender, Double age) {
        // First, set up the attributes:
        atts = new ArrayList<Attribute>();
        atts.add(new Attribute("numSteps"));
        atts.add(new Attribute("bandpower"));
        atts.add(new Attribute("totalHarmonicDistortion"));
        atts.add(new Attribute("xzSwayArea"));
        atts.add(new Attribute("xySwayArea"));
        atts.add(new Attribute("yzSwayArea"));
        atts.add(new Attribute("distYForward"));
        atts.add(new Attribute("rollVelVarianceForward"));
        atts.add(new Attribute("rollVelMedianForward"));
        atts.add(new Attribute("rollVelVarianceBackward"));
        atts.add(new Attribute("pitchVelMedianForward"));
        atts.add(new Attribute("pitchVelVarianceForward"));
        atts.add(new Attribute("pitchVelMedianBackward"));
        atts.add(new Attribute("pitchVelVarianceBackward"));
        atts.add(new Attribute("yawVelMedianForward"));
        atts.add(new Attribute("yawVelVarianceForward"));
        atts.add(new Attribute("yawVelMedianBackward"));
        atts.add(new Attribute("yawVelVarianceBackward"));
        atts.add(new Attribute("pitchForward"));
        atts.add(new Attribute("pitchBackward"));
        atts.add(new Attribute("yawForward"));
        atts.add(new Attribute("xVelMedianBackward"));
        atts.add(new Attribute("yVelMedianForward"));
        atts.add(new Attribute("yVelVarianceBackward"));
        atts.add(new Attribute("yVelMedianBackward"));
        atts.add(new Attribute("height"));
        atts.add(new Attribute("weight"));
        atts.add(new Attribute("age"));
        atts.add(new Attribute("gender"));

        drinksBinVals = new ArrayList<String>();
        drinksBinVals.add("a");
        drinksBinVals.add("b");
        atts.add(new Attribute("DrinksBin", drinksBinVals));

        // Second, create Instances object
        data = new Instances("DataToClassify", atts, 1);

        myDataInstance = new DenseInstance(data.numAttributes());
        data.add(myDataInstance);


        // Third, add data

        data.instance(0).setValue(0, numSteps);
        data.instance(0).setValue(1, bandpower);
        data.instance(0).setValue(2, totalHarmonicDistortion);
        data.instance(0).setValue(3, xzSwayArea);
        data.instance(0).setValue(4, xySwayArea);
        data.instance(0).setValue(5, yzSwayArea);
        data.instance(0).setValue(6, distYForward);
        data.instance(0).setValue(7, rollVelVarianceForward);
        data.instance(0).setValue(8, rollVelMedianForward);
        data.instance(0).setValue(9, rollVelVarianceBackward);
        data.instance(0).setValue(10, pitchVelMedianForward);
        data.instance(0).setValue(11, pitchVelVarianceForward);
        data.instance(0).setValue(12, pitchVelMedianBackward);
        data.instance(0).setValue(13, pitchVelVarianceBackward);
        data.instance(0).setValue(14, yawVelMedianForward);
        data.instance(0).setValue(15, yawVelVarianceForward);
        data.instance(0).setValue(16, yawVelMedianBackward);
        data.instance(0).setValue(17, yawVelVarianceBackward);
        data.instance(0).setValue(18, pitchForward);
        data.instance(0).setValue(19, pitchBackward);
        data.instance(0).setValue(20, yawForward);
        data.instance(0).setValue(21, xVelMedianBackward);
        data.instance(0).setValue(22, yVelMedianForward);
        data.instance(0).setValue(23, yVelVarianceBackward);
        data.instance(0).setValue(24, yVelMedianBackward);
        data.instance(0).setValue(25, height);
        data.instance(0).setValue(26, weight);
        data.instance(0).setValue(27, age);
        data.instance(0).setValue(28, gender);
        data.instance(0).setValue(29, "a");

    }

    /*
     * This function returns the data variable, which is
     * the data in "Instances" form
     */
    public Instances getInstanceData(){
        return data;
    }
}
