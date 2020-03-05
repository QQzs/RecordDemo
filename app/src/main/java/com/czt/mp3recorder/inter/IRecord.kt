package com.czt.mp3recorder.inter


/**
 *  Created by fpc  2019-06-12  11:07
 */
interface IRecord {
    fun startRecord(listener: IRecordListener)

    fun startRecord(name: String, listener: IRecordListener,isNeedVolumeCallback:Boolean=true)

    fun stopRecord()

    fun isRecording(): Boolean
}