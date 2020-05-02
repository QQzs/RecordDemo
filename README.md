# RecordDemo
Android  lame库录制MP3格式音频

##
封装了一套使用AudioRecord录音，然后通过Lame库实时把录音文件已mp3文件写到本地

```Java
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

                override fun calculateVolume(db: Int?) {
                    super.calculateVolume(db)
                    if (db != null){
                        var d = db / 10f
                        Log.d("My_Log db = " , d.toString())
                    }
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
```
