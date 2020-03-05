package com.zs.record

import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.czt.mp3recorder.engine.RecordEngine
import com.czt.mp3recorder.inter.IRecordListener
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var rxPermission = RxPermissions(this)
        rxPermission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.RECORD_AUDIO)
            .subscribe { granted ->
                if (!granted){
                    toast("请开启读写权限、录音权限")
                }
            }

        tv_start?.setOnClickListener {
            RecordEngine.instance.startRecord(object : IRecordListener{
                override fun onRecordStart() {
                    super.onRecordStart()
                    tv_path?.text = "record start"
                }

                override fun onRecordComplete(path: String?) {
                    super.onRecordComplete(path)
                    tv_path?.text = path
                }

                override fun onRecordError(error: String?) {
                    super.onRecordError(error)
                    tv_path?.text = "record error: $error"
                }
            })
        }

        tv_stop?.setOnClickListener {
            RecordEngine.instance.stopRecord()
        }

    }

}
