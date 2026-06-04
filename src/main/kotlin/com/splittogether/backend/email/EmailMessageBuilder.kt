package com.splittogether.backend.email

class EmailMessageBuilder {
    var to: String = ""
        private set
    var subject: String = ""
        private set
    var templateName: String = ""
        private set

    private val variables: MutableMap<String, String> = mutableMapOf()

    fun to(email: String) {
        to = email
    }

    fun subject(subject: String) {
        this.subject = subject
    }

    fun template(name: String) {
        templateName = name
    }

    fun variable(name: String, value: Any) {
        variables[name] = value.toString()
    }

    internal fun variables(): Map<String, String> = variables.toMap()
}
