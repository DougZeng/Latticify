package io.github.tonnyl.latticify.data

import com.google.gson.annotations.SerializedName

/**
 * Created by lizhaotailang on 13/10/2017.
 *
 */
data class SearchAllWrapper(

        @SerializedName("ok")
        val ok: Boolean,

        @SerializedName("query")
        val query: String,

        @SerializedName("messages")
        val messages: SearchMessages,

        @SerializedName("files")
        val files: SearchFiles

)