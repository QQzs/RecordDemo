package com.czt.mp3recorder.inter

/**
 *  Created by fpc  2019-06-12  11:09
 */
interface IRecordListener {

    fun calculateVolume(db: Int?){}

    fun onRecordComplete(path: String?){}

    fun onRecordError(error: String?){}

    fun onRecordStart(){}
}
