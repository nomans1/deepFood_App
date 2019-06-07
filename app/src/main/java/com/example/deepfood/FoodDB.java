package com.example.deepfood;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FoodDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "food";
    private final String TABLE_NAME = "Food";
    private final String COLUMN_ID = "id";
    private final String COLUMN_NAME = "name";
    private final String COLUMN_CALORIES = "calories";

    private final String CREATE_TABLE = "create table " + DATABASE_NAME +"(" +
                                                COLUMN_ID + " INTEGER primary key, " +
                                                COLUMN_NAME + " text, " +
                                                COLUMN_CALORIES + " text)";

    private final String DROP_TABLE = "drop table " + TABLE_NAME;

    private final SQLiteDatabase getWritableDB = this.getWritableDatabase();
    private final SQLiteDatabase getReadableDB = this.getReadableDatabase();
    private ContentValues contentValues = new ContentValues();

    public FoodDB(Context context){
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DROP_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    public List<Food> getAllFood() {
        List<Food> foodList = new ArrayList<Food>();

        try{
            Cursor cursor = getReadableDB.rawQuery("SELECT * FROM " + TABLE_NAME, null);
            cursor.moveToFirst();

            for(int i=0 ; i<cursor.getCount() ; i++){
                foodList.add(new Food(cursor.getString(0), cursor.getString(1), cursor.getString(2)));
                System.out.println("Food Irtem : " + cursor.getString(0) + ", " + cursor.getString(1) + ", " + cursor.getString(2));
                cursor.moveToNext();
            }
        } catch (Exception e){
            e.printStackTrace();
        }


        return foodList;
    }

    public void insertFood(Food food){
        contentValues.put(COLUMN_NAME, food.getName());
        contentValues.put(COLUMN_CALORIES, food.getCalories());
        getWritableDB.insert(TABLE_NAME, null, contentValues);
    }

    public void deleteFood(Food food){
        getWritableDB.delete(TABLE_NAME, COLUMN_ID + " = ? ", new String[] { food.getId() });
    }

    public boolean resetDb(){
        onUpgrade(getReadableDB, 0, 0);
        return true;
    }

    public void resetTable(){
        getWritableDB.execSQL("delete from "+TABLE_NAME);
    }



    private void createTableIfNotExist(){
        if(!isTableExist()){
            getWritableDB.execSQL(CREATE_TABLE);
        }
    }

    private boolean isTableExist(){
        Cursor cursor = getReadableDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+CREATE_TABLE+"'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }
}
