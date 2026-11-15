package io.github.realkhfan.onda.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Twice-a-day background check for new releases. Android's own JobScheduler: no extra library, runs only with a network. */
class ReleaseJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            val reached = try {
                ArtistIds.load(applicationContext)
                Releases.load(applicationContext)
                Releases.check(applicationContext)
            } catch (e: CancellationException) {
                throw e // stopped by the system; it reschedules on its own
            } catch (_: Exception) {
                false
            }
            jobFinished(params, !reached) // retry later if Deezer wasn't reachable
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope.cancel()
        return true
    }

    companion object {
        private const val ID = 4201

        fun schedule(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java) ?: return
            if (js.getPendingJob(ID) != null) return
            js.schedule(
                JobInfo.Builder(ID, ComponentName(context, ReleaseJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(12 * 60 * 60 * 1000L)
                    .setPersisted(true) // survives a restart
                    .build(),
            )
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java)?.cancel(ID)
        }
    }
}
