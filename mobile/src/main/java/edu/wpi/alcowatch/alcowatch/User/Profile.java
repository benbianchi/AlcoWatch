package edu.wpi.alcowatch.alcowatch.User;

/**
 * This class is just a data structure for the app to use. In reality, it is not used in this application. The profile is stored in a shared preferences object.
 * However, this class validates all of the biometric data. Gender can only be "male" or "female".
 * @author Benjamin Bianchi
 * @version  1
 */
public class Profile {
    /**
     * The Birth gender of the subject. Is "male" or "female"
     */
    private String gender;

    /**
     * The age of the subject.
     */
    private Integer age;
    /**
     * The weight of the subject, in pounds.
     */
    private float weight;

    /**
     * The height of the subject, in inches, however, when processed this variable is converted into centimeters.
     */
    private float height;

    /**
     * Create a profile data structre object
     * @param gender the gender of the subject
     * @param age the age of the subject
     * @param weight the weight of the subject, in pounds
     * @param height the height of the subject, in inches
     */
    public Profile(String gender, int age, float weight, float height)
    {
        this.gender = gender;

        this.age = age;

        this.weight=weight;

        this.height=height;
    }

    /**
     * Setter for the subject's gender
     * @param newGender the gender you are setting.
     */
    public void setGender(String newGender)
    {
        if (newGender.toLowerCase() == "male")
            this.gender = "male";
        else if (newGender.toLowerCase() == "female")
            this.gender = "female";

        else
            throw new IllegalArgumentException("Gender was not male or female");

    }

    /**
     * getter for the subjects gender
     * @return returns either "female" or "male"
     */
    public String getGender()
    {
        return this.gender;
    }

    /**
     * Setter for age
     * @param newAge the age you are going to set for the subject
     */
    public void setAge(Integer newAge)
    {
        if (newAge < 0)
            throw new IllegalArgumentException("Cannot be negative years old!");
        else
            this.age = newAge;
    }

    public Integer getAge()
    {
        return this.age;
    }

    public void setWeight(Integer newWeight)
    {
        if (newWeight < 0)
            throw new IllegalArgumentException("Cannot have a negative weight!");
        else
            this.weight = newWeight;
    }

    /**
     * Getter for the weight of the subject
     * @return a float that represents the weight of the subject
     */
    public float getWeight()
    {
        return this.weight;
    }

    /**
     * Setter for the subject's height.
     * @param newHeight the new height in inches for the subject.
     */
    public void setHeight(Integer newHeight)
    {
        if (newHeight < 0)
            throw new IllegalArgumentException("Cannot have a negative height!");
        else
            this.height = newHeight;
    }

    /**
     * getter for the subject's height
     * @return a float denoting the height of the subject.
     */
    public float getHeight(){
        return this.height;
    }

}
