package io.chthonic.template_kotlin.data.model

/**
 * Created by jhavatar on 2/26/2017.
 */
data class TodoItem(
        val title: String,
        val isDone: Boolean = false
)