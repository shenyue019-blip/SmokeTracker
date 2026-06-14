package com.smoketracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.smoketracker.app.R
import com.smoketracker.app.SmokeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 桌面小组件：点一下 = 记录抽一根（默认烟品）。
 * 兼容性要点（vivo OriginOS / 三星 OneUI 等）：
 *  - 点击用显式广播，app 被系统冻结/杀掉时也能拉起进程写库
 *  - PendingIntent 带 FLAG_IMMUTABLE（Android 12+ 必需）
 *  - updatePeriodMillis=0，靠点击主动刷新，避免被省电策略限制
 */
class SmokeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SMOKE) {
            val result = goAsync() // 让进程在异步写库期间保持存活
            scope.launch {
                var name: String? = null
                try {
                    val repo = (context.applicationContext as SmokeApp).repository
                    val cig = repo.defaultCigaretteOnce()
                    if (cig != null) {
                        repo.smokeOne(cig)
                        name = cig.name
                    }
                    pushViews(context)
                } finally {
                    withContext(Dispatchers.Main) {
                        val msg = if (name != null) "已记录一根 $name" else "请先在 App 里添加默认烟品"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    result.finish()
                }
            }
        }
    }

    private fun render(context: Context) {
        scope.launch { pushViews(context) }
    }

    private suspend fun pushViews(context: Context) {
        val repo = (context.applicationContext as SmokeApp).repository
        val count = runCatching { repo.smokeCountSince(startOfToday()) }.getOrDefault(0)

        val views = RemoteViews(context.packageName, R.layout.widget_smoke)
        views.setTextViewText(R.id.widget_count, "今日 $count 根")

        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SmokeWidgetProvider::class.java).setAction(ACTION_SMOKE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pi)

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SmokeWidgetProvider::class.java))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        const val ACTION_SMOKE = "com.smoketracker.app.action.WIDGET_SMOKE"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun startOfToday(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** 供 App 内数据变化后刷新小组件（如撤销、改记录）。 */
        fun refresh(context: Context) {
            val intent = Intent(context, SmokeWidgetProvider::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SmokeWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
