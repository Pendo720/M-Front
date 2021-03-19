package com.njm.mobile_front.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FacadeModelView : ViewModel() {

    private var mFacade: MutableLiveData<String> = MutableLiveData<String>()

    fun getFacade(): MutableLiveData<String> {
        return mFacade
    }
}