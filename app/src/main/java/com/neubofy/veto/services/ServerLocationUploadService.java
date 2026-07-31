package com.neubofy.veto.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.jetbrains.annotations.Nullable;

import com.neubofy.veto.data.BackgroundLocationType;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;
import com.neubofy.veto.utils.VetoLogKt;
import com.neubofy.veto.utils.NetworkUtils;
import com.neubofy.veto.workers.CommandExecutionWorker;


/**
 * Uploads the location at regular intervals in the background
 */
public class ServerLocationUploadService extends VetoJobService {

    private static final String TAG = ServerLocationUploadService.class.getSimpleName();
    public static final String SOURCE_REGULAR_BACKGROUND_UPLOAD = "Regular Background Upload";

    private static final int JOB_ID = 108; // for recurring jobs only

    private SettingsRepository settings;

    @Override
    public @Nullable String getTAG() {
        return TAG;
    }

    public static void scheduleRecurring(Context context) {
        scheduleJob(context, 0);
    }

    public static void scheduleJob(Context context, long delayMinutes) {
        // Feature disabled per user request (Coming Soon)
        cancelJob(context);
        if (true) return;

        VetoLogKt.log(context).d(TAG, "Scheduling upload service");
        SettingsRepository settings = SettingsRepository.Companion.getInstance(context);

        int locTypeInt = (int) settings.get(Settings.SET_VetoSERVER_LOCATION_TYPE);
        BackgroundLocationType locType = new BackgroundLocationType(locTypeInt);

        if (locType.isEmpty()) {
            // user requested NOT to upload any location at regular intervals
            VetoLogKt.log(context).d(TAG, "Not scheduling job. Reason: user requested no upload");
            return;
        }

        ComponentName serviceComponent = new ComponentName(context, ServerLocationUploadService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        // We cannot use setPeriodic() because that only works for periods >= 15 mins
        // builder.setPeriodic(intervalMinutes * 60 * 1000);
        // Instead we use setMinimumLatency().
        builder.setMinimumLatency(delayMinutes * 60 * 1000);

        // Force the job to run within a 2 minute window, even if its constraints aren't satisfied.
        // This is to (hopefully) improve reliability and timeliness.
        // We can abort the job later if there is no network connection.
        builder.setOverrideDeadline(((delayMinutes + 2) * 60 * 1000));

        builder.setPersisted(true);

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    public static void cancelJob(Context context) {
        VetoLogKt.log(context).d(TAG, "Cancelling upload service");
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        super.onStartJob(params);

        settings = SettingsRepository.Companion.getInstance(this);

        if (!settings.serverAccountExists()) {
            VetoLogKt.log(this).i(TAG, "No account, stopping and cancelling job.");
            cancelJob(this);
            return false;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            VetoLogKt.log(this).i(TAG, "No network connection, stopping job.");
            jobFinished();
            return false;
        }

        long now = System.currentTimeMillis();
        long lastUploadTimeMillis = ((Number) settings.get(Settings.SET_LAST_KNOWN_LOCATION_TIME)).longValue();
        long uploadIntervalMillis = ((int) settings.get(Settings.SET_VetoSERVER_UPDATE_TIME)) * 60 * 1000L;
        if (lastUploadTimeMillis + uploadIntervalMillis / 2 > now) {
            VetoLogKt.log(this).i(TAG, "Skipping upload, last upload was recent");
            jobFinished();
            return false;
        }

        int locTypeInt = (int) settings.get(Settings.SET_VetoSERVER_LOCATION_TYPE);
        BackgroundLocationType locType = new BackgroundLocationType(locTypeInt);

        String locateCommand = settings.get(Settings.SET_Veto_COMMAND) + " locate";
        if (locType.getGps()) {
            locateCommand += " gps";
        }

        // TODO: Should we use a PeriodicWorkRequest?? Instead of creating Work from a Job?
        Data inputData = new Data.Builder()
                .putString(CommandExecutionWorker.KEY_COMMAND, locateCommand)
                .putString(CommandExecutionWorker.KEY_TRANSPORT_TYPE, CommandExecutionWorker.TRANS_Veto_SERVER)
                .putString(CommandExecutionWorker.KEY_DESTINATION, SOURCE_REGULAR_BACKGROUND_UPLOAD)
                .build();
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(CommandExecutionWorker.class)
                .setInputData(inputData)
                .build();
        WorkManager.getInstance(this).enqueue(workRequest);

        // Schedule next occurrence now. The work request is single-shot, and is unaware of the periodicity.
        scheduleNextOccurrence();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        super.onStopJob(params);
        scheduleNextOccurrence();
        return false;
    }

    @Override
    public void jobFinished() {
        super.jobFinished();
        scheduleNextOccurrence();
    }

    private void scheduleNextOccurrence() {
        long intervalMinutes = ((Integer) settings.get(Settings.SET_VetoSERVER_UPDATE_TIME)).longValue();
        if (intervalMinutes <= 0) {
            VetoLogKt.log(this).i(TAG, "Raising interval from " + intervalMinutes + " mins to 1 min");
            intervalMinutes = 1;
        }

        scheduleJob(this, intervalMinutes);
    }
}
