package com.mashaffer.mymemory.models

import com.google.firebase.firestore.PropertyName

class UserImageList {

    @PropertyName("images") val images: List<String>? = null // NUllable bc of firebase rules due to default value


}