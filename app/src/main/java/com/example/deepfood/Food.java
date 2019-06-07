package com.example.deepfood;

public class Food {

    private String id;
    private String name;
    private String calories;

    public Food(String id, String name, String calories) {
        this.id = id;
        this.name = name;
        this.calories = calories;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCalories() {
        return calories;
    }

    public void setCalories(String calories) {
        this.calories = calories;
    }
}
