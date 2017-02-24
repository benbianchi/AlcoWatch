package edu.wpi.alcowatch.alcowatch.User;

/**
 * Created by ben on 1/29/17.
 */

public class Profile {

    private String gender;

    private Integer age;

    private Integer weight;

    private Integer height;

    public Profile(String gender, Integer age, Integer weight, Integer height)
    {
        this.gender = gender;

        this.age = age;

        this.weight=weight;

        this.height=height;
    }

    public void setGender(String newGender)
    {
        if (newGender.toLowerCase() == "male")
            this.gender = "male";
        else if (newGender.toLowerCase() == "female")
            this.gender = "female";

        else
            throw new IllegalArgumentException("Gender was not male or female");

    }

    public String getGender()
    {
        return this.gender;
    }

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

    public Integer getWeight()
    {
        return this.weight;
    }

    public void setHeight(Integer newHeight)
    {
        if (newHeight < 0)
            throw new IllegalArgumentException("Cannot have a negative height!");
        else
            this.height = newHeight;
    }

    public Integer getHeight(){
        return this.height;
    }

}
