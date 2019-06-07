package com.example.deepfood;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

//import android.support.v7.app.AppCompatActivity;

public class FoodActivity extends Activity {

    private RecyclerView listView;
    private ArrayList<Food> foodList;
    private FoodDB foodDB;
//    private CustomArrayAdapter customArrayAdapter;
    private RecyclerViewAdapter recyclerAdapter;
    ArrayAdapter adapter;
    private FloatingActionButton floatingActionButton;
    ArrayList<String> stringArrayList;
    private Button delete;
    float x1, x2, y1, y2;
    BottomAppBar bottomAppBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.food_list_layout);



        init();
        fetchFoodList();
        goBacktoCamera();
    }

    private void goBacktoCamera() {
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FoodActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        bottomAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.camera_bottom:
                        Intent intent = new Intent(FoodActivity.this, MainActivity.class);
                        startActivity(intent);
                        break;
                }
                return false;
            }
        });
    }

    private void init(){
        listView = (RecyclerView) findViewById(R.id.food_list);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.camera_fab);
        foodList = new ArrayList<Food>();
        foodDB = new FoodDB(this);
        bottomAppBar = findViewById(R.id.bar);

//        listView.setDivider(null);

//        customArrayAdapter = new CustomArrayAdapter(this, foodList);
        recyclerAdapter = new RecyclerViewAdapter(this,foodList);
        stringArrayList = new ArrayList<String>();
        listView.setAdapter(recyclerAdapter);
        listView.setLayoutManager(new LinearLayoutManager(this));


    }

    private void fetchFoodList(){
        List<Food> newFoodList = foodDB.getAllFood();

        if(newFoodList != null){
            foodList.clear();
            foodList.addAll(newFoodList);
            recyclerAdapter.notifyDataSetChanged();
        }

        for(Food food : foodList)
            stringArrayList.add(food.getName() + "\n" + food.getCalories());

        recyclerAdapter.notifyDataSetChanged();
    }

//    public boolean onTouchEvent(MotionEvent touchEvent){
//        switch(touchEvent.getAction()){
//            case MotionEvent.ACTION_DOWN:
//                x1 = touchEvent.getX();
//                y1 = touchEvent.getY();
//                break;
//            case MotionEvent.ACTION_UP:
//                x2 = touchEvent.getX();
//                y2 = touchEvent.getY();
//                 if(x1 < x2){
//                    Intent i = new Intent(FoodActivity.this, CameraActivity.class);
//                    startActivity(i);
//                }
//                break;
//        }
//        return false;
//    }


}
