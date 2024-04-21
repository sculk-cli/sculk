package tech.jamalam.url

import io.ktor.http.*
import io.ktor.util.*

public class UrlBuilder {
    private val parameters = mutableMapOf<String, String>()
    private val encodedParameters = mutableMapOf<String, String>()
    private var protocol: URLProtocol = URLProtocol.HTTPS
    private var host: String? = null
    private var port: Int? = null
    private var path: String = "/"

    public fun protocol(protocol: URLProtocol) {
        this.protocol = protocol
    }

    public fun host(host: String) {
        this.host = host
    }

    public fun port(port: Int) {
        this.port = port
    }

    public fun path(path: String) {
        this.path = path
    }

    public fun parameter(name: String, value: String) {
        parameters[name] = value
    }

    public fun encodedParameter(name: String, value: String) {
        encodedParameters[name] = value
    }

    public fun build(): Url {
        val builder = URLBuilder()
        builder.protocol = protocol
        host?.let { builder.host = it } ?: throw IllegalStateException("Host must be set")
        port?.let { builder.port = it }
        builder.encodedPath = path
        builder.parameters.appendAll(StringValues.build {
            parameters.forEach { (name, value) ->
                append(name, value)
            }
        })
        builder.encodedParameters.appendAll(StringValues.build {
            encodedParameters.forEach { (name, value) ->
                append(name, value)
            }
        })

        return builder.build()
    }
}

public fun buildUrl(initializer: UrlBuilder.() -> Unit): Url =
    UrlBuilder().apply(initializer).build()

