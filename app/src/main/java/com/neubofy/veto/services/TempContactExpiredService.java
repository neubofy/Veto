package com.neubofy.veto.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

import com.neubofy.veto.R;
import com.neubofy.veto.data.TemporaryAllowlistRepository;
import com.neubofy.veto.transports.SmsTransport;
import com.neubofy.veto.transports.Transport;
import com.neubofy.veto.utils.VetoLogKt;
import kotlin.Pair;

public class TempContactExpiredService extends JobService {

    private final String TAG = TempContactExpiredService.class.getSimpleName();

    @Override
    public boolean onStartJob(JobParameters params) {
        TemporaryAllowlistRepository repo = TemporaryAllowlistRepository.Companion.getInstance(this);
        List<Pair<String, Integer>> expired = repo.removeExpired();

        for (Pair<String, Integer> temporaryPhoneNumber : expired) {
            String msg = getString(R.string.temporary_allowlist_expired);
            Transport<String> transport = new SmsTransport(this, temporaryPhoneNumber.getFirst(), temporaryPhoneNumber.getSecond());
            transport.send(this, msg, null);
            VetoLogKt.log(this).i(TAG, "Phone number expired: " + temporaryPhoneNumber.getFirst());
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private static final long FIVE_MINS_MILLIS = 5 * 60 * 1000L;

    public static void scheduleJob(Context context, int initialDelay) {
        ComponentName serviceComponent = new ComponentName(context, TempContactExpiredService.class);

        // We need a unique jobId so that if multiple different phone numbers access
        // Veto concurrently, each of them gets their own ExpiredService.
        int jobId = ((Long) System.currentTimeMillis()).intValue();

        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);
        builder.setMinimumLatency(initialDelay);
        builder.setOverrideDeadline(initialDelay + FIVE_MINS_MILLIS);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }
}
