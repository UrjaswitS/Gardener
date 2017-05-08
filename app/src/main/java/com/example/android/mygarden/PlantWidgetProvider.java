package com.example.android.mygarden;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.ui.PlantDetailActivity;

import static com.example.android.mygarden.PlantWateringService.ACTION_WATER_PLANT;
import static com.example.android.mygarden.PlantWateringService.EXTRA_PLANT_ID;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidgetProvider extends AppWidgetProvider {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int imgRes, int appWidgetId, long plantId, boolean water) {

        // Get current width to decide on single plant vs garden grid view
        Bundle options = appWidgetManager
                .getAppWidgetOptions(appWidgetId);
        int width = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

        RemoteViews rv;
        if (width < 300) {
            rv = getSinglePlantRemoteView(
                    context, imgRes, plantId, water);
        } else {
            rv = getGardenGridRemoteView(context);
        }

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    /**
     * Creates and returns the RemoteViews to be displayed in the single plant mode widget
     *
     * @param context   The context
     * @param imgRes    The image resource of the plant image to be displayed
     * @param plantId   The database plant Id for watering button functionality
     * @param water Boolean to either show/hide the water drop
     * @return The RemoteViews for the single plant mode widget
     */
    private static RemoteViews getSinglePlantRemoteView(Context context, int imgRes, long plantId, boolean water) {
        // Set the click handler to open the DetailActivity for plant ID,
        // or the MainActivity if plant ID is invalid

        //CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(
                context.getPackageName(), R.layout.plant_widget_provider);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        views.setImageViewResource(R.id.widget_plant_image, imgRes);

        Intent intent;
        if (plantId == PlantContract.INVALID_PLANT_ID) {
            intent = new Intent(context, MainActivity.class);
            views.setViewVisibility(R.id.plant_id_text_view, View.INVISIBLE);
        }
        else{
            intent = (new Intent(context, PlantDetailActivity.class))
                    .putExtra(EXTRA_PLANT_ID, plantId);
            views.setTextViewText(R.id.plant_id_text_view, Long.toString(plantId));
        }

        PendingIntent pendIntent = PendingIntent
                .getActivity(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_plant_image, pendIntent);

        if (!water){
            views.setViewVisibility(R.id.widget_water_button, View.INVISIBLE);
            //appWidgetManager.updateAppWidget(appWidgetId, views);
            return views;
        }

        Intent wateringIntent = (new Intent(context, PlantWateringService.class))
                .setAction(ACTION_WATER_PLANT)
                .putExtra(PlantWateringService.EXTRA_PLANT_ID, plantId);

        PendingIntent waterPendIntent = PendingIntent.getService(context, 0,
                wateringIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.widget_water_button, waterPendIntent);
        return views;
        // Instruct the widget manager to update the widget
       // appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Creates and returns the RemoteViews to be displayed in the GridView mode widget
     *
     * @param context The context
     * @return The RemoteViews for the GridView mode widget
     */
    private static RemoteViews getGardenGridRemoteView(Context context) {

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_grid_view);

        // Set the GridWidgetService intent to act as the adapter for the GridView
        Intent intent = new Intent(context, GridWidgetService.class);
        views.setRemoteAdapter(R.id.widget_grid_view, intent);

        // Set the PlantDetailActivity intent to launch when clicked
        Intent appIntent = new Intent(context, PlantDetailActivity.class);
        PendingIntent appPendingIntent = PendingIntent
                .getActivity(context, 0, appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        views.setPendingIntentTemplate(R.id.widget_grid_view, appPendingIntent);
        // Handle empty gardens
        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view);
        return views;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        PlantWateringService.startActionUpdatePlantWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
       /* for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }*/
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void updatePlantWidgets(Context context,
                                          AppWidgetManager manager, int imgRes, int[] widgetIds, long plantId, boolean water) {
        for (int id : widgetIds){
            updateAppWidget(context, manager, imgRes, id, plantId, water);
        }
    }
}

