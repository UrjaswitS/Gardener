package com.example.android.mygarden;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.INVALID_PLANT_ID;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;
import static com.example.android.mygarden.provider.PlantContract.PlantEntry.COLUMN_CREATION_TIME;
import static com.example.android.mygarden.provider.PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME;

/**
 * Created by UrJasWitK on 01-May-17.
 */

public class PlantWateringService extends IntentService {

    public static final String ACTION_WATER_PLANT = "com.example.android.mygarden.action.water_plant";
    public static final String ACTION_UPDATE_PLANT_WIDGETS = "com.example.android.mygarden.action.update_plant_widgets";
    public static final String EXTRA_PLANT_ID = "plantid";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public PlantWateringService() {
        super("Plant Watering Service");
    }

    public static void startActionWaterPlant(Context context, long plantId){
        context.startService((new Intent(context, PlantWateringService.class)
                                .setAction(ACTION_WATER_PLANT)
                                .putExtra(EXTRA_PLANT_ID, plantId)));
    }

    public static void startActionUpdatePlantWidgets(Context context){
        context.startService((new Intent(context, PlantWateringService.class)
                .setAction(ACTION_UPDATE_PLANT_WIDGETS)));
    }

    private void handleActionWaterPlant(long plantId){
        Uri plantUri = ContentUris.withAppendedId(PlantContract.BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_PLANTS).build(), plantId);

        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_WATERED_TIME, System.currentTimeMillis());
        getContentResolver().update(plantUri,
                values, COLUMN_LAST_WATERED_TIME + ">?",
                new String[]{String.valueOf(
                        System.currentTimeMillis() - PlantUtils.MAX_AGE_WITHOUT_WATER)});

    }



    private void handleActionUpdatePlantWidgets(){

        Uri updateUri = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_PLANTS).build();
        Cursor cursor = getContentResolver().query(
                updateUri, null, null, null, COLUMN_LAST_WATERED_TIME);

        int imgRes = R.drawable.grass;
        boolean water=false;
        long plantId =INVALID_PLANT_ID;
        if (cursor != null && cursor.getCount() > 0){
            cursor.moveToFirst();

            int plantType = cursor.getInt(cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE));
            long watered = cursor.getLong(cursor.getColumnIndex(COLUMN_LAST_WATERED_TIME)),
                    created = cursor.getLong(cursor.getColumnIndex(COLUMN_CREATION_TIME));
            plantId = cursor.getLong(cursor.getColumnIndex(PlantContract.PlantEntry._ID));
            long timeNow = System.currentTimeMillis();

            water = ((timeNow - watered) > PlantUtils.MIN_AGE_BETWEEN_WATER) &&
                    ((timeNow - watered) < PlantUtils.MAX_AGE_WITHOUT_WATER);
            imgRes = PlantUtils.getPlantImageRes(this, timeNow-created,
                    timeNow-watered, plantType);

            cursor.close();
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] widgetIds = manager.getAppWidgetIds(
                new ComponentName(this, PlantWidgetProvider.class));

        manager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_grid_view);
        PlantWidgetProvider.updatePlantWidgets(this,
                manager, imgRes, widgetIds,
                plantId, water);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction().equals(ACTION_WATER_PLANT)){
            handleActionWaterPlant(intent.getLongExtra(EXTRA_PLANT_ID, INVALID_PLANT_ID));
        }
        else if (intent != null && intent.getAction().equals(ACTION_UPDATE_PLANT_WIDGETS)){
            handleActionUpdatePlantWidgets();
        }
    }
}
